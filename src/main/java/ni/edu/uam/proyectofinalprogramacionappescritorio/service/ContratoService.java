package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ContratoService {
    private static final List<String> ESTADOS_QUE_RESERVAN_UNIDAD =
            List.of("Vigente", "Por vencer", "Vencido");

    private final HibernateTransactions tx;
    private final HibernateCrudService db;
    private final PermisoService permisos = new PermisoService();

    public ContratoService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public ContratoService(HibernateTransactions transactions) {
        this.tx = Objects.requireNonNull(transactions);
        this.db = new HibernateCrudService(transactions);
    }

    public List<Contrato> listarTodos() throws SQLException {
        return db.listar(Contrato.class,
                "from Contrato c join fetch c.unidad join fetch c.inquilino "
                        + "left join fetch c.garantiaSeguimiento order by c.inicio desc");
    }

    public List<Contrato> listarVigentes() throws SQLException {
        return db.listar(Contrato.class,
                "from Contrato c join fetch c.unidad join fetch c.inquilino "
                        + "left join fetch c.garantiaSeguimiento where c.estado='Vigente' "
                        + "order by c.inicio desc");
    }

    public void guardar(Contrato contrato) throws SQLException {
        if (contrato == null) {
            throw new IllegalArgumentException("El contrato es obligatorio.");
        }
        validarDatos(contrato.getUnidadId(), contrato.getInquilinoId(), contrato.getInicio(),
                contrato.getFin(), contrato.getEstado());
        if (contrato.getId() != null) {
            throw new IllegalArgumentException("No se guardan contratos existentes como nuevos.");
        }
        if ("Terminado".equals(contrato.getEstado()) || "Cancelado".equals(contrato.getEstado())) {
            throw new IllegalArgumentException("Un contrato nuevo debe guardarse como Borrador o activarse.");
        }
        validarFechaActivacion(contrato.getInicio(), contrato.getEstado());
        contrato.inicializarGarantiaSeguimiento();

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            if (Contrato.reservaUnidad(contrato.getEstado())) {
                bloquearUnidades(session, List.of(contrato.getUnidadId()));
                validarDisponibilidad(session, contrato.getUnidadId(), null);
            }
            session.persist(contrato);
            return null;
        });
    }

    public void actualizar(UUID id, long version, UUID unidadId, UUID inquilinoId,
                          LocalDate inicio, LocalDate fin, BigDecimal canon, String moneda,
                          int diaPago, BigDecimal deposito, String estado) throws SQLException {
        validarDatos(unidadId, inquilinoId, inicio, fin, estado);
        validarFechaActivacion(inicio, estado);

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El registro ya no existe.");
            if (contrato.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            contrato.validarTransicionA(estado);

            boolean reservaAntes = Contrato.reservaUnidad(contrato.getEstado());
            boolean reservaDespues = Contrato.reservaUnidad(estado);
            if (reservaAntes || reservaDespues) {
                bloquearUnidades(session, List.of(contrato.getUnidadId(), unidadId));
            }
            if (reservaDespues) validarDisponibilidad(session, unidadId, id);

            if (cambiaronTerminosFinancieros(contrato, moneda, canon, deposito)
                    && tieneCargosOPagos(session, id)) {
                throw new IllegalStateException(
                        "No se pueden cambiar moneda, canon ni depósito después de registrar cargos o pagos.");
            }

            contrato.setUnidadId(unidadId);
            contrato.setInquilinoId(inquilinoId);
            contrato.setInicio(inicio);
            contrato.setFin(fin);
            contrato.setCanon(canon);
            contrato.setMoneda(moneda);
            contrato.setDiaPago(diaPago);
            contrato.setDepositoPactado(deposito);
            contrato.transicionarA(estado, LocalDate.now());
            return null;
        });
    }

    public void cambiarEstado(UUID id, String estado) throws SQLException {
        if (!Contrato.esEstadoValido(estado)) {
            throw new IllegalArgumentException("El estado del contrato no es válido.");
        }
        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El registro ya no existe.");
            contrato.validarTransicionA(estado);
            validarFechaActivacion(contrato.getInicio(), estado);

            boolean reservaAntes = Contrato.reservaUnidad(contrato.getEstado());
            boolean reservaDespues = Contrato.reservaUnidad(estado);
            if (reservaAntes || reservaDespues) {
                bloquearUnidades(session, List.of(contrato.getUnidadId()));
            }
            if (reservaDespues) validarDisponibilidad(session, contrato.getUnidadId(), id);

            contrato.transicionarA(estado, LocalDate.now());
            return null;
        });
    }

    public void terminarContrato(UUID id) throws SQLException {
        cambiarEstado(id, "Terminado");
    }

    public void eliminar(UUID id) throws SQLException {
        db.eliminar(Contrato.class, id, Permiso.GESTIONAR_USUARIOS);
    }

    private static void validarDatos(UUID unidadId, UUID inquilinoId, LocalDate inicio,
                                     LocalDate fin, String estado) {
        if (unidadId == null || inquilinoId == null || inicio == null || fin == null
                || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("Verifique unidad, inquilino y fechas del contrato.");
        }
        if (!Contrato.esEstadoValido(estado)) {
            throw new IllegalArgumentException("El estado del contrato no es válido.");
        }
    }

    private static void validarFechaActivacion(LocalDate inicio, String estado) {
        if (Contrato.reservaUnidad(estado) && inicio.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Un contrato con inicio futuro debe permanecer como Borrador.");
        }
    }

    private static void bloquearUnidades(Session session, Collection<UUID> ids) {
        ids.stream().filter(Objects::nonNull).distinct().sorted().forEach(id -> {
            if (session.find(Unidad.class, id, LockModeType.PESSIMISTIC_WRITE) == null) {
                throw new IllegalArgumentException("La unidad seleccionada ya no existe.");
            }
        });
    }

    private static Contrato buscarContratoBloqueado(Session session, UUID id) {
        List<UUID> encontrados = session.createNativeQuery(
                        "select id from contrato where id = :id for update", UUID.class)
                .setParameter("id", id)
                .getResultList();
        return encontrados.isEmpty() ? null : session.find(Contrato.class, id);
    }

    private static void validarDisponibilidad(Session session, UUID unidadId, UUID contratoId) {
        var consulta = session.createQuery(
                "select count(c) from Contrato c where c.unidadId = :unidadId "
                        + "and c.estado in :estados"
                        + (contratoId == null ? "" : " and c.id <> :contratoId"), Long.class)
                .setParameter("unidadId", unidadId)
                .setParameter("estados", ESTADOS_QUE_RESERVAN_UNIDAD);
        if (contratoId != null) consulta.setParameter("contratoId", contratoId);
        if (consulta.getSingleResult() > 0) {
            throw new IllegalArgumentException("La unidad ya tiene un contrato activo.");
        }
    }

    private static boolean cambiaronTerminosFinancieros(Contrato contrato, String moneda,
                                                        BigDecimal canon, BigDecimal deposito) {
        return !Objects.equals(contrato.getMoneda(), moneda)
                || !mismoImporte(contrato.getCanon(), canon)
                || !mismoImporte(contrato.getDepositoPactado(), deposito);
    }

    private static boolean mismoImporte(BigDecimal primero, BigDecimal segundo) {
        return primero == null ? segundo == null : segundo != null && primero.compareTo(segundo) == 0;
    }

    private static boolean tieneCargosOPagos(Session session, UUID contratoId) {
        Long cargos = session.createQuery(
                        "select count(c) from Cargo c where c.contratoId = :contratoId", Long.class)
                .setParameter("contratoId", contratoId)
                .getSingleResult();
        if (cargos > 0) return true;
        return session.createQuery(
                        "select count(p) from Pago p where p.contratoId = :contratoId", Long.class)
                .setParameter("contratoId", contratoId)
                .getSingleResult() > 0;
    }
}
