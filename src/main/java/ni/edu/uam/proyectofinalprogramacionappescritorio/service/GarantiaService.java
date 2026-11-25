package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.CargoAplicableGarantiaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MovimientoGarantiaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenGarantiaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta y liquidación transaccional de depósitos en garantía (M07).
 * Las salidas nunca consumen el consecutivo comercial de recibos de pago.
 */
public class GarantiaService {
    private static final int MAXIMO_MOVIMIENTOS_POR_PAQUETE = 100;

    private final HibernateTransactions tx;
    private final HibernateCrudService db;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public GarantiaService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public GarantiaService(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx);
        this.db = new HibernateCrudService(tx);
    }

    public List<Garantia> listarTodas() throws SQLException {
        return db.listar(Garantia.class, "from Garantia g join fetch g.contrato order by g.contrato.inicio desc");
    }

    public void ajustarSaldo(UUID id, BigDecimal saldoActual) throws SQLException {
        if (id == null || saldoActual == null || saldoActual.signum() < 0
                || saldoActual.stripTrailingZeros().scale() > 2
                || saldoActual.precision() > 19) {
            throw new IllegalArgumentException("El saldo debe ser positivo o cero y tener como máximo dos decimales.");
        }
        tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.LIQUIDAR_GARANTIA);
            reloj.comprobar(session);
            Garantia garantia = session.find(Garantia.class, id);
            if (garantia == null) throw new IllegalArgumentException("La garantía ya no existe.");
            garantia.ajustarSaldo(saldoActual.setScale(2));
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "GARANTIA_SALDO_AJUSTADO", "garantia:" + id
                            + ":saldo=" + saldoActual.toPlainString());
            return null;
        });
    }

    public ResumenGarantiaDTO consultarPorContrato(UUID contratoId) {
        if (contratoId == null) {
            throw new IllegalArgumentException("Falta el identificador del contrato.");
        }
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return consultarEnSesion(session, contratoId);
        });
    }

    public List<CargoAplicableGarantiaDTO> listarCargosAplicables(UUID contratoId) {
        if (contratoId == null) {
            throw new IllegalArgumentException("Falta el identificador del contrato.");
        }
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Contrato contrato = session.find(Contrato.class, contratoId);
            if (contrato == null) {
                throw new IllegalArgumentException("El contrato ya no existe.");
            }
            List<Cargo> cargos = session.createQuery("""
                            from Cargo c
                            where c.contratoId = :contratoId
                              and c.estado not in ('Cancelado', 'Anulado')
                            order by c.venceEl, c.id
                            """, Cargo.class)
                    .setParameter("contratoId", contratoId)
                    .getResultList();
            if (cargos.isEmpty()) return List.of();

            List<UUID> ids = cargos.stream().map(Cargo::getId).toList();
            Map<UUID, BigDecimal> aplicadoPorCargo = new HashMap<>();
            for (Object[] fila : session.createQuery("""
                            select a.cargoId, sum(a.monto)
                            from AplicacionPago a left join Pago p on a.pagoId = p.id
                            where a.cargoId in :ids
                              and a.revertidaEn is null
                              and (a.pagoId is null or p.estado <> 'Anulado')
                            group by a.cargoId
                            """, Object[].class)
                    .setParameter("ids", ids)
                    .getResultList()) {
                aplicadoPorCargo.put((UUID) fila[0], (BigDecimal) fila[1]);
            }

            List<CargoAplicableGarantiaDTO> resultado = new ArrayList<>();
            for (Cargo cargo : cargos) {
                BigDecimal saldo = cargo.saldoPendiente(aplicadoPorCargo.get(cargo.getId()));
                if (saldo.signum() > 0) {
                    resultado.add(new CargoAplicableGarantiaDTO(
                            cargo.getId(),
                            cargo.getTipo(),
                            cargo.getConcepto(),
                            cargo.getVenceEl(),
                            cargo.getImporte().setScale(2, RoundingMode.HALF_EVEN),
                            saldo,
                            contrato.getMoneda()));
                }
            }
            return resultado;
        });
    }

    public ResumenGarantiaDTO liquidar(SolicitudLiquidacion solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud de liquidación es obligatoria.");
        }
        try {
            return tx.ejecutar(session -> {
                Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                        Permiso.LIQUIDAR_GARANTIA);
                RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
                return liquidarEnSesion(session, actor, tiempo, solicitud);
            });
        } catch (ConflictoDatosException e) {
            ResumenGarantiaDTO repetido = buscarRepeticionIdempotente(solicitud);
            if (repetido != null) return repetido;
            throw e;
        }
    }

    ResumenGarantiaDTO liquidarEnSesion(Session session, Usuario actor,
                                        RelojService.TiempoOperativo tiempo,
                                        SolicitudLiquidacion solicitud) {
        Objects.requireNonNull(session, "Falta la sesión.");
        Objects.requireNonNull(tiempo, "Falta el instante operativo.");
        if (actor == null) {
            actor = permisos.exigir(session, SesionUsuario.getActual(), Permiso.LIQUIDAR_GARANTIA);
        } else {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.LIQUIDAR_GARANTIA);
        }

        if (solicitud == null || solicitud.contratoId() == null) {
            throw new IllegalArgumentException("El contrato es obligatorio para liquidar la garantía.");
        }
        if (solicitud.liquidacionId() == null) {
            throw new IllegalArgumentException("El identificador de liquidación es obligatorio.");
        }
        if (solicitud.movimientos() == null || solicitud.movimientos().isEmpty()) {
            throw new IllegalArgumentException("Debes indicar al menos un movimiento de garantía.");
        }
        if (solicitud.movimientos().size() > MAXIMO_MOVIMIENTOS_POR_PAQUETE) {
            throw new IllegalArgumentException("La liquidación no puede superar "
                    + MAXIMO_MOVIMIENTOS_POR_PAQUETE + " movimientos.");
        }

        List<ItemNormalizado> items = normalizarItems(solicitud.movimientos(), tiempo.fecha());
        String liquidacionHash = calcularHuella(solicitud.contratoId(), items);

        Contrato contrato = bloquearContrato(session, solicitud.contratoId());
        if (contrato == null) {
            throw new IllegalArgumentException("El contrato ya no existe.");
        }
        Garantia garantia = bloquearGarantiaPorContrato(session, solicitud.contratoId());
        if (garantia == null) {
            throw new IllegalArgumentException("El contrato no tiene seguimiento de garantía.");
        }

        List<MovimientoGarantia> existentes = session.createQuery("""
                        from MovimientoGarantia m
                        where m.liquidacionId = :liquidacionId
                        order by m.orden
                        """, MovimientoGarantia.class)
                .setParameter("liquidacionId", solicitud.liquidacionId())
                .getResultList();
        if (!existentes.isEmpty()) {
            boolean coincide = existentes.stream().allMatch(m ->
                    garantia.getId().equals(m.getGarantiaId())
                            && liquidacionHash.equals(m.getLiquidacionHash()));
            if (!coincide) {
                throw new ConflictoDatosException(
                        "La clave de liquidación ya fue utilizada con un contenido diferente.");
            }
            return consultarEnSesion(session, solicitud.contratoId());
        }

        if (garantia.getMontoInicial().signum() == 0) {
            throw new IllegalArgumentException(
                    "Una garantía con depósito pactado cero permanece Retenida y no admite movimientos.");
        }

        BigDecimal totalSalidas = BigDecimal.ZERO;
        for (ItemNormalizado item : items) {
            if (item.fechaMovimiento().isAfter(tiempo.fecha())) {
                throw new IllegalArgumentException(
                        "La fecha del movimiento de garantía no puede ser futura.");
            }
            if (item.fechaMovimiento().isBefore(contrato.getInicio())) {
                throw new IllegalArgumentException(
                        "La fecha del movimiento de garantía no puede preceder el inicio del contrato.");
            }
            totalSalidas = totalSalidas.add(item.importe());
        }
        if (totalSalidas.compareTo(garantia.getSaldoActual()) > 0) {
            throw new IllegalArgumentException(
                    "El total de salidas supera el saldo retenido de la garantía.");
        }

        short orden = 1;
        for (ItemNormalizado item : items) {
            MovimientoGarantia movimiento;
            if (MovimientoGarantia.TIPO_APLICACION.equals(item.tipo())) {
                Cargo cargo = bloquearCargo(session, item.cargoId());
                if (cargo == null
                        || !solicitud.contratoId().equals(cargo.getContratoId())
                        || "Cancelado".equals(cargo.getEstadoPersistido())
                        || "Anulado".equals(cargo.getEstadoPersistido())) {
                    throw new IllegalArgumentException(
                            "ERR-061: El cargo no existe, está anulado o pertenece a otro contrato.");
                }
                BigDecimal yaAplicado = session.createQuery("""
                                select sum(ap.monto)
                                from AplicacionPago ap left join Pago p on ap.pagoId = p.id
                                where ap.cargoId = :cargoId
                                  and ap.revertidaEn is null
                                  and (ap.pagoId is null or p.estado <> 'Anulado')
                                """, BigDecimal.class)
                        .setParameter("cargoId", cargo.getId())
                        .getSingleResult();
                if (yaAplicado == null) yaAplicado = BigDecimal.ZERO;
                BigDecimal saldoCargo = cargo.saldoPendiente(yaAplicado);
                if (item.importe().compareTo(saldoCargo) > 0) {
                    throw new IllegalArgumentException(
                            "La aplicación de garantía supera el saldo pendiente del cargo.");
                }
                movimiento = MovimientoGarantia.aplicacion(
                        garantia.getId(), cargo.getId(), solicitud.liquidacionId(),
                        liquidacionHash, orden, item.importe(), item.fechaMovimiento(),
                        item.concepto(), tiempo.instante(), actor.getId());
                session.persist(movimiento);
                session.flush();
                session.persist(AplicacionPago.desdeGarantia(
                        movimiento.getId(), cargo.getId(), item.importe()));
            } else if (MovimientoGarantia.TIPO_DEDUCCION.equals(item.tipo())) {
                movimiento = MovimientoGarantia.deduccion(
                        garantia.getId(), solicitud.liquidacionId(),
                        liquidacionHash, orden, item.importe(), item.fechaMovimiento(),
                        item.concepto(), tiempo.instante(), actor.getId());
                session.persist(movimiento);
            } else {
                movimiento = MovimientoGarantia.devolucion(
                        garantia.getId(), solicitud.liquidacionId(),
                        liquidacionHash, orden, item.importe(), item.fechaMovimiento(),
                        item.metodo(), item.concepto(), tiempo.instante(), actor.getId());
                session.persist(movimiento);
            }
            orden++;
        }

        BigDecimal nuevoSaldo = garantia.getSaldoActual().subtract(totalSalidas)
                .setScale(2, RoundingMode.HALF_EVEN);
        garantia.ajustarSaldo(nuevoSaldo);
        session.flush();

        ResumenGarantiaDTO resumen = consultarEnSesion(session, solicitud.contratoId());
        if (!resumen.conciliado()) {
            throw new IllegalStateException("La liquidación rompió la conciliación de la garantía.");
        }
        auditoria.registrar(session, actor, UUID.randomUUID(),
                "GARANTIA_LIQUIDADA",
                "garantia:" + garantia.getId() + ":liquidacion=" + solicitud.liquidacionId());
        return resumen;
    }

    ResumenGarantiaDTO consultarEnSesion(Session session, UUID contratoId) {
        Contrato contrato = session.find(Contrato.class, contratoId);
        if (contrato == null) {
            throw new IllegalArgumentException("El contrato ya no existe.");
        }
        Garantia garantia = session.createQuery(
                        "from Garantia g where g.contrato.id = :contratoId", Garantia.class)
                .setParameter("contratoId", contratoId)
                .uniqueResult();
        if (garantia == null) {
            throw new IllegalArgumentException("El contrato no tiene seguimiento de garantía.");
        }

        List<MovimientoGarantia> movimientos = session.createQuery("""
                        from MovimientoGarantia m
                        where m.garantiaId = :garantiaId
                        order by m.fechaMovimiento, m.orden, m.id
                        """, MovimientoGarantia.class)
                .setParameter("garantiaId", garantia.getId())
                .getResultList();

        Map<UUID, String> resumenCargos = new HashMap<>();
        List<UUID> cargoIds = movimientos.stream()
                .map(MovimientoGarantia::getCargoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!cargoIds.isEmpty()) {
            for (Cargo cargo : session.createQuery(
                            "from Cargo c where c.id in :ids", Cargo.class)
                    .setParameter("ids", cargoIds)
                    .getResultList()) {
                resumenCargos.put(cargo.getId(), cargo.getTipo() + " · " + cargo.getConcepto());
            }
        }

        BigDecimal aplicado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal deducido = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal devuelto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        List<MovimientoGarantiaDTO> movimientosDto = new ArrayList<>(movimientos.size());
        for (MovimientoGarantia mov : movimientos) {
            BigDecimal importe = mov.getImporte().setScale(2, RoundingMode.HALF_EVEN);
            switch (mov.getTipo()) {
                case MovimientoGarantia.TIPO_APLICACION -> aplicado = aplicado.add(importe);
                case MovimientoGarantia.TIPO_DEDUCCION -> deducido = deducido.add(importe);
                case MovimientoGarantia.TIPO_DEVOLUCION -> devuelto = devuelto.add(importe);
                default -> throw new IllegalStateException("Tipo de movimiento no reconocido.");
            }
            movimientosDto.add(new MovimientoGarantiaDTO(
                    mov.getId(),
                    mov.getGarantiaId(),
                    mov.getCargoId(),
                    mov.getCargoId() == null ? null : resumenCargos.get(mov.getCargoId()),
                    mov.getLiquidacionId(),
                    mov.getOrden(),
                    mov.getTipo(),
                    importe,
                    mov.getFechaMovimiento(),
                    mov.getMetodo(),
                    mov.getConcepto(),
                    mov.getCreadoEn(),
                    mov.getCreadoPor()));
        }

        BigDecimal montoInicial = garantia.getMontoInicial().setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal saldoActual = garantia.getSaldoActual().setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal suma = aplicado.add(deducido).add(devuelto).add(saldoActual);
        boolean conciliado = montoInicial.compareTo(suma) == 0;
        String estado = Garantia.derivarEstado(montoInicial, saldoActual, devuelto);

        return new ResumenGarantiaDTO(
                garantia.getId(),
                contratoId,
                contrato.getMoneda(),
                montoInicial,
                aplicado,
                deducido,
                devuelto,
                saldoActual,
                estado,
                conciliado,
                List.copyOf(movimientosDto));
    }

    private ResumenGarantiaDTO buscarRepeticionIdempotente(SolicitudLiquidacion solicitud) {
        if (solicitud.contratoId() == null || solicitud.liquidacionId() == null
                || solicitud.movimientos() == null || solicitud.movimientos().isEmpty()) {
            return null;
        }
        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.LIQUIDAR_GARANTIA);
            LocalDate fechaDefecto = reloj.leer(session).fecha();
            List<ItemNormalizado> items = normalizarItems(solicitud.movimientos(), fechaDefecto);
            String hash = calcularHuella(solicitud.contratoId(), items);
            Garantia garantia = session.createQuery(
                            "from Garantia g where g.contrato.id = :contratoId", Garantia.class)
                    .setParameter("contratoId", solicitud.contratoId())
                    .uniqueResult();
            if (garantia == null) return null;
            List<MovimientoGarantia> existentes = session.createQuery("""
                            from MovimientoGarantia m
                            where m.liquidacionId = :liquidacionId
                            order by m.orden
                            """, MovimientoGarantia.class)
                    .setParameter("liquidacionId", solicitud.liquidacionId())
                    .getResultList();
            if (existentes.isEmpty()) return null;
            boolean coincide = existentes.stream().allMatch(m ->
                    garantia.getId().equals(m.getGarantiaId())
                            && hash.equals(m.getLiquidacionHash()));
            return coincide ? consultarEnSesion(session, solicitud.contratoId()) : null;
        });
    }

    private static List<ItemNormalizado> normalizarItems(List<ItemMovimiento> movimientos,
                                                         LocalDate fechaDefecto) {
        List<ItemNormalizado> normalizados = new ArrayList<>(movimientos.size());
        for (ItemMovimiento item : movimientos) {
            if (item == null) {
                throw new IllegalArgumentException("La liquidación contiene un movimiento vacío.");
            }
            String tipo = item.tipo() == null ? null : item.tipo().strip();
            if (tipo == null || !MovimientoGarantia.TIPOS.contains(tipo)) {
                throw new IllegalArgumentException(
                        "El tipo de movimiento debe ser Aplicacion, Deduccion o Devolucion.");
            }
            BigDecimal importe = item.importe();
            if (importe == null || importe.signum() <= 0 || importe.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException(
                        "El importe del movimiento debe ser mayor que cero y tener hasta dos decimales.");
            }
            importe = importe.setScale(2, RoundingMode.HALF_EVEN);
            if (item.concepto() == null || item.concepto().isBlank()) {
                throw new IllegalArgumentException(
                        "El concepto del movimiento de garantía es obligatorio.");
            }
            String concepto = item.concepto().strip();
            if (concepto.length() > 300) {
                throw new IllegalArgumentException(
                        "El concepto no puede superar 300 caracteres.");
            }
            LocalDate fecha = item.fechaMovimiento() == null ? fechaDefecto : item.fechaMovimiento();
            String metodo = item.metodo() == null || item.metodo().isBlank() ? null : item.metodo().strip();

            if (MovimientoGarantia.TIPO_APLICACION.equals(tipo)) {
                if (item.cargoId() == null) {
                    throw new IllegalArgumentException(
                            "ERR-061: La aplicación de garantía requiere seleccionar un cargo.");
                }
                if (metodo != null) {
                    throw new IllegalArgumentException(
                            "La aplicación de garantía no debe incluir método de devolución.");
                }
            } else if (MovimientoGarantia.TIPO_DEDUCCION.equals(tipo)) {
                if (item.cargoId() != null) {
                    throw new IllegalArgumentException("La deducción no debe asociarse a un cargo.");
                }
                if (metodo != null) {
                    throw new IllegalArgumentException(
                            "La deducción no debe incluir método de devolución.");
                }
            } else {
                if (item.cargoId() != null) {
                    throw new IllegalArgumentException("La devolución no debe asociarse a un cargo.");
                }
                if (metodo == null || !MovimientoGarantia.METODOS_DEVOLUCION.contains(metodo)) {
                    throw new IllegalArgumentException(
                            "El método de devolución debe ser Efectivo, Transferencia, Depósito bancario u Otro.");
                }
            }
            normalizados.add(new ItemNormalizado(tipo, item.cargoId(), importe, fecha, metodo, concepto));
        }
        return normalizados;
    }

    private static String calcularHuella(UUID contratoId, List<ItemNormalizado> items) {
        StringBuilder canonica = new StringBuilder(contratoId.toString());
        for (ItemNormalizado item : items) {
            canonica.append('|')
                    .append(item.tipo()).append(':')
                    .append(item.cargoId() == null ? "" : item.cargoId()).append(':')
                    .append(item.importe().toPlainString()).append(':')
                    .append(item.fechaMovimiento()).append(':')
                    .append(item.metodo() == null ? "" : item.metodo()).append(':')
                    .append(item.concepto());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonica.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular la huella de liquidación.", e);
        }
    }

    private static Contrato bloquearContrato(Session session, UUID contratoId) {
        Object fila = session.createNativeQuery("select id from contrato where id = :id for update")
                .setParameter("id", contratoId)
                .uniqueResult();
        return fila == null ? null : session.find(Contrato.class, contratoId);
    }

    private static Garantia bloquearGarantiaPorContrato(Session session, UUID contratoId) {
        UUID garantiaId = session.createNativeQuery(
                        "select id from garantia where contrato_id = :contratoId for update", UUID.class)
                .setParameter("contratoId", contratoId)
                .uniqueResult();
        return garantiaId == null ? null : session.find(Garantia.class, garantiaId);
    }

    private static Cargo bloquearCargo(Session session, UUID cargoId) {
        Object fila = session.createNativeQuery("select id from cargo where id = :id for update")
                .setParameter("id", cargoId)
                .uniqueResult();
        return fila == null ? null : session.find(Cargo.class, cargoId);
    }

    public record ItemMovimiento(
            String tipo,
            UUID cargoId,
            BigDecimal importe,
            LocalDate fechaMovimiento,
            String metodo,
            String concepto) {
    }

    public record SolicitudLiquidacion(
            UUID contratoId,
            UUID liquidacionId,
            List<ItemMovimiento> movimientos) {
    }

    private record ItemNormalizado(
            String tipo,
            UUID cargoId,
            BigDecimal importe,
            LocalDate fechaMovimiento,
            String metodo,
            String concepto) {
    }
}
