package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.OperacionCanceladaException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.GestorH5Reportes;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

/** Controlador del Centro de Reportes operativo (PAN-15 / M10 / H5). */
public class ReportesController {

    private static final String TODAS = "Todas";
    private static final String TODOS = "Todos";

    public enum TipoReporte {
        REP_02("REP-02 · Estado de cuenta de contrato",
                "Cargos, abonos, anulaciones, saldo y crédito disponible de un contrato al corte."),
        REP_03("REP-03 · Cartera vencida por antigüedad",
                "Saldos abiertos al corte clasificados por tramos (Al día, 1-30, 31-60, 61-90, +90)."),
        REP_04("REP-04 · Ingresos y gastos por moneda",
                "Separa rentas netas, otros cobros, gastos de mantenimiento y movimientos de garantía."),
        REP_05("REP-05 · Ocupación por propiedad (al instante)",
                "Unidades ocupadas sobre unidades activas al instante de generación, sin selector histórico."),
        REP_06("REP-06 · Contratos por vencer",
                "Contratos activos cuyo vencimiento ocurre dentro del umbral configurado."),
        REP_07("REP-07 · Historial de pagos del inquilino",
                "Pagos registrados y anulados con número de recibo; excluye anulados del neto."),
        REP_08("REP-08 · Liquidación de depósitos en garantía",
                "Depósito inicial, aplicaciones, deducciones, devoluciones y remanente conciliable."),
        REP_09("REP-09 · Mantenimiento y gastos de propiedad",
                "Solicitudes por unidad/estado/prioridad y costos efectivos separados por moneda."),
        REP_10("REP-10 · Tasas utilizadas en pagos",
                "Tasas históricas efectivamente aplicadas en pagos (no redistribuye catálogo externo).");

        private final String etiqueta;
        private final String descripcion;

        TipoReporte(String etiqueta, String descripcion) {
            this.etiqueta = etiqueta;
            this.descripcion = descripcion;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    @FXML private VBox rootView;
    @FXML private Label lblTitulo;
    @FXML private Button btnRefrescarCatalogos;
    @FXML private ComboBox<TipoReporte> cmbTipoReporte;
    @FXML private Label lblDescripcionReporte;

    @FXML private VBox boxFechaCorte;
    @FXML private DatePicker dpFechaCorte;
    @FXML private VBox boxDesde;
    @FXML private DatePicker dpDesde;
    @FXML private VBox boxHasta;
    @FXML private DatePicker dpHasta;
    @FXML private VBox boxUmbralDias;
    @FXML private TextField txtUmbralDias;
    @FXML private VBox boxMoneda;
    @FXML private ComboBox<String> cmbMoneda;
    @FXML private VBox boxPropiedad;
    @FXML private ComboBox<Propiedad> cmbPropiedad;
    @FXML private VBox boxUnidad;
    @FXML private ComboBox<Unidad> cmbUnidad;
    @FXML private VBox boxTipoUnidad;
    @FXML private ComboBox<String> cmbTipoUnidad;
    @FXML private VBox boxContrato;
    @FXML private ComboBox<Contrato> cmbContrato;
    @FXML private VBox boxInquilino;
    @FXML private ComboBox<Inquilino> cmbInquilino;
    @FXML private VBox boxEstadoMantenimiento;
    @FXML private ComboBox<String> cmbEstadoMantenimiento;
    @FXML private VBox boxPrioridadMantenimiento;
    @FXML private ComboBox<String> cmbPrioridadMantenimiento;
    @FXML private VBox boxOrigenTasa;
    @FXML private TextField txtOrigenTasa;

    @FXML private Label lblEstadoH5;
    @FXML private Label lblError;
    @FXML private Button btnVistaPrevia;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnExportarXlsx;
    @FXML private Button btnCancelarH5;

    private final ClienteCatalogos catalogos = new ClienteCatalogos(ClienteLatch.api());
    private final ReporteService reporteService = new ReporteService();
    private GestorH5Reportes.EjecucionH5 ejecucionActiva;

    private record CatalogosCargados(
            List<Propiedad> propiedades,
            List<Unidad> unidades,
            List<Contrato> contratos,
            List<Inquilino> inquilinos) { }

    @FXML
    public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.REPORTE, 20));
        Iconos.asignar(btnRefrescarCatalogos, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnVistaPrevia, Iconos.Nombre.VER, 16);
        Iconos.asignar(btnExportarPdf, Iconos.Nombre.DESCARGAR, 16);
        Iconos.asignar(btnExportarXlsx, Iconos.Nombre.DESCARGAR, 16);
        Iconos.asignar(btnCancelarH5, Iconos.Nombre.CERRAR, 16);

