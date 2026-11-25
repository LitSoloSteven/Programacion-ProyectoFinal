package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class UnidadService {
    private static final Set<String> ESTADOS_VALIDOS = Set.of("Disponible", "En mantenimiento", "Inactiva");

    private final HibernateCrudService db;
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();

    public UnidadService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public UnidadService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
        this.db = new HibernateCrudService(this.transactions);
    }

    public List<Unidad> listarTodas() throws SQLException {
        return db.listar(Unidad.class, "from Unidad order by codigo");
    }

    public List<Unidad> listarDisponibles() throws SQLException {
        return db.listar(Unidad.class, "from Unidad where estado='Disponible' order by codigo");
    }

    public void guardar(Unidad u) throws SQLException {
        if (u == null || u.getPropiedadId() == null || u.getCodigo() == null || u.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Seleccione propiedad e ingrese el código.");
        }
        if (u.getDescripcion() != null && u.getDescripcion().length() > 255) {
            throw new IllegalArgumentException("La descripción no puede superar 255 caracteres.");
        }
        if (u.getId() != null) {
            throw new IllegalArgumentException(
                    "No se guardan entidades desconectadas; actualice la entidad administrada.");
        }
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.GESTIONAR_UNIDADES);
            Propiedad propiedad = session.find(Propiedad.class, u.getPropiedadId());
            if (propiedad == null || !propiedad.isActiva()) {
                throw new IllegalArgumentException("La propiedad seleccionada no está activa.");
            }
            session.persist(u);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), "CREAR_UNIDAD", "unidad:" + u.getId());
            return null;
        });
    }

    public void actualizar(UUID id, long version, UUID propiedadId, String codigo, String descripcion,
                           String tipo, String estado, BigDecimal canon, String moneda) throws SQLException {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Unidad u = session.find(Unidad.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (u == null) throw new IllegalArgumentException("El registro ya no existe.");
            boolean propietario = Rol.PROPIETARIO.equals(actor.getRol());
            if (!propietario
                    && (!Objects.equals(u.getPropiedadId(), propiedadId)
                    || !Objects.equals(sinEspacios(u.getCodigo()), sinEspacios(codigo))
                    || !Objects.equals(u.getTipo(), tipo)
                    || !Objects.equals(u.getEstado(), estado))) {
                throw new SecurityException(
                        "El Asistente solo puede editar el canon de referencia, la moneda y la descripción de una unidad.");
            }
            if (u.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            if (propietario) {
                if (!Objects.equals(u.getEstado(), estado)) {
                    exigirSinContratoActivo(session, id);
                }
                u.setPropiedadId(propiedadId);
                u.setCodigo(codigo);
                u.setTipo(tipo);
                u.setEstado(estado);
            }
            u.setDescripcion(descripcion);
            u.setCanonReferencia(canon);
            u.setMoneda(moneda);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), "ACTUALIZAR_UNIDAD", "unidad:" + id);
            return null;
        });
    }

    private static String sinEspacios(String codigo) {
        return codigo == null ? null : codigo.strip();
    }

    public void cambiarEstado(UUID id, String estado) throws SQLException {
        if (estado == null || !ESTADOS_VALIDOS.contains(estado)) {
            throw new IllegalArgumentException("El estado debe ser Disponible, En mantenimiento o Inactiva.");
        }
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.GESTIONAR_UNIDADES);
            Unidad u = session.find(Unidad.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (u == null) throw new IllegalArgumentException("El registro ya no existe.");
            if (!Objects.equals(u.getEstado(), estado)) {
                exigirSinContratoActivo(session, id);
            }
            u.setEstado(estado);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "CAMBIAR_ESTADO_UNIDAD", "unidad:" + id + ";estado:" + estado);
            return null;
        });
    }

    public void eliminar(UUID id) throws SQLException {
        db.eliminar(Unidad.class, id, Permiso.GESTIONAR_UNIDADES);
    }

    private static void exigirSinContratoActivo(Session session, UUID unidadId) {
        Long contratosActivos = session.createQuery(
                        "select count(c) from Contrato c where c.unidadId = :unidadId"
                                + " and c.estado in ('Vigente', 'Por vencer', 'Vencido')",
                        Long.class)
                .setParameter("unidadId", unidadId)
                .getSingleResult();
        if (contratosActivos > 0) {
            throw new ConflictoDatosException(
                    "No se puede cambiar el estado de una unidad con contrato activo.");
        }
    }
}
