package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TablaReporteXlsx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.GastoPropiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Moneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTasa;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PatronLike;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Consultas de solo lectura y construcción de DTOs PDF/XLSX para los reportes
 * del catálogo REP-02 a REP-10 (M10).
 */
final class ConsultasReporteHelper {

    private static final String PLANTILLA_CARTERA_VENCIDA = "/reportes/cartera_vencida.jrxml";
    private static final String PLANTILLA_INGRESOS_GASTOS = "/reportes/ingresos_gastos.jrxml";
    private static final String PLANTILLA_OCUPACION = "/reportes/ocupacion.jrxml";
    private static final String PLANTILLA_CONTRATOS_POR_VENCER = "/reportes/contratos_por_vencer.jrxml";
    private static final String PLANTILLA_HISTORIAL_PAGOS = "/reportes/historial_pagos.jrxml";
    private static final String PLANTILLA_GARANTIAS_LIQUIDACION = "/reportes/garantias_liquidacion.jrxml";
    private static final String PLANTILLA_MANTENIMIENTO = "/reportes/mantenimiento.jrxml";
    private static final String PLANTILLA_TASAS_UTILIZADAS = "/reportes/tasas_utilizadas.jrxml";

    private static final DateTimeFormatter FORMATO_INSTANTE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(RelojService.ZONA_OPERATIVA);

    private ConsultasReporteHelper() {
    }

    static void verificarCancelacion(BooleanSupplier cancelado) {
        if (cancelado != null && cancelado.getAsBoolean()) {
            throw new OperacionCanceladaException();
        }
    }

