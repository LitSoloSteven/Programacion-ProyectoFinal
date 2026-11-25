package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PatronLike;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CargoService {
    public static final int TAMANO_PAGINA_MAXIMO = 50;
    private static final int PAGINA_MAXIMA = 100_000;
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("1000000000000");
    private static final Set<String> ESTADOS = Set.of("Pendiente", "Pagado", "Cancelado");

    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public CargoService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public CargoService(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx);
    }

    public List<Cargo> listarTodos() {
        return listarTodos(1, TAMANO_PAGINA_MAXIMO, null).elementos();
    }

    public PaginaResponse<Cargo> listarTodos(int pagina, int tamano, String texto) {
        validarPaginacion(pagina, tamano);
        String criterio = texto == null || texto.isBlank()
                ? null : PatronLike.contieneLiteral(texto.strip().toLowerCase(Locale.ROOT));
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            String filtro = criterio == null ? "" : """
                     where lower(c.concepto) like :texto escape '!'
                        or lower(c.tipo) like :texto escape '!'
                        or lower(i.nombre) like :texto escape '!'
                        or lower(prop.nombre) like :texto escape '!'
                        or lower(u.codigo) like :texto escape '!'
                        or lower(u.tipo) like :texto escape '!'
                    """;
            var conteo = session.createQuery("""
                    select count(c) from Cargo c
                    join c.contrato con join con.inquilino i
                    join con.unidad u join u.propiedad prop
                    """ + filtro, Long.class);
            if (criterio != null) conteo.setParameter("texto", criterio);
            long total = conteo.getSingleResult();

            var consulta = session.createQuery("""
                    select c from Cargo c
                    join fetch c.contrato con
                    left join fetch con.garantiaSeguimiento
                    join fetch con.inquilino i
                    join fetch con.unidad u
                    join fetch u.propiedad prop
                    """ + filtro + " order by c.periodo desc, c.id", Cargo.class);
            if (criterio != null) consulta.setParameter("texto", criterio);
            List<Cargo> cargos = consulta
                    .setFirstResult((pagina - 1) * tamano)
                    .setMaxResults(tamano)
                    .getResultList();
            if (cargos.isEmpty()) return new PaginaResponse<>(pagina, tamano, total, cargos);

            List<UUID> ids = cargos.stream().map(Cargo::getId).toList();
            Map<UUID, BigDecimal> aplicadoPorCargo = new HashMap<>();
            for (Object[] fila : session.createQuery("""
                            select a.cargoId, sum(a.monto)
                            from AplicacionPago a left join Pago p on a.pagoId = p.id
                            where a.cargoId in :ids
                              and a.revertidaEn is null
                              and (a.pagoId is null or p.estado <> :anulado)
                            group by a.cargoId
                            """, Object[].class)
                    .setParameter("ids", ids)
                    .setParameter("anulado", "Anulado")
                    .getResultList()) {
                aplicadoPorCargo.put((UUID) fila[0], (BigDecimal) fila[1]);
            }

            LocalDate hoy = reloj.leer(session).fecha();
            for (Cargo cargo : cargos) {
                cargo.establecerEstadoDerivado(aplicadoPorCargo.get(cargo.getId()), hoy);
            }
            return new PaginaResponse<>(pagina, tamano, total, cargos);
        });
    }

    public Cargo obtenerParaEdicion(UUID id) {
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Cargo cargo = session.find(Cargo.class, id);
            if (cargo == null) throw new IllegalArgumentException("El cargo ya no existe.");
            return cargo;
        });
    }

    private static void validarPaginacion(int pagina, int tamano) {
        if (pagina < 1 || pagina > PAGINA_MAXIMA) {
            throw new IllegalArgumentException("La página debe estar entre 1 y " + PAGINA_MAXIMA + ".");
        }
        if (tamano < 1 || tamano > TAMANO_PAGINA_MAXIMO) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y "
                    + TAMANO_PAGINA_MAXIMO + ".");
        }
    }

    public UUID guardar(Solicitud solicitud) throws SQLException {
        validar(solicitud);
        return tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.OPERAR_ALQUILERES);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            Cargo cargo;
            String accion;
            if (solicitud.id() == null) {
                if ("Mora".equalsIgnoreCase(solicitud.tipo().strip())) {
                    throw new IllegalArgumentException(
                            "Los cargos de Mora se generan automáticamente tras vencer el plazo de gracia.");
                }
                if (session.createNativeQuery("select id from contrato where id = :id for update")
                        .setParameter("id", solicitud.contratoId()).uniqueResult() == null) {
                    throw new IllegalArgumentException("El contrato seleccionado ya no existe.");
                }
                cargo = new Cargo();
                aplicar(cargo, solicitud);
                cargo.setCreadoPor(actor.getId());
                if ("Cancelado".equals(solicitud.estado())) {
                    permisos.exigir(session, SesionUsuario.getActual(), Permiso.ANULAR_CARGOS);
                    cargo.anular("Registrado como anulado", actor.getId(), tiempo.instante());
                }
                session.persist(cargo);
                session.flush();
                if (!"Cancelado".equals(cargo.getEstadoPersistido())) {
                    CobranzaService.aplicarCreditoDisponibleEnSesion(session, cargo, actor.getId(), tiempo);
                }
                accion = "CARGO_CREADO";
            } else {
                cargo = session.find(Cargo.class, solicitud.id());
                if (cargo == null) throw new IllegalArgumentException("El cargo ya no existe.");
                if (cargo.getVersion() != solicitud.version()) {
                    throw new ConflictoDatosException(
                            "Otro usuario modificó el cargo. Actualiza los datos antes de reintentar.");
                }
                if (tieneAplicacionesActivas(session, cargo.getId())
                        && (cargo.getImporte().compareTo(solicitud.importe()) != 0
                        || !Objects.equals(cargo.getEstadoPersistido(), solicitud.estado())
                        || !Objects.equals(cargo.getContratoId(), solicitud.contratoId()))) {
                    throw new IllegalStateException(
                            "No se puede cambiar el importe, estado o contrato de un cargo con pagos aplicados.");
                }
                if (!Objects.equals(cargo.getEstadoPersistido(), solicitud.estado())
                        && "Cancelado".equals(solicitud.estado())) {
                    permisos.exigir(session, SesionUsuario.getActual(), Permiso.ANULAR_CARGOS);
                }
                aplicar(cargo, solicitud);
                accion = "CARGO_ACTUALIZADO";
            }
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), accion,
                    "cargo:" + cargo.getId());
            return cargo.getId();
        });
    }

    public void anular(UUID id) throws SQLException {
        anular(id, "Anulación manual desde la interfaz");
    }

    public void anular(UUID id, String motivo) throws SQLException {
        Objects.requireNonNull(id, "Falta el identificador del cargo.");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de anulación es obligatorio.");
        }
        String limpio = motivo.strip();
        if (limpio.length() > 500) {
            throw new IllegalArgumentException("El motivo de anulación no puede superar 500 caracteres.");
        }
        tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.ANULAR_CARGOS);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            Object fila = session.createNativeQuery("select id from cargo where id = :id for update")
                    .setParameter("id", id)
                    .uniqueResult();
            Cargo cargo = fila == null ? null : session.find(Cargo.class, id);
            if (cargo == null) throw new IllegalArgumentException("El cargo ya no existe.");
            if ("Cancelado".equals(cargo.getEstadoPersistido())
                    || "Anulado".equals(cargo.getEstadoPersistido())) {
                throw new ConflictoDatosException("El cargo ya está anulado.");
            }
            if (tieneAplicacionesActivas(session, id)) {
                throw new IllegalStateException(
                        "No se puede anular un cargo con pagos aplicados.");
            }
            cargo.anular(limpio, actor.getId(), tiempo.instante());
            auditoria.registrar(session, actor, UUID.randomUUID(), "CARGO_ANULADO",
                    "cargo:" + id);
            return null;
        });
    }

    private static void validar(Solicitud solicitud) {
        if (solicitud == null || solicitud.contratoId() == null
                || solicitud.importe() == null || solicitud.tipo() == null
                || solicitud.periodo() == null || solicitud.fechaEmision() == null
                || solicitud.venceEl() == null || solicitud.concepto() == null
                || solicitud.estado() == null) {
            throw new IllegalArgumentException("Complete todos los campos del cargo.");
        }
        if (solicitud.importe().signum() <= 0
                || solicitud.importe().abs().compareTo(MONTO_MAXIMO) > 0
                || solicitud.importe().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El importe debe ser positivo, válido y no superar 1e12.");
        }
        if (solicitud.venceEl().isBefore(solicitud.fechaEmision())) {
            throw new IllegalArgumentException("El vencimiento no puede preceder a la emisión.");
        }
        if (!ESTADOS.contains(solicitud.estado())) {
            throw new IllegalArgumentException("El estado del cargo no es válido.");
        }
    }

    private static void aplicar(Cargo cargo, Solicitud solicitud) {
        cargo.setContratoId(solicitud.contratoId());
        cargo.setTipo(solicitud.tipo());
        cargo.setPeriodo(solicitud.periodo());
        cargo.setFechaEmision(solicitud.fechaEmision());
        cargo.setVenceEl(solicitud.venceEl());
        cargo.setImporte(solicitud.importe().setScale(2));
        cargo.setConcepto(solicitud.concepto().strip());
        cargo.setEstado(solicitud.estado());
    }

    private static boolean tieneAplicacionesActivas(Session session, UUID cargoId) {
        Long total = session.createQuery("""
                        select count(a) from AplicacionPago a left join Pago p on a.pagoId = p.id
                        where a.cargoId = :cargoId
                          and a.revertidaEn is null
                          and (a.pagoId is null or p.estado <> :anulado)
                        """, Long.class)
                .setParameter("cargoId", cargoId)
                .setParameter("anulado", "Anulado")
                .getSingleResult();
        return total > 0;
    }

    public record Solicitud(UUID id, long version, UUID contratoId, String tipo,
                            LocalDate periodo, LocalDate fechaEmision, LocalDate venceEl,
                            BigDecimal importe, String concepto, String estado) { }
}