        configurarControles();
        cargarCatalogos();
    }

    private void configurarControles() {
        cmbTipoReporte.setItems(FXCollections.observableArrayList(TipoReporte.values()));
        cmbTipoReporte.setValue(TipoReporte.REP_03);
        cmbTipoReporte.valueProperty().addListener((o, a, b) -> actualizarVisibilidadFiltros());

        dpFechaCorte.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
        txtUmbralDias.setText("30");

        cmbMoneda.setItems(FXCollections.observableArrayList(TODAS, "USD", "NIO"));
        cmbMoneda.setValue(TODAS);

        cmbTipoUnidad.setItems(FXCollections.observableArrayList(
                TODOS, "Apartamento", "Casa", "Local", "Oficina", "Bodega", "Otro"));
        cmbTipoUnidad.setValue(TODOS);

        cmbEstadoMantenimiento.setItems(FXCollections.observableArrayList(
                TODOS, SolicitudMantenimiento.ESTADO_ABIERTA,
                SolicitudMantenimiento.ESTADO_EN_PROCESO,
                SolicitudMantenimiento.ESTADO_RESUELTA,
                SolicitudMantenimiento.ESTADO_CANCELADA));
        cmbEstadoMantenimiento.setValue(TODOS);

        cmbPrioridadMantenimiento.setItems(FXCollections.observableArrayList(
                TODAS, SolicitudMantenimiento.PRIORIDAD_BAJA,
                SolicitudMantenimiento.PRIORIDAD_MEDIA,
                SolicitudMantenimiento.PRIORIDAD_ALTA));
        cmbPrioridadMantenimiento.setValue(TODAS);

        cmbPropiedad.setConverter(new StringConverter<>() {
            @Override public String toString(Propiedad p) { return p == null ? "Todas las propiedades" : p.getNombre(); }
            @Override public Propiedad fromString(String s) { return null; }
        });
        cmbUnidad.setConverter(new StringConverter<>() {
            @Override public String toString(Unidad u) { return u == null ? "Todas las unidades" : u.toString(); }
            @Override public Unidad fromString(String s) { return null; }
        });
        cmbContrato.setConverter(new StringConverter<>() {
            @Override public String toString(Contrato c) { return c == null ? "Todos / Seleccionar contrato" : c.toString(); }
            @Override public Contrato fromString(String s) { return null; }
        });
        cmbInquilino.setConverter(new StringConverter<>() {
            @Override public String toString(Inquilino i) { return i == null ? "Todos los inquilinos" : i.getNombre(); }
            @Override public Inquilino fromString(String s) { return null; }
        });

        actualizarVisibilidadFiltros();
    }

    @FXML
    public void cargarCatalogos() {
        ocultarError();
        btnRefrescarCatalogos.setDisable(true);
        Task<CatalogosCargados> tarea = new Task<>() {
            @Override
            protected CatalogosCargados call() {
                return new CatalogosCargados(
                        catalogos.propiedades(false),
                        catalogos.unidades(),
                        catalogos.contratos(false),
                        catalogos.inquilinos(false));
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            btnRefrescarCatalogos.setDisable(false);
            poblarComboConNulo(cmbPropiedad, datos.propiedades());
            poblarComboConNulo(cmbUnidad, datos.unidades());
            poblarComboConNulo(cmbContrato, datos.contratos());
            poblarComboConNulo(cmbInquilino, datos.inquilinos());
        }, error -> {
            btnRefrescarCatalogos.setDisable(false);
            mostrarError("No se pudieron cargar los catálogos para filtros.");
        });
    }

    private <T> void poblarComboConNulo(ComboBox<T> combo, List<T> elementos) {
        T seleccionado = combo.getValue();
        var lista = FXCollections.<T>observableArrayList();
        lista.add(null);
        lista.addAll(elementos);
        combo.setItems(lista);
        combo.setValue(seleccionado);
    }

    private void actualizarVisibilidadFiltros() {
        TipoReporte tipo = cmbTipoReporte.getValue();
        if (tipo == null) return;
        lblDescripcionReporte.setText(tipo.descripcion);
        ocultarError();

        ocultarTodosFiltros();
        switch (tipo) {
            case REP_02 -> {
                mostrar(boxFechaCorte);
                mostrar(boxContrato);
            }
            case REP_03 -> {
                mostrar(boxFechaCorte);
                mostrar(boxPropiedad);
                mostrar(boxContrato);
                mostrar(boxMoneda);
            }
            case REP_04 -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxPropiedad);
                mostrar(boxMoneda);
            }
            case REP_05 -> {
                // REP-05 toma el instante de generación; no ofrece selector de fecha histórica.
                mostrar(boxPropiedad);
                mostrar(boxTipoUnidad);
            }
            case REP_06 -> {
                mostrar(boxFechaCorte);
                mostrar(boxUmbralDias);
                mostrar(boxPropiedad);
                mostrar(boxMoneda);
            }
            case REP_07 -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxContrato);
                mostrar(boxInquilino);
                mostrar(boxMoneda);
            }
            case REP_08 -> {
                mostrar(boxFechaCorte);
                mostrar(boxContrato);
                mostrar(boxMoneda);
            }
            case REP_09 -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxPropiedad);
                mostrar(boxUnidad);
                mostrar(boxEstadoMantenimiento);
                mostrar(boxPrioridadMantenimiento);
                mostrar(boxMoneda);
            }
            case REP_10 -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxOrigenTasa);
            }
        }
    }

    private void ocultarTodosFiltros() {
        for (VBox box : List.of(
                boxFechaCorte, boxDesde, boxHasta, boxUmbralDias, boxMoneda,
                boxPropiedad, boxUnidad, boxTipoUnidad, boxContrato, boxInquilino,
                boxEstadoMantenimiento, boxPrioridadMantenimiento, boxOrigenTasa)) {
            box.setVisible(false);
            box.setManaged(false);
        }
    }

    private static void mostrar(VBox box) {
        box.setVisible(true);
        box.setManaged(true);
    }

    @FXML
    public void handleVistaPrevia() {
        ejecutarH5(ModoSalida.VISTA_PREVIA, null);
    }

    @FXML
    public void handleExportarPdf() {
        ejecutarExportacionDirecta(false);
    }

    @FXML
    public void handleExportarXlsx() {
        ejecutarExportacionDirecta(true);
    }

    @FXML
    public void handleCancelarH5() {
        if (ejecucionActiva != null) {
            ejecucionActiva.solicitarCancelacion();
            btnCancelarH5.setDisable(true);
            btnCancelarH5.setText("Cancelando…");
            mostrarEstadoH5("Solicitando cancelación cooperativa H5…");
        }
    }

    private void ejecutarExportacionDirecta(boolean comoXlsx) {
        TipoReporte tipo = cmbTipoReporte.getValue();
        if (tipo == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(comoXlsx ? "Exportar reporte a XLSX" : "Exportar reporte a PDF");
        String base = tipo.name().replace('_', '-');
        chooser.setInitialFileName(base + (comoXlsx ? ".xlsx" : ".pdf"));
        chooser.getExtensionFilters().add(comoXlsx
                ? new FileChooser.ExtensionFilter("Libro Excel (*.xlsx)", "*.xlsx")
                : new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));

        File destino = chooser.showSaveDialog(ventanaActual());
        if (destino == null) return;
        ejecutarH5(comoXlsx ? ModoSalida.EXPORTAR_XLSX : ModoSalida.EXPORTAR_PDF, destino.toPath());
    }

    private enum ModoSalida { VISTA_PREVIA, EXPORTAR_PDF, EXPORTAR_XLSX }

    private void ejecutarH5(ModoSalida modo, Path destino) {
        ocultarError();
        TipoReporte tipo = cmbTipoReporte.getValue();
        if (tipo == null) {
            mostrarError("Selecciona un reporte del catálogo.");
            return;
        }
        if (tipo == TipoReporte.REP_02 && cmbContrato.getValue() == null) {
            mostrarError("El estado de cuenta requiere seleccionar un contrato.");
            return;
        }
        Integer umbralDias = null;
        if (tipo == TipoReporte.REP_06 && txtUmbralDias.getText() != null && !txtUmbralDias.getText().isBlank()) {
            try {
                umbralDias = Integer.parseInt(txtUmbralDias.getText().strip());
            } catch (NumberFormatException e) {
                mostrarError("El umbral de días debe ser un número entero.");
                return;
            }
        }

        final GestorH5Reportes.EjecucionH5 ejecucion;
        try {
            ejecucion = GestorH5Reportes.iniciar(tipo.etiqueta);
        } catch (IllegalStateException ocupada) {
            AlertaUtil.mostrarAdvertencia("Reporte en curso", ocupada.getMessage());
            return;
        }

        LocalDate corte = dpFechaCorte.getValue();
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        UUID propiedadId = cmbPropiedad.getValue() != null ? cmbPropiedad.getValue().getId() : null;
        UUID unidadId = cmbUnidad.getValue() != null ? cmbUnidad.getValue().getId() : null;
        UUID contratoId = cmbContrato.getValue() != null ? cmbContrato.getValue().getId() : null;
        UUID inquilinoId = cmbInquilino.getValue() != null ? cmbInquilino.getValue().getId() : null;
        String moneda = TODAS.equals(cmbMoneda.getValue()) ? null : cmbMoneda.getValue();
        String tipoUnidad = TODOS.equals(cmbTipoUnidad.getValue()) ? null : cmbTipoUnidad.getValue();
        String estadoMant = TODOS.equals(cmbEstadoMantenimiento.getValue()) ? null : cmbEstadoMantenimiento.getValue();
        String prioMant = TODAS.equals(cmbPrioridadMantenimiento.getValue()) ? null : cmbPrioridadMantenimiento.getValue();
        String origenTasa = txtOrigenTasa.getText();
        Integer umbralFinal = umbralDias;

        ejecucionActiva = ejecucion;
        setOcupado(true, "Generando " + tipo.etiqueta + " en segundo plano…");

        Task<ResultadoH5> tarea = new Task<>() {
            @Override
            protected ResultadoH5 call() throws Exception {
                try {
                    BooleanSupplier cancelado = ejecucion::estaCancelada;
                    DocumentoReporte doc = generarDocumento(
                            tipo, corte, desde, hasta, umbralFinal, propiedadId, unidadId,
                            contratoId, inquilinoId, moneda, tipoUnidad, estadoMant, prioMant, origenTasa, cancelado);
                    if (modo == ModoSalida.VISTA_PREVIA) {
                        return new ResultadoH5(doc, null);
                    }
                    VistaPreviaReporte.ResultadoPublicacion pub = modo == ModoSalida.EXPORTAR_XLSX
                            ? VistaPreviaReporte.exportarXlsxAtomico(doc, destino, cancelado)
                            : VistaPreviaReporte.exportarPdfAtomico(doc.jasperPrint(), destino, cancelado);
                    return new ResultadoH5(doc, pub);
                } finally {
                    ejecucion.close();
                }
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tarea,
                res -> {
                    ejecucion.close();
                    ejecucionActiva = null;
                    setOcupado(false, null);
                    if (modo == ModoSalida.VISTA_PREVIA) {
                        VistaPreviaReporte.mostrar(ventanaActual(), res.documento());
                    } else if (res.publicacion() != null) {
                        String msg = res.publicacion().publicadoAntesDeCancelar()
                                ? "El archivo concluyó antes de aplicar la cancelación: "
                                + res.publicacion().rutaFinal().getFileName()
                                : "El reporte se guardó correctamente en "
                                + res.publicacion().rutaFinal().getFileName() + ".";
                        AlertaUtil.mostrarExito("Exportación completada", msg);
                    }
                },
                error -> {
                    ejecucion.close();
                    ejecucionActiva = null;
                    setOcupado(false, null);
                    Throwable causa = extraerCausa(error);
                    if (causa instanceof OperacionCanceladaException) {
                        AlertaUtil.mostrarExito("Reporte cancelado", OperacionCanceladaException.MSG_089);
                    } else if (causa instanceof IllegalArgumentException || causa instanceof SecurityException) {
                        mostrarError(causa.getMessage());
                    } else {
                        mostrarError("No se pudo completar la generación del reporte.");
                    }
                }
        );
    }

    private DocumentoReporte generarDocumento(TipoReporte tipo, LocalDate corte, LocalDate desde, LocalDate hasta,
                                              Integer umbral, UUID propiedadId, UUID unidadId, UUID contratoId,
                                              UUID inquilinoId, String moneda, String tipoUnidad,
                                              String estadoMant, String prioMant, String origenTasa,
                                              BooleanSupplier cancelado) {
        return switch (tipo) {
            case REP_02 -> reporteService.generarDocumentoEstadoCuenta(contratoId, corte, cancelado);
            case REP_03 -> reporteService.generarDocumentoCarteraVencida(corte, propiedadId, contratoId, moneda, cancelado);
            case REP_04 -> reporteService.generarDocumentoIngresosYGastos(desde, hasta, propiedadId, moneda, cancelado);
            case REP_05 -> reporteService.generarDocumentoOcupacion(propiedadId, tipoUnidad, cancelado);
            case REP_06 -> reporteService.generarDocumentoContratosPorVencer(corte, umbral, propiedadId, moneda, cancelado);
            case REP_07 -> reporteService.generarDocumentoHistorialPagos(inquilinoId, contratoId, desde, hasta, moneda, cancelado);
            case REP_08 -> reporteService.generarDocumentoLiquidacionGarantias(contratoId, corte, moneda, cancelado);
            case REP_09 -> reporteService.generarDocumentoMantenimiento(
                    propiedadId, unidadId, desde, hasta, estadoMant, prioMant, moneda, cancelado);
            case REP_10 -> reporteService.generarDocumentoTasasUtilizadas(desde, hasta, origenTasa, cancelado);
        };
    }

    private void setOcupado(boolean ocupado, String mensaje) {
        btnVistaPrevia.setDisable(ocupado);
        btnExportarPdf.setDisable(ocupado);
        btnExportarXlsx.setDisable(ocupado);
        btnCancelarH5.setVisible(ocupado);
        btnCancelarH5.setManaged(ocupado);
        btnCancelarH5.setDisable(!ocupado);
        btnCancelarH5.setText("Cancelar tarea");
        if (ocupado && mensaje != null) {
            mostrarEstadoH5(mensaje);
        } else {
            lblEstadoH5.setVisible(false);
            lblEstadoH5.setManaged(false);
        }
    }

    private void mostrarEstadoH5(String mensaje) {
        lblEstadoH5.setText(mensaje);
        lblEstadoH5.setVisible(true);
        lblEstadoH5.setManaged(true);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private Window ventanaActual() {
        return rootView != null && rootView.getScene() != null ? rootView.getScene().getWindow() : null;
    }

    private static Throwable extraerCausa(Throwable error) {
        Throwable actual = error;
        while (actual instanceof ExecutionException && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }

    private record ResultadoH5(DocumentoReporte documento, VistaPreviaReporte.ResultadoPublicacion publicacion) { }
}