    static String normalizarMoneda(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            return null;
        }
        return Moneda.desde(moneda.strip()).name();
    }

    static String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    static void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }
    }

    static EstadoCuentaResponse consultarEstadoCuenta(Session session, UUID contratoId, LocalDate fechaSolicitada) {
        LocalDate fechaCorte = fechaSolicitada == null ? new RelojService().leer(session).fecha() : fechaSolicitada;
        Contrato contrato = session.createQuery("""
                        from Contrato c
                        join fetch c.unidad u
                        join fetch u.propiedad
                        join fetch c.inquilino
                        left join fetch c.garantiaSeguimiento
                        where c.id = :id
                        """, Contrato.class)
                .setParameter("id", contratoId)
                .uniqueResult();
        if (contrato == null) {
            throw new IllegalArgumentException("No se encontró el contrato seleccionado.");
        }

        List<Cargo> cargos = session.createQuery("""
                        from Cargo c
                        where c.contratoId = :contratoId and c.fechaEmision <= :corte
                        order by c.fechaEmision, c.periodo, c.tipo
                        """, Cargo.class)
                .setParameter("contratoId", contratoId)
                .setParameter("corte", fechaCorte)
                .getResultList();
        List<Pago> listaPagos = session.createQuery("""
                        from Pago p
                        where p.contratoId = :contratoId and p.fechaPago <= :corte
                        order by p.fechaPago, p.id
                        """, Pago.class)
                .setParameter("contratoId", contratoId)
                .setParameter("corte", fechaCorte)
                .getResultList();

        Map<UUID, Pago> pagosPorId = new HashMap<>();
        for (Pago pago : listaPagos) {
            pagosPorId.put(pago.getId(), pago);
        }

        Instant finCorte = fechaCorte.plusDays(1).atStartOfDay(RelojService.ZONA_OPERATIVA).toInstant();
        Set<UUID> cargosVigentes = new HashSet<>();
        for (Cargo cargo : cargos) {
            if (!estaCargoAnuladoAlCorte(cargo, finCorte)) {
                cargosVigentes.add(cargo.getId());
            }
        }

        Map<UUID, BigDecimal> aplicadoPorCargo = new HashMap<>();
        Map<UUID, BigDecimal> aplicadoIncluidoPorPago = new HashMap<>();
        if (!listaPagos.isEmpty()) {
            List<AplicacionPago> aplicaciones = session.createQuery(
                            "from AplicacionPago a where a.pagoId in :pagos", AplicacionPago.class)
                    .setParameter("pagos", pagosPorId.keySet())
                    .getResultList();
            for (AplicacionPago app : aplicaciones) {
                Pago pago = pagosPorId.get(app.getPagoId());
                if (pago == null) continue;
                if (estaPagoAnuladoAlCorte(pago, finCorte)) continue;
                if (app.getRevertidaEn() != null && app.getRevertidaEn().isBefore(finCorte)) continue;
                if (app.getFechaAplicacion() != null && app.getFechaAplicacion().isAfter(fechaCorte)) continue;
                aplicadoPorCargo.merge(app.getCargoId(), app.getMonto(), BigDecimal::add);
                if (cargosVigentes.contains(app.getCargoId())) {
                    aplicadoIncluidoPorPago.merge(app.getPagoId(), app.getMonto(), BigDecimal::add);
                }
            }
        }
        if (!cargos.isEmpty()) {
            List<UUID> cargoIds = cargos.stream().map(Cargo::getId).toList();
            for (AplicacionPago app : session.createQuery("""
                            select a from AplicacionPago a, MovimientoGarantia m
                            where a.movimientoId = m.id
                              and a.cargoId in :cargos
                              and a.revertidaEn is null
                              and m.fechaMovimiento <= :corte
                            """, AplicacionPago.class)
                    .setParameter("cargos", cargoIds)
                    .setParameter("corte", fechaCorte)
                    .getResultList()) {
                aplicadoPorCargo.merge(app.getCargoId(), app.getMonto(), BigDecimal::add);
            }
        }

        Map<UUID, Long> recibos = consultarNumerosRecibo(session, pagosPorId.keySet());

        List<EstadoCuentaResponse.CargoEstadoCuenta> cargosDto = new ArrayList<>();
        BigDecimal totalCargos = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        for (Cargo cargo : cargos) {
            BigDecimal importe = nz(cargo.getImporte());
            BigDecimal aplicado = nz(aplicadoPorCargo.get(cargo.getId()));
            cargo.establecerEstadoDerivado(aplicado, fechaCorte);
            String estado = cargo.getEstado();
            BigDecimal saldo = cargo.saldoPendiente(aplicado);
            if (!"Anulado".equals(estado)) {
                totalCargos = totalCargos.add(importe);
                totalPagado = totalPagado.add(aplicado);
            }
            cargosDto.add(new EstadoCuentaResponse.CargoEstadoCuenta(
                    cargo.getId().toString(),
                    cargo.getTipo(),
                    cargo.getPeriodo().toString(),
                    cargo.getFechaEmision().toString(),
                    cargo.getVenceEl().toString(),
                    cargo.getConcepto(),
                    decimal(importe),
                    decimal(aplicado),
                    decimal(saldo),
                    estado));
        }

        List<EstadoCuentaResponse.PagoEstadoCuenta> pagosDto = new ArrayList<>();
        BigDecimal totalCredito = BigDecimal.ZERO;
        for (Pago pago : listaPagos) {
            boolean anulado = estaPagoAnuladoAlCorte(pago, finCorte);
            BigDecimal aplicado = anulado ? BigDecimal.ZERO : nz(aplicadoIncluidoPorPago.get(pago.getId()));
            BigDecimal credito = anulado ? BigDecimal.ZERO : nz(pago.getMontoContrato()).subtract(aplicado).max(BigDecimal.ZERO);
            if (!anulado) {
                totalCredito = totalCredito.add(credito);
            }
            pagosDto.add(new EstadoCuentaResponse.PagoEstadoCuenta(
                    pago.getId().toString(),
                    recibos.get(pago.getId()),
                    pago.getFechaPago().toString(),
                    decimal(pago.getMontoOriginal()),
                    pago.getMonedaOriginal(),
                    decimal(pago.getMontoContrato()),
                    pago.getMetodo(),
                    pago.getReferencia(),
                    pago.getEstado(),
                    decimal(aplicado),
                    decimal(credito)));
        }

        Garantia garantia = contrato.getGarantiaSeguimiento();
        EstadoCuentaResponse.GarantiaEstadoCuenta garantiaDto = garantia == null ? null
                : new EstadoCuentaResponse.GarantiaEstadoCuenta(
                decimal(contrato.getDepositoPactado()),
                decimal(garantia.getSaldoActual()));

        return new EstadoCuentaResponse(
                contrato.getId().toString(),
                fechaCorte.toString(),
                contrato.getMoneda(),
                decimal(contrato.getCanon()),
                cargosDto,
                pagosDto,
                new EstadoCuentaResponse.TotalesEstadoCuenta(
                        decimal(totalCargos),
                        decimal(totalPagado),
                        decimal(totalCargos.subtract(totalPagado)),
                        decimal(totalCredito)),
                garantiaDto);
    }

    static DocumentoReporte generarCarteraVencida(ReporteService service,
                                                  LocalDate fechaCorteSolicitada,
                                                  UUID propiedadId,
                                                  UUID contratoId,
                                                  String moneda,
                                                  BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        String monedaFiltro = normalizarMoneda(moneda);

        record DatosCartera(
                LocalDate corte,
                List<FilaCarteraVencida> filas,
                ResumenMonedaCartera usd,
                ResumenMonedaCartera nio,
                String filtros) {}

        DatosCartera datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            LocalDate corte = fechaCorteSolicitada != null ? fechaCorteSolicitada : new RelojService().leer(session).fecha();
            Instant finCorte = corte.plusDays(1).atStartOfDay(RelojService.ZONA_OPERATIVA).toInstant();

            StringBuilder hql = new StringBuilder("""
                    select c, con, i, u, prop from Cargo c
                    join c.contrato con
                    join con.inquilino i
                    join con.unidad u
                    join u.propiedad prop
                    where c.fechaEmision <= :corte
                    """);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("corte", corte);
            if (propiedadId != null) {
                hql.append(" and prop.id = :propiedadId");
                params.put("propiedadId", propiedadId);
            }
            if (contratoId != null) {
                hql.append(" and con.id = :contratoId");
                params.put("contratoId", contratoId);
            }
            if (monedaFiltro != null) {
                hql.append(" and con.moneda = :moneda");
                params.put("moneda", monedaFiltro);
            }
            hql.append(" order by c.venceEl asc, c.id asc");

            var query = session.createQuery(hql.toString(), Object[].class);
            params.forEach(query::setParameter);
            List<Object[]> registros = query.getResultList();

            List<UUID> cargoIds = registros.stream().map(r -> ((Cargo) r[0]).getId()).toList();
            Map<UUID, BigDecimal> aplicadoPorCargo = calcularAplicadoAlCorte(session, cargoIds, corte, finCorte);

            List<FilaCarteraVencida> filas = new ArrayList<>();
            ResumenMonedaCartera usd = new ResumenMonedaCartera("USD");
            ResumenMonedaCartera nio = new ResumenMonedaCartera("NIO");

            for (Object[] reg : registros) {
                Cargo cargo = (Cargo) reg[0];
                Contrato con = (Contrato) reg[1];
                Inquilino inq = (Inquilino) reg[2];
                Unidad uni = (Unidad) reg[3];
                Propiedad prop = (Propiedad) reg[4];

                if (estaCargoAnuladoAlCorte(cargo, finCorte)) continue;
                BigDecimal aplicado = nz(aplicadoPorCargo.get(cargo.getId()));
                cargo.establecerEstadoDerivado(aplicado, corte);
                BigDecimal saldo = cargo.saldoPendiente(aplicado);
                if (saldo.signum() <= 0) continue;

                long diasVencidos = Math.max(0L, ChronoUnit.DAYS.between(cargo.getVenceEl(), corte));
                String tramo = CobranzaService.clasificarTramo(diasVencidos);
                String contratoDesc = inq.getNombre() + " — " + uni.getCodigo();
                boolean esMora = "Mora".equalsIgnoreCase(cargo.getTipo());

                filas.add(new FilaCarteraVencida(
                        contratoDesc,
                        prop.getNombre(),
                        cargo.getTipo(),
                        cargo.getConcepto(),
                        cargo.getVenceEl().toString(),
                        cargo.getVenceEl(),
                        saldo,
                        con.getMoneda(),
                        diasVencidos,
                        tramo,
                        cargo.getEstado()));

                ResumenMonedaCartera acum = "NIO".equalsIgnoreCase(con.getMoneda()) ? nio : usd;
                acum.acumular(tramo, saldo, diasVencidos, esMora);
            }

            String filtros = construirFiltrosTexto(propiedadId != null ? "Propiedad filtrada" : null,
                    contratoId != null ? "Contrato filtrado" : null,
                    monedaFiltro != null ? "Moneda: " + monedaFiltro : "Moneda: Todas");
            return new DatosCartera(corte, List.copyOf(filas), usd, nio, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Cartera Vencida por Antigüedad";
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("FECHA_CORTE", datos.corte().toString());
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("RESUMEN_USD", datos.usd().formatearResumen());
        parametros.put("RESUMEN_NIO", datos.nio().formatearResumen());

        var jasperPrint = service.llenarReporte(PLANTILLA_CARTERA_VENCIDA, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Fecha de corte", datos.corte().toString());
        meta.put("Filtros", datos.filtros());
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Contrato / Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tipo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Concepto", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Vence", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Saldo pendiente", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Días vencidos", TablaReporteXlsx.Tipo.ENTERO),
                new TablaReporteXlsx.Columna("Tramo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getContrato(), f.getPropiedad(), f.getTipo(), f.getConcepto(),
                        f.getVenceFecha(), f.getSaldoPendiente(), f.getMoneda(),
                        f.getDiasVencidos(), f.getTramo(), f.getEstado()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = new ArrayList<>();
        datos.usd().agregarResumenes(resumenes);
        datos.nio().agregarResumenes(resumenes);

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-03", "Cartera vencida", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Cartera-Vencida-" + datos.corte() + ".pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarIngresosYGastos(ReporteService service,
                                                   LocalDate desde,
                                                   LocalDate hasta,
                                                   UUID propiedadId,
                                                   String moneda,
                                                   BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        Objects.requireNonNull(desde, "La fecha inicial es obligatoria.");
        Objects.requireNonNull(hasta, "La fecha final es obligatoria.");
        validarRangoFechas(desde, hasta);
        String monedaFiltro = normalizarMoneda(moneda);

        record DatosIngresosGastos(
                List<FilaIngresoGasto> filas,
                AcumuladorIngresos usd,
                AcumuladorIngresos nio,
                int pagosAnulados,
                String filtros) {}

        DatosIngresosGastos datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);

            StringBuilder hqlPagos = new StringBuilder("""
                    select p, c, i, u, prop from Pago p
                    join p.contrato c
                    join c.inquilino i
                    join c.unidad u
                    join u.propiedad prop
                    where p.fechaPago >= :desde and p.fechaPago <= :hasta
                    """);
            if (propiedadId != null) hqlPagos.append(" and prop.id = :propiedadId");
            if (monedaFiltro != null) hqlPagos.append(" and p.monedaContrato = :moneda");
            hqlPagos.append(" order by p.fechaPago asc, p.id asc");

            var qPagos = session.createQuery(hqlPagos.toString(), Object[].class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta);
            if (propiedadId != null) qPagos.setParameter("propiedadId", propiedadId);
            if (monedaFiltro != null) qPagos.setParameter("moneda", monedaFiltro);
            List<Object[]> pagos = qPagos.getResultList();

            Set<UUID> pagoIds = new HashSet<>();
            for (Object[] r : pagos) pagoIds.add(((Pago) r[0]).getId());

            Map<UUID, BigDecimal> rentaPorPago = new HashMap<>();
            if (!pagoIds.isEmpty()) {
                for (Object[] fila : session.createQuery("""
                                select a.pagoId, c.tipo, sum(a.monto)
                                from AplicacionPago a, Cargo c
                                where a.cargoId = c.id
                                  and a.pagoId in :pagoIds
                                  and a.revertidaEn is null
                                group by a.pagoId, c.tipo
                                """, Object[].class)
                        .setParameter("pagoIds", pagoIds)
                        .getResultList()) {
                    UUID pId = (UUID) fila[0];
                    String tipoCargo = (String) fila[1];
                    BigDecimal suma = nz((BigDecimal) fila[2]);
                    if ("Renta".equalsIgnoreCase(tipoCargo)) {
                        rentaPorPago.merge(pId, suma, BigDecimal::add);
                    }
                }
            }

            List<FilaIngresoGasto> filas = new ArrayList<>();
            AcumuladorIngresos usd = new AcumuladorIngresos("USD");
            AcumuladorIngresos nio = new AcumuladorIngresos("NIO");
            int anulados = 0;

            for (Object[] r : pagos) {
                Pago p = (Pago) r[0];
                Inquilino inq = (Inquilino) r[2];
                Unidad uni = (Unidad) r[3];
                Propiedad prop = (Propiedad) r[4];
                String mon = p.getMonedaContrato();
                AcumuladorIngresos acum = "NIO".equalsIgnoreCase(mon) ? nio : usd;
                String ref = p.getReferencia() != null && !p.getReferencia().isBlank() ? p.getReferencia() : p.getMetodo();

                if ("Anulado".equalsIgnoreCase(p.getEstado())) {
                    anulados++;
                    filas.add(new FilaIngresoGasto(
                            "Pago (Anulado)", p.getFechaPago().toString(), p.getFechaPago(),
                            prop.getNombre(), ref, inq.getNombre() + " — " + uni.getCodigo(),
                            mon, p.getMontoContrato(), "Anulado"));
                    continue;
                }

                BigDecimal totalPago = nz(p.getMontoContrato()).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal parteRenta = nz(rentaPorPago.get(p.getId())).min(totalPago).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal parteOtros = totalPago.subtract(parteRenta).setScale(2, RoundingMode.HALF_EVEN);

                if (parteRenta.signum() > 0) {
                    acum.sumarRenta(parteRenta);
                    filas.add(new FilaIngresoGasto(
                            "Renta", p.getFechaPago().toString(), p.getFechaPago(),
                            prop.getNombre(), ref, inq.getNombre() + " — " + uni.getCodigo(),
                            mon, parteRenta, p.getEstado()));
                }
                if (parteOtros.signum() > 0 || parteRenta.signum() == 0) {
                    acum.sumarOtros(parteOtros);
                    filas.add(new FilaIngresoGasto(
                            "Otros cobros", p.getFechaPago().toString(), p.getFechaPago(),
                            prop.getNombre(), ref, inq.getNombre() + " — " + uni.getCodigo() + " (Otros / Crédito)",
                            mon, parteOtros, p.getEstado()));
                }
            }

            StringBuilder hqlGastos = new StringBuilder("""
                    select g, prop from GastoPropiedad g
                    join g.propiedad prop
                    where g.fechaGasto >= :desde and g.fechaGasto <= :hasta
                    """);
            if (propiedadId != null) hqlGastos.append(" and g.propiedadId = :propiedadId");
            if (monedaFiltro != null) hqlGastos.append(" and g.moneda = :moneda");
            hqlGastos.append(" order by g.fechaGasto asc, g.id asc");

            var qGastos = session.createQuery(hqlGastos.toString(), Object[].class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta);
            if (propiedadId != null) qGastos.setParameter("propiedadId", propiedadId);
            if (monedaFiltro != null) qGastos.setParameter("moneda", monedaFiltro);

            for (Object[] r : qGastos.getResultList()) {
                GastoPropiedad g = (GastoPropiedad) r[0];
                Propiedad prop = (Propiedad) r[1];
                AcumuladorIngresos acum = "NIO".equalsIgnoreCase(g.getMoneda()) ? nio : usd;
                acum.sumarGasto(g.getImporte());
                filas.add(new FilaIngresoGasto(
                        "Gasto", g.getFechaGasto().toString(), g.getFechaGasto(),
                        prop.getNombre(), g.getSolicitudId() != null ? "Mantenimiento" : "General",
                        g.getConcepto(), g.getMoneda(), g.getImporte(), "Registrado"));
            }

            StringBuilder hqlGar = new StringBuilder("""
                    select m, c, i, u, prop from MovimientoGarantia m, Garantia g
                    join g.contrato c
                    join c.inquilino i
                    join c.unidad u
                    join u.propiedad prop
                    where m.garantiaId = g.id
                      and m.fechaMovimiento >= :desde
                      and m.fechaMovimiento <= :hasta
                    """);
            if (propiedadId != null) hqlGar.append(" and prop.id = :propiedadId");
            if (monedaFiltro != null) hqlGar.append(" and c.moneda = :moneda");
            hqlGar.append(" order by m.fechaMovimiento asc, m.orden asc");

            var qGar = session.createQuery(hqlGar.toString(), Object[].class)
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta);
            if (propiedadId != null) qGar.setParameter("propiedadId", propiedadId);
            if (monedaFiltro != null) qGar.setParameter("moneda", monedaFiltro);

            for (Object[] r : qGar.getResultList()) {
                MovimientoGarantia m = (MovimientoGarantia) r[0];
                Contrato c = (Contrato) r[1];
                Inquilino i = (Inquilino) r[2];
                AcumuladorIngresos acum = "NIO".equalsIgnoreCase(c.getMoneda()) ? nio : usd;
                acum.sumarGarantia(m.getImporte());
                filas.add(new FilaIngresoGasto(
                        "Garantía (separada)", m.getFechaMovimiento().toString(), m.getFechaMovimiento(),
                        ((Propiedad) r[4]).getNombre(), m.getTipo(),
                        i.getNombre() + " · " + m.getConcepto(), c.getMoneda(), m.getImporte(), "Garantía"));
            }

            String filtros = construirFiltrosTexto(propiedadId != null ? "Propiedad filtrada" : null,
                    monedaFiltro != null ? "Moneda: " + monedaFiltro : "Moneda: Todas",
                    "Anulados: " + anulados);
            return new DatosIngresosGastos(List.copyOf(filas), usd, nio, anulados, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Ingresos, Gastos y Garantías";
        String periodoTexto = desde + " a " + hasta;
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("PERIODO_TEXTO", periodoTexto);
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("RESUMEN_USD", datos.usd().resumenLinea());
        parametros.put("RESUMEN_NIO", datos.nio().resumenLinea());

        var jasperPrint = service.llenarReporte(PLANTILLA_INGRESOS_GASTOS, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Período", periodoTexto);
        meta.put("Filtros", datos.filtros());
        meta.put("Pagos anulados (excluidos de neto)", Integer.toString(datos.pagosAnulados()));
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Sección", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Referencia", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Detalle / Concepto", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Importe", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getSeccion(), f.getFechaValor(), f.getPropiedad(), f.getReferencia(),
                        f.getDetalle(), f.getMoneda(), f.getImporte(), f.getEstado()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = new ArrayList<>();
        datos.usd().agregarResumenes(resumenes);
        datos.nio().agregarResumenes(resumenes);

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-04", "Ingresos y gastos", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Ingresos-Gastos-" + desde + "_" + hasta + ".pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarOcupacion(ReporteService service,
                                             UUID propiedadId,
                                             String tipoUnidad,
                                             BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        String tipoFiltro = normalizarTexto(tipoUnidad);

        record DatosOcupacion(
                Instant instante,
                List<FilaOcupacion> filas,
                int activas,
                int ocupadas,
                int disponibles,
                int enMantenimiento,
                int inactivas,
                double porcentaje,
                String filtros) {}

        DatosOcupacion datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            Instant instante = new RelojService().leer(session).instante();

            StringBuilder hql = new StringBuilder("""
                    select u, prop from Unidad u
                    join u.propiedad prop
                    where 1 = 1
                    """);
            if (propiedadId != null) hql.append(" and prop.id = :propiedadId");
            if (tipoFiltro != null) hql.append(" and lower(u.tipo) = :tipo");
            hql.append(" order by prop.nombre asc, u.codigo asc");

            var query = session.createQuery(hql.toString(), Object[].class);
            if (propiedadId != null) query.setParameter("propiedadId", propiedadId);
            if (tipoFiltro != null) query.setParameter("tipo", tipoFiltro.toLowerCase(Locale.ROOT));
            List<Object[]> registros = query.getResultList();

            List<UUID> unidadIds = registros.stream().map(r -> ((Unidad) r[0]).getId()).toList();
            Map<UUID, String> contratoActivoPorUnidad = new HashMap<>();
            if (!unidadIds.isEmpty()) {
                for (Object[] cRow : session.createQuery("""
                                select c.unidadId, i.nombre, c.estado, c.fin
                                from Contrato c join c.inquilino i
                                where c.unidadId in :unidadIds
                                  and c.estado in ('Vigente', 'Por vencer', 'Vencido')
                                """, Object[].class)
                        .setParameter("unidadIds", unidadIds)
                        .getResultList()) {
                    contratoActivoPorUnidad.put((UUID) cRow[0], cRow[1] + " (" + cRow[2] + " · fin " + cRow[3] + ")");
                }
            }

            List<FilaOcupacion> filas = new ArrayList<>();
            int activas = 0;
            int ocupadas = 0;
            int disponibles = 0;
            int enMantenimiento = 0;
            int inactivas = 0;

            for (Object[] reg : registros) {
                Unidad u = (Unidad) reg[0];
                Propiedad p = (Propiedad) reg[1];
                String estado = u.getEstado() == null ? "" : u.getEstado().strip();
                boolean unidadInactiva = !p.isActiva() || "Inactiva".equalsIgnoreCase(estado);

                if (unidadInactiva) {
                    inactivas++;
                } else {
                    activas++;
                    if ("Ocupada".equalsIgnoreCase(estado) || "Ocupado".equalsIgnoreCase(estado)) {
                        ocupadas++;
                    } else if ("En mantenimiento".equalsIgnoreCase(estado)) {
                        enMantenimiento++;
                    } else {
                        disponibles++;
                    }
                }

                String contratoTexto = contratoActivoPorUnidad.getOrDefault(u.getId(), "Sin contrato activo");
                filas.add(new FilaOcupacion(
                        p.getNombre(),
                        u.getCodigo(),
                        u.getTipo(),
                        unidadInactiva ? estado + " (Excluida)" : estado,
                        contratoTexto,
                        u.getMoneda(),
                        u.getCanonReferencia()));
            }

            double porcentaje = activas == 0 ? 0.0 : (ocupadas * 100.0) / activas;
            String filtros = construirFiltrosTexto(propiedadId != null ? "Propiedad filtrada" : "Propiedad: Todas",
                    tipoFiltro != null ? "Tipo: " + tipoFiltro : "Tipo: Todos");
            return new DatosOcupacion(instante, List.copyOf(filas), activas, ocupadas,
                    disponibles, enMantenimiento, inactivas, porcentaje, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Ocupación Actual por Propiedad";
        String instanteTexto = FORMATO_INSTANTE.format(datos.instante());
        String ocupacionResumen = String.format(Locale.ROOT, "Ocupación: %d / %d (%.1f%%)",
                datos.ocupadas(), datos.activas(), datos.porcentaje());
        String desglose = String.format(Locale.ROOT,
                "Activas (denominador): %d · Ocupadas: %d · Disponibles: %d · En mantenimiento: %d · Inactivas (fuera): %d",
                datos.activas(), datos.ocupadas(), datos.disponibles(), datos.enMantenimiento(), datos.inactivas());

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("INSTANTE_GENERACION", instanteTexto);
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("OCUPACION_RESUMEN", ocupacionResumen);
        parametros.put("DESGLOSE_ESTADOS", desglose);

        var jasperPrint = service.llenarReporte(PLANTILLA_OCUPACION, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Instante de generación", instanteTexto);
        meta.put("Filtros", datos.filtros());
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Código unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tipo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Inquilino / Contrato activo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Canon referencia", TablaReporteXlsx.Tipo.MONTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getPropiedad(), f.getCodigoUnidad(), f.getTipoUnidad(), f.getEstadoUnidad(),
                        f.getInquilinoOContrato(), f.getMoneda(), f.getCanonReferencia()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.texto("Ocupadas / Activas", datos.ocupadas() + " / " + datos.activas()),
                TablaReporteXlsx.Resumen.texto("Porcentaje de ocupación",
                        String.format(Locale.ROOT, "%.1f%%", datos.porcentaje())),
                TablaReporteXlsx.Resumen.texto("Disponibles", Integer.toString(datos.disponibles())),
                TablaReporteXlsx.Resumen.texto("En mantenimiento (en denominador)", Integer.toString(datos.enMantenimiento())),
                TablaReporteXlsx.Resumen.texto("Inactivas (excluidas del denominador)", Integer.toString(datos.inactivas()))
        );

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-05", "Ocupación", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Ocupacion.pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarContratosPorVencer(ReporteService service,
                                                      LocalDate fechaBaseSolicitada,
                                                      Integer umbralSolicitado,
                                                      UUID propiedadId,
                                                      String moneda,
                                                      BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        if (umbralSolicitado != null && umbralSolicitado < 0) {
            throw new IllegalArgumentException("El umbral de días no puede ser negativo.");
        }
        String monedaFiltro = normalizarMoneda(moneda);

        record DatosVencer(
                LocalDate fechaBase,
                int umbralDias,
                List<FilaContratoPorVencer> filas,
                BigDecimal totalUsd,
                BigDecimal totalNio,
                String filtros) {}

        DatosVencer datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            RelojService.TiempoOperativo tiempo = new RelojService().leer(session);
            LocalDate base = fechaBaseSolicitada != null ? fechaBaseSolicitada : tiempo.fecha();
            int umbral = umbralSolicitado != null ? umbralSolicitado : leerUmbralVencimiento(session, tiempo.instante());
            LocalDate limite = base.plusDays(umbral);

            StringBuilder hql = new StringBuilder("""
                    select c, i, u, prop from Contrato c
                    join c.inquilino i
                    join c.unidad u
                    join u.propiedad prop
                    where c.estado in ('Vigente', 'Por vencer', 'Vencido')
                      and c.fin >= :base
                      and c.fin <= :limite
                    """);
            if (propiedadId != null) hql.append(" and prop.id = :propiedadId");
            if (monedaFiltro != null) hql.append(" and c.moneda = :moneda");
            hql.append(" order by c.fin asc, c.id asc");

            var query = session.createQuery(hql.toString(), Object[].class)
                    .setParameter("base", base)
                    .setParameter("limite", limite);
            if (propiedadId != null) query.setParameter("propiedadId", propiedadId);
            if (monedaFiltro != null) query.setParameter("moneda", monedaFiltro);

            List<FilaContratoPorVencer> filas = new ArrayList<>();
            BigDecimal canonUsd = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal canonNio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            for (Object[] reg : query.getResultList()) {
                Contrato c = (Contrato) reg[0];
                Inquilino i = (Inquilino) reg[1];
                Unidad u = (Unidad) reg[2];
                Propiedad p = (Propiedad) reg[3];

                long dias = ChronoUnit.DAYS.between(base, c.getFin());
                filas.add(new FilaContratoPorVencer(
                        i.getNombre(), p.getNombre(), u.getCodigo(), c.getEstado(),
                        c.getInicio().toString(), c.getFin().toString(),
                        c.getInicio(), c.getFin(), dias, c.getMoneda(), c.getCanon()));

                if ("USD".equalsIgnoreCase(c.getMoneda())) {
                    canonUsd = canonUsd.add(nz(c.getCanon())).setScale(2, RoundingMode.HALF_EVEN);
                } else if ("NIO".equalsIgnoreCase(c.getMoneda())) {
                    canonNio = canonNio.add(nz(c.getCanon())).setScale(2, RoundingMode.HALF_EVEN);
                }
            }
            String filtros = construirFiltrosTexto(propiedadId != null ? "Propiedad filtrada" : null,
                    monedaFiltro != null ? "Moneda: " + monedaFiltro : "Moneda: Todas");
            return new DatosVencer(base, umbral, List.copyOf(filas), canonUsd, canonNio, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Contratos por Vencer";
        String resumen = String.format(Locale.ROOT,
                "Contratos por vencer: %d   |   Canon mensual USD: %s USD   |   Canon mensual NIO: %s NIO",
                datos.filas().size(),
                ReporteService.formatearMontoStatic(datos.totalUsd()),
                ReporteService.formatearMontoStatic(datos.totalNio()));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("FECHA_BASE", datos.fechaBase().toString());
        parametros.put("UMBRAL_DIAS", datos.umbralDias());
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("RESUMEN_TOTALES", resumen);

        var jasperPrint = service.llenarReporte(PLANTILLA_CONTRATOS_POR_VENCER, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Fecha base", datos.fechaBase().toString());
        meta.put("Umbral (días)", Integer.toString(datos.umbralDias()));
        meta.put("Filtros", datos.filtros());
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Inicio", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Fin", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Días restantes", TablaReporteXlsx.Tipo.ENTERO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Canon", TablaReporteXlsx.Tipo.MONTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getInquilino(), f.getPropiedad(), f.getUnidad(), f.getEstado(),
                        f.getInicioFecha(), f.getFinFecha(), f.getDiasRestantes(), f.getMoneda(), f.getCanon()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.texto("Total contratos por vencer", Integer.toString(datos.filas().size())),
                TablaReporteXlsx.Resumen.numerico("Canon mensual expuesto USD", "USD", datos.totalUsd()),
                TablaReporteXlsx.Resumen.numerico("Canon mensual expuesto NIO", "NIO", datos.totalNio())
        );

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-06", "Contratos por vencer", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Contratos-Por-Vencer-" + datos.fechaBase() + ".pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarHistorialPagos(ReporteService service,
                                                  UUID inquilinoId,
                                                  UUID contratoId,
                                                  LocalDate desde,
                                                  LocalDate hasta,
                                                  String moneda,
                                                  BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        validarRangoFechas(desde, hasta);
        String monedaFiltro = normalizarMoneda(moneda);

        record DatosHistorial(
                List<FilaHistorialPago> filas,
                BigDecimal netoUsd,
                BigDecimal netoNio,
                int anulados,
                String filtros) {}

        DatosHistorial datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);

            StringBuilder hql = new StringBuilder("""
                    select p, c, i, u, prop from Pago p
                    join p.contrato c
                    join c.inquilino i
                    join c.unidad u
                    join u.propiedad prop
                    where 1 = 1
                    """);
            Map<String, Object> params = new LinkedHashMap<>();
            if (inquilinoId != null) {
                hql.append(" and i.id = :inquilinoId");
                params.put("inquilinoId", inquilinoId);
            }
            if (contratoId != null) {
                hql.append(" and c.id = :contratoId");
                params.put("contratoId", contratoId);
            }
            if (desde != null) {
                hql.append(" and p.fechaPago >= :desde");
                params.put("desde", desde);
            }
            if (hasta != null) {
                hql.append(" and p.fechaPago <= :hasta");
                params.put("hasta", hasta);
            }
            if (monedaFiltro != null) {
                hql.append(" and p.monedaContrato = :moneda");
                params.put("moneda", monedaFiltro);
            }
            hql.append(" order by p.fechaPago desc, p.creadoEn desc");

            var query = session.createQuery(hql.toString(), Object[].class);
            params.forEach(query::setParameter);
            List<Object[]> registros = query.getResultList();

            Set<UUID> pagoIds = new HashSet<>();
            for (Object[] r : registros) pagoIds.add(((Pago) r[0]).getId());
            Map<UUID, Long> recibos = consultarNumerosRecibo(session, pagoIds);

            List<FilaHistorialPago> filas = new ArrayList<>();
            BigDecimal netoUsd = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal netoNio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            int anulados = 0;

            for (Object[] r : registros) {
                Pago p = (Pago) r[0];
                Inquilino i = (Inquilino) r[2];
                Unidad u = (Unidad) r[3];
                Propiedad prop = (Propiedad) r[4];
                Long rec = recibos.get(p.getId());
                String recTexto = rec != null ? "#" + rec : "—";
                boolean anulado = "Anulado".equalsIgnoreCase(p.getEstado());
                if (anulado) {
                    anulados++;
                } else if ("USD".equalsIgnoreCase(p.getMonedaContrato())) {
                    netoUsd = netoUsd.add(nz(p.getMontoContrato())).setScale(2, RoundingMode.HALF_EVEN);
                } else if ("NIO".equalsIgnoreCase(p.getMonedaContrato())) {
                    netoNio = netoNio.add(nz(p.getMontoContrato())).setScale(2, RoundingMode.HALF_EVEN);
                }

                filas.add(new FilaHistorialPago(
                        recTexto,
                        p.getFechaPago().toString(),
                        p.getFechaPago(),
                        i.getNombre(),
                        prop.getNombre() + " — " + u.getCodigo(),
                        p.getMontoOriginal(),
                        p.getMonedaOriginal(),
                        p.getMontoContrato(),
                        p.getMonedaContrato(),
                        ReporteService.formatearMontoStatic(p.getMontoOriginal()) + " " + p.getMonedaOriginal(),
                        ReporteService.formatearMontoStatic(p.getMontoContrato()) + " " + p.getMonedaContrato(),
                        p.getMetodo(),
                        p.getReferencia() == null ? "—" : p.getReferencia(),
                        p.getEstado(),
                        p.getMotivoAnulacion() == null ? "" : p.getMotivoAnulacion()));
            }

            String filtros = construirFiltrosTexto(
                    inquilinoId != null ? "Inquilino filtrado" : null,
                    contratoId != null ? "Contrato filtrado" : null,
                    desde != null || hasta != null ? "Período: " + (desde != null ? desde : "…") + " a " + (hasta != null ? hasta : "…") : "Período: Todos",
                    monedaFiltro != null ? "Moneda: " + monedaFiltro : "Moneda: Todas");
            return new DatosHistorial(List.copyOf(filas), netoUsd, netoNio, anulados, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Historial de Pagos del Inquilino";
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("TOTAL_REGISTROS", datos.filas().size());
        parametros.put("PAGOS_ANULADOS", datos.anulados());
        parametros.put("TOTAL_NETO_USD", ReporteService.formatearMontoStatic(datos.netoUsd()));
        parametros.put("TOTAL_NETO_NIO", ReporteService.formatearMontoStatic(datos.netoNio()));

        var jasperPrint = service.llenarReporte(PLANTILLA_HISTORIAL_PAGOS, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Filtros", datos.filtros());
        meta.put("Total registros", Integer.toString(datos.filas().size()));
        meta.put("Pagos anulados", Integer.toString(datos.anulados()));
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Recibo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha pago", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Propiedad / Unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Monto original", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda original", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Monto contrato", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Método", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Referencia", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Motivo anulación", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getNumeroRecibo(), f.getFechaValor(), f.getInquilino(), f.getContrato(),
                        f.getMontoOriginal(), f.getMonedaOriginal(), f.getMontoContrato(), f.getMonedaContrato(),
                        f.getMetodo(), f.getReferencia(), f.getEstado(), f.getMotivoAnulacion()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Total neto USD (excluye anulados)", "USD", datos.netoUsd()),
                TablaReporteXlsx.Resumen.numerico("Total neto NIO (excluye anulados)", "NIO", datos.netoNio()),
                TablaReporteXlsx.Resumen.texto("Pagos anulados identificados", Integer.toString(datos.anulados()))
        );

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-07", "Historial pagos", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Historial-Pagos.pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarLiquidacionGarantias(ReporteService service,
                                                        UUID contratoId,
                                                        LocalDate fechaCorteSolicitada,
                                                        String moneda,
                                                        BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        String monedaFiltro = normalizarMoneda(moneda);

        record DatosGarantias(
                LocalDate corte,
                List<FilaGarantiaLiquidacion> filas,
                AcumuladorGarantia usd,
                AcumuladorGarantia nio,
                String filtros) {}

        DatosGarantias datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            LocalDate corte = fechaCorteSolicitada != null ? fechaCorteSolicitada : new RelojService().leer(session).fecha();

            StringBuilder hql = new StringBuilder("""
                    select g, c, i, u, prop from Garantia g
                    join g.contrato c
                    join c.inquilino i
                    join c.unidad u
                    join u.propiedad prop
                    where c.inicio <= :corte
                    """);
            if (contratoId != null) hql.append(" and c.id = :contratoId");
            if (monedaFiltro != null) hql.append(" and c.moneda = :moneda");
            hql.append(" order by c.inicio desc, c.id asc");

            var q = session.createQuery(hql.toString(), Object[].class).setParameter("corte", corte);
            if (contratoId != null) q.setParameter("contratoId", contratoId);
            if (monedaFiltro != null) q.setParameter("moneda", monedaFiltro);
            List<Object[]> garantias = q.getResultList();

            List<UUID> garantiaIds = garantias.stream().map(r -> ((Garantia) r[0]).getId()).toList();
            Map<UUID, List<MovimientoGarantia>> movsPorGarantia = new HashMap<>();
            if (!garantiaIds.isEmpty()) {
                for (MovimientoGarantia mov : session.createQuery("""
                                from MovimientoGarantia m
                                where m.garantiaId in :ids
                                  and m.fechaMovimiento <= :corte
                                order by m.fechaMovimiento asc, m.orden asc
                                """, MovimientoGarantia.class)
                        .setParameter("ids", garantiaIds)
                        .setParameter("corte", corte)
                        .getResultList()) {
                    movsPorGarantia.computeIfAbsent(mov.getGarantiaId(), k -> new ArrayList<>()).add(mov);
                }
            }

            List<FilaGarantiaLiquidacion> filas = new ArrayList<>();
            AcumuladorGarantia usd = new AcumuladorGarantia("USD");
            AcumuladorGarantia nio = new AcumuladorGarantia("NIO");

            for (Object[] r : garantias) {
                Garantia g = (Garantia) r[0];
                Contrato c = (Contrato) r[1];
                Inquilino i = (Inquilino) r[2];
                Unidad u = (Unidad) r[3];
                String contratoDesc = i.getNombre() + " — " + u.getCodigo();
                String mon = c.getMoneda();
                BigDecimal inicial = nz(g.getMontoInicial()).setScale(2, RoundingMode.HALF_EVEN);

                List<MovimientoGarantia> movs = movsPorGarantia.getOrDefault(g.getId(), List.of());
                BigDecimal aplicado = BigDecimal.ZERO.setScale(2);
                BigDecimal deducido = BigDecimal.ZERO.setScale(2);
                BigDecimal devuelto = BigDecimal.ZERO.setScale(2);
                for (MovimientoGarantia m : movs) {
                    if (MovimientoGarantia.TIPO_APLICACION.equals(m.getTipo())) {
                        aplicado = aplicado.add(m.getImporte());
                    } else if (MovimientoGarantia.TIPO_DEDUCCION.equals(m.getTipo())) {
                        deducido = deducido.add(m.getImporte());
                    } else if (MovimientoGarantia.TIPO_DEVOLUCION.equals(m.getTipo())) {
                        devuelto = devuelto.add(m.getImporte());
                    }
                }
                BigDecimal totalSalidas = aplicado.add(deducido).add(devuelto).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal remanente = inicial.subtract(totalSalidas).setScale(2, RoundingMode.HALF_EVEN);

                AcumuladorGarantia acum = "NIO".equalsIgnoreCase(mon) ? nio : usd;
                acum.acumular(inicial, aplicado, deducido, devuelto, remanente);

                String estadoGarantia = Garantia.derivarEstado(inicial, remanente, devuelto);
                filas.add(new FilaGarantiaLiquidacion(
                        contratoDesc, c.getInicio().toString(), c.getInicio(),
                        "Resumen (" + estadoGarantia + ")", "Depósito inicial pactado",
                        "—", mon, inicial, totalSalidas, remanente));

                BigDecimal saldoCorriente = inicial;
                for (MovimientoGarantia m : movs) {
                    saldoCorriente = saldoCorriente.subtract(m.getImporte()).setScale(2, RoundingMode.HALF_EVEN);
                    filas.add(new FilaGarantiaLiquidacion(
                            contratoDesc, m.getFechaMovimiento().toString(), m.getFechaMovimiento(),
                            m.getTipo(), m.getConcepto(),
                            m.getMetodo() == null ? "—" : m.getMetodo(),
                            mon, BigDecimal.ZERO.setScale(2), m.getImporte(), saldoCorriente));
                }
            }

            String filtros = construirFiltrosTexto(
                    contratoId != null ? "Contrato filtrado" : "Contratos: Todos",
                    monedaFiltro != null ? "Moneda: " + monedaFiltro : "Moneda: Todas");
            return new DatosGarantias(corte, List.copyOf(filas), usd, nio, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Liquidación de Depósitos de Garantía";
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("FECHA_CORTE", datos.corte().toString());
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("RESUMEN_USD", datos.usd().resumenLinea());
        parametros.put("RESUMEN_NIO", datos.nio().resumenLinea());

        var jasperPrint = service.llenarReporte(PLANTILLA_GARANTIAS_LIQUIDACION, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Fecha de corte", datos.corte().toString());
        meta.put("Filtros", datos.filtros());
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Contrato / Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Movimiento", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Concepto", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Método", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Inicial", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Salida", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Remanente", TablaReporteXlsx.Tipo.MONTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getContrato(), f.getFechaValor(), f.getTipoRegistro(), f.getConcepto(),
                        f.getMetodo(), f.getMoneda(), f.getInicial(), f.getSalida(), f.getRemanente()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = new ArrayList<>();
        datos.usd().agregarResumenes(resumenes);
        datos.nio().agregarResumenes(resumenes);

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-08", "Garantías", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Garantias-Liquidacion-" + datos.corte() + ".pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarMantenimiento(ReporteService service,
                                                 UUID propiedadId,
                                                 UUID unidadId,
                                                 LocalDate desde,
                                                 LocalDate hasta,
                                                 String estado,
                                                 String prioridad,
                                                 String moneda,
                                                 BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        validarRangoFechas(desde, hasta);
        String estadoFiltro = normalizarTexto(estado);
        String prioridadFiltro = normalizarTexto(prioridad);
        String monedaFiltro = normalizarMoneda(moneda);

        record DatosMantenimiento(
                List<FilaMantenimiento> filas,
                BigDecimal totalUsd,
                BigDecimal totalNio,
                int abiertas,
                int enProceso,
                int resueltas,
                int canceladas,
                String filtros) {}

        DatosMantenimiento datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);

            StringBuilder hqlSol = new StringBuilder("""
                    select s, u, prop from SolicitudMantenimiento s
                    join s.unidad u
                    join u.propiedad prop
                    where 1 = 1
                    """);
            Map<String, Object> paramsSol = new LinkedHashMap<>();
            if (propiedadId != null) {
                hqlSol.append(" and u.propiedadId = :propiedadId");
                paramsSol.put("propiedadId", propiedadId);
            }
            if (unidadId != null) {
                hqlSol.append(" and s.unidadId = :unidadId");
                paramsSol.put("unidadId", unidadId);
            }
            if (desde != null) {
                hqlSol.append(" and s.abiertaEl >= :desde");
                paramsSol.put("desde", desde);
            }
            if (hasta != null) {
                hqlSol.append(" and s.abiertaEl <= :hasta");
                paramsSol.put("hasta", hasta);
            }
            if (estadoFiltro != null) {
                hqlSol.append(" and s.estado = :estado");
                paramsSol.put("estado", estadoFiltro);
            }
            if (prioridadFiltro != null) {
                hqlSol.append(" and s.prioridad = :prioridad");
                paramsSol.put("prioridad", prioridadFiltro);
            }
            hqlSol.append(" order by s.abiertaEl desc, s.creadoEn desc");

            var qSol = session.createQuery(hqlSol.toString(), Object[].class);
            paramsSol.forEach(qSol::setParameter);
            List<Object[]> solicitudes = qSol.getResultList();

            List<UUID> solicitudIds = solicitudes.stream()
                    .map(r -> ((SolicitudMantenimiento) r[0]).getId()).toList();
            Map<UUID, BigDecimal[]> gastosPorSolicitud = new HashMap<>();
            if (!solicitudIds.isEmpty()) {
                StringBuilder hqlGS = new StringBuilder("""
                        select g.solicitudId, g.moneda, sum(g.importe)
                        from GastoPropiedad g
                        where g.solicitudId in :ids
                        """);
                if (desde != null) hqlGS.append(" and g.fechaGasto >= :desde");
                if (hasta != null) hqlGS.append(" and g.fechaGasto <= :hasta");
                if (monedaFiltro != null) hqlGS.append(" and g.moneda = :moneda");
                hqlGS.append(" group by g.solicitudId, g.moneda");

                var qGS = session.createQuery(hqlGS.toString(), Object[].class)
                        .setParameter("ids", solicitudIds);
                if (desde != null) qGS.setParameter("desde", desde);
                if (hasta != null) qGS.setParameter("hasta", hasta);
                if (monedaFiltro != null) qGS.setParameter("moneda", monedaFiltro);
                for (Object[] fg : qGS.getResultList()) {
                    UUID sId = (UUID) fg[0];
                    String mon = (String) fg[1];
                    BigDecimal sum = nz((BigDecimal) fg[2]).setScale(2, RoundingMode.HALF_EVEN);
                    BigDecimal[] arr = gastosPorSolicitud.computeIfAbsent(sId,
                            k -> new BigDecimal[]{BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)});
                    if ("USD".equals(mon)) arr[0] = sum;
                    else if ("NIO".equals(mon)) arr[1] = sum;
                }
            }

            List<FilaMantenimiento> filas = new ArrayList<>();
            BigDecimal totalUsd = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalNio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            int abiertas = 0;
            int enProceso = 0;
            int resueltas = 0;
            int canceladas = 0;

            for (Object[] r : solicitudes) {
                SolicitudMantenimiento s = (SolicitudMantenimiento) r[0];
                Unidad u = (Unidad) r[1];
                Propiedad p = (Propiedad) r[2];
                BigDecimal[] costos = gastosPorSolicitud.getOrDefault(s.getId(),
                        new BigDecimal[]{BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)});

                if (SolicitudMantenimiento.ESTADO_ABIERTA.equals(s.getEstado())) abiertas++;
                else if (SolicitudMantenimiento.ESTADO_EN_PROCESO.equals(s.getEstado())) enProceso++;
                else if (SolicitudMantenimiento.ESTADO_RESUELTA.equals(s.getEstado())) resueltas++;
                else if (SolicitudMantenimiento.ESTADO_CANCELADA.equals(s.getEstado())) canceladas++;

                totalUsd = totalUsd.add(costos[0]).setScale(2, RoundingMode.HALF_EVEN);
                totalNio = totalNio.add(costos[1]).setScale(2, RoundingMode.HALF_EVEN);

                filas.add(new FilaMantenimiento(
                        "Solicitud",
                        p.getNombre(),
                        u.getCodigo(),
                        s.getAbiertaEl().toString(),
                        s.getAbiertaEl(),
                        s.getPrioridad(),
                        s.getEstado(),
                        s.getResponsable() == null ? "—" : s.getResponsable(),
                        s.getDescripcion(),
                        costos[0],
                        costos[1]));
            }

            if (unidadId == null && estadoFiltro == null && prioridadFiltro == null) {
                StringBuilder hqlGen = new StringBuilder("""
                        select g, prop from GastoPropiedad g
                        join g.propiedad prop
                        where g.solicitudId is null
                        """);
                if (propiedadId != null) hqlGen.append(" and g.propiedadId = :propiedadId");
                if (desde != null) hqlGen.append(" and g.fechaGasto >= :desde");
                if (hasta != null) hqlGen.append(" and g.fechaGasto <= :hasta");
                if (monedaFiltro != null) hqlGen.append(" and g.moneda = :moneda");
                hqlGen.append(" order by g.fechaGasto desc, g.creadoEn desc");

                var qGen = session.createQuery(hqlGen.toString(), Object[].class);
                if (propiedadId != null) qGen.setParameter("propiedadId", propiedadId);
                if (desde != null) qGen.setParameter("desde", desde);
                if (hasta != null) qGen.setParameter("hasta", hasta);
                if (monedaFiltro != null) qGen.setParameter("moneda", monedaFiltro);

                for (Object[] r : qGen.getResultList()) {
                    GastoPropiedad g = (GastoPropiedad) r[0];
                    Propiedad p = (Propiedad) r[1];
                    BigDecimal usd = "USD".equals(g.getMoneda()) ? g.getImporte() : BigDecimal.ZERO.setScale(2);
                    BigDecimal nio = "NIO".equals(g.getMoneda()) ? g.getImporte() : BigDecimal.ZERO.setScale(2);
                    totalUsd = totalUsd.add(usd).setScale(2, RoundingMode.HALF_EVEN);
                    totalNio = totalNio.add(nio).setScale(2, RoundingMode.HALF_EVEN);
                    filas.add(new FilaMantenimiento(
                            "Gasto",
                            p.getNombre(),
                            "General",
                            g.getFechaGasto().toString(),
                            g.getFechaGasto(),
                            "—",
                            "Registrado",
                            "—",
                            g.getConcepto(),
                            usd,
                            nio));
                }
            }

            String filtros = construirFiltrosTexto(
                    propiedadId != null ? "Propiedad filtrada" : null,
                    unidadId != null ? "Unidad filtrada" : null,
                    desde != null || hasta != null ? "Período: " + (desde != null ? desde : "…") + " a " + (hasta != null ? hasta : "…") : "Período: Todos",
                    estadoFiltro != null ? "Estado: " + estadoFiltro : null,
                    prioridadFiltro != null ? "Prioridad: " + prioridadFiltro : null,
                    monedaFiltro != null ? "Moneda: " + monedaFiltro : null);
            return new DatosMantenimiento(List.copyOf(filas), totalUsd, totalNio,
                    abiertas, enProceso, resueltas, canceladas, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Reporte de Mantenimiento y Gastos";
        String conteo = String.format(Locale.ROOT, "Abiertas: %d · En proceso: %d · Resueltas: %d · Canceladas: %d",
                datos.abiertas(), datos.enProceso(), datos.resueltas(), datos.canceladas());

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("CONTEO_SOLICITUDES", conteo);
        parametros.put("TOTAL_COSTO_USD", ReporteService.formatearMontoStatic(datos.totalUsd()));
        parametros.put("TOTAL_COSTO_NIO", ReporteService.formatearMontoStatic(datos.totalNio()));

        var jasperPrint = service.llenarReporte(PLANTILLA_MANTENIMIENTO, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Filtros", datos.filtros());
        meta.put("Conteo por estado", conteo);
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Tipo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Prioridad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Responsable", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Descripción / Concepto", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Costo USD", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Costo NIO", TablaReporteXlsx.Tipo.MONTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getClaseRegistro(), f.getPropiedad(), f.getUnidad(), f.getFechaValor(),
                        f.getPrioridad(), f.getEstado(), f.getResponsable(), f.getDetalle(),
                        f.getCostoUsd(), f.getCostoNio()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Costo total USD", "USD", datos.totalUsd()),
                TablaReporteXlsx.Resumen.numerico("Costo total NIO", "NIO", datos.totalNio())
        );

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-09", "Mantenimiento", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Mantenimiento.pdf", jasperPrint, tabla);
    }

    static DocumentoReporte generarTasasUtilizadas(ReporteService service,
                                                   LocalDate desde,
                                                   LocalDate hasta,
                                                   String origenFuente,
                                                   BooleanSupplier cancelado) {
        verificarCancelacion(cancelado);
        validarRangoFechas(desde, hasta);
        String origen = normalizarTexto(origenFuente);

        record DatosTasas(
                List<FilaTasaUtilizada> filas,
                BigDecimal netoUsd,
                BigDecimal netoNio,
                String filtros) {}

        DatosTasas datos = service.tx().ejecutar(session -> {
            service.permisos().exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);

            StringBuilder hql = new StringBuilder("""
                    select p, tc, aut, c, i, u, prop from Pago p
                    join p.tipoCambio tc
                    left join tc.autorizadoPor aut
                    join p.contrato c
                    join c.inquilino i
                    join c.unidad u
                    join u.propiedad prop
                    where 1 = 1
                    """);
            Map<String, Object> params = new LinkedHashMap<>();
            if (desde != null) {
                hql.append(" and p.fechaPago >= :desde");
                params.put("desde", desde);
            }
            if (hasta != null) {
                hql.append(" and p.fechaPago <= :hasta");
                params.put("hasta", hasta);
            }
            if (origen != null) {
                hql.append(" and lower(tc.fuente) like :fuente escape '!'");
                params.put("fuente", PatronLike.contieneLiteral(origen.toLowerCase(Locale.ROOT)));
            }
            hql.append(" order by p.fechaPago desc, p.creadoEn desc");

            var query = session.createQuery(hql.toString(), Object[].class);
            params.forEach(query::setParameter);
            List<Object[]> registros = query.getResultList();

            Set<UUID> pagoIds = new HashSet<>();
            for (Object[] r : registros) pagoIds.add(((Pago) r[0]).getId());
            Map<UUID, Long> recibos = consultarNumerosRecibo(session, pagoIds);

            List<FilaTasaUtilizada> filas = new ArrayList<>();
            BigDecimal netoUsd = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal netoNio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

            for (Object[] r : registros) {
                Pago p = (Pago) r[0];
                TipoCambio tc = (TipoCambio) r[1];
                Usuario aut = (Usuario) r[2];
                Inquilino i = (Inquilino) r[4];
                Unidad u = (Unidad) r[5];
                Long rec = recibos.get(p.getId());

                if (!"Anulado".equalsIgnoreCase(p.getEstado())) {
                    if ("USD".equalsIgnoreCase(p.getMonedaContrato())) {
                        netoUsd = netoUsd.add(nz(p.getMontoContrato())).setScale(2, RoundingMode.HALF_EVEN);
                    } else if ("NIO".equalsIgnoreCase(p.getMonedaContrato())) {
                        netoNio = netoNio.add(nz(p.getMontoContrato())).setScale(2, RoundingMode.HALF_EVEN);
                    }
                }

                filas.add(new FilaTasaUtilizada(
                        p.getFechaPago().toString(),
                        p.getFechaPago(),
                        rec != null ? "#" + rec : "—",
                        i.getNombre() + " — " + u.getCodigo(),
                        p.getMontoOriginal(),
                        p.getMonedaOriginal(),
                        p.getMontoContrato(),
                        p.getMonedaContrato(),
                        ReporteService.formatearMontoStatic(p.getMontoOriginal()) + " " + p.getMonedaOriginal(),
                        ReporteService.formatearMontoStatic(p.getMontoContrato()) + " " + p.getMonedaContrato(),
                        tc.getNioPorUsd(),
                        FormatoTasa.formatear(tc.getNioPorUsd()),
                        tc.getFecha().toString(),
                        tc.getFecha(),
                        tc.getFuente(),
                        aut != null ? aut.getNombre() : "—",
                        p.getEstado()));
            }

            String filtros = construirFiltrosTexto(
                    desde != null || hasta != null ? "Período: " + (desde != null ? desde : "…") + " a " + (hasta != null ? hasta : "…") : "Período: Todos",
                    origen != null ? "Origen: " + origen : "Origen: Todos");
            return new DatosTasas(List.copyOf(filas), netoUsd, netoNio, filtros);
        });

        verificarCancelacion(cancelado);

        SesionUsuario usuario = SesionUsuario.getActual();
        String titulo = "LATCH — Tasas Utilizadas en Pagos";
        String resumen = String.format(Locale.ROOT,
                "Pagos con tasa aplicada: %d   |   Neto USD: %s USD   |   Neto NIO: %s NIO",
                datos.filas().size(),
                ReporteService.formatearMontoStatic(datos.netoUsd()),
                ReporteService.formatearMontoStatic(datos.netoNio()));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", nombreUsuario(usuario));
        parametros.put("FILTROS_TEXTO", datos.filtros());
        parametros.put("RESUMEN_TOTALES", resumen);

        var jasperPrint = service.llenarReporte(PLANTILLA_TASAS_UTILIZADAS, parametros, datos.filas());

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Filtros", datos.filtros());
        meta.put("Alcance", "Tasas históricas congeladas en pagos (no redistribuye catálogo API)");
        meta.put("Generado por", nombreUsuario(usuario));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Fecha pago", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Recibo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Contrato / Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Monto original", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda original", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Monto contrato", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tasa NIO/USD", TablaReporteXlsx.Tipo.TASA),
                new TablaReporteXlsx.Columna("Fecha efectiva tasa", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Fuente", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Autorizador", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado pago", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = datos.filas().stream()
                .map(f -> List.<Object>of(
                        f.getFechaPagoValor(), f.getNumeroRecibo(), f.getContrato(),
                        f.getMontoOriginal(), f.getMonedaOriginal(), f.getMontoContrato(), f.getMonedaContrato(),
                        f.getTasaValor(), f.getFechaTasaValor(), f.getFuente(), f.getAutorizador(), f.getEstadoPago()))
                .toList();

        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.texto("Conversiones registradas", Integer.toString(datos.filas().size())),
                TablaReporteXlsx.Resumen.numerico("Total neto USD", "USD", datos.netoUsd()),
                TablaReporteXlsx.Resumen.numerico("Total neto NIO", "NIO", datos.netoNio())
        );

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-10", "Tasas utilizadas", titulo, meta, columnas, filasXlsx, resumenes);
        return new DocumentoReporte(titulo, "Reporte-Tasas-Utilizadas.pdf", jasperPrint, tabla);
    }

    static TablaReporteXlsx tablaEstadoCuenta(EstadoCuentaResponse ec, String contratoDesc, String generadoPor) {
        String moneda = ec.moneda() != null && !ec.moneda().isBlank() ? ec.moneda().strip() : "USD";
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Contrato", contratoDesc);
        meta.put("Fecha de corte", ec.corte());
        meta.put("Moneda", moneda);
        meta.put("Generado por", generadoPor);

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Sección", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Referencia", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Detalle", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Importe", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Aplicado", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Saldo / Crédito", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filas = new ArrayList<>();
        if (ec.cargos() != null) {
            for (var c : ec.cargos()) {
                if (c == null) continue;
                String fTxt = c.venceEl() != null && !c.venceEl().isBlank() ? c.venceEl() : c.fechaEmision();
                filas.add(List.of("Cargos", parseFechaOObjeto(fTxt), nv(c.tipo()), nv(c.concepto()),
                        ReporteService.parsearImporteStatic(c.importe()),
                        ReporteService.parsearImporteStatic(c.aplicado()),
                        ReporteService.parsearImporteStatic(c.saldo()),
                        moneda, nv(c.estado())));
            }
        }
        if (ec.pagos() != null) {
            for (var p : ec.pagos()) {
                if (p == null) continue;
                String ref = p.numeroRecibo() != null ? "Recibo #" + p.numeroRecibo() : nv(p.referencia());
                filas.add(List.of("Pagos", parseFechaOObjeto(p.fechaPago()), ref, nv(p.metodo()),
                        ReporteService.parsearImporteStatic(p.montoContrato()),
                        ReporteService.parsearImporteStatic(p.aplicado()),
                        ReporteService.parsearImporteStatic(p.credito()),
                        moneda, nv(p.estado())));
            }
        }
        var t = ec.totales();
        List<TablaReporteXlsx.Resumen> resumenes = new ArrayList<>();
        resumenes.add(TablaReporteXlsx.Resumen.numerico("Total cargos", moneda,
                ReporteService.parsearImporteStatic(t != null ? t.cargos() : null)));
        resumenes.add(TablaReporteXlsx.Resumen.numerico("Total pagado", moneda,
                ReporteService.parsearImporteStatic(t != null ? t.pagado() : null)));
        resumenes.add(TablaReporteXlsx.Resumen.numerico("Saldo pendiente", moneda,
                ReporteService.parsearImporteStatic(t != null ? t.saldo() : null)));
        resumenes.add(TablaReporteXlsx.Resumen.numerico("Crédito disponible", moneda,
                ReporteService.parsearImporteStatic(t != null ? t.credito() : null)));
        if (ec.garantia() != null) {
            resumenes.add(TablaReporteXlsx.Resumen.numerico("Garantía pactada", moneda,
                    ReporteService.parsearImporteStatic(ec.garantia().pactada())));
            resumenes.add(TablaReporteXlsx.Resumen.numerico("Garantía saldo actual", moneda,
                    ReporteService.parsearImporteStatic(ec.garantia().saldoActual())));
        }
        return new TablaReporteXlsx(
                "REP-02", "Estado de cuenta", "LATCH — Estado de Cuenta", meta, columnas, filas, resumenes);
    }

    static TablaReporteXlsx tablaDashboard(MetricasResponse metricas, String instante, String generadoPor) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Instante", instante);
        meta.put("Generado por", generadoPor);
        meta.put("Propiedades activas", Long.toString(metricas.propiedades()));
        meta.put("Unidades totales", Long.toString(metricas.unidades()));
        meta.put("Inquilinos activos", Long.toString(metricas.inquilinos()));
        meta.put("Contratos vigentes", Long.toString(metricas.contratosVigentes()));

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Indicador", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Cantidad", TablaReporteXlsx.Tipo.ENTERO),
                new TablaReporteXlsx.Columna("Total", TablaReporteXlsx.Tipo.MONTO)
        );
        List<List<Object>> filas = new ArrayList<>();
        agregarFilasMetricas(filas, "Cargos pendientes", metricas.cargosPendientes());
        agregarFilasMetricas(filas, "Pagos registrados", metricas.pagosRegistrados());
        return new TablaReporteXlsx(
                "REP-05", "Resumen Panel", "LATCH — Resumen Ejecutivo del Panel", meta, columnas, filas, List.of());
    }

    private static void agregarFilasMetricas(List<List<Object>> filas, String indicador,
                                             List<ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse> lista) {
        for (String mon : List.of("USD", "NIO")) {
            var item = lista == null ? null : lista.stream()
                    .filter(Objects::nonNull)
                    .filter(t -> mon.equalsIgnoreCase(t.moneda()))
                    .findFirst().orElse(null);
            long cantidad = item != null ? item.cantidad() : 0L;
            BigDecimal monto = item != null ? ReporteService.parsearImporteStatic(item.total()) : BigDecimal.ZERO.setScale(2);
            filas.add(List.of(indicador, mon, cantidad, monto));
        }
    }

    private static Object parseFechaOObjeto(String texto) {
        if (texto == null || texto.isBlank()) return "—";
        try {
            return LocalDate.parse(texto.strip());
        } catch (Exception ignored) {
            return texto.strip();
        }
    }

    private static String nv(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto.strip();
    }

    private static Map<UUID, Long> consultarNumerosRecibo(Session session, Set<UUID> pagoIds) {
        Map<UUID, Long> recibos = new HashMap<>();
        if (pagoIds == null || pagoIds.isEmpty()) {
            return recibos;
        }
        for (Object[] fila : session.createQuery("""
                        select r.numero, p.id from Recibo r join r.pago p
                        where p.id in :pagos
                        """, Object[].class)
                .setParameter("pagos", pagoIds)
                .getResultList()) {
            recibos.put((UUID) fila[1], ((Number) fila[0]).longValue());
        }
        return recibos;
    }

    private static Map<UUID, BigDecimal> calcularAplicadoAlCorte(Session session, List<UUID> cargoIds,
                                                                 LocalDate corte, Instant finCorte) {
        Map<UUID, BigDecimal> aplicado = new HashMap<>();
        if (cargoIds == null || cargoIds.isEmpty()) {
            return aplicado;
        }
        for (Object[] fila : session.createQuery("""
                        select a, p from AplicacionPago a left join Pago p on a.pagoId = p.id
                        where a.cargoId in :cargoIds
                        """, Object[].class)
                .setParameter("cargoIds", cargoIds)
                .getResultList()) {
            AplicacionPago app = (AplicacionPago) fila[0];
            Pago pago = (Pago) fila[1];
            if (app.getRevertidaEn() != null && app.getRevertidaEn().isBefore(finCorte)) continue;
            if (app.getPagoId() != null) {
                if (pago == null || estaPagoAnuladoAlCorte(pago, finCorte)) continue;
                if (pago.getFechaPago().isAfter(corte)) continue;
                if (app.getFechaAplicacion() != null && app.getFechaAplicacion().isAfter(corte)) continue;
                aplicado.merge(app.getCargoId(), app.getMonto(), BigDecimal::add);
            } else if (app.getMovimientoId() != null) {
                MovimientoGarantia mov = session.find(MovimientoGarantia.class, app.getMovimientoId());
                if (mov != null && !mov.getFechaMovimiento().isAfter(corte)) {
                    aplicado.merge(app.getCargoId(), app.getMonto(), BigDecimal::add);
                }
            }
        }
        return aplicado;
    }

    private static boolean estaCargoAnuladoAlCorte(Cargo cargo, Instant finCorte) {
        String persistido = cargo.getEstadoPersistido();
        if (!"Cancelado".equals(persistido) && !"Anulado".equals(persistido)) return false;
        return cargo.getAnuladoEn() == null || cargo.getAnuladoEn().isBefore(finCorte);
    }

    private static boolean estaPagoAnuladoAlCorte(Pago pago, Instant finCorte) {
        if (!"Anulado".equalsIgnoreCase(pago.getEstado())) return false;
        return pago.getAnuladoEn() == null || pago.getAnuladoEn().isBefore(finCorte);
    }

    private static int leerUmbralVencimiento(Session session, Instant ahora) {
        return ConfiguracionService.buscarParametroVigente(session, "dias_aviso_vencimiento", ahora)
                .map(p -> {
                    try {
                        String raw = p.getValor().strip();
                        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                            raw = raw.substring(1, raw.length() - 1);
                        }
                        int v = Integer.parseInt(raw.strip());
                        return v >= 0 ? v : 30;
                    } catch (NumberFormatException e) {
                        return 30;
                    }
                })
                .orElse(30);
    }

    private static String nombreUsuario(SesionUsuario usuario) {
        return usuario != null ? usuario.getNombre() : "Sistema";
    }

    private static String construirFiltrosTexto(String... partes) {
        List<String> validas = new ArrayList<>();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                validas.add(p.strip());
            }
        }
        return validas.isEmpty() ? "Sin filtros adicionales" : String.join(" · ", validas);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String decimal(BigDecimal v) {
        return nz(v).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    // --- Beans JasperReports ---

    public static final class FilaCarteraVencida {
        private final String contrato;
        private final String propiedad;
        private final String tipo;
        private final String concepto;
        private final String venceEl;
        private final LocalDate venceFecha;
        private final BigDecimal saldoPendiente;
        private final String moneda;
        private final long diasVencidos;
        private final String tramo;
        private final String estado;

        public FilaCarteraVencida(String contrato, String propiedad, String tipo, String concepto,
                                  String venceEl, LocalDate venceFecha, BigDecimal saldoPendiente,
                                  String moneda, long diasVencidos, String tramo, String estado) {
            this.contrato = contrato;
            this.propiedad = propiedad;
            this.tipo = tipo;
            this.concepto = concepto;
            this.venceEl = venceEl;
            this.venceFecha = venceFecha;
            this.saldoPendiente = saldoPendiente;
            this.moneda = moneda;
            this.diasVencidos = diasVencidos;
            this.tramo = tramo;
            this.estado = estado;
        }

        public String getContrato() { return contrato; }
        public String getPropiedad() { return propiedad; }
        public String getTipo() { return tipo; }
        public String getConcepto() { return concepto; }
        public String getVenceEl() { return venceEl; }
        public LocalDate getVenceFecha() { return venceFecha; }
        public BigDecimal getSaldoPendiente() { return saldoPendiente; }
        public String getMoneda() { return moneda; }
        public long getDiasVencidos() { return diasVencidos; }
        public String getTramo() { return tramo; }
        public String getEstado() { return estado; }
    }

    public static final class FilaIngresoGasto {
        private final String seccion;
        private final String fecha;
        private final LocalDate fechaValor;
        private final String propiedad;
        private final String referencia;
        private final String detalle;
        private final String moneda;
        private final BigDecimal importe;
        private final String estado;

        public FilaIngresoGasto(String seccion, String fecha, LocalDate fechaValor, String propiedad,
                                String referencia, String detalle, String moneda, BigDecimal importe, String estado) {
            this.seccion = seccion;
            this.fecha = fecha;
            this.fechaValor = fechaValor;
            this.propiedad = propiedad;
            this.referencia = referencia;
            this.detalle = detalle;
            this.moneda = moneda;
            this.importe = importe;
            this.estado = estado;
        }

        public String getSeccion() { return seccion; }
        public String getFecha() { return fecha; }
        public LocalDate getFechaValor() { return fechaValor; }
        public String getPropiedad() { return propiedad; }
        public String getReferencia() { return referencia; }
        public String getDetalle() { return detalle; }
        public String getMoneda() { return moneda; }
        public BigDecimal getImporte() { return importe; }
        public String getEstado() { return estado; }
    }

    public static final class FilaOcupacion {
        private final String propiedad;
        private final String codigoUnidad;
        private final String tipoUnidad;
        private final String estadoUnidad;
        private final String inquilinoOContrato;
        private final String moneda;
        private final BigDecimal canonReferencia;

        public FilaOcupacion(String propiedad, String codigoUnidad, String tipoUnidad,
                             String estadoUnidad, String inquilinoOContrato, String moneda,
                             BigDecimal canonReferencia) {
            this.propiedad = propiedad;
            this.codigoUnidad = codigoUnidad;
            this.tipoUnidad = tipoUnidad;
            this.estadoUnidad = estadoUnidad;
            this.inquilinoOContrato = inquilinoOContrato;
            this.moneda = moneda;
            this.canonReferencia = canonReferencia;
        }

        public String getPropiedad() { return propiedad; }
        public String getCodigoUnidad() { return codigoUnidad; }
        public String getTipoUnidad() { return tipoUnidad; }
        public String getEstadoUnidad() { return estadoUnidad; }
        public String getInquilinoOContrato() { return inquilinoOContrato; }
        public String getMoneda() { return moneda; }
        public BigDecimal getCanonReferencia() { return canonReferencia; }
    }

    public static final class FilaContratoPorVencer {
        private final String inquilino;
        private final String propiedad;
        private final String unidad;
        private final String estado;
        private final String inicio;
        private final String fin;
        private final LocalDate inicioFecha;
        private final LocalDate finFecha;
        private final long diasRestantes;
        private final String moneda;
        private final BigDecimal canon;

        public FilaContratoPorVencer(String inquilino, String propiedad, String unidad, String estado,
                                     String inicio, String fin, LocalDate inicioFecha, LocalDate finFecha,
                                     long diasRestantes, String moneda, BigDecimal canon) {
            this.inquilino = inquilino;
            this.propiedad = propiedad;
            this.unidad = unidad;
            this.estado = estado;
            this.inicio = inicio;
            this.fin = fin;
            this.inicioFecha = inicioFecha;
            this.finFecha = finFecha;
            this.diasRestantes = diasRestantes;
            this.moneda = moneda;
            this.canon = canon;
        }

        public String getInquilino() { return inquilino; }
        public String getPropiedad() { return propiedad; }
        public String getUnidad() { return unidad; }
        public String getEstado() { return estado; }
        public String getInicio() { return inicio; }
        public String getFin() { return fin; }
        public LocalDate getInicioFecha() { return inicioFecha; }
        public LocalDate getFinFecha() { return finFecha; }
        public long getDiasRestantes() { return diasRestantes; }
        public String getMoneda() { return moneda; }
        public BigDecimal getCanon() { return canon; }
    }

    public static final class FilaHistorialPago {
        private final String numeroRecibo;
        private final String fechaPago;
        private final LocalDate fechaValor;
        private final String inquilino;
        private final String contrato;
        private final BigDecimal montoOriginal;
        private final String monedaOriginal;
        private final BigDecimal montoContrato;
        private final String monedaContrato;
        private final String montoOriginalTexto;
        private final String montoContratoTexto;
        private final String metodo;
        private final String referencia;
        private final String estado;
        private final String motivoAnulacion;

        public FilaHistorialPago(String numeroRecibo, String fechaPago, LocalDate fechaValor,
                                 String inquilino, String contrato, BigDecimal montoOriginal,
                                 String monedaOriginal, BigDecimal montoContrato, String monedaContrato,
                                 String montoOriginalTexto, String montoContratoTexto, String metodo,
                                 String referencia, String estado, String motivoAnulacion) {
            this.numeroRecibo = numeroRecibo;
            this.fechaPago = fechaPago;
            this.fechaValor = fechaValor;
            this.inquilino = inquilino;
            this.contrato = contrato;
            this.montoOriginal = montoOriginal;
            this.monedaOriginal = monedaOriginal;
            this.montoContrato = montoContrato;
            this.monedaContrato = monedaContrato;
            this.montoOriginalTexto = montoOriginalTexto;
            this.montoContratoTexto = montoContratoTexto;
            this.metodo = metodo;
            this.referencia = referencia;
            this.estado = estado;
            this.motivoAnulacion = motivoAnulacion;
        }

        public String getNumeroRecibo() { return numeroRecibo; }
        public String getFechaPago() { return fechaPago; }
        public LocalDate getFechaValor() { return fechaValor; }
        public String getInquilino() { return inquilino; }
        public String getContrato() { return contrato; }
        public BigDecimal getMontoOriginal() { return montoOriginal; }
        public String getMonedaOriginal() { return monedaOriginal; }
        public BigDecimal getMontoContrato() { return montoContrato; }
        public String getMonedaContrato() { return monedaContrato; }
        public String getMontoOriginalTexto() { return montoOriginalTexto; }
        public String getMontoContratoTexto() { return montoContratoTexto; }
        public String getMetodo() { return metodo; }
        public String getReferencia() { return referencia; }
        public String getEstado() { return estado; }
        public String getMotivoAnulacion() { return motivoAnulacion; }
    }

    public static final class FilaGarantiaLiquidacion {
        private final String contrato;
        private final String fecha;
        private final LocalDate fechaValor;
        private final String tipoRegistro;
        private final String concepto;
        private final String metodo;
        private final String moneda;
        private final BigDecimal inicial;
        private final BigDecimal salida;
        private final BigDecimal remanente;

        public FilaGarantiaLiquidacion(String contrato, String fecha, LocalDate fechaValor,
                                       String tipoRegistro, String concepto, String metodo,
                                       String moneda, BigDecimal inicial, BigDecimal salida,
                                       BigDecimal remanente) {
            this.contrato = contrato;
            this.fecha = fecha;
            this.fechaValor = fechaValor;
            this.tipoRegistro = tipoRegistro;
            this.concepto = concepto;
            this.metodo = metodo;
            this.moneda = moneda;
            this.inicial = inicial;
            this.salida = salida;
            this.remanente = remanente;
        }

        public String getContrato() { return contrato; }
        public String getFecha() { return fecha; }
        public LocalDate getFechaValor() { return fechaValor; }
        public String getTipoRegistro() { return tipoRegistro; }
        public String getConcepto() { return concepto; }
        public String getMetodo() { return metodo; }
        public String getMoneda() { return moneda; }
        public BigDecimal getInicial() { return inicial; }
        public BigDecimal getSalida() { return salida; }
        public BigDecimal getRemanente() { return remanente; }
    }

    public static final class FilaMantenimiento {
        private final String claseRegistro;
        private final String propiedad;
        private final String unidad;
        private final String fecha;
        private final LocalDate fechaValor;
        private final String prioridad;
        private final String estado;
        private final String responsable;
        private final String detalle;
        private final BigDecimal costoUsd;
        private final BigDecimal costoNio;

        public FilaMantenimiento(String claseRegistro, String propiedad, String unidad, String fecha,
                                 LocalDate fechaValor, String prioridad, String estado,
                                 String responsable, String detalle, BigDecimal costoUsd, BigDecimal costoNio) {
            this.claseRegistro = claseRegistro;
            this.propiedad = propiedad;
            this.unidad = unidad;
            this.fecha = fecha;
            this.fechaValor = fechaValor;
            this.prioridad = prioridad;
            this.estado = estado;
            this.responsable = responsable;
            this.detalle = detalle;
            this.costoUsd = costoUsd;
            this.costoNio = costoNio;
        }

        public String getClaseRegistro() { return claseRegistro; }
        public String getPropiedad() { return propiedad; }
        public String getUnidad() { return unidad; }
        public String getFecha() { return fecha; }
        public LocalDate getFechaValor() { return fechaValor; }
        public String getPrioridad() { return prioridad; }
        public String getEstado() { return estado; }
        public String getResponsable() { return responsable; }
        public String getDetalle() { return detalle; }
        public BigDecimal getCostoUsd() { return costoUsd; }
        public BigDecimal getCostoNio() { return costoNio; }
    }

    public static final class FilaTasaUtilizada {
        private final String fechaPago;
        private final LocalDate fechaPagoValor;
        private final String numeroRecibo;
        private final String contrato;
        private final BigDecimal montoOriginal;
        private final String monedaOriginal;
        private final BigDecimal montoContrato;
        private final String monedaContrato;
        private final String montoOriginalTexto;
        private final String montoContratoTexto;
        private final BigDecimal tasaValor;
        private final String tasaAplicada;
        private final String fechaEfectivaTasa;
        private final LocalDate fechaTasaValor;
        private final String fuente;
        private final String autorizador;
        private final String estadoPago;

        public FilaTasaUtilizada(String fechaPago, LocalDate fechaPagoValor, String numeroRecibo,
                                 String contrato, BigDecimal montoOriginal, String monedaOriginal,
                                 BigDecimal montoContrato, String monedaContrato,
                                 String montoOriginalTexto, String montoContratoTexto,
                                 BigDecimal tasaValor, String tasaAplicada,
                                 String fechaEfectivaTasa, LocalDate fechaTasaValor,
                                 String fuente, String autorizador, String estadoPago) {
            this.fechaPago = fechaPago;
            this.fechaPagoValor = fechaPagoValor;
            this.numeroRecibo = numeroRecibo;
            this.contrato = contrato;
            this.montoOriginal = montoOriginal;
            this.monedaOriginal = monedaOriginal;
            this.montoContrato = montoContrato;
            this.monedaContrato = monedaContrato;
            this.montoOriginalTexto = montoOriginalTexto;
            this.montoContratoTexto = montoContratoTexto;
            this.tasaValor = tasaValor;
            this.tasaAplicada = tasaAplicada;
            this.fechaEfectivaTasa = fechaEfectivaTasa;
            this.fechaTasaValor = fechaTasaValor;
            this.fuente = fuente;
            this.autorizador = autorizador;
            this.estadoPago = estadoPago;
        }

        public String getFechaPago() { return fechaPago; }
        public LocalDate getFechaPagoValor() { return fechaPagoValor; }
        public String getNumeroRecibo() { return numeroRecibo; }
        public String getContrato() { return contrato; }
        public BigDecimal getMontoOriginal() { return montoOriginal; }
        public String getMonedaOriginal() { return monedaOriginal; }
        public BigDecimal getMontoContrato() { return montoContrato; }
        public String getMonedaContrato() { return monedaContrato; }
        public String getMontoOriginalTexto() { return montoOriginalTexto; }
        public String getMontoContratoTexto() { return montoContratoTexto; }
        public BigDecimal getTasaValor() { return tasaValor; }
        public String getTasaAplicada() { return tasaAplicada; }
        public String getFechaEfectivaTasa() { return fechaEfectivaTasa; }
        public LocalDate getFechaTasaValor() { return fechaTasaValor; }
        public String getFuente() { return fuente; }
        public String getAutorizador() { return autorizador; }
        public String getEstadoPago() { return estadoPago; }
    }

    // --- Acumuladores por moneda ---

    private static final class ResumenMonedaCartera {
        private final String moneda;
        private BigDecimal alDia = BigDecimal.ZERO.setScale(2);
        private BigDecimal t1a30 = BigDecimal.ZERO.setScale(2);
        private BigDecimal t31a60 = BigDecimal.ZERO.setScale(2);
        private BigDecimal t61a90 = BigDecimal.ZERO.setScale(2);
        private BigDecimal tMas90 = BigDecimal.ZERO.setScale(2);
        private BigDecimal principalVencido = BigDecimal.ZERO.setScale(2);
        private BigDecimal moraVencida = BigDecimal.ZERO.setScale(2);

        private ResumenMonedaCartera(String moneda) {
            this.moneda = moneda;
        }

        private void acumular(String tramo, BigDecimal saldo, long dias, boolean esMora) {
            switch (tramo) {
                case CobranzaService.TRAMO_AL_DIA -> alDia = alDia.add(saldo);
                case CobranzaService.TRAMO_1_30 -> t1a30 = t1a30.add(saldo);
                case CobranzaService.TRAMO_31_60 -> t31a60 = t31a60.add(saldo);
                case CobranzaService.TRAMO_61_90 -> t61a90 = t61a90.add(saldo);
                default -> tMas90 = tMas90.add(saldo);
            }
            if (dias > 0) {
                if (esMora) moraVencida = moraVencida.add(saldo);
                else principalVencido = principalVencido.add(saldo);
            }
        }

        private BigDecimal totalVencido() {
            return t1a30.add(t31a60).add(t61a90).add(tMas90).setScale(2, RoundingMode.HALF_EVEN);
        }

        private String formatearResumen() {
            return moneda + ": Al día " + ReporteService.formatearMontoStatic(alDia)
                    + " | 1-30d " + ReporteService.formatearMontoStatic(t1a30)
                    + " | 31-60d " + ReporteService.formatearMontoStatic(t31a60)
                    + " | 61-90d " + ReporteService.formatearMontoStatic(t61a90)
                    + " | +90d " + ReporteService.formatearMontoStatic(tMas90)
                    + " | Vencido " + ReporteService.formatearMontoStatic(totalVencido())
                    + " (Princ. " + ReporteService.formatearMontoStatic(principalVencido)
                    + " / Mora " + ReporteService.formatearMontoStatic(moraVencida) + ")";
        }

        private void agregarResumenes(List<TablaReporteXlsx.Resumen> lista) {
            lista.add(TablaReporteXlsx.Resumen.numerico("Cartera Al día " + moneda, moneda, alDia));
            lista.add(TablaReporteXlsx.Resumen.numerico("Cartera 1 a 30 días " + moneda, moneda, t1a30));
            lista.add(TablaReporteXlsx.Resumen.numerico("Cartera 31 a 60 días " + moneda, moneda, t31a60));
            lista.add(TablaReporteXlsx.Resumen.numerico("Cartera 61 a 90 días " + moneda, moneda, t61a90));
            lista.add(TablaReporteXlsx.Resumen.numerico("Cartera Más de 90 días " + moneda, moneda, tMas90));
            lista.add(TablaReporteXlsx.Resumen.numerico("Total vencido " + moneda, moneda, totalVencido()));
            lista.add(TablaReporteXlsx.Resumen.numerico("Principal vencido " + moneda, moneda, principalVencido));
            lista.add(TablaReporteXlsx.Resumen.numerico("Mora vencida " + moneda, moneda, moraVencida));
        }
    }

    private static final class AcumuladorIngresos {
        private final String moneda;
        private BigDecimal rentas = BigDecimal.ZERO.setScale(2);
        private BigDecimal otros = BigDecimal.ZERO.setScale(2);
        private BigDecimal gastos = BigDecimal.ZERO.setScale(2);
        private BigDecimal garantias = BigDecimal.ZERO.setScale(2);

        private AcumuladorIngresos(String moneda) {
            this.moneda = moneda;
        }

        private void sumarRenta(BigDecimal m) { rentas = rentas.add(m).setScale(2, RoundingMode.HALF_EVEN); }
        private void sumarOtros(BigDecimal m) { otros = otros.add(m).setScale(2, RoundingMode.HALF_EVEN); }
        private void sumarGasto(BigDecimal m) { gastos = gastos.add(m).setScale(2, RoundingMode.HALF_EVEN); }
        private void sumarGarantia(BigDecimal m) { garantias = garantias.add(m).setScale(2, RoundingMode.HALF_EVEN); }
        private BigDecimal totalIngresos() { return rentas.add(otros).setScale(2, RoundingMode.HALF_EVEN); }
        private BigDecimal resultadoNeto() { return totalIngresos().subtract(gastos).setScale(2, RoundingMode.HALF_EVEN); }

        private String resumenLinea() {
            return moneda + ": Rentas " + ReporteService.formatearMontoStatic(rentas)
                    + " | Otros " + ReporteService.formatearMontoStatic(otros)
                    + " | Gastos " + ReporteService.formatearMontoStatic(gastos)
                    + " | Neto " + ReporteService.formatearMontoStatic(resultadoNeto())
                    + " | Garantías (sep.) " + ReporteService.formatearMontoStatic(garantias);
        }

        private void agregarResumenes(List<TablaReporteXlsx.Resumen> lista) {
            lista.add(TablaReporteXlsx.Resumen.numerico("Rentas netas " + moneda, moneda, rentas));
            lista.add(TablaReporteXlsx.Resumen.numerico("Otros cobros netos " + moneda, moneda, otros));
            lista.add(TablaReporteXlsx.Resumen.numerico("Total ingresos netos " + moneda, moneda, totalIngresos()));
            lista.add(TablaReporteXlsx.Resumen.numerico("Gastos de mantenimiento " + moneda, moneda, gastos));
            lista.add(TablaReporteXlsx.Resumen.numerico("Resultado neto operativo " + moneda, moneda, resultadoNeto()));
            lista.add(TablaReporteXlsx.Resumen.numerico("Movimientos de garantía (separados) " + moneda, moneda, garantias));
        }
    }

    private static final class AcumuladorGarantia {
        private final String moneda;
        private BigDecimal inicial = BigDecimal.ZERO.setScale(2);
        private BigDecimal aplicado = BigDecimal.ZERO.setScale(2);
        private BigDecimal deducido = BigDecimal.ZERO.setScale(2);
        private BigDecimal devuelto = BigDecimal.ZERO.setScale(2);
        private BigDecimal remanente = BigDecimal.ZERO.setScale(2);

        private AcumuladorGarantia(String moneda) {
            this.moneda = moneda;
        }

        private void acumular(BigDecimal ini, BigDecimal apl, BigDecimal ded, BigDecimal dev, BigDecimal rem) {
            inicial = inicial.add(ini);
            aplicado = aplicado.add(apl);
            deducido = deducido.add(ded);
            devuelto = devuelto.add(dev);
            remanente = remanente.add(rem);
        }

        private String resumenLinea() {
            return moneda + ": Inicial " + ReporteService.formatearMontoStatic(inicial)
                    + " | Aplicado " + ReporteService.formatearMontoStatic(aplicado)
                    + " | Deducido " + ReporteService.formatearMontoStatic(deducido)
                    + " | Devuelto " + ReporteService.formatearMontoStatic(devuelto)
                    + " | Remanente " + ReporteService.formatearMontoStatic(remanente);
        }

        private void agregarResumenes(List<TablaReporteXlsx.Resumen> lista) {
            lista.add(TablaReporteXlsx.Resumen.numerico("Depósito inicial " + moneda, moneda, inicial));
            lista.add(TablaReporteXlsx.Resumen.numerico("Aplicado a deuda " + moneda, moneda, aplicado));
            lista.add(TablaReporteXlsx.Resumen.numerico("Deducido " + moneda, moneda, deducido));
            lista.add(TablaReporteXlsx.Resumen.numerico("Devuelto " + moneda, moneda, devuelto));
            lista.add(TablaReporteXlsx.Resumen.numerico("Remanente al corte " + moneda, moneda, remanente));
        }
    }
}
