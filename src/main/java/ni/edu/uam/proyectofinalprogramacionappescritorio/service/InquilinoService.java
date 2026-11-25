package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.HistorialInquilinoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class InquilinoService {
    private final HibernateCrudService db;
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();

    public InquilinoService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public InquilinoService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
        this.db = new HibernateCrudService(this.transactions);
    }

    public List<Inquilino> listarTodos() throws SQLException {
        return db.listar(Inquilino.class, "from Inquilino order by nombre");
    }

    public List<Inquilino> listarActivos() throws SQLException {
        return db.listar(Inquilino.class, "from Inquilino where activo=true order by nombre");
    }

    public void guardar(Inquilino i) throws SQLException {
        if (i == null || i.getNombre() == null || i.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (i.getId() != null) {
            throw new IllegalArgumentException(
                    "No se guardan entidades desconectadas; actualice la entidad administrada.");
        }
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            if (!i.isActivo() && !Rol.PROPIETARIO.equals(actor.getRol())) {
                throw new SecurityException("Solo el Propietario puede desactivar inquilinos.");
            }
            session.persist(i);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), "CREAR_INQUILINO", "inquilino:" + i.getId());
            return null;
        });
    }

    public void actualizar(UUID id, long version, String nombre, String identificacion, String telefono,
                           String correo, String contacto, boolean activo) throws SQLException {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Inquilino i = session.find(Inquilino.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (i == null) throw new IllegalArgumentException("El registro ya no existe.");
            if (i.isActivo() != activo && !Rol.PROPIETARIO.equals(actor.getRol())) {
                throw new SecurityException("Solo el Propietario puede desactivar o reactivar inquilinos.");
            }
            if (i.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            if (i.isActivo() != activo && !activo) {
                exigirSinContratoActivo(session, id);
            }
            i.setNombre(nombre);
            i.setIdentificacion(identificacion);
            i.setTelefono(telefono);
            i.setCorreo(correo);
            i.setContactoEmergencia(contacto);
            i.setActivo(activo);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), "ACTUALIZAR_INQUILINO", "inquilino:" + id);
            return null;
        });
    }

    public void cambiarEstado(UUID id, boolean activo) throws SQLException {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.DESACTIVAR_INQUILINO);
            Inquilino i = session.find(Inquilino.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (i == null) throw new IllegalArgumentException("El registro ya no existe.");
            if (!activo) {
                exigirSinContratoActivo(session, id);
            }
            i.setActivo(activo);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    activo ? "ACTIVAR_INQUILINO" : "DESACTIVAR_INQUILINO",
                    "inquilino:" + id);
            return null;
        });
    }

    public void darDeBaja(UUID id) throws SQLException {
        cambiarEstado(id, false);
    }

    public void eliminar(UUID id) throws SQLException {
        db.eliminar(Inquilino.class, id, Permiso.DESACTIVAR_INQUILINO);
    }

    public List<Inquilino> buscarCoincidenciasIdentificacion(String identificacion, UUID excluirId) {
        if (identificacion == null || identificacion.isBlank()) return List.of();
        String normalizada = identificacion.strip().toLowerCase(Locale.ROOT);
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            String hql = excluirId == null
                    ? "from Inquilino i where lower(trim(i.identificacion)) = :ident order by i.nombre"
                    : "from Inquilino i where lower(trim(i.identificacion)) = :ident and i.id <> :excluir order by i.nombre";
            var query = session.createQuery(hql, Inquilino.class).setParameter("ident", normalizada);
            if (excluirId != null) query.setParameter("excluir", excluirId);
            return query.getResultList();
        });
    }

    public HistorialInquilinoDTO consultarHistorial(UUID inquilinoId) {
        Objects.requireNonNull(inquilinoId, "Falta el identificador del inquilino.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Inquilino inquilino = session.find(Inquilino.class, inquilinoId);
            if (inquilino == null) throw new IllegalArgumentException("El inquilino no existe.");

            List<Contrato> contratos = session.createQuery("""
                            select c from Contrato c
                            join fetch c.unidad u
                            left join fetch u.propiedad
                            where c.inquilinoId = :inquilinoId
                            order by c.inicio desc
                            """, Contrato.class)
                    .setParameter("inquilinoId", inquilinoId)
                    .getResultList();

            if (contratos.isEmpty()) {
                return new HistorialInquilinoDTO(
                        inquilino.getId(), inquilino.getNombre(), inquilino.getIdentificacion(),
                        inquilino.isActivo(), List.of(), List.of(), List.of());
            }

            List<UUID> contratoIds = contratos.stream().map(Contrato::getId).toList();
            Map<UUID, String> monedasPorContrato = new HashMap<>();
            for (Contrato c : contratos) {
                monedasPorContrato.put(c.getId(), c.getMoneda());
            }

            List<Cargo> cargos = session.createQuery("""
                            from Cargo cg
                            where cg.contratoId in (:contratoIds)
                            order by cg.venceEl desc, cg.periodo desc
                            """, Cargo.class)
                    .setParameter("contratoIds", contratoIds)
                    .getResultList();

            List<Pago> pagos = session.createQuery("""
                            from Pago p
                            where p.contratoId in (:contratoIds)
                            order by p.fechaPago desc
                            """, Pago.class)
                    .setParameter("contratoIds", contratoIds)
                    .getResultList();

            Map<UUID, Long> recibosPorPago = new HashMap<>();
            if (!pagos.isEmpty()) {
                List<UUID> pagoIds = pagos.stream().map(Pago::getId).toList();
                List<Recibo> recibos = session.createQuery("""
                                from Recibo r
                                where r.pago.id in (:pagoIds)
                                """, Recibo.class)
                        .setParameter("pagoIds", pagoIds)
                        .getResultList();
                for (Recibo r : recibos) {
                    recibosPorPago.put(r.getPago().getId(), r.getNumero());
                }
            }

            List<HistorialInquilinoDTO.ContratoResumen> contratosDto = contratos.stream()
                    .map(c -> new HistorialInquilinoDTO.ContratoResumen(
                            c.getId(), c.getUnidadInfo(), c.getInicio(), c.getFin(),
                            c.getCanon(), c.getMoneda(), c.getEstado()))
                    .toList();

            List<HistorialInquilinoDTO.CargoResumen> cargosDto = cargos.stream()
                    .map(cg -> new HistorialInquilinoDTO.CargoResumen(
                            cg.getId(), cg.getContratoId(), cg.getTipo(), cg.getPeriodo(),
                            cg.getVenceEl(), cg.getImporte(),
                            monedasPorContrato.getOrDefault(cg.getContratoId(), "USD"),
                            cg.getEstado()))
                    .toList();

            List<HistorialInquilinoDTO.PagoResumen> pagosDto = pagos.stream()
                    .map(p -> new HistorialInquilinoDTO.PagoResumen(
                            p.getId(), p.getContratoId(), p.getFechaPago(),
                            p.getMontoOriginal(), p.getMonedaOriginal(),
                            p.getMontoContrato(), p.getMonedaContrato(),
                            p.getMetodo(), p.getEstado(), recibosPorPago.get(p.getId())))
                    .toList();

            return new HistorialInquilinoDTO(
                    inquilino.getId(), inquilino.getNombre(), inquilino.getIdentificacion(),
                    inquilino.isActivo(), contratosDto, cargosDto, pagosDto);
        });
    }

    private static void exigirSinContratoActivo(Session session, UUID inquilinoId) {
        Long contratosActivos = session.createQuery(
                        "select count(c) from Contrato c where c.inquilinoId = :inquilinoId"
                                + " and c.estado in ('Vigente', 'Por vencer', 'Vencido')",
                        Long.class)
                .setParameter("inquilinoId", inquilinoId)
                .getSingleResult();
        if (contratosActivos > 0) {
            throw new ConflictoDatosException(
                    "No se puede desactivar el inquilino porque tiene un contrato activo.");
        }
    }
}