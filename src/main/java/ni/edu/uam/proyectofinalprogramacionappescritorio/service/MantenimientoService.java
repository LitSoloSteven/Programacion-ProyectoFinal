package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.GastoPropiedadDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenGastosMantenimientoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.SolicitudMantenimientoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.GastoPropiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Moneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio transaccional de solicitudes de mantenimiento y gastos de propiedad (M08 / RN-23, RN-27).
 */
public final class MantenimientoService {
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public MantenimientoService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions, "Faltan las transacciones.");
    }

    public SolicitudMantenimientoDTO abrir(SesionUsuario identidad, UUID unidadId, String descripcion,
                                           String prioridad, String responsable, LocalDate abiertaEl) {
        if (unidadId == null) {
            throw new IllegalArgumentException("Selecciona una unidad registrada.");
        }
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            Unidad unidad = session.find(Unidad.class, unidadId);
            if (unidad == null) {
                throw new IllegalArgumentException("La unidad seleccionada no existe.");
            }
            RelojService.TiempoOperativo tiempo = reloj.leer(session);
            LocalDate fechaApertura = abiertaEl == null ? tiempo.fecha() : abiertaEl;
            SolicitudMantenimiento solicitud = new SolicitudMantenimiento(
                    unidadId, descripcion, prioridad, responsable, fechaApertura, tiempo.instante(), actor);
            session.persist(solicitud);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "CREAR_MANTENIMIENTO", "solicitud:" + solicitud.getId() + ";unidad:" + unidadId);
            return aDto(solicitud, unidad, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        });
    }

    public SolicitudMantenimientoDTO actualizarDetalles(SesionUsuario identidad, UUID solicitudId,
                                                        long versionEsperada, String descripcion,
                                                        String prioridad, String responsable) {
        Objects.requireNonNull(solicitudId, "Falta el identificador de la solicitud.");
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            SolicitudMantenimiento solicitud = session.find(
                    SolicitudMantenimiento.class, solicitudId, LockModeType.PESSIMISTIC_WRITE);
            if (solicitud == null) {
                throw new IllegalArgumentException("La solicitud de mantenimiento no existe.");
            }
            if (solicitud.getVersion() != versionEsperada) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó la solicitud. Actualiza los datos antes de guardar.");
            }
            solicitud.actualizarDetalles(descripcion, prioridad, responsable);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "ACTUALIZAR_MANTENIMIENTO", "solicitud:" + solicitudId);
            Map<UUID, TotalesMoneda> totales = totalesPorSolicitud(session, List.of(solicitudId));
            TotalesMoneda t = totales.getOrDefault(solicitudId, TotalesMoneda.CERO);
            Unidad unidad = session.find(Unidad.class, solicitud.getUnidadId());
            return aDto(solicitud, unidad, t.usd(), t.nio());
        });
    }

    public SolicitudMantenimientoDTO cambiarEstado(SesionUsuario identidad, UUID solicitudId,
                                                   long versionEsperada, String estadoDestino,
                                                   LocalDate fechaCierre, String motivoOResolucion) {
        Objects.requireNonNull(solicitudId, "Falta el identificador de la solicitud.");
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            SolicitudMantenimiento solicitud = session.find(
                    SolicitudMantenimiento.class, solicitudId, LockModeType.PESSIMISTIC_WRITE);
            if (solicitud == null) {
                throw new IllegalArgumentException("La solicitud de mantenimiento no existe.");
            }
            if (solicitud.getVersion() != versionEsperada) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó la solicitud. Actualiza los datos antes de reintentar.");
            }
            LocalDate cierreEfectivo = fechaCierre;
            if (cierreEfectivo == null
                    && (SolicitudMantenimiento.ESTADO_RESUELTA.equals(estadoDestino)
                    || SolicitudMantenimiento.ESTADO_CANCELADA.equals(estadoDestino))) {
                cierreEfectivo = reloj.leer(session).fecha();
            }
            boolean cambio = solicitud.transicionarA(estadoDestino, cierreEfectivo, motivoOResolucion);
            if (cambio) {
                session.flush();
                auditoria.registrar(session, actor, UUID.randomUUID(),
                        "CAMBIAR_ESTADO_MANTENIMIENTO",
                        "solicitud:" + solicitudId + ";estado:" + solicitud.getEstado());
            }
            Map<UUID, TotalesMoneda> totales = totalesPorSolicitud(session, List.of(solicitudId));
            TotalesMoneda t = totales.getOrDefault(solicitudId, TotalesMoneda.CERO);
            Unidad unidad = session.find(Unidad.class, solicitud.getUnidadId());
            return aDto(solicitud, unidad, t.usd(), t.nio());
        });
    }

    public GastoPropiedadDTO registrarGasto(SesionUsuario identidad, UUID propiedadId, UUID solicitudIdOpcional,
                                            LocalDate fechaGasto, BigDecimal importe,
                                            String moneda, String concepto) {
        if (propiedadId == null) {
            throw new IllegalArgumentException("Selecciona la propiedad del gasto.");
        }
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            Propiedad propiedad = session.find(Propiedad.class, propiedadId);
            if (propiedad == null) {
                throw new IllegalArgumentException("La propiedad seleccionada no existe.");
            }
            SolicitudMantenimiento solicitud = null;
            if (solicitudIdOpcional != null) {
                solicitud = session.find(SolicitudMantenimiento.class, solicitudIdOpcional);
                if (solicitud == null) {
                    throw new IllegalArgumentException("La solicitud asociada no existe.");
                }
                Unidad unidad = session.find(Unidad.class, solicitud.getUnidadId());
                if (unidad == null || !Objects.equals(unidad.getPropiedadId(), propiedadId)) {
                    throw new PropiedadIncompatibleException(
                            "ERR-065: La unidad de la solicitud pertenece a otra propiedad.");
                }
            }
            RelojService.TiempoOperativo tiempo = reloj.leer(session);
            LocalDate fechaEfectiva = fechaGasto == null ? tiempo.fecha() : fechaGasto;
            GastoPropiedad gasto = new GastoPropiedad(
                    propiedadId, solicitudIdOpcional, fechaEfectiva, importe, moneda, concepto,
                    tiempo.instante(), actor);
            session.persist(gasto);
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "REGISTRAR_GASTO_MANTENIMIENTO",
                    "gasto:" + gasto.getId() + ";propiedad:" + propiedadId
                            + (solicitudIdOpcional != null ? ";solicitud:" + solicitudIdOpcional : ""));
            return new GastoPropiedadDTO(
                    gasto.getId(), propiedadId, propiedad.getNombre(),
                    solicitud == null ? null : solicitud.getId(),
                    solicitud == null ? null : solicitud.getDescripcion(),
                    gasto.getFechaGasto(), gasto.getImporte(), gasto.getMoneda(),
                    gasto.getConcepto(), gasto.getCreadoEn(), actor.getNombre());
        });
    }

    public Optional<SolicitudMantenimientoDTO> obtenerSolicitud(SesionUsuario identidad, UUID solicitudId) {
        Objects.requireNonNull(solicitudId, "Falta el identificador de la solicitud.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            SolicitudMantenimiento s = session.createQuery("""
                            select s from SolicitudMantenimiento s
                            join fetch s.unidad u
                            join fetch u.propiedad
                            join fetch s.creadoPor
                            where s.id = :id
                            """, SolicitudMantenimiento.class)
                    .setParameter("id", solicitudId)
                    .uniqueResultOptional()
                    .orElse(null);
            if (s == null) return Optional.empty();
            TotalesMoneda t = totalesPorSolicitud(session, List.of(s.getId()))
                    .getOrDefault(s.getId(), TotalesMoneda.CERO);
            return Optional.of(aDto(s, s.getUnidad(), t.usd(), t.nio()));
        });
    }

    public List<SolicitudMantenimientoDTO> listarSolicitudes(SesionUsuario identidad, UUID propiedadId,
                                                           UUID unidadId, String prioridad, String estado) {
        String prioridadFiltro = normalizarOpcional(prioridad);
        String estadoFiltro = normalizarOpcional(estado);
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            StringBuilder hql = new StringBuilder("""
                    select s from SolicitudMantenimiento s
                    join fetch s.unidad u
                    join fetch u.propiedad p
                    join fetch s.creadoPor
                    """);
            StringBuilder where = new StringBuilder();
            Map<String, Object> params = new LinkedHashMap<>();
            if (propiedadId != null) {
                where.append("u.propiedadId = :propiedadId");
                params.put("propiedadId", propiedadId);
            }
            if (unidadId != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("s.unidadId = :unidadId");
                params.put("unidadId", unidadId);
            }
            if (prioridadFiltro != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("s.prioridad = :prioridad");
                params.put("prioridad", prioridadFiltro);
            }
            if (estadoFiltro != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("s.estado = :estado");
                params.put("estado", estadoFiltro);
            }
            if (!where.isEmpty()) hql.append(" where ").append(where);
            hql.append(" order by s.abiertaEl desc, s.creadoEn desc");

            var query = session.createQuery(hql.toString(), SolicitudMantenimiento.class);
            params.forEach(query::setParameter);
            List<SolicitudMantenimiento> solicitudes = query.getResultList();
            if (solicitudes.isEmpty()) return List.of();

            List<UUID> ids = solicitudes.stream().map(SolicitudMantenimiento::getId).toList();
            Map<UUID, TotalesMoneda> totales = totalesPorSolicitud(session, ids);
            return solicitudes.stream()
                    .map(s -> {
                        TotalesMoneda t = totales.getOrDefault(s.getId(), TotalesMoneda.CERO);
                        return aDto(s, s.getUnidad(), t.usd(), t.nio());
                    })
                    .toList();
        });
    }

    public ResumenGastosMantenimientoDTO consultarGastos(SesionUsuario identidad, UUID propiedadId,
                                                         LocalDate desde, LocalDate hasta, String moneda) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        }
        String monedaRaw = normalizarOpcional(moneda);
        String monedaFiltro = monedaRaw == null ? null : Moneda.desde(monedaRaw).name();
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            StringBuilder hql = new StringBuilder("""
                    select g from GastoPropiedad g
                    join fetch g.propiedad p
                    left join fetch g.solicitud s
                    join fetch g.creadoPor
                    """);
            StringBuilder where = new StringBuilder();
            Map<String, Object> params = new LinkedHashMap<>();
            if (propiedadId != null) {
                where.append("g.propiedadId = :propiedadId");
                params.put("propiedadId", propiedadId);
            }
            if (desde != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("g.fechaGasto >= :desde");
                params.put("desde", desde);
            }
            if (hasta != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("g.fechaGasto <= :hasta");
                params.put("hasta", hasta);
            }
            if (monedaFiltro != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("g.moneda = :moneda");
                params.put("moneda", monedaFiltro);
            }
            if (!where.isEmpty()) hql.append(" where ").append(where);
            hql.append(" order by g.fechaGasto desc, g.creadoEn desc");

            var query = session.createQuery(hql.toString(), GastoPropiedad.class);
            params.forEach(query::setParameter);
            List<GastoPropiedad> gastos = query.getResultList();

            BigDecimal totalUsd = BigDecimal.ZERO;
            BigDecimal totalNio = BigDecimal.ZERO;
            List<GastoPropiedadDTO> filas = new java.util.ArrayList<>(gastos.size());
            for (GastoPropiedad g : gastos) {
                if ("USD".equals(g.getMoneda())) {
                    totalUsd = totalUsd.add(g.getImporte());
                } else if ("NIO".equals(g.getMoneda())) {
                    totalNio = totalNio.add(g.getImporte());
                }
                filas.add(new GastoPropiedadDTO(
                        g.getId(),
                        g.getPropiedadId(),
                        g.getPropiedad() == null ? "—" : g.getPropiedad().getNombre(),
                        g.getSolicitudId(),
                        g.getSolicitud() == null ? null : g.getSolicitud().getDescripcion(),
                        g.getFechaGasto(),
                        g.getImporte(),
                        g.getMoneda(),
                        g.getConcepto(),
                        g.getCreadoEn(),
                        g.getCreadoPor().getNombre()
                ));
            }
            return new ResumenGastosMantenimientoDTO(
                    List.copyOf(filas),
                    totalUsd.setScale(2, RoundingMode.HALF_EVEN),
                    totalNio.setScale(2, RoundingMode.HALF_EVEN));
        });
    }

    private static Map<UUID, TotalesMoneda> totalesPorSolicitud(Session session, List<UUID> solicitudIds) {
        if (solicitudIds == null || solicitudIds.isEmpty()) return Map.of();
        List<Object[]> filas = session.createQuery("""
                        select g.solicitudId, g.moneda, sum(g.importe)
                        from GastoPropiedad g
                        where g.solicitudId in (:ids)
                        group by g.solicitudId, g.moneda
                        """, Object[].class)
                .setParameter("ids", solicitudIds)
                .getResultList();
        Map<UUID, TotalesMoneda> mapa = new HashMap<>();
        for (Object[] fila : filas) {
            UUID id = (UUID) fila[0];
            String moneda = (String) fila[1];
            BigDecimal suma = ((BigDecimal) fila[2]).setScale(2, RoundingMode.HALF_EVEN);
            TotalesMoneda actual = mapa.getOrDefault(id, TotalesMoneda.CERO);
            if ("USD".equals(moneda)) {
                mapa.put(id, new TotalesMoneda(suma, actual.nio()));
            } else if ("NIO".equals(moneda)) {
                mapa.put(id, new TotalesMoneda(actual.usd(), suma));
            }
        }
        return mapa;
    }

    private static SolicitudMantenimientoDTO aDto(SolicitudMantenimiento s, Unidad unidad,
                                                  BigDecimal usd, BigDecimal nio) {
        return new SolicitudMantenimientoDTO(
                s.getId(),
                s.getUnidadId(),
                unidad == null ? "—" : unidad.getCodigo(),
                unidad == null ? null : unidad.getPropiedadId(),
                unidad == null ? "—" : unidad.getPropiedadNombre(),
                s.getDescripcion(),
                s.getPrioridad(),
                s.getEstado(),
                s.getResponsable(),
                s.getAbiertaEl(),
                s.getCerradaEl(),
                s.getMotivoCancelacion(),
                s.getDetalleResolucion(),
                usd.setScale(2, RoundingMode.HALF_EVEN),
                nio.setScale(2, RoundingMode.HALF_EVEN),
                s.getCreadoEn(),
                s.getCreadoPor().getNombre(),
                s.getVersion());
    }

    private static String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private record TotalesMoneda(BigDecimal usd, BigDecimal nio) {
        private static final TotalesMoneda CERO = new TotalesMoneda(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
    }
}
