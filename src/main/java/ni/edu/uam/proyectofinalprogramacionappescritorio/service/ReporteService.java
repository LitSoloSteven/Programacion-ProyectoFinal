package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TablaReporteXlsx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ExportadorXlsx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTasa;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ReporteService {

    private static final String PLANTILLA_UNIDADES = "/reportes/unidades.jrxml";
    private static final String PLANTILLA_RECIBO = "/reportes/recibo.jrxml";
    private static final String PLANTILLA_PAGOS_TABLA = "/reportes/pagos_tabla.jrxml";
    private static final String PLANTILLA_CONTRATOS_OCUPACION = "/reportes/contratos_ocupacion.jrxml";
    private static final String PLANTILLA_PROPIEDADES_UNIDADES = "/reportes/propiedades_unidades.jrxml";
    private static final String PLANTILLA_CARGOS_CARTERA = "/reportes/cargos_cartera.jrxml";
    private static final String PLANTILLA_INQUILINOS = "/reportes/inquilinos.jrxml";
    private static final String PLANTILLA_ESTADO_CUENTA = "/reportes/estado_cuenta.jrxml";
    private static final String PLANTILLA_TASAS_CAMBIO = "/reportes/tasas_cambio.jrxml";
    private static final String PLANTILLA_DASHBOARD_RESUMEN = "/reportes/dashboard_resumen.jrxml";
    private static final String PLANTILLA_GARANTIAS_DEPOSITOS = "/reportes/garantias_depositos.jrxml";

    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();
    private final PagoService pagoService;

    public ReporteService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public ReporteService(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx, "Faltan las transacciones.");
        this.pagoService = new PagoService(tx);
    }

    /**
     * Genera el PDF con los pagos cargados en la tabla al pulsar el botón.
     */
    public byte[] generarReportePagosPdf(List<Pago> pagosEnTabla) {
        return exportarPdf(generarDocumentoPagos(pagosEnTabla).jasperPrint());
    }

    public DocumentoReporte generarDocumentoPagos(List<Pago> pagosEnTabla) {
        Objects.requireNonNull(pagosEnTabla, "La lista de pagos no puede ser nula.");
        if (pagosEnTabla.isEmpty()) {
            throw new IllegalArgumentException("No hay pagos en la tabla para generar el reporte.");
        }

        List<FilaPagoReporte> filas = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return pagosEnTabla.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapearFila)
                    .toList();
        });

        int pagosAnulados = (int) filas.stream()
                .filter(ReporteService::esPagoAnulado)
                .count();
        BigDecimal totalNetoUsd = sumarPorMoneda(
                filas,
                "USD",
                fila -> !esPagoAnulado(fila),
                FilaPagoReporte::getMonedaContrato,
                FilaPagoReporte::getMontoContrato);
        BigDecimal totalNetoNio = sumarPorMoneda(
                filas,
                "NIO",
                fila -> !esPagoAnulado(fila),
                FilaPagoReporte::getMonedaContrato,
                FilaPagoReporte::getMontoContrato);

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Reporte de Pagos";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_REGISTROS", filas.size());
        parametros.put("PAGOS_ANULADOS", pagosAnulados);
        parametros.put("TOTAL_NETO_USD", formatearMonto(totalNetoUsd));
        parametros.put("TOTAL_NETO_NIO", formatearMonto(totalNetoNio));

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_PAGOS_TABLA, parametros, filas);
        TablaReporteXlsx tabla = construirTablaPagos(titulo, usuarioActual, filas, totalNetoUsd, totalNetoNio, pagosAnulados);
        return new DocumentoReporte(titulo, "Reporte-Pagos.pdf", jasperPrint, tabla);
    }

    public byte[] generarPdfPago(UUID pagoId) {
        return exportarPdf(generarDocumentoRecibo(pagoId).jasperPrint());
    }

    public DocumentoReporte generarDocumentoRecibo(UUID pagoId) {
        return generarDocumentoRecibo(pagoId, null);
    }

    public DocumentoReporte generarDocumentoRecibo(UUID pagoId, BooleanSupplier cancelado) {
        ConsultasReporteHelper.verificarCancelacion(cancelado);
        Objects.requireNonNull(pagoId, "Falta el identificador del pago.");

        record DatosReciboCompleto(
                ReciboDTO dto,
                String tasaInfo,
                String aplicacionesInfo,
                String creditoInfo) {}

        DatosReciboCompleto datos = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            Recibo recibo = session.createQuery("""
                            from Recibo r
                            join fetch r.pago p
                            left join fetch p.tipoCambio tc
                            left join fetch tc.autorizadoPor
                            where p.id = :pagoId
                            """, Recibo.class)
                    .setParameter("pagoId", pagoId)
                    .uniqueResultOptional()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontró un recibo asociado al pago indicado."));

            Pago pago = recibo.getPago();
            TipoCambio tc = pago.getTipoCambio();
            String tasaInfo = tc == null
                    ? "Misma moneda"
                    : FormatoTasa.formatear(tc.getNioPorUsd()) + " NIO/USD (" + tc.getFuente() + ", " + tc.getFecha() + ")";

            List<AplicacionPago> aplicaciones = session.createQuery("""
                            from AplicacionPago a
                            where a.pagoId = :pagoId and a.revertidaEn is null
                            order by a.fechaAplicacion asc, a.id asc
                            """, AplicacionPago.class)
                    .setParameter("pagoId", pagoId)
                    .getResultList();

            BigDecimal totalAplicado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            List<String> detallesApp = new ArrayList<>();
            if (!aplicaciones.isEmpty()) {
                List<UUID> cargoIds = aplicaciones.stream().map(AplicacionPago::getCargoId).distinct().toList();
                Map<UUID, Cargo> cargosPorId = new HashMap<>();
                if (!cargoIds.isEmpty()) {
                    for (Cargo cargo : session.createQuery("from Cargo c where c.id in :ids", Cargo.class)
                            .setParameter("ids", cargoIds).getResultList()) {
                        cargosPorId.put(cargo.getId(), cargo);
                    }
                }
                for (AplicacionPago app : aplicaciones) {
                    totalAplicado = totalAplicado.add(app.getMonto()).setScale(2, RoundingMode.HALF_EVEN);
                    Cargo cargo = cargosPorId.get(app.getCargoId());
                    String nombreCargo = cargo != null ? cargo.getConcepto() : "Cargo";
                    detallesApp.add(nombreCargo + " (" + formatearMonto(app.getMonto()) + " " + pago.getMonedaContrato() + ")");
                }
            }

            BigDecimal credito = "Anulado".equalsIgnoreCase(pago.getEstado())
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                    : (pago.getMontoContrato() == null ? BigDecimal.ZERO : pago.getMontoContrato())
                    .subtract(totalAplicado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
            String creditoInfo = formatearMonto(credito) + " " + pago.getMonedaContrato();
            String aplicacionesInfo = detallesApp.isEmpty() ? "Sin aplicaciones directas" : String.join("; ", detallesApp);

            return new DatosReciboCompleto(recibo.presentar(), tasaInfo, aplicacionesInfo, creditoInfo);
        });

        ConsultasReporteHelper.verificarCancelacion(cancelado);

        ReciboDTO datosRecibo = datos.dto();
        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", "LATCH — Comprobante de Recibo");
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("EMISOR_INFO", datosRecibo.getEmisorInfo());
        parametros.put("NUMERO_RECIBO", String.valueOf(datosRecibo.getNumero()));
        parametros.put("ESTADO_RECIBO", datosRecibo.isAnulado() ? "ANULADO" : "VIGENTE");
        parametros.put("TASA_INFO", datos.tasaInfo());
        parametros.put("APLICACIONES_INFO", datos.aplicacionesInfo());
        parametros.put("CREDITO_INFO", datos.creditoInfo());

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_RECIBO, parametros, List.of(datosRecibo));
        return new DocumentoReporte(
                "LATCH — Recibo #" + datosRecibo.getNumero(),
                "Recibo-" + datosRecibo.getNumero() + ".pdf",
                jasperPrint,
                null);
    }

    /**
     * Genera el PDF del resumen de contratos y ocupación a partir de los contratos en vista.
     */
    public byte[] generarReporteContratosOcupacionPdf(List<Contrato> contratosEnVista) {
        return exportarPdf(generarDocumentoContratosOcupacion(contratosEnVista).jasperPrint());
    }

    public DocumentoReporte generarDocumentoContratosOcupacion(List<Contrato> contratosEnVista) {
        Objects.requireNonNull(contratosEnVista, "La lista de contratos no puede ser nula.");
        if (contratosEnVista.isEmpty()) {
            throw new IllegalArgumentException("No hay contratos para generar el reporte.");
        }

        ResumenOcupacion datosResumen = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);

            List<Unidad> todasLasUnidades = session
                    .createQuery("from Unidad", Unidad.class)
                    .getResultList();

            int totalUnidades = todasLasUnidades.size();
            int unidadesOcupadas = (int) todasLasUnidades.stream()
                    .filter(Objects::nonNull)
                    .map(Unidad::getEstado)
                    .filter(Objects::nonNull)
                    .map(estado -> estado.strip().toLowerCase(Locale.ROOT))
                    .filter(estado -> estado.equals("ocupada") || estado.equals("ocupado"))
                    .count();

            double porcentajeOcupacion = totalUnidades == 0
                    ? 0.0
                    : (unidadesOcupadas * 100.0) / totalUnidades;

            List<FilaContratoOcupacionReporte> filas = contratosEnVista.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapearFilaContrato)
                    .toList();

            int vigentes = (int) filas.stream()
                    .filter(f -> "Vigente".equalsIgnoreCase(f.getEstadoContrato()))
                    .count();
            int porVencer = (int) filas.stream()
                    .filter(f -> "Por vencer".equalsIgnoreCase(f.getEstadoContrato()))
                    .count();

            return new ResumenOcupacion(
                    filas,
                    filas.size(),
                    vigentes,
                    porVencer,
                    totalUnidades,
                    unidadesOcupadas,
                    porcentajeOcupacion
            );
        });

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Resumen de Contratos y Ocupación";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_CONTRATOS", datosResumen.totalContratos());
        parametros.put("CONTRATOS_VIGENTES", datosResumen.contratosVigentes());
        parametros.put("CONTRATOS_POR_VENCER", datosResumen.contratosPorVencer());
        parametros.put("TOTAL_UNIDADES", datosResumen.totalUnidades());
        parametros.put("UNIDADES_OCUPADAS", datosResumen.unidadesOcupadas());
        parametros.put("PORCENTAJE_OCUPACION", String.format(Locale.ROOT, "%.1f%%", datosResumen.porcentajeOcupacion()));

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_CONTRATOS_OCUPACION, parametros, datosResumen.filas());
        TablaReporteXlsx tabla = construirTablaContratosOcupacion(titulo, usuarioActual, datosResumen);
        return new DocumentoReporte(titulo, "Resumen-Contratos-Ocupacion.pdf", jasperPrint, tabla);
    }

    private FilaPagoReporte mapearFila(Pago pago) {
        String contratoTexto = null;
        try {
            contratoTexto = pago.getContratoInfo();
        } catch (Exception ignored) {
            // Respaldo seguro si el proxy no está inicializado fuera de sesión
        }
        if (contratoTexto == null || contratoTexto.isBlank()) {
            contratoTexto = pago.getContratoId() != null
                    ? pago.getContratoId().toString()
                    : "—";
        }

        String monedaOriginal = pago.getMonedaOriginal() != null && !pago.getMonedaOriginal().isBlank()
                ? pago.getMonedaOriginal()
                : "";

        String monedaContrato = pago.getMonedaContrato() != null && !pago.getMonedaContrato().isBlank()
                ? pago.getMonedaContrato()
                : monedaOriginal;

        String metodoTexto = pago.getMetodo() != null && !pago.getMetodo().isBlank()
                ? pago.getMetodo()
                : "—";

        String estadoTexto = pago.getEstado() != null && !pago.getEstado().isBlank()
                ? pago.getEstado()
                : "Registrado";

        return new FilaPagoReporte(
                contratoTexto,
                pago.getFechaPago(),
                pago.getMontoOriginal(),
                monedaOriginal,
                pago.getMontoContrato(),
                monedaContrato,
                metodoTexto,
                pago.getReferencia(),
                estadoTexto
        );
    }

    private FilaContratoOcupacionReporte mapearFilaContrato(Contrato contrato) {
        String inquilinoTexto = null;
        try {
            inquilinoTexto = contrato.getInquilinoNombre();
        } catch (Exception ignored) {
            // Respaldo si el proxy no está inicializado
        }
        if (inquilinoTexto == null || inquilinoTexto.isBlank()) {
            inquilinoTexto = contrato.getInquilinoId() != null
                    ? contrato.getInquilinoId().toString()
                    : "—";
        }

        String unidadTexto = null;
        try {
            unidadTexto = contrato.getUnidadInfo();
        } catch (Exception ignored) {
            // Respaldo si el proxy no está inicializado
        }
        if (unidadTexto == null || unidadTexto.isBlank()) {
            unidadTexto = contrato.getUnidadId() != null
                    ? contrato.getUnidadId().toString()
                    : "—";
        }

        String estadoContrato = contrato.getEstado() != null && !contrato.getEstado().isBlank()
                ? contrato.getEstado()
                : "—";

        String reservaTexto = Contrato.reservaUnidad(estadoContrato)
                ? "Reserva unidad"
                : "No reserva";

        String monedaTexto = contrato.getMoneda() != null && !contrato.getMoneda().isBlank()
                ? contrato.getMoneda()
                : "";

        return new FilaContratoOcupacionReporte(
                inquilinoTexto,
                unidadTexto,
                reservaTexto,
                estadoContrato,
                contrato.getInicio(),
                contrato.getFin(),
                contrato.getCanon(),
                monedaTexto
        );
    }

    <T> JasperPrint llenarReporte(String rutaPlantilla,
                                    Map<String, Object> parametros,
                                    List<T> listaDatos) {
        try (InputStream streamPlantilla = ReporteService.class.getResourceAsStream(rutaPlantilla)) {
            if (streamPlantilla == null) {
                throw new IllegalStateException("No existe la plantilla en resources: " + rutaPlantilla);
            }
            JasperReport reporteCompilado = JasperCompileManager.compileReport(streamPlantilla);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(listaDatos);
            return JasperFillManager.fillReport(reporteCompilado, parametros, dataSource);
        } catch (JRException e) {
            throw new IllegalStateException("JasperReports falló al llenar el reporte: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Error inesperado al llenar el reporte.", e);
        }
    }

    public byte[] exportarPdf(JasperPrint jasperPrint) {
        Objects.requireNonNull(jasperPrint, "Falta el documento JasperPrint.");
        try {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new IllegalStateException("JasperReports falló al exportar el PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Error inesperado al exportar el reporte.", e);
        }
    }

    public byte[] exportarXlsx(DocumentoReporte documento) {
        Objects.requireNonNull(documento, "Falta el documento del reporte.");
        if (!documento.admiteXlsx()) {
            throw new IllegalArgumentException("Este reporte solo admite exportación a PDF.");
        }
        return ExportadorXlsx.exportarBytes(documento.datosXlsx());
    }

    public byte[] exportarXlsx(TablaReporteXlsx tabla) {
        Objects.requireNonNull(tabla, "Faltan los datos tabulares del reporte XLSX.");
        return ExportadorXlsx.exportarBytes(tabla);
    }

    HibernateTransactions tx() {
        return tx;
    }

    PermisoService permisos() {
        return permisos;
    }

    static String formatearMontoStatic(BigDecimal monto) {
        return formatearMonto(monto);
    }

    static BigDecimal parsearImporteStatic(String texto) {
        return parsearImporteFormateado(texto);
    }

    private record ResumenOcupacion(
            List<FilaContratoOcupacionReporte> filas,
            int totalContratos,
            int contratosVigentes,
            int contratosPorVencer,
            int totalUnidades,
            int unidadesOcupadas,
            double porcentajeOcupacion
    ) {}

    public static final class FilaPagoReporte {
        private final String contrato;
        private final LocalDate fechaPago;
        private final BigDecimal montoOriginal;
        private final String monedaOriginal;
        private final BigDecimal montoContrato;
        private final String monedaContrato;
        private final String metodo;
        private final String referencia;
        private final String estado;

        public FilaPagoReporte(String contrato,
                               LocalDate fechaPago,
                               BigDecimal montoOriginal,
                               String monedaOriginal,
                               BigDecimal montoContrato,
                               String monedaContrato,
                               String metodo,
                               String referencia,
                               String estado) {
            this.contrato = contrato;
            this.fechaPago = fechaPago;
            this.montoOriginal = montoOriginal;
            this.monedaOriginal = monedaOriginal;
            this.montoContrato = montoContrato;
            this.monedaContrato = monedaContrato;
            this.metodo = metodo;
            this.referencia = referencia;
            this.estado = estado;
        }

        public String getContrato() { return contrato; }
        public LocalDate getFechaPago() { return fechaPago; }
        public BigDecimal getMontoOriginal() { return montoOriginal; }
        public String getMonedaOriginal() { return monedaOriginal; }
        public BigDecimal getMontoContrato() { return montoContrato; }
        public String getMonedaContrato() { return monedaContrato; }
        public String getMetodo() { return metodo; }
        public String getReferencia() { return referencia; }
        public String getEstado() { return estado; }
    }

    public static final class FilaContratoOcupacionReporte {
        private final String inquilino;
        private final String unidad;
        private final String reservaUnidad;
        private final String estadoContrato;
        private final LocalDate fechaInicio;
        private final LocalDate fechaFin;
        private final BigDecimal canon;
        private final String moneda;

        public FilaContratoOcupacionReporte(String inquilino,
                                            String unidad,
                                            String reservaUnidad,
                                            String estadoContrato,
                                            LocalDate fechaInicio,
                                            LocalDate fechaFin,
                                            BigDecimal canon,
                                            String moneda) {
            this.inquilino = inquilino;
            this.unidad = unidad;
            this.reservaUnidad = reservaUnidad;
            this.estadoContrato = estadoContrato;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            this.canon = canon;
            this.moneda = moneda;
        }

        public String getInquilino() { return inquilino; }
        public String getUnidad() { return unidad; }
        public String getReservaUnidad() { return reservaUnidad; }
        public String getEstadoContrato() { return estadoContrato; }
        public LocalDate getFechaInicio() { return fechaInicio; }
        public LocalDate getFechaFin() { return fechaFin; }
        public BigDecimal getCanon() { return canon; }
        public String getMoneda() { return moneda; }
    }

    public static final class FilaPropiedadUnidadReporte {
        private final String propiedad;
        private final String direccion;
        private final String estadoPropiedad;
        private final String codigoUnidad;
        private final String tipoUnidad;
        private final String estadoUnidad;
        private final BigDecimal canonReferencia;
        private final String moneda;

        public FilaPropiedadUnidadReporte(String propiedad,
                                          String direccion,
                                          String estadoPropiedad,
                                          String codigoUnidad,
                                          String tipoUnidad,
                                          String estadoUnidad,
                                          BigDecimal canonReferencia,
                                          String moneda) {
            this.propiedad = propiedad;
            this.direccion = direccion;
            this.estadoPropiedad = estadoPropiedad;
            this.codigoUnidad = codigoUnidad;
            this.tipoUnidad = tipoUnidad;
            this.estadoUnidad = estadoUnidad;
            this.canonReferencia = canonReferencia;
            this.moneda = moneda;
        }

        public String getPropiedad() { return propiedad; }
        public String getDireccion() { return direccion; }
        public String getEstadoPropiedad() { return estadoPropiedad; }
        public String getCodigoUnidad() { return codigoUnidad; }
        public String getTipoUnidad() { return tipoUnidad; }
        public String getEstadoUnidad() { return estadoUnidad; }
        public BigDecimal getCanonReferencia() { return canonReferencia; }
        public String getMoneda() { return moneda; }
    }


    /**
     * Genera el PDF del inventario de propiedades y sus unidades registrado en la base de datos.
     */
    public byte[] generarReportePropiedadesPdf() {
        return exportarPdf(generarDocumentoPropiedades().jasperPrint());
    }

    public DocumentoReporte generarDocumentoPropiedades() {
        List<FilaPropiedadUnidadReporte> filas = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);

            List<Propiedad> propiedades = session
                    .createQuery("select distinct p from Propiedad p left join fetch p.unidades order by p.nombre", Propiedad.class)
                    .getResultList();

            List<FilaPropiedadUnidadReporte> resultado = new java.util.ArrayList<>();
            for (Propiedad propiedad : propiedades) {
                if (propiedad == null) continue;

                String nombrePropiedad = propiedad.getNombre() != null && !propiedad.getNombre().isBlank()
                        ? propiedad.getNombre() : "—";
                String direccion = propiedad.getDireccion() != null && !propiedad.getDireccion().isBlank()
                        ? propiedad.getDireccion() : "—";
                String estadoPropiedad = propiedad.getEstadoTexto();

                List<Unidad> unidadesDePropiedad = propiedad.getUnidades();
                if (unidadesDePropiedad == null || unidadesDePropiedad.isEmpty()) {
                    resultado.add(new FilaPropiedadUnidadReporte(
                            nombrePropiedad,
                            direccion,
                            estadoPropiedad,
                            "—",
                            "Sin unidades",
                            "—",
                            BigDecimal.ZERO,
                            ""
                    ));
                } else {
                    for (Unidad u : unidadesDePropiedad) {
                        if (u == null) continue;
                        String codigo = u.getCodigo() != null && !u.getCodigo().isBlank() ? u.getCodigo() : "—";
                        String tipo = u.getTipo() != null && !u.getTipo().isBlank() ? u.getTipo() : "—";
                        String estadoUnidad = u.getEstado() != null && !u.getEstado().isBlank() ? u.getEstado() : "—";
                        String moneda = u.getMoneda() != null && !u.getMoneda().isBlank() ? u.getMoneda() : "";

                        resultado.add(new FilaPropiedadUnidadReporte(
                                nombrePropiedad,
                                direccion,
                                estadoPropiedad,
                                codigo,
                                tipo,
                                estadoUnidad,
                                u.getCanonReferencia(),
                                moneda
                        ));
                    }
                }
            }
            return resultado;
        });

        if (filas.isEmpty()) {
            throw new IllegalArgumentException("No hay propiedades registradas para generar el reporte.");
        }

        int totalUnidades = (int) filas.stream()
                .filter(f -> !"Sin unidades".equals(f.getTipoUnidad()))
                .count();
        int ocupadas = (int) filas.stream()
                .map(FilaPropiedadUnidadReporte::getEstadoUnidad)
                .filter(Objects::nonNull)
                .map(s -> s.strip().toLowerCase(Locale.ROOT))
                .filter(s -> s.equals("ocupada") || s.equals("ocupado"))
                .count();

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Inventario de Propiedades y Unidades";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_FILAS", filas.size());
        parametros.put("TOTAL_UNIDADES", totalUnidades);
        parametros.put("UNIDADES_OCUPADAS", ocupadas);

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_PROPIEDADES_UNIDADES, parametros, filas);
        TablaReporteXlsx tabla = construirTablaPropiedades(titulo, usuarioActual, filas, totalUnidades, ocupadas);
        return new DocumentoReporte(titulo, "Reporte-Propiedades-Unidades.pdf", jasperPrint, tabla);
    }

    public byte[] generarReporteCargosCarteraDesdeListaPdf(
            List<ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse> cargosEnVista) {
        return exportarPdf(generarDocumentoCargosCarteraDesdeLista(cargosEnVista).jasperPrint());
    }

    public DocumentoReporte generarDocumentoCargosCarteraDesdeLista(
            List<ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse> cargosEnVista) {
        Objects.requireNonNull(cargosEnVista, "La lista de cargos no puede ser nula.");
        if (cargosEnVista.isEmpty()) {
            throw new IllegalArgumentException("No hay cargos para generar el reporte de cartera y mora.");
        }

        LocalDate fechaCorte = LocalDate.now();

        List<FilaCargoCarteraReporte> filas = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return cargosEnVista.stream()
                    .filter(Objects::nonNull)
                    .map(item -> mapearFilaCargoLista(item, fechaCorte))
                    .toList();
        });

        int totalCargos = filas.size();
        int cargosVencidos = (int) filas.stream()
                .filter(f -> "Vencido".equalsIgnoreCase(f.getEstadoDerivado()))
                .count();
        int cargosPendientes = (int) filas.stream()
                .filter(f -> "Pendiente".equalsIgnoreCase(f.getEstadoDerivado())
                        || "Parcial".equalsIgnoreCase(f.getEstadoDerivado()))
                .count();

        BigDecimal totalSaldoUsd = sumarPorMoneda(
                filas, "USD", f -> true,
                FilaCargoCarteraReporte::getMoneda,
                FilaCargoCarteraReporte::getSaldoPendiente);
        BigDecimal totalSaldoNio = sumarPorMoneda(
                filas, "NIO", f -> true,
                FilaCargoCarteraReporte::getMoneda,
                FilaCargoCarteraReporte::getSaldoPendiente);
        BigDecimal totalMoraUsd = sumarPorMoneda(
                filas, "USD", f -> f.getDiasMora() > 0,
                FilaCargoCarteraReporte::getMoneda,
                FilaCargoCarteraReporte::getSaldoPendiente);
        BigDecimal totalMoraNio = sumarPorMoneda(
                filas, "NIO", f -> f.getDiasMora() > 0,
                FilaCargoCarteraReporte::getMoneda,
                FilaCargoCarteraReporte::getSaldoPendiente);

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Cartera de Cargos y Mora";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("FECHA_CORTE", fechaCorte);
        parametros.put("TOTAL_CARGOS", totalCargos);
        parametros.put("CARGOS_VENCIDOS", cargosVencidos);
        parametros.put("CARGOS_PENDIENTES", cargosPendientes);
        parametros.put("TOTAL_SALDO_USD", formatearMonto(totalSaldoUsd));
        parametros.put("TOTAL_SALDO_NIO", formatearMonto(totalSaldoNio));
        parametros.put("TOTAL_MORA_USD", formatearMonto(totalMoraUsd));
        parametros.put("TOTAL_MORA_NIO", formatearMonto(totalMoraNio));

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_CARGOS_CARTERA, parametros, filas);
        TablaReporteXlsx tabla = construirTablaCargosLista(
                titulo, usuarioActual, fechaCorte, filas, totalSaldoUsd, totalSaldoNio, totalMoraUsd, totalMoraNio);
        return new DocumentoReporte(titulo, "Reporte-Cargos-Cartera-Mora.pdf", jasperPrint, tabla);
    }

    private FilaCargoCarteraReporte mapearFilaCargoLista(
            ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse item,
            LocalDate fechaCorte) {
        String contrato = (item.contratoInfo() != null && !item.contratoInfo().isBlank())
                ? item.contratoInfo() : "—";
        String tipo = (item.tipo() != null && !item.tipo().isBlank())
                ? item.tipo() : "—";
        String concepto = (item.concepto() != null && !item.concepto().isBlank())
                ? item.concepto() : "—";

        LocalDate vence = null;
        if (item.venceEl() != null && !item.venceEl().isBlank()) {
            try {
                vence = LocalDate.parse(item.venceEl().trim());
            } catch (Exception ignored) {
                vence = null;
            }
        }

        String textoImporte = item.importeFormateado() == null ? "" : item.importeFormateado();
        String moneda = "—";
        if (textoImporte.contains("USD")) moneda = "USD";
        else if (textoImporte.contains("NIO") || textoImporte.contains("C$")) moneda = "NIO";

        BigDecimal importe = parsearImporteFormateado(textoImporte);

        String estadoRaw = (item.estado() != null && !item.estado().isBlank())
                ? item.estado().trim() : "Pendiente";
        boolean anulado = "Anulado".equalsIgnoreCase(estadoRaw) || "Cancelado".equalsIgnoreCase(estadoRaw);
        boolean pagado = "Pagado".equalsIgnoreCase(estadoRaw);

        BigDecimal saldo = (anulado || pagado)
                ? BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN)
                : importe.setScale(2, java.math.RoundingMode.HALF_EVEN);

        String estadoDerivado = anulado ? "Anulado"
                : pagado ? "Pagado"
                : (vence != null && vence.isBefore(fechaCorte) && saldo.signum() > 0) ? "Vencido"
                : estadoRaw;

        long diasMora = 0;
        if (vence != null && vence.isBefore(fechaCorte) && saldo.signum() > 0 && !anulado && !pagado) {
            diasMora = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(vence, fechaCorte));
        }

        return new FilaCargoCarteraReporte(
                contrato,
                tipo,
                concepto,
                null,
                vence,
                importe,
                saldo,
                moneda,
                diasMora,
                estadoDerivado
        );
    }

    private static BigDecimal parsearImporteFormateado(String texto) {
        if (texto == null || texto.isBlank()) return BigDecimal.ZERO;
        String limpio = texto.replaceAll("[^0-9.,-]", "").trim();
        if (limpio.isEmpty()) return BigDecimal.ZERO;
        // Si tiene coma y punto (ej. 1,250.00), quitar comas de miles
        if (limpio.contains(",") && limpio.contains(".")) {
            limpio = limpio.replace(",", "");
        } else if (limpio.contains(",") && !limpio.contains(".")) {
            // Si solo tiene coma, revisar si es decimal (2 dígitos al final) o miles
            int idx = limpio.lastIndexOf(',');
            if (limpio.length() - idx - 1 == 2) {
                limpio = limpio.replace(",", ".");
            } else {
                limpio = limpio.replace(",", "");
            }
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean esPagoAnulado(FilaPagoReporte fila) {
        return fila != null
                && fila.getEstado() != null
                && "Anulado".equalsIgnoreCase(fila.getEstado().strip());
    }

    private static <T> BigDecimal sumarPorMoneda(List<T> filas,
                                                 String monedaObjetivo,
                                                 Predicate<T> filtro,
                                                 Function<T, String> extractorMoneda,
                                                 Function<T, BigDecimal> extractorMonto) {
        BigDecimal total = filas.stream()
                .filter(Objects::nonNull)
                .filter(filtro)
                .filter(fila -> {
                    String moneda = extractorMoneda.apply(fila);
                    return moneda != null && monedaObjetivo.equalsIgnoreCase(moneda.strip());
                })
                .map(extractorMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String formatearMonto(BigDecimal monto) {
        BigDecimal valor = (monto == null ? BigDecimal.ZERO : monto).setScale(2, RoundingMode.HALF_EVEN);
        java.text.NumberFormat formato = java.text.NumberFormat.getNumberInstance(Locale.US);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }

    public static final class FilaCargoCarteraReporte {
        private final String contrato;
        private final String tipo;
        private final String concepto;
        private final LocalDate periodo;
        private final LocalDate venceEl;
        private final BigDecimal importe;
        private final BigDecimal saldoPendiente;
        private final String moneda;
        private final long diasMora;
        private final String estadoDerivado;

        public FilaCargoCarteraReporte(String contrato,
                                       String tipo,
                                       String concepto,
                                       LocalDate periodo,
                                       LocalDate venceEl,
                                       BigDecimal importe,
                                       BigDecimal saldoPendiente,
                                       String moneda,
                                       long diasMora,
                                       String estadoDerivado) {
            this.contrato = contrato;
            this.tipo = tipo;
            this.concepto = concepto;
            this.periodo = periodo;
            this.venceEl = venceEl;
            this.importe = importe;
            this.saldoPendiente = saldoPendiente;
            this.moneda = moneda;
            this.diasMora = diasMora;
            this.estadoDerivado = estadoDerivado;
        }

        public String getContrato() { return contrato; }
        public String getTipo() { return tipo; }
        public String getConcepto() { return concepto; }
        public LocalDate getPeriodo() { return periodo; }
        public LocalDate getVenceEl() { return venceEl; }
        public BigDecimal getImporte() { return importe; }
        public BigDecimal getSaldoPendiente() { return saldoPendiente; }
        public String getMoneda() { return moneda; }
        public long getDiasMora() { return diasMora; }
        public String getEstadoDerivado() { return estadoDerivado; }
    }

    public byte[] generarReporteInquilinosPdf(List<Inquilino> inquilinosEnVista) {
        return exportarPdf(generarDocumentoInquilinos(inquilinosEnVista).jasperPrint());
    }

    public DocumentoReporte generarDocumentoInquilinos(List<Inquilino> inquilinosEnVista) {
        Objects.requireNonNull(inquilinosEnVista, "La lista de inquilinos no puede ser nula.");
        if (inquilinosEnVista.isEmpty()) {
            throw new IllegalArgumentException("No hay inquilinos en la vista para generar el reporte.");
        }

        List<FilaInquilinoReporte> filas = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return inquilinosEnVista.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapearFilaInquilino)
                    .toList();
        });

        int totalInquilinos = filas.size();
        int activos = (int) filas.stream()
                .filter(f -> "Activo".equalsIgnoreCase(f.getEstado()))
                .count();
        int inactivos = totalInquilinos - activos;

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Directorio de Inquilinos";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_INQUILINOS", totalInquilinos);
        parametros.put("INQUILINOS_ACTIVOS", activos);
        parametros.put("INQUILINOS_INACTIVOS", inactivos);

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_INQUILINOS, parametros, filas);
        TablaReporteXlsx tabla = construirTablaInquilinos(titulo, usuarioActual, filas, activos, inactivos);
        return new DocumentoReporte(titulo, "Reporte-Inquilinos.pdf", jasperPrint, tabla);
    }

    private FilaInquilinoReporte mapearFilaInquilino(Inquilino inquilino) {
        String nombre = (inquilino.getNombre() != null && !inquilino.getNombre().isBlank())
                ? inquilino.getNombre().trim() : "—";
        String identificacion = (inquilino.getIdentificacion() != null && !inquilino.getIdentificacion().isBlank())
                ? inquilino.getIdentificacion().trim() : "—";
        String telefono = (inquilino.getTelefono() != null && !inquilino.getTelefono().isBlank())
                ? inquilino.getTelefono().trim() : "—";
        String correo = (inquilino.getCorreo() != null && !inquilino.getCorreo().isBlank())
                ? inquilino.getCorreo().trim() : "—";
        String emergencia = (inquilino.getContactoEmergencia() != null && !inquilino.getContactoEmergencia().isBlank())
                ? inquilino.getContactoEmergencia().trim() : "—";
        String estado = inquilino.getEstadoTexto();

        return new FilaInquilinoReporte(
                nombre,
                identificacion,
                telefono,
                correo,
                emergencia,
                estado
        );
    }

    public static final class FilaInquilinoReporte {
        private final String nombre;
        private final String identificacion;
        private final String telefono;
        private final String correo;
        private final String contactoEmergencia;
        private final String estado;

        public FilaInquilinoReporte(String nombre,
                                    String identificacion,
                                    String telefono,
                                    String correo,
                                    String contactoEmergencia,
                                    String estado) {
            this.nombre = nombre;
            this.identificacion = identificacion;
            this.telefono = telefono;
            this.correo = correo;
            this.contactoEmergencia = contactoEmergencia;
            this.estado = estado;
        }

        public String getNombre() { return nombre; }
        public String getIdentificacion() { return identificacion; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo; }
        public String getContactoEmergencia() { return contactoEmergencia; }
        public String getEstado() { return estado; }


    }

    public byte[] generarReporteUnidadesPdf(List<Unidad> unidadesEnVista) {
        return exportarPdf(generarDocumentoUnidades(unidadesEnVista).jasperPrint());
    }

    public DocumentoReporte generarDocumentoUnidades(List<Unidad> unidadesEnVista) {
        Objects.requireNonNull(unidadesEnVista, "La lista de unidades no puede ser nula.");
        if (unidadesEnVista.isEmpty()) {
            throw new IllegalArgumentException("No hay unidades en la tabla para generar el reporte.");
        }

        List<FilaUnidadReporte> filas = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return unidadesEnVista.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapearFilaUnidad)
                    .toList();
        });

        int totalUnidades = filas.size();
        int disponibles = (int) filas.stream()
                .filter(f -> "Disponible".equalsIgnoreCase(f.getEstado()))
                .count();
        int ocupadas = (int) filas.stream()
                .filter(f -> {
                    String est = f.getEstado() == null ? "" : f.getEstado().strip().toLowerCase(Locale.ROOT);
                    return "ocupada".equals(est) || "ocupado".equals(est);
                })
                .count();

        BigDecimal totalCanonUsd = sumarPorMoneda(
                filas, "USD", f -> true,
                FilaUnidadReporte::getMoneda,
                FilaUnidadReporte::getCanonReferencia);
        BigDecimal totalCanonNio = sumarPorMoneda(
                filas, "NIO", f -> true,
                FilaUnidadReporte::getMoneda,
                FilaUnidadReporte::getCanonReferencia);

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Catálogo de Unidades";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_UNIDADES", totalUnidades);
        parametros.put("UNIDADES_DISPONIBLES", disponibles);
        parametros.put("UNIDADES_OCUPADAS", ocupadas);
        parametros.put("TOTAL_CANON_USD", formatearMonto(totalCanonUsd));
        parametros.put("TOTAL_CANON_NIO", formatearMonto(totalCanonNio));

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_UNIDADES, parametros, filas);
        TablaReporteXlsx tabla = construirTablaUnidades(titulo, usuarioActual, filas, disponibles, ocupadas, totalCanonUsd, totalCanonNio);
        return new DocumentoReporte(titulo, "Reporte-Unidades.pdf", jasperPrint, tabla);
    }

    private FilaUnidadReporte mapearFilaUnidad(Unidad unidad) {
        String propiedad = null;
        try {
            propiedad = unidad.getPropiedadNombre();
        } catch (Exception ignored) {
            // Respaldo si el proxy no está inicializado
        }
        if (propiedad == null || propiedad.isBlank()) {
            propiedad = unidad.getPropiedadId() != null
                    ? unidad.getPropiedadId().toString()
                    : "—";
        }

        String codigo = (unidad.getCodigo() != null && !unidad.getCodigo().isBlank())
                ? unidad.getCodigo().trim() : "—";
        String tipo = (unidad.getTipo() != null && !unidad.getTipo().isBlank())
                ? unidad.getTipo().trim() : "—";
        String estado = (unidad.getEstado() != null && !unidad.getEstado().isBlank())
                ? unidad.getEstado().trim() : "—";
        BigDecimal canon = unidad.getCanonReferencia() == null
                ? BigDecimal.ZERO : unidad.getCanonReferencia();
        String moneda = (unidad.getMoneda() != null && !unidad.getMoneda().isBlank())
                ? unidad.getMoneda().trim() : "";
        String descripcion = (unidad.getDescripcion() != null && !unidad.getDescripcion().isBlank())
                ? unidad.getDescripcion().trim() : "—";

        return new FilaUnidadReporte(
                propiedad,
                codigo,
                tipo,
                estado,
                canon,
                moneda,
                descripcion
        );
    }

    public static final class FilaUnidadReporte {
        private final String propiedad;
        private final String codigo;
        private final String tipo;
        private final String estado;
        private final BigDecimal canonReferencia;
        private final String moneda;
        private final String descripcion;

        public FilaUnidadReporte(String propiedad,
                                 String codigo,
                                 String tipo,
                                 String estado,
                                 BigDecimal canonReferencia,
                                 String moneda,
                                 String descripcion) {
            this.propiedad = propiedad;
            this.codigo = codigo;
            this.tipo = tipo;
            this.estado = estado;
            this.canonReferencia = canonReferencia;
            this.moneda = moneda;
            this.descripcion = descripcion;
        }

        public String getPropiedad() { return propiedad; }
        public String getCodigo() { return codigo; }
        public String getTipo() { return tipo; }
        public String getEstado() { return estado; }
        public BigDecimal getCanonReferencia() { return canonReferencia; }
        public String getMoneda() { return moneda; }
        public String getDescripcion() { return descripcion; }
    }

    public DocumentoReporte generarDocumentoEstadoCuenta(EstadoCuentaResponse estadoCuenta,
                                                         String descripcionContrato) {
        Objects.requireNonNull(estadoCuenta, "Faltan los datos del estado de cuenta.");

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return null;
        });

        String moneda = textoOVacio(estadoCuenta.moneda(), "USD");
        String fechaCorte = textoOVacio(estadoCuenta.corte(), LocalDate.now().toString());
        String contratoDesc = textoOVacio(descripcionContrato, textoOVacio(estadoCuenta.contratoId(), "—"));

        List<FilaMovimientoEstadoCuenta> filas = new java.util.ArrayList<>();
        if (estadoCuenta.cargos() != null) {
            for (EstadoCuentaResponse.CargoEstadoCuenta cargo : estadoCuenta.cargos()) {
                if (cargo == null) continue;
                String fecha = textoOVacio(cargo.venceEl(), textoOVacio(cargo.fechaEmision(), "—"));
                String referencia = textoOVacio(cargo.tipo(), "Cargo");
                String detalle = textoOVacio(cargo.concepto(), "—");
                filas.add(new FilaMovimientoEstadoCuenta(
                        "Cargos",
                        fecha,
                        referencia,
                        detalle,
                        formatearTextoImporte(cargo.importe(), moneda),
                        formatearTextoImporte(cargo.aplicado(), moneda),
                        formatearTextoImporte(cargo.saldo(), moneda),
                        textoOVacio(cargo.estado(), "—")
                ));
            }
        }
        if (estadoCuenta.pagos() != null) {
            for (EstadoCuentaResponse.PagoEstadoCuenta pago : estadoCuenta.pagos()) {
                if (pago == null) continue;
                String referencia = pago.numeroRecibo() != null
                        ? "Recibo #" + pago.numeroRecibo()
                        : textoOVacio(pago.referencia(), "Pago");
                StringBuilder detalle = new StringBuilder(textoOVacio(pago.metodo(), "Pago"));
                if (pago.referencia() != null && !pago.referencia().isBlank() && pago.numeroRecibo() != null) {
                    detalle.append(" · Ref: ").append(pago.referencia().strip());
                }
                if (pago.monedaOriginal() != null && !pago.monedaOriginal().isBlank()
                        && !moneda.equalsIgnoreCase(pago.monedaOriginal().strip())
                        && pago.montoOriginal() != null && !pago.montoOriginal().isBlank()) {
                    detalle.append(" (Orig: ")
                            .append(formatearTextoImporte(pago.montoOriginal(), pago.monedaOriginal().strip()))
                            .append(")");
                }
                filas.add(new FilaMovimientoEstadoCuenta(
                        "Pagos",
                        textoOVacio(pago.fechaPago(), "—"),
                        referencia,
                        detalle.toString(),
                        formatearTextoImporte(pago.montoContrato(), moneda),
                        formatearTextoImporte(pago.aplicado(), moneda),
                        formatearTextoImporte(pago.credito(), moneda),
                        textoOVacio(pago.estado(), "—")
                ));
            }
        }
        if (filas.isEmpty()) {
            filas.add(new FilaMovimientoEstadoCuenta(
                    "Movimientos",
                    fechaCorte,
                    "—",
                    "Sin movimientos registrados a la fecha de corte.",
                    "—",
                    "—",
                    "—",
                    "—"
            ));
        }

        EstadoCuentaResponse.TotalesEstadoCuenta totales = estadoCuenta.totales();
        EstadoCuentaResponse.GarantiaEstadoCuenta garantia = estadoCuenta.garantia();
        boolean tieneGarantia = garantia != null;

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Estado de Cuenta";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("CONTRATO_DESCRIPCION", contratoDesc);
        parametros.put("FECHA_CORTE", fechaCorte);
        parametros.put("MONEDA", moneda);
        parametros.put("CANON", formatearTextoNumerico(estadoCuenta.canon()));
        parametros.put("TOTAL_CARGOS", formatearTextoNumerico(totales != null ? totales.cargos() : null));
        parametros.put("TOTAL_PAGADO", formatearTextoNumerico(totales != null ? totales.pagado() : null));
        parametros.put("TOTAL_SALDO", formatearTextoNumerico(totales != null ? totales.saldo() : null));
        parametros.put("TOTAL_CREDITO", formatearTextoNumerico(totales != null ? totales.credito() : null));
        parametros.put("TIENE_GARANTIA", tieneGarantia);
        parametros.put("GARANTIA_PACTADA", formatearTextoNumerico(tieneGarantia ? garantia.pactada() : null));
        parametros.put("GARANTIA_SALDO", formatearTextoNumerico(tieneGarantia ? garantia.saldoActual() : null));

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_ESTADO_CUENTA, parametros, filas);
        TablaReporteXlsx tabla = ConsultasReporteHelper.tablaEstadoCuenta(
                estadoCuenta, contratoDesc, usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        return new DocumentoReporte(titulo, "Estado-Cuenta-" + fechaCorte + ".pdf", jasperPrint, tabla);
    }

    private static String textoOVacio(String valor, String respaldo) {
        return valor != null && !valor.isBlank() ? valor.strip() : respaldo;
    }

    private static String formatearTextoNumerico(String montoTexto) {
        return formatearMonto(parsearImporteFormateado(montoTexto));
    }

    private static String formatearTextoImporte(String montoTexto, String moneda) {
        return formatearTextoNumerico(montoTexto) + " " + moneda;
    }

    public static final class FilaMovimientoEstadoCuenta {
        private final String seccion;
        private final String fecha;
        private final String referencia;
        private final String detalle;
        private final String importe;
        private final String aplicado;
        private final String saldoOCredito;
        private final String estado;

        public FilaMovimientoEstadoCuenta(String seccion,
                                          String fecha,
                                          String referencia,
                                          String detalle,
                                          String importe,
                                          String aplicado,
                                          String saldoOCredito,
                                          String estado) {
            this.seccion = seccion;
            this.fecha = fecha;
            this.referencia = referencia;
            this.detalle = detalle;
            this.importe = importe;
            this.aplicado = aplicado;
            this.saldoOCredito = saldoOCredito;
            this.estado = estado;
        }

        public String getSeccion() { return seccion; }
        public String getFecha() { return fecha; }
        public String getReferencia() { return referencia; }
        public String getDetalle() { return detalle; }
        public String getImporte() { return importe; }
        public String getAplicado() { return aplicado; }
        public String getSaldoOCredito() { return saldoOCredito; }
        public String getEstado() { return estado; }
    }

    public DocumentoReporte generarDocumentoTasasCambio(List<TasaResponse> tasasEnVista) {
        Objects.requireNonNull(tasasEnVista, "La lista de tasas no puede ser nula.");
        if (tasasEnVista.isEmpty()) {
            throw new IllegalArgumentException("No hay tasas en la tabla para generar el reporte.");
        }

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return null;
        });

        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatoInstante = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(RelojService.ZONA_OPERATIVA);

        List<FilaTasaReporte> filas = tasasEnVista.stream()
                .filter(Objects::nonNull)
                .map(tasa -> {
                    String fecha = textoOVacio(tasa.fecha(), "—");
                    try {
                        fecha = LocalDate.parse(fecha).format(formatoFecha);
                    } catch (Exception ignored) {
                    }

                    String tasaTexto = "—";
                    if (tasa.nioPorUsd() != null && !tasa.nioPorUsd().isBlank()) {
                        tasaTexto = FormatoTasa.formatear(new BigDecimal(tasa.nioPorUsd().strip()));
                    }

                    String registro = textoOVacio(tasa.registradoEn(), "—");
                    try {
                        registro = formatoInstante.format(OffsetDateTime.parse(registro).toInstant());
                    } catch (Exception ignored) {
                    }

                    return new FilaTasaReporte(
                            fecha,
                            tasaTexto,
                            textoOVacio(tasa.fuente(), "—"),
                            tasa.autorizada() ? "Autorizada" : "Pendiente",
                            textoOVacio(tasa.autorizadoPorNombre(), "—"),
                            registro,
                            textoOVacio(tasa.motivo(), "—")
                    );
                })
                .toList();

        int totalTasas = filas.size();
        int autorizadas = (int) filas.stream()
                .filter(f -> "Autorizada".equalsIgnoreCase(f.getEstado()))
                .count();
        int pendientes = totalTasas - autorizadas;

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Tasas de Cambio";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_TASAS", totalTasas);
        parametros.put("TASAS_AUTORIZADAS", autorizadas);
        parametros.put("TASAS_PENDIENTES", pendientes);

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_TASAS_CAMBIO, parametros, filas);
        TablaReporteXlsx tabla = construirTablaTasasCambio(titulo, usuarioActual, filas, autorizadas, pendientes);
        return new DocumentoReporte(titulo, "Reporte-Tasas-Cambio.pdf", jasperPrint, tabla);
    }

    public DocumentoReporte generarDocumentoResumenDashboard(MetricasResponse metricas) {
        Objects.requireNonNull(metricas, "Faltan las métricas del panel.");

        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return null;
        });

        String instante = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(RelojService.ZONA_OPERATIVA)
                .format(Instant.now());

        List<FilaResumenDashboard> filas = List.of(
                filaDashboard("Cargos pendientes", "USD", metricas.cargosPendientes()),
                filaDashboard("Cargos pendientes", "NIO", metricas.cargosPendientes()),
                filaDashboard("Pagos registrados", "USD", metricas.pagosRegistrados()),
                filaDashboard("Pagos registrados", "NIO", metricas.pagosRegistrados())
        );

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Resumen Ejecutivo del Panel";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("INSTANTE_GENERACION", instante);
        parametros.put("PROPIEDADES_ACTIVAS", metricas.propiedades());
        parametros.put("UNIDADES_TOTAL", metricas.unidades());
        parametros.put("INQUILINOS_ACTIVOS", metricas.inquilinos());
        parametros.put("CONTRATOS_VIGENTES", metricas.contratosVigentes());

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_DASHBOARD_RESUMEN, parametros, filas);
        TablaReporteXlsx tabla = ConsultasReporteHelper.tablaDashboard(
                metricas, instante, usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        return new DocumentoReporte(titulo, "Resumen-Panel-Latch.pdf", jasperPrint, tabla);
    }

    private static FilaResumenDashboard filaDashboard(String indicador,
                                                      String moneda,
                                                      List<TotalMonedaResponse> lista) {
        TotalMonedaResponse encontrado = lista == null ? null : lista.stream()
                .filter(Objects::nonNull)
                .filter(item -> moneda.equalsIgnoreCase(textoOVacio(item.moneda(), "")))
                .findFirst()
                .orElse(null);
        long cantidad = encontrado != null ? encontrado.cantidad() : 0L;
        String total = encontrado != null
                ? formatearTextoImporte(encontrado.total(), moneda)
                : "0.00 " + moneda;
        return new FilaResumenDashboard(indicador, moneda, cantidad, total);
    }

    public DocumentoReporte generarDocumentoGarantias() {
        List<FilaGarantiaReporte> filas = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            List<Garantia> garantias = session.createQuery("""
                            from Garantia g
                            join fetch g.contrato c
                            join fetch c.inquilino
                            join fetch c.unidad u
                            join fetch u.propiedad
                            order by c.inicio desc
                            """, Garantia.class)
                    .getResultList();

            return garantias.stream()
                    .filter(Objects::nonNull)
                    .map(g -> {
                        Contrato contrato = g.getContrato();
                        String inquilino = contrato != null && contrato.getInquilinoNombre() != null
                                && !contrato.getInquilinoNombre().isBlank()
                                ? contrato.getInquilinoNombre().strip() : "—";
                        String unidad = contrato != null && contrato.getUnidadInfo() != null
                                && !contrato.getUnidadInfo().isBlank()
                                ? contrato.getUnidadInfo().strip() : "—";
                        String estadoContrato = contrato != null && contrato.getEstado() != null
                                && !contrato.getEstado().isBlank()
                                ? contrato.getEstado().strip() : "—";
                        String moneda = contrato != null && contrato.getMoneda() != null
                                && !contrato.getMoneda().isBlank()
                                ? contrato.getMoneda().strip() : "";
                        return new FilaGarantiaReporte(
                                inquilino,
                                unidad,
                                estadoContrato,
                                moneda,
                                g.getMontoInicial(),
                                g.getSaldoActual()
                        );
                    })
                    .toList();
        });

        if (filas.isEmpty()) {
            throw new IllegalArgumentException("No hay depósitos de garantía registrados para generar el reporte.");
        }

        BigDecimal totalPactadoUsd = sumarPorMoneda(
                filas, "USD", f -> true,
                FilaGarantiaReporte::getMoneda,
                FilaGarantiaReporte::getMontoPactado);
        BigDecimal totalSaldoUsd = sumarPorMoneda(
                filas, "USD", f -> true,
                FilaGarantiaReporte::getMoneda,
                FilaGarantiaReporte::getSaldoActual);
        BigDecimal totalPactadoNio = sumarPorMoneda(
                filas, "NIO", f -> true,
                FilaGarantiaReporte::getMoneda,
                FilaGarantiaReporte::getMontoPactado);
        BigDecimal totalSaldoNio = sumarPorMoneda(
                filas, "NIO", f -> true,
                FilaGarantiaReporte::getMoneda,
                FilaGarantiaReporte::getSaldoActual);

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        String titulo = "LATCH — Depósitos de Garantía";
        parametros.put("TITULO_SISTEMA", titulo);
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_GARANTIAS", filas.size());
        parametros.put("TOTAL_PACTADO_USD", formatearMonto(totalPactadoUsd));
        parametros.put("TOTAL_SALDO_USD", formatearMonto(totalSaldoUsd));
        parametros.put("TOTAL_PACTADO_NIO", formatearMonto(totalPactadoNio));
        parametros.put("TOTAL_SALDO_NIO", formatearMonto(totalSaldoNio));

        JasperPrint jasperPrint = llenarReporte(PLANTILLA_GARANTIAS_DEPOSITOS, parametros, filas);
        TablaReporteXlsx tabla = construirTablaGarantiasBasica(
                titulo, usuarioActual, filas, totalPactadoUsd, totalSaldoUsd, totalPactadoNio, totalSaldoNio);
        return new DocumentoReporte(titulo, "Reporte-Garantias-Depositos.pdf", jasperPrint, tabla);
    }

    // --- Catálogo completo REP-02 a REP-10 (consulta directa en BD con permisos y cancelación cooperativa) ---

    public DocumentoReporte generarDocumentoEstadoCuenta(UUID contratoId, LocalDate fechaCorte) {
        return generarDocumentoEstadoCuenta(contratoId, fechaCorte, null);
    }

    public DocumentoReporte generarDocumentoEstadoCuenta(UUID contratoId,
                                                         LocalDate fechaCorte,
                                                         BooleanSupplier cancelado) {
        ConsultasReporteHelper.verificarCancelacion(cancelado);
        Objects.requireNonNull(contratoId, "Selecciona un contrato para generar su estado de cuenta.");
        record EstadoConDesc(EstadoCuentaResponse estado, String descripcion) {}
        EstadoConDesc datos = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            EstadoCuentaResponse resp = ConsultasReporteHelper.consultarEstadoCuenta(session, contratoId, fechaCorte);
            Contrato c = session.createQuery("""
                            from Contrato c
                            join fetch c.inquilino
                            join fetch c.unidad u
                            join fetch u.propiedad
                            where c.id = :id
                            """, Contrato.class)
                    .setParameter("id", contratoId)
                    .uniqueResult();
            String desc = c != null ? c.toString() : contratoId.toString();
            return new EstadoConDesc(resp, desc);
        });
        ConsultasReporteHelper.verificarCancelacion(cancelado);
        return generarDocumentoEstadoCuenta(datos.estado(), datos.descripcion());
    }

    public DocumentoReporte generarDocumentoCarteraVencida(LocalDate fechaCorte,
                                                           UUID propiedadId,
                                                           UUID contratoId,
                                                           String moneda) {
        return generarDocumentoCarteraVencida(fechaCorte, propiedadId, contratoId, moneda, null);
    }

    public DocumentoReporte generarDocumentoCarteraVencida(LocalDate fechaCorte,
                                                           UUID propiedadId,
                                                           UUID contratoId,
                                                           String moneda,
                                                           BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarCarteraVencida(this, fechaCorte, propiedadId, contratoId, moneda, cancelado);
    }

    public DocumentoReporte generarDocumentoIngresosYGastos(LocalDate desde,
                                                             LocalDate hasta,
                                                             UUID propiedadId,
                                                             String moneda) {
        return generarDocumentoIngresosYGastos(desde, hasta, propiedadId, moneda, null);
    }

    public DocumentoReporte generarDocumentoIngresosYGastos(LocalDate desde,
                                                             LocalDate hasta,
                                                             UUID propiedadId,
                                                             String moneda,
                                                             BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarIngresosYGastos(this, desde, hasta, propiedadId, moneda, cancelado);
    }

    public DocumentoReporte generarDocumentoOcupacion(UUID propiedadId, String tipoUnidad) {
        return generarDocumentoOcupacion(propiedadId, tipoUnidad, null);
    }

    public DocumentoReporte generarDocumentoOcupacion(UUID propiedadId,
                                                      String tipoUnidad,
                                                      BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarOcupacion(this, propiedadId, tipoUnidad, cancelado);
    }

    public DocumentoReporte generarDocumentoContratosPorVencer(LocalDate fechaBase,
                                                               Integer umbralDias,
                                                               UUID propiedadId,
                                                               String moneda) {
        return generarDocumentoContratosPorVencer(fechaBase, umbralDias, propiedadId, moneda, null);
    }

    public DocumentoReporte generarDocumentoContratosPorVencer(LocalDate fechaBase,
                                                               Integer umbralDias,
                                                               UUID propiedadId,
                                                               String moneda,
                                                               BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarContratosPorVencer(
                this, fechaBase, umbralDias, propiedadId, moneda, cancelado);
    }

    public DocumentoReporte generarDocumentoHistorialPagos(UUID inquilinoId,
                                                           UUID contratoId,
                                                           LocalDate desde,
                                                           LocalDate hasta,
                                                           String moneda) {
        return generarDocumentoHistorialPagos(inquilinoId, contratoId, desde, hasta, moneda, null);
    }

    public DocumentoReporte generarDocumentoHistorialPagos(UUID inquilinoId,
                                                           UUID contratoId,
                                                           LocalDate desde,
                                                           LocalDate hasta,
                                                           String moneda,
                                                           BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarHistorialPagos(
                this, inquilinoId, contratoId, desde, hasta, moneda, cancelado);
    }

    public DocumentoReporte generarDocumentoLiquidacionGarantias(UUID contratoId,
                                                                 LocalDate fechaCorte,
                                                                 String moneda) {
        return generarDocumentoLiquidacionGarantias(contratoId, fechaCorte, moneda, null);
    }

    public DocumentoReporte generarDocumentoLiquidacionGarantias(UUID contratoId,
                                                                 LocalDate fechaCorte,
                                                                 String moneda,
                                                                 BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarLiquidacionGarantias(this, contratoId, fechaCorte, moneda, cancelado);
    }

    public DocumentoReporte generarDocumentoMantenimiento(UUID propiedadId,
                                                          UUID unidadId,
                                                          LocalDate desde,
                                                          LocalDate hasta,
                                                          String estado,
                                                          String prioridad,
                                                          String moneda) {
        return generarDocumentoMantenimiento(propiedadId, unidadId, desde, hasta, estado, prioridad, moneda, null);
    }

    public DocumentoReporte generarDocumentoMantenimiento(UUID propiedadId,
                                                          UUID unidadId,
                                                          LocalDate desde,
                                                          LocalDate hasta,
                                                          String estado,
                                                          String prioridad,
                                                          String moneda,
                                                          BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarMantenimiento(
                this, propiedadId, unidadId, desde, hasta, estado, prioridad, moneda, cancelado);
    }

    public DocumentoReporte generarDocumentoTasasUtilizadas(LocalDate desde,
                                                            LocalDate hasta,
                                                            String origenFuente) {
        return generarDocumentoTasasUtilizadas(desde, hasta, origenFuente, null);
    }

    public DocumentoReporte generarDocumentoTasasUtilizadas(LocalDate desde,
                                                            LocalDate hasta,
                                                            String origenFuente,
                                                            BooleanSupplier cancelado) {
        return ConsultasReporteHelper.generarTasasUtilizadas(this, desde, hasta, origenFuente, cancelado);
    }

    private static Map<String, String> metaBase(SesionUsuario usuario) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Generado por", usuario != null ? usuario.getNombre() : "Sistema");
        return meta;
    }

    private static TablaReporteXlsx construirTablaPagos(String titulo, SesionUsuario usuario,
                                                        List<FilaPagoReporte> filas,
                                                        BigDecimal netoUsd, BigDecimal netoNio, int anulados) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Registros", Integer.toString(filas.size()));
        meta.put("Pagos anulados", Integer.toString(anulados));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha pago", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Monto original", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda original", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Monto contrato", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Método", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Referencia", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getContrato(),
                        f.getFechaPago() != null ? f.getFechaPago() : "—",
                        f.getMontoOriginal() != null ? f.getMontoOriginal() : BigDecimal.ZERO,
                        f.getMonedaOriginal(),
                        f.getMontoContrato() != null ? f.getMontoContrato() : BigDecimal.ZERO,
                        f.getMonedaContrato(),
                        f.getMetodo(),
                        f.getReferencia() == null ? "—" : f.getReferencia(),
                        f.getEstado()))
                .toList();
        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Total neto USD (excluye anulados)", "USD", netoUsd),
                TablaReporteXlsx.Resumen.numerico("Total neto NIO (excluye anulados)", "NIO", netoNio)
        );
        return new TablaReporteXlsx("REP-07", "Pagos", titulo, meta, columnas, filasXlsx, resumenes);
    }

    private static TablaReporteXlsx construirTablaContratosOcupacion(String titulo, SesionUsuario usuario,
                                                                     ResumenOcupacion r) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Ocupación", r.unidadesOcupadas() + " / " + r.totalUnidades()
                + String.format(Locale.ROOT, " (%.1f%%)", r.porcentajeOcupacion()));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Reserva unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Inicio", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Fin", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Canon", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = r.filas().stream()
                .map(f -> List.<Object>of(
                        f.getInquilino(), f.getUnidad(), f.getReservaUnidad(), f.getEstadoContrato(),
                        f.getFechaInicio() != null ? f.getFechaInicio() : "—",
                        f.getFechaFin() != null ? f.getFechaFin() : "—",
                        f.getCanon() != null ? f.getCanon() : BigDecimal.ZERO,
                        f.getMoneda()))
                .toList();
        return new TablaReporteXlsx("REP-05", "Contratos y ocupación", titulo, meta, columnas, filasXlsx, List.of());
    }

    private static TablaReporteXlsx construirTablaPropiedades(String titulo, SesionUsuario usuario,
                                                              List<FilaPropiedadUnidadReporte> filas,
                                                              int totalUnidades, int ocupadas) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Total unidades", Integer.toString(totalUnidades));
        meta.put("Unidades ocupadas", Integer.toString(ocupadas));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Dirección", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Código unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tipo unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado unidad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Canon referencia", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getPropiedad(), f.getDireccion(), f.getEstadoPropiedad(), f.getCodigoUnidad(),
                        f.getTipoUnidad(), f.getEstadoUnidad(),
                        f.getCanonReferencia() != null ? f.getCanonReferencia() : BigDecimal.ZERO,
                        f.getMoneda()))
                .toList();
        return new TablaReporteXlsx("REP-05", "Propiedades", titulo, meta, columnas, filasXlsx, List.of());
    }

    private static TablaReporteXlsx construirTablaCargosLista(String titulo, SesionUsuario usuario,
                                                              LocalDate corte, List<FilaCargoCarteraReporte> filas,
                                                              BigDecimal saldoUsd, BigDecimal saldoNio,
                                                              BigDecimal moraUsd, BigDecimal moraNio) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Fecha de corte", corte.toString());
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tipo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Concepto", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Vence", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Importe", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Saldo pendiente", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Días mora", TablaReporteXlsx.Tipo.ENTERO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getContrato(), f.getTipo(), f.getConcepto(),
                        f.getVenceEl() != null ? f.getVenceEl() : "—",
                        f.getImporte() != null ? f.getImporte() : BigDecimal.ZERO,
                        f.getSaldoPendiente() != null ? f.getSaldoPendiente() : BigDecimal.ZERO,
                        f.getMoneda(), f.getDiasMora(), f.getEstadoDerivado()))
                .toList();
        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Saldo pendiente USD", "USD", saldoUsd),
                TablaReporteXlsx.Resumen.numerico("Saldo pendiente NIO", "NIO", saldoNio),
                TablaReporteXlsx.Resumen.numerico("Mora vencida USD", "USD", moraUsd),
                TablaReporteXlsx.Resumen.numerico("Mora vencida NIO", "NIO", moraNio)
        );
        return new TablaReporteXlsx("REP-03", "Cargos y cartera", titulo, meta, columnas, filasXlsx, resumenes);
    }

    private static TablaReporteXlsx construirTablaInquilinos(String titulo, SesionUsuario usuario,
                                                             List<FilaInquilinoReporte> filas,
                                                             int activos, int inactivos) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Activos", Integer.toString(activos));
        meta.put("Inactivos", Integer.toString(inactivos));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Nombre", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Identificación", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Teléfono", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Correo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Contacto emergencia", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getNombre(), f.getIdentificacion(), f.getTelefono(),
                        f.getCorreo(), f.getContactoEmergencia(), f.getEstado()))
                .toList();
        return new TablaReporteXlsx("REP-07", "Inquilinos", titulo, meta, columnas, filasXlsx, List.of());
    }

    private static TablaReporteXlsx construirTablaUnidades(String titulo, SesionUsuario usuario,
                                                           List<FilaUnidadReporte> filas,
                                                           int disponibles, int ocupadas,
                                                           BigDecimal canonUsd, BigDecimal canonNio) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Disponibles", Integer.toString(disponibles));
        meta.put("Ocupadas", Integer.toString(ocupadas));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Código", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tipo", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Canon referencia", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Descripción", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getPropiedad(), f.getCodigo(), f.getTipo(), f.getEstado(),
                        f.getCanonReferencia() != null ? f.getCanonReferencia() : BigDecimal.ZERO,
                        f.getMoneda(), f.getDescripcion()))
                .toList();
        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Total canon USD", "USD", canonUsd),
                TablaReporteXlsx.Resumen.numerico("Total canon NIO", "NIO", canonNio)
        );
        return new TablaReporteXlsx("REP-05", "Unidades", titulo, meta, columnas, filasXlsx, resumenes);
    }

    private static TablaReporteXlsx construirTablaTasasCambio(String titulo, SesionUsuario usuario,
                                                              List<FilaTasaReporte> filas,
                                                              int autorizadas, int pendientes) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Autorizadas", Integer.toString(autorizadas));
        meta.put("Pendientes", Integer.toString(pendientes));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Tasa NIO/USD", TablaReporteXlsx.Tipo.TASA),
                new TablaReporteXlsx.Columna("Fuente", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Autorizado por", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Registrado en", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Motivo", TablaReporteXlsx.Tipo.TEXTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getFecha(),
                        parseTasaOBigDecimal(f.getTasaFormateada()),
                        f.getFuente(), f.getEstado(), f.getAutorizadoPor(),
                        f.getRegistradoEn(), f.getMotivo()))
                .toList();
        return new TablaReporteXlsx("REP-10", "Tasas de cambio", titulo, meta, columnas, filasXlsx, List.of());
    }

    private static Object parseTasaOBigDecimal(String texto) {
        if (texto == null || texto.isBlank() || "—".equals(texto.strip())) {
            return "—";
        }
        try {
            return new BigDecimal(texto.strip().replace(",", ""));
        } catch (NumberFormatException ignored) {
            return texto;
        }
    }

    private static TablaReporteXlsx construirTablaGarantiasBasica(String titulo, SesionUsuario usuario,
                                                                   List<FilaGarantiaReporte> filas,
                                                                   BigDecimal pactadoUsd, BigDecimal saldoUsd,
                                                                   BigDecimal pactadoNio, BigDecimal saldoNio) {
        Map<String, String> meta = metaBase(usuario);
        meta.put("Total garantías", Integer.toString(filas.size()));
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Inquilino", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Unidad / Propiedad", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Estado contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Moneda", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Monto pactado", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Saldo actual", TablaReporteXlsx.Tipo.MONTO)
        );
        List<List<Object>> filasXlsx = filas.stream()
                .map(f -> List.<Object>of(
                        f.getInquilino(), f.getUnidad(), f.getEstadoContrato(), f.getMoneda(),
                        f.getMontoPactado() != null ? f.getMontoPactado() : BigDecimal.ZERO,
                        f.getSaldoActual() != null ? f.getSaldoActual() : BigDecimal.ZERO))
                .toList();
        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Pactado USD", "USD", pactadoUsd),
                TablaReporteXlsx.Resumen.numerico("Saldo USD", "USD", saldoUsd),
                TablaReporteXlsx.Resumen.numerico("Pactado NIO", "NIO", pactadoNio),
                TablaReporteXlsx.Resumen.numerico("Saldo NIO", "NIO", saldoNio)
        );
        return new TablaReporteXlsx("REP-08", "Garantías", titulo, meta, columnas, filasXlsx, resumenes);
    }

    public static final class FilaTasaReporte {
        private final String fecha;
        private final String tasaFormateada;
        private final String fuente;
        private final String estado;
        private final String autorizadoPor;
        private final String registradoEn;
        private final String motivo;

        public FilaTasaReporte(String fecha,
                               String tasaFormateada,
                               String fuente,
                               String estado,
                               String autorizadoPor,
                               String registradoEn,
                               String motivo) {
            this.fecha = fecha;
            this.tasaFormateada = tasaFormateada;
            this.fuente = fuente;
            this.estado = estado;
            this.autorizadoPor = autorizadoPor;
            this.registradoEn = registradoEn;
            this.motivo = motivo;
        }

        public String getFecha() { return fecha; }
        public String getTasaFormateada() { return tasaFormateada; }
        public String getFuente() { return fuente; }
        public String getEstado() { return estado; }
        public String getAutorizadoPor() { return autorizadoPor; }
        public String getRegistradoEn() { return registradoEn; }
        public String getMotivo() { return motivo; }
    }

    public static final class FilaResumenDashboard {
        private final String indicador;
        private final String moneda;
        private final long cantidad;
        private final String total;

        public FilaResumenDashboard(String indicador, String moneda, long cantidad, String total) {
            this.indicador = indicador;
            this.moneda = moneda;
            this.cantidad = cantidad;
            this.total = total;
        }

        public String getIndicador() { return indicador; }
        public String getMoneda() { return moneda; }
        public long getCantidad() { return cantidad; }
        public String getTotal() { return total; }
    }

    public static final class FilaGarantiaReporte {
        private final String inquilino;
        private final String unidad;
        private final String estadoContrato;
        private final String moneda;
        private final BigDecimal montoPactado;
        private final BigDecimal saldoActual;

        public FilaGarantiaReporte(String inquilino,
                                   String unidad,
                                   String estadoContrato,
                                   String moneda,
                                   BigDecimal montoPactado,
                                   BigDecimal saldoActual) {
            this.inquilino = inquilino;
            this.unidad = unidad;
            this.estadoContrato = estadoContrato;
            this.moneda = moneda;
            this.montoPactado = montoPactado;
            this.saldoActual = saldoActual;
        }

        public String getInquilino() { return inquilino; }
        public String getUnidad() { return unidad; }
        public String getEstadoContrato() { return estadoContrato; }
        public String getMoneda() { return moneda; }
        public BigDecimal getMontoPactado() { return montoPactado; }
        public BigDecimal getSaldoActual() { return saldoActual; }
    }

}