package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Moneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PatronLike;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Registro, consulta y anulación de pagos. Las escrituras comprueban el
 * permiso vigente del actor, convierten con una tasa autorizada y conservan
 * el recibo y las aplicaciones a cargos dentro de la misma transacción.
 */
public class PagoService {
    public static final int TAMANO_PAGINA_MAXIMO = 50;
    private static final int PAGINA_MAXIMA = 100_000;
    private static final Set<String> MONEDAS = Set.of("USD", "NIO");

    private final HibernateCrudService db;
    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public PagoService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public PagoService(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx, "Faltan las transacciones.");
        this.db = new HibernateCrudService(tx);
    }

    public List<Pago> listarTodos() {
        return listarTodos(1, TAMANO_PAGINA_MAXIMO, null).elementos();
    }

    public PaginaResponse<Pago> listarTodos(int pagina, int tamano, String texto) {
        validarPaginacion(pagina, tamano);
        String criterio = texto == null || texto.isBlank()
                ? null : PatronLike.contieneLiteral(texto.strip().toLowerCase(Locale.ROOT));
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            String filtro = criterio == null ? "" : """
                     where lower(p.referencia) like :texto escape '!'
                        or lower(p.metodo) like :texto escape '!'
                        or lower(i.nombre) like :texto escape '!'
                        or lower(prop.nombre) like :texto escape '!'
                        or lower(u.codigo) like :texto escape '!'
                        or lower(u.tipo) like :texto escape '!'
                    """;
            var conteo = session.createQuery("""
                    select count(p) from Pago p
                    join p.contrato c join c.inquilino i
                    join c.unidad u join u.propiedad prop
                    """ + filtro, Long.class);
            if (criterio != null) conteo.setParameter("texto", criterio);
            long total = conteo.getSingleResult();

            var consulta = session.createQuery("""
                    select p from Pago p
                    join fetch p.contrato c
                    left join fetch c.garantiaSeguimiento
                    join fetch c.inquilino i
                    join fetch c.unidad u
                    join fetch u.propiedad prop
                    left join fetch p.tipoCambio
                    """ + filtro + " order by p.fechaPago desc, p.id", Pago.class);
            if (criterio != null) consulta.setParameter("texto", criterio);
            List<Pago> pagos = consulta
                    .setFirstResult((pagina - 1) * tamano)
                    .setMaxResults(tamano)
                    .getResultList();
            return new PaginaResponse<>(pagina, tamano, total, pagos);
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

    public void registrarPago(Pago pago) throws SQLException {
        registrarPago(pago, List.of());
    }

    /**
     * Registra el pago y sus aplicaciones. El pago debe venir con los datos
     * originales; la conversión a la moneda del contrato la calcula este
     * Service. Devuelve el recibo y el crédito no aplicado.
     */
    public RegistroPago registrarPago(Pago pago, List<AplicacionPago> aplicaciones) {
        validarPago(pago);
        return tx.ejecutar(session -> registrarPagoEnSesion(session, pago, aplicaciones));
    }

    /**
     * Variante transaccional usada por la API para compartir la transacción
     * con el registro de idempotencia. No abre una transacción propia.
     */
    public RegistroPago registrarPagoEnSesion(Session session, Pago pago,
                                              List<AplicacionPago> aplicaciones) {
        Objects.requireNonNull(session, "Falta la sesión.");
        return registrarPagoEnSesion(session, pago, aplicaciones, reloj.comprobar(session));
    }

    public RegistroPago registrarPagoEnSesion(Session session, Pago pago,
                                              List<AplicacionPago> aplicaciones,
                                              RelojService.TiempoOperativo tiempo) {
        Objects.requireNonNull(session, "Falta la sesión.");
        Objects.requireNonNull(tiempo, "Falta la hora operativa.");
        validarPago(pago);
        Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                Permiso.OPERAR_ALQUILERES);

        Contrato contrato = bloquearContrato(session, pago.getContratoId());
        if (contrato == null) {
            throw new IllegalArgumentException("El contrato ya no existe.");
        }

        String monedaContrato = contrato.getMoneda();
        if (monedaContrato.equals(pago.getMonedaOriginal())) {
            pago.aplicarConversion(monedaContrato,
                    pago.getMontoOriginal().setScale(2, RoundingMode.HALF_EVEN), null);
        } else {
            TipoCambio tasa = session.createQuery("""
                            from TipoCambio t
                            where t.fecha = :fecha and t.autorizadoPor is not null
                            order by t.registradoEn desc
                            """, TipoCambio.class)
                    .setParameter("fecha", pago.getFechaPago())
                    .setMaxResults(1)
                    .uniqueResultOptional()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No hay una tasa autorizada para la fecha del pago."));
            pago.aplicarConversion(monedaContrato,
                    tasa.convertir(pago.getMontoOriginal(),
                            Moneda.desde(pago.getMonedaOriginal()),
                            Moneda.desde(monedaContrato), pago.getFechaPago()),
                    tasa);
        }
        pago.setEstado("Registrado");
        session.persist(pago);
        session.flush();

        long numeroRecibo = reservarNumeroRecibo(session);
        Recibo recibo = new Recibo(numeroRecibo, pago, contrato.toString());
        session.persist(recibo);
        BigDecimal credito = aplicarDistribucion(session, pago, aplicaciones);
        auditoria.registrar(session, actor, UUID.randomUUID(), "PAGO_REGISTRADO",
                "pago:" + pago.getId());
        return new RegistroPago(pago, recibo, credito);
    }

    /** Comprueba el reloj antes de crear una idempotencia para un pago nuevo. */
    public RelojService.TiempoOperativo comprobarReloj(Session session) {
        return reloj.comprobar(session);
    }

    public Optional<Pago> obtener(UUID id) {
        Objects.requireNonNull(id, "Falta el identificador del pago.");
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return Optional.ofNullable(session.createQuery("""
                            from Pago p
                            join fetch p.contrato c
                            join fetch c.unidad
                            join fetch c.inquilino
                            where p.id = :id
                            """, Pago.class)
                    .setParameter("id", id)
                    .uniqueResult());
        });
    }

    public Optional<Recibo> obtenerRecibo(long numero) {
        if (numero <= 0) throw new IllegalArgumentException("El número de recibo no es válido.");
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return Optional.ofNullable(session.createQuery("""
                            from Recibo r
                            join fetch r.pago p
                            join fetch p.contrato c
                            join fetch c.unidad
                            join fetch c.inquilino
                            where r.numero = :numero
                            """, Recibo.class)
                    .setParameter("numero", numero)
                    .uniqueResult());
        });
    }

    public Optional<Recibo> obtenerReciboDePago(UUID pagoId) {
        Objects.requireNonNull(pagoId, "Falta el identificador del pago.");
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return session.createQuery("""
                            from Recibo r
                            join fetch r.pago p
                            where p.id = :pagoId
                            """, Recibo.class)
                    .setParameter("pagoId", pagoId)
                    .uniqueResultOptional();
        });
    }

    public List<AplicacionPago> listarAplicaciones(UUID pagoId) {
        Objects.requireNonNull(pagoId, "Falta el identificador del pago.");
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return session.createQuery("""
                            from AplicacionPago a
                            where a.pagoId = :pagoId
                            order by a.id
                            """, AplicacionPago.class)
                    .setParameter("pagoId", pagoId)
                    .getResultList();
        });
    }

    public void anular(UUID id) throws SQLException {
        if (anular(id, "Anulación manual desde la interfaz").isEmpty()) {
            throw new IllegalArgumentException("El registro ya no existe.");
        }
    }

    /**
     * Anula un pago. Solo el propietario puede hacerlo; el motivo queda
     * guardado y la acción se audita en la misma transacción.
     */
    public Optional<Pago> anular(UUID id, String motivo) {
        Objects.requireNonNull(id, "Falta el identificador del pago.");
        String limpio = textoRequerido(motivo, "motivo de anulación", 500);
        return tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.ANULAR_PAGOS);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            Pago pago = session.find(Pago.class, id);
            if (pago == null) return Optional.<Pago>empty();
            if ("Anulado".equals(pago.getEstado())) {
                throw new ConflictoDatosException("El pago ya está anulado.");
            }
            pago.anular(limpio, actor.getId(), tiempo.instante());
            auditoria.registrar(session, actor, UUID.randomUUID(), "PAGO_ANULADO",
                    "pago:" + pago.getId());
            return Optional.of(pago);
        });
    }

    public void eliminar(UUID id) throws SQLException {
        db.eliminar(Pago.class, id, Permiso.GESTIONAR_USUARIOS);
    }

    private BigDecimal aplicarDistribucion(Session session, Pago pago,
                                           List<AplicacionPago> aplicaciones) {
        BigDecimal montoContrato = pago.getMontoContrato()
                .setScale(2, RoundingMode.HALF_EVEN);
        if (aplicaciones == null || aplicaciones.isEmpty()) {
            return montoContrato;
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<UUID> cargosVistos = new HashSet<>();
        List<UUID> cargosSolicitados = new ArrayList<>();
        for (AplicacionPago aplicacion : aplicaciones) {
            if (aplicacion == null) {
                throw new IllegalArgumentException("La distribución contiene una fila vacía.");
            }
            UUID cargoId = aplicacion.getCargoId();
            if (!cargosVistos.add(cargoId)) {
                throw new IllegalArgumentException(
                        "La distribución repite el mismo cargo.");
            }
            cargosSolicitados.add(cargoId);
        }

        List<UUID> cargosOrdenados = session.createQuery("""
                        select c.id from Cargo c
                        where c.id in :ids and c.contratoId = :contratoId
                        order by c.venceEl, c.id
                        """, UUID.class)
                .setParameter("ids", cargosSolicitados)
                .setParameter("contratoId", pago.getContratoId())
                .getResultList();
        if (cargosOrdenados.size() != cargosSolicitados.size()) {
            throw new IllegalArgumentException(
                    "Uno de los cargos no existe o no pertenece al contrato del pago.");
        }

        Map<UUID, Cargo> cargosBloqueados = new HashMap<>();
        for (UUID cargoId : cargosOrdenados) {
            Cargo cargo = bloquearCargo(session, cargoId);
            if (cargo == null) {
                throw new IllegalArgumentException("Uno de los cargos ya no existe.");
            }
            cargosBloqueados.put(cargoId, cargo);
        }

        for (AplicacionPago aplicacion : aplicaciones) {
            UUID cargoId = aplicacion.getCargoId();
            Cargo cargo = cargosBloqueados.get(cargoId);
            if (!pago.getContratoId().equals(cargo.getContratoId())) {
                throw new IllegalArgumentException(
                        "Todos los cargos deben pertenecer al contrato del pago.");
            }
            if ("Cancelado".equals(cargo.getEstado())) {
                throw new IllegalArgumentException(
                        "No se puede aplicar un pago a un cargo cancelado.");
            }

            BigDecimal yaAplicado = session.createQuery("""
                            select sum(ap.monto) from AplicacionPago ap, Pago p
                            where ap.pagoId = p.id and ap.cargoId = :cargoId
                              and p.estado <> 'Anulado'
                            """, BigDecimal.class)
                    .setParameter("cargoId", cargoId)
                    .getSingleResult();
            if (yaAplicado == null) yaAplicado = BigDecimal.ZERO;
            BigDecimal importe = cargo.getImporte() == null
                    ? BigDecimal.ZERO : cargo.getImporte();
            if (yaAplicado.add(aplicacion.getMonto()).compareTo(importe) > 0) {
                throw new IllegalArgumentException(
                        "La distribución supera el saldo del cargo seleccionado.");
            }

            total = total.add(aplicacion.getMonto());
            aplicacion.setPagoId(pago.getId());
            session.persist(aplicacion);
        }

        if (total.compareTo(montoContrato) > 0) {
            throw new IllegalArgumentException(
                    "La distribución supera el monto del pago en la moneda del contrato.");
        }
        return montoContrato.subtract(total).setScale(2, RoundingMode.HALF_EVEN);
    }

    private Contrato bloquearContrato(Session session, UUID contratoId) {
        Object filaBloqueada = session.createNativeQuery(
                        "select id from contrato where id = :id for update")
                .setParameter("id", contratoId)
                .uniqueResult();
        return filaBloqueada == null ? null : session.find(Contrato.class, contratoId);
    }

    private long reservarNumeroRecibo(Session session) {
        String producto = session.doReturningWork(
                conexion -> conexion.getMetaData().getDatabaseProductName());
        String consulta;
        if ("H2".equalsIgnoreCase(producto)) {
            // H2 se usa para pruebas rápidas; producción reserva con la función transaccional de PostgreSQL.
            consulta = "select coalesce(max(numero), 0) + 1 from recibo";
        } else if ("PostgreSQL".equalsIgnoreCase(producto)) {
            consulta = "select public.fn_reservar_numero()";
        } else {
            throw new IllegalStateException("La numeración de recibos requiere PostgreSQL.");
        }
        return session.createNativeQuery(consulta, Long.class).getSingleResult();
    }

    private Cargo bloquearCargo(Session session, UUID cargoId) {
        Object filaBloqueada = session.createNativeQuery(
                        "select id from cargo where id = :id for update")
                .setParameter("id", cargoId)
                .uniqueResult();
        return filaBloqueada == null ? null : session.find(Cargo.class, cargoId);
    }

    private static void validarPago(Pago pago) {
        if (pago == null || pago.getContratoId() == null) {
            throw new IllegalArgumentException("Seleccione un contrato.");
        }
        if (pago.getFechaPago() == null) {
            throw new IllegalArgumentException("La fecha del pago es obligatoria.");
        }
        String moneda = pago.getMonedaOriginal();
        if (moneda == null || !MONEDAS.contains(moneda)) {
            throw new IllegalArgumentException("La moneda debe ser USD o NIO.");
        }
        BigDecimal monto = pago.getMontoOriginal();
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor que cero.");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El monto admite como máximo 2 decimales.");
        }
        pago.setMontoOriginal(monto.setScale(2, RoundingMode.HALF_EVEN));

        String metodo = textoRequerido(pago.getMetodo(), "método de pago", 24);
        pago.setMetodo(metodo);
        String referencia = textoOpcional(pago.getReferencia(), "referencia", 100);
        pago.setReferencia(referencia);
        String notas = textoOpcional(pago.getNotas(), "notas", 255);
        pago.setNotas(notas);
    }

    private static String textoRequerido(String valor, String campo, int maximo) {
        String limpio = textoOpcional(valor, campo, maximo);
        if (limpio == null) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return limpio;
    }

    private static String textoOpcional(String valor, String campo, int maximo) {
        if (valor == null) return null;
        String limpio = valor.strip();
        if (limpio.isEmpty()) return null;
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }

    public record RegistroPago(Pago pago, Recibo recibo, BigDecimal credito) { }
}
