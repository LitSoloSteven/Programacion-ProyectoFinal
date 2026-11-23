package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ReporteService {
    private static final String PLANTILLA_RECIBO = "/reportes/recibo.jrxml";
    private static final String PLANTILLA_PAGOS_TABLA = "/reportes/pagos_tabla.jrxml";
    private static final String PLANTILLA_CONTRATOS_OCUPACION = "/reportes/contratos_ocupacion.jrxml";
    private static final String PLANTILLA_PROPIEDADES_UNIDADES = "/reportes/propiedades_unidades.jrxml";

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

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", "LATCH — Reporte de Pagos");
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_REGISTROS", filas.size());

        return exportarPdf(PLANTILLA_PAGOS_TABLA, parametros, filas);
    }

    public byte[] generarPdfPago(UUID pagoId) {
        Objects.requireNonNull(pagoId, "Falta el identificador del pago.");

        ReciboDTO datosRecibo = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            Recibo recibo = pagoService.obtenerReciboDePago(pagoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontró un recibo asociado al pago indicado."));
            return recibo.presentar();
        });

        SesionUsuario usuarioActual = SesionUsuario.getActual();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO_SISTEMA", "LATCH — Gestión de Arrendamientos");
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");

        return exportarPdf(PLANTILLA_RECIBO, parametros, List.of(datosRecibo));
    }

    /**
     * Genera el PDF del resumen de contratos y ocupación a partir de los contratos en vista.
     */
    public byte[] generarReporteContratosOcupacionPdf(List<Contrato> contratosEnVista) {
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
        parametros.put("TITULO_SISTEMA", "LATCH — Resumen de Contratos y Ocupación");
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_CONTRATOS", datosResumen.totalContratos());
        parametros.put("CONTRATOS_VIGENTES", datosResumen.contratosVigentes());
        parametros.put("CONTRATOS_POR_VENCER", datosResumen.contratosPorVencer());
        parametros.put("TOTAL_UNIDADES", datosResumen.totalUnidades());
        parametros.put("UNIDADES_OCUPADAS", datosResumen.unidadesOcupadas());
        parametros.put("PORCENTAJE_OCUPACION", String.format(Locale.ROOT, "%.1f%%", datosResumen.porcentajeOcupacion()));

        return exportarPdf(PLANTILLA_CONTRATOS_OCUPACION, parametros, datosResumen.filas());
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

    private static <T> byte[] exportarPdf(String rutaPlantilla,
                                          Map<String, Object> parametros,
                                          List<T> listaDatos) {
        try (InputStream streamPlantilla = ReporteService.class.getResourceAsStream(rutaPlantilla)) {
            if (streamPlantilla == null) {
                throw new IllegalStateException("No existe la plantilla en resources: " + rutaPlantilla);
            }
            JasperReport reporteCompilado = JasperCompileManager.compileReport(streamPlantilla);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(listaDatos);
            JasperPrint reporteLleno = JasperFillManager.fillReport(reporteCompilado, parametros, dataSource);
            return JasperExportManager.exportReportToPdf(reporteLleno);
        } catch (JRException e) {
            throw new IllegalStateException("JasperReports falló al generar el PDF: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Error inesperado al exportar el reporte.", e);
        }
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
        parametros.put("TITULO_SISTEMA", "LATCH — Inventario de Propiedades y Unidades");
        parametros.put("GENERADO_POR", usuarioActual != null ? usuarioActual.getNombre() : "Sistema");
        parametros.put("TOTAL_FILAS", filas.size());
        parametros.put("TOTAL_UNIDADES", totalUnidades);
        parametros.put("UNIDADES_OCUPADAS", ocupadas);

        return exportarPdf(PLANTILLA_PROPIEDADES_UNIDADES, parametros, filas);
    }



}