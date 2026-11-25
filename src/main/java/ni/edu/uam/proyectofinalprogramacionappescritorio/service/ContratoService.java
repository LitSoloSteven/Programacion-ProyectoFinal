package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ContratoService {
    private static final List<String> ESTADOS_QUE_RESERVAN_UNIDAD =
            List.of("Vigente", "Por vencer", "Vencido");
    private static final Set<String> MONEDAS = Set.of("USD", "NIO");

    private final HibernateTransactions tx;
    private final HibernateCrudService db;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();
    private final GarantiaService garantiaService;

    public ContratoService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public ContratoService(HibernateTransactions transactions) {
        this.tx = Objects.requireNonNull(transactions);
        this.db = new HibernateCrudService(transactions);
        this.garantiaService = new GarantiaService(transactions);
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

    public List<Contrato> listarFiltrado(UUID unidadId, UUID inquilinoId, String estado) {
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            StringBuilder hql = new StringBuilder("""
                    from Contrato c
                    join fetch c.unidad
                    join fetch c.inquilino
                    left join fetch c.garantiaSeguimiento
                    """);
            Map<String, Object> params = new LinkedHashMap<>();
            if (unidadId != null) {
                agregarClausula(hql, "c.unidadId = :unidadId");
                params.put("unidadId", unidadId);
            }
            if (inquilinoId != null) {
                agregarClausula(hql, "c.inquilinoId = :inquilinoId");
                params.put("inquilinoId", inquilinoId);
            }
            if (estado != null && !estado.isBlank()) {
                String limpio = estado.strip();
                if (!Contrato.esEstadoValido(limpio)) {
                    throw new IllegalArgumentException("El estado del contrato no es válido.");
                }
                agregarClausula(hql, "c.estado = :estado");
                params.put("estado", limpio);
            }
            hql.append(" order by c.inicio desc, c.id");
            var consulta = session.createQuery(hql.toString(), Contrato.class);
            params.forEach(consulta::setParameter);
            return consulta.getResultList();
        });
    }

    public List<Contrato> listarSucesoresBorrador(UUID predecesorId) {
        if (predecesorId == null) {
            throw new IllegalArgumentException("Falta el contrato predecesor.");
        }
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return session.createQuery("""
                            from Contrato c
                            join fetch c.unidad
                            join fetch c.inquilino
                            left join fetch c.garantiaSeguimiento
                            where c.predecesorId = :predecesorId
                              and c.estado = 'Borrador'
                            order by c.inicio, c.id
                            """, Contrato.class)
                    .setParameter("predecesorId", predecesorId)
                    .getResultList();
        });
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
        contrato.inicializarGarantiaSeguimiento();

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            validarFechaActivacion(contrato.getInicio(), contrato.getEstado(), tiempo.fecha());
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

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            validarFechaActivacion(inicio, estado, tiempo.fecha());
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El registro ya no existe.");
            if (contrato.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            contrato.validarTransicionA(estado);
            protegerDisponibilidad(session, contrato.getUnidadId(), contrato.getEstado(),
                    unidadId, estado, id);
            validarTerminosFinancieros(session, contrato, id, moneda, canon, deposito);
            aplicarActualizacion(contrato, unidadId, inquilinoId, inicio, fin,
                    canon, moneda, diaPago, deposito, estado, tiempo.fecha());
            return null;
        });
    }

    public void activar(UUID id, long version) {
        if (id == null) throw new IllegalArgumentException("Falta el identificador del contrato.");
        tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El contrato ya no existe.");
            if (contrato.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            if (!"Borrador".equals(contrato.getEstado())) {
                throw new IllegalArgumentException("Solo se pueden activar contratos en estado Borrador.");
            }
            activarContratoEnSesion(session, actor, tiempo, contrato);
            return null;
        });
    }

    public void cancelarBorrador(UUID id, long version, String motivo) {
        if (id == null) throw new IllegalArgumentException("Falta el identificador del contrato.");
        String motivoLimpio = validarMotivo(motivo, "cancelación");
        tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            reloj.comprobar(session);
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El contrato ya no existe.");
            if (contrato.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            if (!"Borrador".equals(contrato.getEstado())) {
                throw new IllegalArgumentException("Solo se pueden cancelar contratos en estado Borrador.");
            }
            if (tieneCargosOPagos(session, id) || tieneMovimientosGarantia(session, id)) {
                throw new IllegalStateException(
                        "El borrador tiene movimientos asociados y no puede cancelarse sin revisión.");
            }
            contrato.cancelar(motivoLimpio);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), "CONTRATO_CANCELADO",
                    "contrato:" + id);
            return null;
        });
    }

    public UUID renovar(UUID anteriorId, SolicitudRenovacion solicitud) {
        if (anteriorId == null) throw new IllegalArgumentException("Falta el contrato a renovar.");
        if (solicitud == null) throw new IllegalArgumentException("Faltan las condiciones de la renovación.");
        ValidadorEntrada.validarRangoFechas(solicitud.inicio(), solicitud.fin(), 1);
        BigDecimal canon = validarMontoPositivo(solicitud.canon(), "canon");
        String moneda = validarMoneda(solicitud.moneda());
        int diaPago = validarDiaPago(solicitud.diaPago());
        BigDecimal deposito = validarMontoNoNegativo(solicitud.depositoPactado(), "depósito");

        return tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            reloj.comprobar(session);
            Contrato anterior = buscarContratoBloqueado(session, anteriorId);
            if (anterior == null) throw new IllegalArgumentException("El contrato a renovar ya no existe.");
            if (!"Vigente".equals(anterior.getEstado()) && !"Por vencer".equals(anterior.getEstado())) {
                throw new IllegalArgumentException(
                        "Solo se puede iniciar una renovación desde un contrato Vigente o Por vencer.");
            }
            Unidad unidad = session.find(Unidad.class, anterior.getUnidadId());
            if (unidad == null || "Inactiva".equals(unidad.getEstado())
                    || "En mantenimiento".equals(unidad.getEstado())) {
                throw new IllegalArgumentException("La unidad no está habilitada para renovar.");
            }
            Inquilino inquilino = session.find(Inquilino.class, anterior.getInquilinoId());
            if (inquilino == null || !inquilino.isActivo()) {
                throw new IllegalArgumentException("El inquilino debe estar activo para renovar.");
            }

            Contrato sucesor = new Contrato();
            sucesor.setUnidad(unidad);
            sucesor.setInquilino(inquilino);
            sucesor.setPredecesorId(anterior.getId());
            sucesor.setInicio(solicitud.inicio());
            sucesor.setFin(solicitud.fin());
            sucesor.setCanon(canon);
            sucesor.setMoneda(moneda);
            sucesor.setDiaPago(diaPago);
            sucesor.setDepositoPactado(deposito);
            sucesor.setEstado("Borrador");
            sucesor.inicializarGarantiaSeguimiento();

            session.persist(sucesor);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(), "CONTRATO_RENOVACION_CREADA",
                    "contrato:" + sucesor.getId() + ":predecesor=" + anteriorId);
            return sucesor.getId();
        });
    }

    public void terminar(UUID id, long version, SolicitudTerminacion solicitud) {
        if (id == null) throw new IllegalArgumentException("Falta el identificador del contrato.");
        if (solicitud == null) throw new IllegalArgumentException("Faltan los datos de terminación.");
        if (solicitud.fechaTerminacion() == null) {
            throw new IllegalArgumentException("La fecha de terminación del contrato es obligatoria.");
        }
        String motivoLimpio = validarMotivo(solicitud.motivo(), "terminación");
        if (solicitud.activarSucesorId() != null && solicitud.activarSucesorId().equals(id)) {
            throw new IllegalArgumentException("Un contrato no puede ser su propio sucesor.");
        }

        tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El contrato ya no existe.");
            if (contrato.getVersion() != version) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }
            if (!"Vigente".equals(contrato.getEstado())
                    && !"Por vencer".equals(contrato.getEstado())
                    && !"Vencido".equals(contrato.getEstado())) {
                throw new IllegalArgumentException(
                        "Solo se pueden terminar contratos en estado Vigente, Por vencer o Vencido.");
            }
            if (solicitud.fechaTerminacion().isBefore(contrato.getInicio())) {
                throw new IllegalArgumentException(
                        "La fecha de terminación no puede ser anterior al inicio del contrato.");
            }

            Contrato sucesor = null;
            if (solicitud.activarSucesorId() != null) {
                sucesor = buscarContratoBloqueado(session, solicitud.activarSucesorId());
                if (sucesor == null) {
                    throw new IllegalArgumentException("El contrato sucesor ya no existe.");
                }
                if (!"Borrador".equals(sucesor.getEstado())) {
                    throw new IllegalArgumentException("El contrato sucesor debe estar en estado Borrador.");
                }
                if (!id.equals(sucesor.getPredecesorId())) {
                    throw new IllegalArgumentException(
                            "El contrato sucesor debe estar enlazado al contrato que se termina.");
                }
                bloquearUnidades(session, List.of(contrato.getUnidadId(), sucesor.getUnidadId()));
            } else {
                bloquearUnidades(session, List.of(contrato.getUnidadId()));
            }

            Garantia garantia = bloquearGarantiaPorContrato(session, id);
            if (solicitud.liquidacion() != null
                    && solicitud.liquidacion().movimientos() != null
                    && !solicitud.liquidacion().movimientos().isEmpty()) {
                if (solicitud.liquidacion().contratoId() != null
                        && !id.equals(solicitud.liquidacion().contratoId())) {
                    throw new IllegalArgumentException(
                            "La liquidación de garantía pertenece a otro contrato.");
                }
                var liq = new GarantiaService.SolicitudLiquidacion(
                        id,
                        solicitud.liquidacion().liquidacionId(),
                        solicitud.liquidacion().movimientos());
                garantiaService.liquidarEnSesion(session, actor, tiempo, liq);
            }

            if (garantia != null) {
                long movimientosPrevios = contarMovimientosGarantia(session, garantia.getId());
                if (garantia.getMontoInicial().signum() > 0 || movimientosPrevios > 0) {
                    permisos.exigir(session, SesionUsuario.getActual(), Permiso.LIQUIDAR_GARANTIA);
                    if (garantia.getSaldoActual().signum() > 0) {
                        throw new IllegalStateException(
                                "ERR-062: No se puede terminar el contrato mientras la garantía tenga saldo retenido pendiente de liquidar.");
                    }
                }
            }

            contrato.terminar(solicitud.fechaTerminacion(), motivoLimpio);
            auditoria.registrar(session, actor, UUID.randomUUID(), "CONTRATO_TERMINADO",
                    "contrato:" + id);

            if (sucesor != null) {
                // Libera el índice único parcial uq_contrato_activo antes de activar el sucesor.
                session.flush();
                activarContratoEnSesion(session, actor, tiempo, sucesor);
            } else {
                session.flush();
            }
            return null;
        });
    }

    public void cambiarEstado(UUID id, String estado) throws SQLException {
        if (!Contrato.esEstadoValido(estado)) {
            throw new IllegalArgumentException("El estado del contrato no es válido.");
        }
        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            Contrato contrato = buscarContratoBloqueado(session, id);
            if (contrato == null) throw new IllegalArgumentException("El registro ya no existe.");
            contrato.validarTransicionA(estado);
            validarFechaActivacion(contrato.getInicio(), estado, tiempo.fecha());
            protegerDisponibilidad(session, contrato.getUnidadId(), contrato.getEstado(),
                    contrato.getUnidadId(), estado, id);
            contrato.transicionarA(estado, tiempo.fecha());
            return null;
        });
    }

    public void terminarContrato(UUID id) throws SQLException {
        cambiarEstado(id, "Terminado");
    }

    public void eliminar(UUID id) throws SQLException {
        db.eliminar(Contrato.class, id, Permiso.GESTIONAR_USUARIOS);
    }

    private void activarContratoEnSesion(Session session, Usuario actor,
                                         RelojService.TiempoOperativo tiempo,
                                         Contrato contrato) {
        ValidadorEntrada.validarRangoFechas(contrato.getInicio(), contrato.getFin(), 1);
        validarFechaActivacion(contrato.getInicio(), "Vigente", tiempo.fecha());
        List<Unidad> unidades = bloquearUnidades(session, List.of(contrato.getUnidadId()));
        Unidad unidad = unidades.getFirst();
        if ("Inactiva".equals(unidad.getEstado()) || "En mantenimiento".equals(unidad.getEstado())) {
            throw new IllegalArgumentException(
                    "No se puede activar el contrato porque la unidad está " + unidad.getEstado() + ".");
        }
        Inquilino inquilino = session.find(Inquilino.class, contrato.getInquilinoId());
        if (inquilino == null || !inquilino.isActivo()) {
            throw new IllegalArgumentException(
                    "No se puede activar el contrato porque el inquilino está inactivo.");
        }
        validarDisponibilidad(session, contrato.getUnidadId(), contrato.getId());
        contrato.inicializarGarantiaSeguimiento();
        contrato.transicionarA("Vigente", tiempo.fecha());
        session.flush();
        auditoria.registrar(session, actor, UUID.randomUUID(), "CONTRATO_ACTIVADO",
                "contrato:" + contrato.getId());
    }

    private static void validarDatos(UUID unidadId, UUID inquilinoId, LocalDate inicio,
                                     LocalDate fin, String estado) {
        if (unidadId == null || inquilinoId == null) {
            throw new IllegalArgumentException("Verifique unidad e inquilino del contrato.");
        }
        ValidadorEntrada.validarRangoFechas(inicio, fin, 1);
        if (!Contrato.esEstadoValido(estado)) {
            throw new IllegalArgumentException("El estado del contrato no es válido.");
        }
    }

    private static void validarFechaActivacion(LocalDate inicio, String estado,
                                               LocalDate fechaOperativa) {
        if (Contrato.reservaUnidad(estado) && inicio.isAfter(fechaOperativa)) {
            throw new IllegalArgumentException(
                    "Un contrato con inicio futuro debe permanecer como Borrador.");
        }
    }

    private static List<Unidad> bloquearUnidades(Session session, Collection<UUID> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().sorted().map(id -> {
            Unidad unidad = session.find(Unidad.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (unidad == null) {
                throw new IllegalArgumentException("La unidad seleccionada ya no existe.");
            }
            return unidad;
        }).toList();
    }

    private static void protegerDisponibilidad(Session session, UUID unidadActual,
                                               String estadoActual, UUID unidadNueva,
                                               String estadoNuevo, UUID contratoId) {
        boolean reservaAntes = Contrato.reservaUnidad(estadoActual);
        boolean reservaDespues = Contrato.reservaUnidad(estadoNuevo);
        if (!reservaAntes && !reservaDespues) return;

        bloquearUnidades(session, List.of(unidadActual, unidadNueva));
        if (reservaDespues) validarDisponibilidad(session, unidadNueva, contratoId);
    }

    private static void validarTerminosFinancieros(Session session, Contrato contrato,
                                                   UUID contratoId, String moneda,
                                                   BigDecimal canon, BigDecimal deposito) {
        if (cambiaronTerminosFinancieros(contrato, moneda, canon, deposito)
                && tieneCargosOPagos(session, contratoId)) {
            throw new IllegalStateException(
                    "No se pueden cambiar moneda, canon ni depósito después de registrar cargos o pagos.");
        }
    }

    private static void aplicarActualizacion(Contrato contrato, UUID unidadId,
                                             UUID inquilinoId, LocalDate inicio,
                                             LocalDate fin, BigDecimal canon, String moneda,
                                             int diaPago, BigDecimal deposito, String estado,
                                             LocalDate fechaOperativa) {
        contrato.setUnidadId(unidadId);
        contrato.setInquilinoId(inquilinoId);
        contrato.setInicio(inicio);
        contrato.setFin(fin);
        contrato.setCanon(canon);
        contrato.setMoneda(moneda);
        contrato.setDiaPago(diaPago);
        contrato.setDepositoPactado(deposito);
        contrato.transicionarA(estado, fechaOperativa);
    }

    private static Contrato buscarContratoBloqueado(Session session, UUID id) {
        List<UUID> encontrados = session.createNativeQuery(
                        "select id from contrato where id = :id for update", UUID.class)
                .setParameter("id", id)
                .getResultList();
        return encontrados.isEmpty() ? null : session.find(Contrato.class, id);
    }

    private static Garantia bloquearGarantiaPorContrato(Session session, UUID contratoId) {
        List<UUID> ids = session.createNativeQuery(
                        "select id from garantia where contrato_id = :contratoId for update", UUID.class)
                .setParameter("contratoId", contratoId)
                .getResultList();
        return ids.isEmpty() ? null : session.find(Garantia.class, ids.getFirst());
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

    private static boolean tieneMovimientosGarantia(Session session, UUID contratoId) {
        Long total = session.createQuery("""
                        select count(m) from MovimientoGarantia m, Garantia g
                        where m.garantiaId = g.id and g.contrato.id = :contratoId
                        """, Long.class)
                .setParameter("contratoId", contratoId)
                .getSingleResult();
        return total > 0;
    }

    private static long contarMovimientosGarantia(Session session, UUID garantiaId) {
        return session.createQuery(
                        "select count(m) from MovimientoGarantia m where m.garantiaId = :garantiaId",
                        Long.class)
                .setParameter("garantiaId", garantiaId)
                .getSingleResult();
    }

    private static void agregarClausula(StringBuilder hql, String clausula) {
        hql.append(hql.indexOf(" where ") >= 0 ? " and " : " where ").append(clausula);
    }

    private static String validarMotivo(String motivo, String accion) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de " + accion + " es obligatorio.");
        }
        String limpio = motivo.strip();
        if (limpio.length() > 500) {
            throw new IllegalArgumentException("El motivo de " + accion + " no puede superar 500 caracteres.");
        }
        return limpio;
    }

    private static BigDecimal validarMontoPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0 || valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El " + campo + " debe ser mayor que cero y tener hasta dos decimales.");
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal validarMontoNoNegativo(BigDecimal valor, String campo) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor;
        if (monto.signum() < 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El " + campo + " debe ser cero o mayor y tener hasta dos decimales.");
        }
        return monto.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String validarMoneda(String moneda) {
        if (moneda == null || !MONEDAS.contains(moneda.strip().toUpperCase())) {
            throw new IllegalArgumentException("La moneda debe ser USD o NIO.");
        }
        return moneda.strip().toUpperCase();
    }

    private static int validarDiaPago(int diaPago) {
        if (diaPago < 1 || diaPago > 31) {
            throw new IllegalArgumentException("El día de pago debe estar entre 1 y 31.");
        }
        return diaPago;
    }

    public record SolicitudRenovacion(
            LocalDate inicio,
            LocalDate fin,
            BigDecimal canon,
            String moneda,
            int diaPago,
            BigDecimal depositoPactado) {
    }

    public record SolicitudTerminacion(
            LocalDate fechaTerminacion,
            String motivo,
            UUID activarSucesorId,
            GarantiaService.SolicitudLiquidacion liquidacion) {
    }
}
