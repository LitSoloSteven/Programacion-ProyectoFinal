package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.DocumentoReporteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.GenerarReporteRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.ReciboBusquedaItemResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteReportes;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.OperacionCanceladaException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.BarraErrorContextual;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.GestorH5Reportes;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RecargadorVista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

/** Controlador del Centro de Reportes unificado vía API HTTP. */
public class ReportesController {

    private static final String TODAS = "Todas";
    private static final String TODOS = "Todos";
    private static final String PERIODO_ACTUAL = "Mes actual";

    public enum CategoriaReporte {
        TODAS("Todas las tareas"),
        COMPROBANTES_CUENTA("Comprobantes y cuenta"),
        COBRANZA_FINANZAS("Cobranza y finanzas"),
        INMUEBLES_CONTRATOS("Inmuebles y contratos"),
        OPERACION_TASAS("Operación y tasas");

        private final String etiqueta;

        CategoriaReporte(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    public enum TipoReporte {
        RECIBO("RECIBO", CategoriaReporte.COMPROBANTES_CUENTA,
                "Recibo de pago",
                "Comprobante individual de un pago con su numeración y estado vigente o anulado.",
                false, "Recibo"),
        ESTADO_CUENTA("ESTADO_CUENTA", CategoriaReporte.COMPROBANTES_CUENTA,
                "Estado de cuenta",
                "Cargos, abonos, anulaciones, saldo y crédito disponible de un contrato al corte.",
                true, "Estado-Cuenta"),
        DASHBOARD_RESUMEN("DASHBOARD_RESUMEN", CategoriaReporte.COBRANZA_FINANZAS,
                "Resumen ejecutivo del panel",
                "Indicadores consolidados de ocupación, contratos, cargos pendientes y pagos por moneda.",
                true, "Resumen-Panel-Latch"),
        CARTERA_VENCIDA("CARTERA_VENCIDA", CategoriaReporte.COBRANZA_FINANZAS,
                "Cartera vencida por antigüedad",
                "Saldos abiertos al corte clasificados por tramos de antigüedad y moneda.",
                true, "Reporte-Cartera-Vencida"),
        CARGOS_CARTERA("CARGOS_CARTERA", CategoriaReporte.COBRANZA_FINANZAS,
                "Cargos, cartera y mora",
                "Detalle de cargos filtrados con días de mora y saldos acumulados por moneda.",
                true, "Reporte-Cargos-Cartera-Mora"),
        PAGOS_TABLA("PAGOS_TABLA", CategoriaReporte.COBRANZA_FINANZAS,
                "Pagos recibidos",
                "Relación general de pagos con monto original, monto en moneda de contrato y neto.",
                true, "Reporte-Pagos"),
        HISTORIAL_PAGOS("HISTORIAL_PAGOS", CategoriaReporte.COBRANZA_FINANZAS,
                "Historial de pagos del inquilino",
                "Pagos registrados y anulados con número de recibo; excluye anulados del total neto.",
                true, "Reporte-Historial-Pagos"),
        INGRESOS_GASTOS("INGRESOS_GASTOS", CategoriaReporte.COBRANZA_FINANZAS,
                "Ingresos y gastos por moneda",
                "Separa rentas netas, otros cobros, gastos de mantenimiento y movimientos de garantía.",
                true, "Reporte-Ingresos-Gastos"),
        GARANTIAS_DEPOSITOS("GARANTIAS_DEPOSITOS", CategoriaReporte.COBRANZA_FINANZAS,
                "Depósitos en garantía",
                "Resumen global de depósitos pactados y saldos vigentes por contrato y moneda.",
                true, "Reporte-Garantias-Depositos"),
        LIQUIDACION_GARANTIAS("LIQUIDACION_GARANTIAS", CategoriaReporte.COBRANZA_FINANZAS,
                "Liquidación de depósitos en garantía",
                "Depósito inicial, aplicaciones, deducciones, devoluciones y remanente conciliable al corte.",
                true, "Reporte-Garantias-Liquidacion"),
        PROPIEDADES_UNIDADES("PROPIEDADES_UNIDADES", CategoriaReporte.INMUEBLES_CONTRATOS,
                "Inventario de propiedades y unidades",
                "Listado de propiedades activas e inactivas junto con sus unidades y cánones de referencia.",
                true, "Reporte-Propiedades-Unidades"),
        UNIDADES_CATALOGO("UNIDADES_CATALOGO", CategoriaReporte.INMUEBLES_CONTRATOS,
                "Catálogo de unidades",
                "Unidades filtrables por propiedad, tipo, estado y búsqueda con totales de canon por moneda.",
                true, "Reporte-Unidades"),
        OCUPACION("OCUPACION", CategoriaReporte.INMUEBLES_CONTRATOS,
                "Ocupación por propiedad",
                "Unidades ocupadas sobre unidades activas al momento de generar el informe.",
                true, "Reporte-Ocupacion"),
        INQUILINOS_CATALOGO("INQUILINOS_CATALOGO", CategoriaReporte.INMUEBLES_CONTRATOS,
                "Directorio de inquilinos",
                "Listado de inquilinos con datos de contacto, identificación y estado.",
                true, "Reporte-Inquilinos"),
        CONTRATOS_OCUPACION("CONTRATOS_OCUPACION", CategoriaReporte.INMUEBLES_CONTRATOS,
                "Contratos y ocupación",
                "Contratos filtrables por estado, unidad o inquilino junto con el resumen de ocupación.",
                true, "Resumen-Contratos-Ocupacion"),
        CONTRATOS_POR_VENCER("CONTRATOS_POR_VENCER", CategoriaReporte.INMUEBLES_CONTRATOS,
                "Contratos por vencer",
                "Contratos activos cuyo vencimiento ocurre dentro del umbral de días indicado.",
                true, "Reporte-Contratos-Por-Vencer"),
        MANTENIMIENTO("MANTENIMIENTO", CategoriaReporte.OPERACION_TASAS,
                "Mantenimiento y gastos de propiedad",
                "Solicitudes por unidad, estado o prioridad y costos efectivos separados por moneda.",
                true, "Reporte-Mantenimiento"),
        TASAS_CATALOGO("TASAS_CATALOGO", CategoriaReporte.OPERACION_TASAS,
                "Catálogo de tasas de cambio",
                "Registro de tasas de cambio con fecha, fuente, autorización y motivo.",
                true, "Reporte-Tasas-Cambio"),
        TASAS_UTILIZADAS("TASAS_UTILIZADAS", CategoriaReporte.OPERACION_TASAS,
                "Tasas utilizadas en pagos",
                "Tasas históricas efectivamente aplicadas en pagos multimoneda.",
                true, "Reporte-Tasas-Utilizadas");

        private final String codigoApi;
        private final CategoriaReporte categoria;
        private final String etiqueta;
        private final String descripcion;
        private final boolean admiteXlsx;
        private final String baseArchivo;

        TipoReporte(String codigoApi,
                    CategoriaReporte categoria,
                    String etiqueta,
                    String descripcion,
                    boolean admiteXlsx,
                    String baseArchivo) {
            this.codigoApi = codigoApi;
            this.categoria = categoria;
            this.etiqueta = etiqueta;
            this.descripcion = descripcion;
            this.admiteXlsx = admiteXlsx;
            this.baseArchivo = baseArchivo;
        }

        public String codigoApi() {
            return codigoApi;
        }

        public CategoriaReporte categoria() {
            return categoria;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public String descripcion() {
            return descripcion;
        }

        public boolean admiteXlsx() {
            return admiteXlsx;
        }

        public String baseArchivo() {
            return baseArchivo;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    @FXML private VBox rootView;
    @FXML private Label lblTitulo;
    @FXML private ComboBox<CategoriaReporte> cmbCategoriaReporte;
    @FXML private ComboBox<TipoReporte> cmbTipoReporte;
    @FXML private Label lblDescripcionReporte;
    @FXML private Label lblFormatosDisponibles;

    @FXML private VBox panelFiltros;
    @FXML private Label lblSinFiltros;
    @FXML private FlowPane flowFiltros;

    @FXML private VBox boxPeriodo;
    @FXML private ComboBox<String> cmbPeriodo;
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
    @FXML private ComboBox<PropiedadResponse> cmbPropiedad;
    @FXML private VBox boxUnidad;
    @FXML private ComboBox<UnidadResponse> cmbUnidad;
    @FXML private VBox boxTipoUnidad;
    @FXML private ComboBox<String> cmbTipoUnidad;
    @FXML private VBox boxEstadoUnidad;
    @FXML private ComboBox<String> cmbEstadoUnidad;
    @FXML private VBox boxFiltroContrato;
    @FXML private TextField txtBuscarContrato;
    @FXML private VBox boxContrato;
    @FXML private ComboBox<ContratoResponse> cmbContrato;
    @FXML private VBox boxEstadoContrato;
    @FXML private ComboBox<String> cmbEstadoContrato;
    @FXML private VBox boxInquilino;
    @FXML private ComboBox<InquilinoResponse> cmbInquilino;
    @FXML private VBox boxEstadoInquilino;
    @FXML private ComboBox<String> cmbEstadoInquilino;
    @FXML private VBox boxEstadoMantenimiento;
    @FXML private ComboBox<String> cmbEstadoMantenimiento;
    @FXML private VBox boxPrioridadMantenimiento;
    @FXML private ComboBox<String> cmbPrioridadMantenimiento;
    @FXML private VBox boxOrigenTasa;
    @FXML private TextField txtOrigenTasa;
    @FXML private VBox boxTextoBusqueda;
    @FXML private Label lblTextoBusqueda;
    @FXML private TextField txtTextoBusqueda;

    @FXML private VBox boxBuscadorRecibos;
    @FXML private TextField txtNumeroRecibo;
    @FXML private TextField txtBuscarRecibo;
    @FXML private Button btnLimpiarRecibos;
    @FXML private Label lblResumenRecibos;
    @FXML private TableView<ReciboBusquedaItemResponse> tblRecibos;
    @FXML private TableColumn<ReciboBusquedaItemResponse, String> colReciboNumero;
    @FXML private TableColumn<ReciboBusquedaItemResponse, LocalDate> colReciboFecha;
    @FXML private TableColumn<ReciboBusquedaItemResponse, String> colReciboInquilino;
    @FXML private TableColumn<ReciboBusquedaItemResponse, String> colReciboUnidad;
    @FXML private TableColumn<ReciboBusquedaItemResponse, String> colReciboMonto;
    @FXML private TableColumn<ReciboBusquedaItemResponse, String> colReciboEstado;

    @FXML private Label lblEstadoH5;
    @FXML private Label lblError;
    @FXML private Button btnVistaPrevia;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnExportarXlsx;
    @FXML private Button btnCancelarH5;

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ClienteReportes reportes = ClienteLatch.reportes();
    private final ObservableList<ReciboBusquedaItemResponse> recibosEncontrados = FXCollections.observableArrayList();
    private List<ContratoResponse> todosLosContratos = List.of();
    private GestorH5Reportes.EjecucionH5 ejecucionActiva;
    private RecargadorVista recargadorCatalogos;
    private RecargadorVista recargadorRecibos;
    private BarraErrorContextual barraError;
    private boolean actualizandoTipos;

    private record CatalogosCargados(
            List<PropiedadResponse> propiedades,
            List<UnidadResponse> unidades,
            List<ContratoResponse> contratos,
            List<InquilinoResponse> inquilinos) { }

    private record SolicitudValidada(GenerarReporteRequest request, Long numeroReciboSugerido) { }

    @FXML
    public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.REPORTE, 20));
        recargadorCatalogos = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        recargadorRecibos = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblError);
        Iconos.asignar(btnVistaPrevia, Iconos.Nombre.VER, 16);
        Iconos.asignar(btnExportarPdf, Iconos.Nombre.DESCARGAR, 16);
        Iconos.asignar(btnExportarXlsx, Iconos.Nombre.DESCARGAR, 16);
        Iconos.asignar(btnCancelarH5, Iconos.Nombre.CERRAR, 16);
        Iconos.asignar(btnLimpiarRecibos, Iconos.Nombre.CERRAR, 14);

        configurarControles();
        cargarCatalogos();
    }

    private void configurarControles() {
        cmbCategoriaReporte.setItems(FXCollections.observableArrayList(CategoriaReporte.values()));
        cmbCategoriaReporte.setValue(CategoriaReporte.TODAS);
        cmbCategoriaReporte.valueProperty().addListener((o, a, b) -> actualizarOpcionesPorCategoria());

        cmbTipoReporte.setItems(FXCollections.observableArrayList(TipoReporte.values()));
        cmbTipoReporte.setValue(TipoReporte.RECIBO);
        cmbTipoReporte.valueProperty().addListener((o, a, b) -> {
            if (!actualizandoTipos) {
                actualizarVisibilidadFiltros();
            }
        });

        LocalDate hoy = LocalDate.now(ConstantesLatch.ZONA_OPERATIVA);
        dpFechaCorte.setValue(hoy);
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(hoy);
        txtUmbralDias.setText("30");

        List<String> periodos = new ArrayList<>();
        periodos.add(PERIODO_ACTUAL);
        YearMonth mesActual = YearMonth.now(ConstantesLatch.ZONA_OPERATIVA);
        for (int i = 0; i < 12; i++) {
            periodos.add(mesActual.minusMonths(i).toString());
        }
        cmbPeriodo.setItems(FXCollections.observableArrayList(periodos));
        cmbPeriodo.setValue(PERIODO_ACTUAL);

        cmbMoneda.setItems(FXCollections.observableArrayList(
                TODAS, ConstantesLatch.MONEDA_USD, ConstantesLatch.MONEDA_NIO));
        cmbMoneda.setValue(TODAS);

        cmbTipoUnidad.setItems(FXCollections.observableArrayList(
                TODOS, "Apartamento", "Casa", "Local", "Oficina", "Bodega", "Otro"));
        cmbTipoUnidad.setValue(TODOS);

        cmbEstadoUnidad.setItems(FXCollections.observableArrayList(
                TODOS, "Disponible", "Ocupada", "En mantenimiento", "Inactiva"));
        cmbEstadoUnidad.setValue(TODOS);

        cmbEstadoContrato.setItems(FXCollections.observableArrayList(
                TODOS, "Borrador", "Vigente", "Por vencer", "Vencido", "Terminado", "Cancelado"));
        cmbEstadoContrato.setValue(TODOS);

        cmbEstadoInquilino.setItems(FXCollections.observableArrayList(
                TODOS, "Activos", "Inactivos"));
        cmbEstadoInquilino.setValue(TODOS);

        cmbEstadoMantenimiento.setItems(FXCollections.observableArrayList(
                TODOS, ConstantesLatch.MANT_ESTADO_ABIERTA,
                ConstantesLatch.MANT_ESTADO_EN_PROCESO,
                ConstantesLatch.MANT_ESTADO_RESUELTA,
                ConstantesLatch.MANT_ESTADO_CANCELADA));
        cmbEstadoMantenimiento.setValue(TODOS);

        cmbPrioridadMantenimiento.setItems(FXCollections.observableArrayList(
                TODAS, ConstantesLatch.MANT_PRIORIDAD_BAJA,
                ConstantesLatch.MANT_PRIORIDAD_MEDIA,
                ConstantesLatch.MANT_PRIORIDAD_ALTA));
        cmbPrioridadMantenimiento.setValue(TODAS);

        cmbPropiedad.setConverter(new StringConverter<>() {
            @Override public String toString(PropiedadResponse p) {
                return p == null ? "Todas las propiedades" : p.nombre();
            }
            @Override public PropiedadResponse fromString(String s) { return null; }
        });
        cmbUnidad.setConverter(new StringConverter<>() {
            @Override public String toString(UnidadResponse u) {
                return u == null ? "Todas las unidades" : u.propiedadNombre() + " — " + u.codigo();
            }
            @Override public UnidadResponse fromString(String s) { return null; }
        });
        cmbContrato.setConverter(new StringConverter<>() {
            @Override public String toString(ContratoResponse c) {
                return c == null ? "Todos / Seleccionar contrato"
                        : c.unidadInfo() + " · " + c.inquilinoNombre() + " (" + c.estado() + ")";
            }
            @Override public ContratoResponse fromString(String s) { return null; }
        });
        cmbInquilino.setConverter(new StringConverter<>() {
            @Override public String toString(InquilinoResponse i) {
                return i == null ? "Todos los inquilinos" : i.nombre();
            }
            @Override public InquilinoResponse fromString(String s) { return null; }
        });

        txtBuscarContrato.textProperty().addListener((o, a, b) -> aplicarFiltroContratos());

        configurarBuscadorRecibos();
        actualizarVisibilidadFiltros();
    }

    private void configurarBuscadorRecibos() {
        tblRecibos.setItems(recibosEncontrados);
        tblRecibos.setPlaceholder(PanelEstadoTabla.crear("Sin recibos para mostrar",
                "Ajusta el número o el texto de búsqueda para localizar un comprobante.", null, null));
        colReciboNumero.setCellValueFactory(c -> new ReadOnlyStringWrapper("#" + c.getValue().numeroRecibo()));
        colReciboNumero.setCellFactory(FormatoTabla.numerica());
        colReciboFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(LocalDate.parse(c.getValue().fechaPago())));
        colReciboFecha.setCellFactory(FormatoTabla.fecha());
        colReciboInquilino.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().inquilinoNombre()));
        colReciboUnidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().unidadInfo()));
        colReciboMonto.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().monto() + " " + c.getValue().moneda()));
        colReciboMonto.setCellFactory(FormatoTabla.numerica());
        colReciboEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().anulado() ? "Anulado" : "Vigente"));
        colReciboEstado.setCellFactory(FormatoTabla.estado());

        tblRecibos.getSelectionModel().selectedItemProperty().addListener((obs, ant, act) -> {
            if (act != null) {
                ocultarError();
            }
        });
        tblRecibos.setRowFactory(tv -> {
            TableRow<ReciboBusquedaItemResponse> fila = new TableRow<>();
            fila.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !fila.isEmpty()) {
                    handleVistaPrevia();
                }
            });
            return fila;
        });

        txtNumeroRecibo.textProperty().addListener((obs, ant, nuevo) -> {
            if (cmbTipoReporte.getValue() == TipoReporte.RECIBO) {
                buscarRecibosAgrupado();
            }
        });
        txtBuscarRecibo.textProperty().addListener((obs, ant, nuevo) -> {
            if (cmbTipoReporte.getValue() == TipoReporte.RECIBO) {
                buscarRecibosAgrupado();
            }
        });
    }

    private void actualizarOpcionesPorCategoria() {
        CategoriaReporte categoria = cmbCategoriaReporte.getValue();
        List<TipoReporte> filtrados = Arrays.stream(TipoReporte.values())
                .filter(t -> categoria == null || categoria == CategoriaReporte.TODAS || t.categoria() == categoria)
                .toList();
        TipoReporte previo = cmbTipoReporte.getValue();
        actualizandoTipos = true;
        try {
            cmbTipoReporte.setItems(FXCollections.observableArrayList(filtrados));
            if (previo != null && filtrados.contains(previo)) {
                cmbTipoReporte.setValue(previo);
            } else if (!filtrados.isEmpty()) {
                cmbTipoReporte.setValue(filtrados.getFirst());
            }
        } finally {
            actualizandoTipos = false;
        }
        actualizarVisibilidadFiltros();
    }

    private void cargarCatalogos() {
        ocultarError();
        if (ejecucionActiva == null) {
            mostrarEstadoProceso("Cargando filtros de documentos…");
        }
        recargadorCatalogos.recargar(
                () -> new CatalogosCargados(
                        catalogos.propiedades(false),
                        catalogos.unidades(),
                        catalogos.contratos(false),
                        catalogos.inquilinos(false)),
                datos -> {
                    if (ejecucionActiva == null) {
                        ocultarEstadoProceso();
                    }
                    todosLosContratos = datos.contratos() == null ? List.of() : List.copyOf(datos.contratos());
                    RecargadorVista.conservarComboPorId(cmbPropiedad, datos.propiedades(), true, PropiedadResponse::id);
                    RecargadorVista.conservarComboPorId(cmbUnidad, datos.unidades(), true, UnidadResponse::id);
                    aplicarFiltroContratos();
                    RecargadorVista.conservarComboPorId(cmbInquilino, datos.inquilinos(), true, InquilinoResponse::id);
                },
                error -> {
                    if (ejecucionActiva == null) {
                        ocultarEstadoProceso();
                    }
                    barraError.mostrar("No se pudieron cargar los catálogos para filtros.", this::cargarCatalogos);
                });
    }

    private void aplicarFiltroContratos() {
        String filtro = txtBuscarContrato == null || txtBuscarContrato.getText() == null
                ? "" : txtBuscarContrato.getText().strip().toLowerCase(Locale.ROOT);
        List<ContratoResponse> filtrados = todosLosContratos.stream()
                .filter(c -> filtro.isEmpty()
                        || contiene(c.inquilinoNombre(), filtro)
                        || contiene(c.unidadInfo(), filtro)
                        || contiene(c.estado(), filtro))
                .toList();
        RecargadorVista.conservarComboPorId(cmbContrato, filtrados, true, ContratoResponse::id);
        if (!filtro.isEmpty() && filtrados.size() == 1 && cmbContrato.getValue() == null) {
            cmbContrato.setValue(filtrados.getFirst());
        }
    }

    private void buscarRecibosAgrupado() {
        Long numero = leerNumeroReciboOpcional(false);
        if (numero != null && numero < 0) {
            return;
        }
        String texto = txtBuscarRecibo == null ? null : txtBuscarRecibo.getText();
        recargadorRecibos.recargarAgrupado(
                Duration.millis(250),
                () -> reportes.buscarRecibos(numero, texto),
                this::mostrarRecibos,
                error -> barraError.mostrar("No se pudieron consultar los recibos.", this::cargarRecibos));
    }

    private void cargarRecibos() {
        Long numero = leerNumeroReciboOpcional(false);
        if (numero != null && numero < 0) {
            return;
        }
        String texto = txtBuscarRecibo == null ? null : txtBuscarRecibo.getText();
        if (lblResumenRecibos != null) {
            lblResumenRecibos.setText("Consultando recibos…");
        }
        recargadorRecibos.recargar(
                () -> reportes.buscarRecibos(numero, texto),
                this::mostrarRecibos,
                error -> {
                    if (lblResumenRecibos != null) {
                        lblResumenRecibos.setText("No se pudieron consultar los recibos.");
                    }
                    barraError.mostrar("No se pudieron consultar los recibos.", this::cargarRecibos);
                });
    }

    private void mostrarRecibos(List<ReciboBusquedaItemResponse> items) {
        List<ReciboBusquedaItemResponse> lista = items == null ? List.of() : items;
        RecargadorVista.conservarSeleccionPorId(tblRecibos, ReciboBusquedaItemResponse::pagoId,
                () -> recibosEncontrados.setAll(lista));
        if (lblResumenRecibos != null) {
            int n = lista.size();
            if (n == 0) {
                lblResumenRecibos.setText("No se encontraron recibos con ese criterio.");
            } else if (n == 1) {
                lblResumenRecibos.setText("1 recibo encontrado.");
            } else {
                lblResumenRecibos.setText(n + " recibos encontrados. Haz doble clic o usa Vista previa / Exportar PDF.");
            }
        }
    }

    private void actualizarVisibilidadFiltros() {
        TipoReporte tipo = cmbTipoReporte.getValue();
        if (tipo == null) return;
        lblDescripcionReporte.setText(tipo.descripcion());
        lblFormatosDisponibles.setText(tipo.admiteXlsx()
                ? "Formatos disponibles: PDF y Excel (XLSX)"
                : "Formatos disponibles: PDF");
        if (btnExportarXlsx != null) {
            btnExportarXlsx.setVisible(tipo.admiteXlsx());
            btnExportarXlsx.setManaged(tipo.admiteXlsx());
        }
        ocultarError();
        ocultarTodosFiltros();

        switch (tipo) {
            case RECIBO -> {
                mostrar(boxBuscadorRecibos);
                cargarRecibos();
            }
            case ESTADO_CUENTA -> {
                mostrar(boxFechaCorte);
                mostrar(boxFiltroContrato);
                mostrar(boxContrato);
            }
            case DASHBOARD_RESUMEN -> mostrar(boxPeriodo);
            case CARTERA_VENCIDA -> {
                mostrar(boxFechaCorte);
                mostrar(boxPropiedad);
                mostrar(boxContrato);
                mostrar(boxMoneda);
            }
            case CARGOS_CARTERA -> {
                configurarTextoBusqueda("Buscar cargo", "Contrato, inquilino, tipo o concepto");
                mostrar(boxTextoBusqueda);
            }
            case PAGOS_TABLA -> {
                configurarTextoBusqueda("Buscar pago", "Inquilino, unidad, referencia o método");
                mostrar(boxTextoBusqueda);
            }
            case HISTORIAL_PAGOS -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxContrato);
                mostrar(boxInquilino);
                mostrar(boxMoneda);
            }
            case INGRESOS_GASTOS -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxPropiedad);
                mostrar(boxMoneda);
            }
            case GARANTIAS_DEPOSITOS, PROPIEDADES_UNIDADES -> mostrarSinFiltros();
            case LIQUIDACION_GARANTIAS -> {
                mostrar(boxFechaCorte);
                mostrar(boxContrato);
                mostrar(boxMoneda);
            }
            case UNIDADES_CATALOGO -> {
                configurarTextoBusqueda("Buscar unidad", "Código o descripción");
                mostrar(boxPropiedad);
                mostrar(boxTipoUnidad);
                mostrar(boxEstadoUnidad);
                mostrar(boxTextoBusqueda);
            }
            case OCUPACION -> {
                mostrar(boxPropiedad);
                mostrar(boxTipoUnidad);
            }
            case INQUILINOS_CATALOGO -> {
                configurarTextoBusqueda("Buscar inquilino", "Nombre, identificación o correo");
                mostrar(boxEstadoInquilino);
                mostrar(boxTextoBusqueda);
            }
            case CONTRATOS_OCUPACION -> {
                mostrar(boxEstadoContrato);
                mostrar(boxUnidad);
                mostrar(boxInquilino);
            }
            case CONTRATOS_POR_VENCER -> {
                mostrar(boxFechaCorte);
                mostrar(boxUmbralDias);
                mostrar(boxPropiedad);
                mostrar(boxMoneda);
            }
            case MANTENIMIENTO -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxPropiedad);
                mostrar(boxUnidad);
                mostrar(boxEstadoMantenimiento);
                mostrar(boxPrioridadMantenimiento);
                mostrar(boxMoneda);
            }
            case TASAS_CATALOGO -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
            }
            case TASAS_UTILIZADAS -> {
                mostrar(boxDesde);
                mostrar(boxHasta);
                mostrar(boxOrigenTasa);
            }
        }
        boolean mostrarFlow = flowFiltros.getChildren().stream().anyMatch(n -> n.isManaged() && n.isVisible());
        flowFiltros.setVisible(mostrarFlow);
        flowFiltros.setManaged(mostrarFlow);
    }

    private void configurarTextoBusqueda(String etiqueta, String placeholder) {
        if (lblTextoBusqueda != null) {
            lblTextoBusqueda.setText(etiqueta);
        }
        if (txtTextoBusqueda != null) {
            txtTextoBusqueda.setPromptText(placeholder);
        }
    }

    private void mostrarSinFiltros() {
        if (lblSinFiltros != null) {
            lblSinFiltros.setVisible(true);
            lblSinFiltros.setManaged(true);
        }
    }

    private void ocultarTodosFiltros() {
        if (lblSinFiltros != null) {
            lblSinFiltros.setVisible(false);
            lblSinFiltros.setManaged(false);
        }
        for (VBox box : List.of(
                boxPeriodo, boxFechaCorte, boxDesde, boxHasta, boxUmbralDias, boxMoneda,
                boxPropiedad, boxUnidad, boxTipoUnidad, boxEstadoUnidad,
                boxFiltroContrato, boxContrato, boxEstadoContrato,
                boxInquilino, boxEstadoInquilino,
                boxEstadoMantenimiento, boxPrioridadMantenimiento, boxOrigenTasa,
                boxTextoBusqueda, boxBuscadorRecibos)) {
            box.setVisible(false);
            box.setManaged(false);
        }
    }

    private static void mostrar(VBox box) {
        box.setVisible(true);
        box.setManaged(true);
    }

    @FXML
    public void handleLimpiarRecibos() {
        txtNumeroRecibo.clear();
        txtBuscarRecibo.clear();
        tblRecibos.getSelectionModel().clearSelection();
        ocultarError();
        cargarRecibos();
    }

    @FXML
    public void handleVistaPrevia() {
        ejecutarDocumento(ModoSalida.VISTA_PREVIA, null);
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
            mostrarEstadoProceso("Solicitando cancelación…");
        }
    }

    private void ejecutarExportacionDirecta(boolean comoXlsx) {
        ocultarError();
        TipoReporte tipo = cmbTipoReporte.getValue();
        if (tipo == null) {
            mostrarError("Selecciona un documento del catálogo.");
            return;
        }
        if (comoXlsx && !tipo.admiteXlsx()) {
            mostrarError("Este documento solo admite exportación a PDF.");
            return;
        }
        SolicitudValidada validada = validarYConstruirSolicitud(tipo);
        if (validada == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(comoXlsx ? "Exportar documento a XLSX" : "Exportar documento a PDF");
        chooser.setInitialFileName(nombreArchivoSugerido(
                tipo, comoXlsx, dpFechaCorte.getValue(), dpDesde.getValue(), dpHasta.getValue(),
                validada.numeroReciboSugerido()));
        chooser.getExtensionFilters().add(comoXlsx
                ? new FileChooser.ExtensionFilter("Libro Excel (*.xlsx)", "*.xlsx")
                : new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));

        File destino = chooser.showSaveDialog(ventanaActual());
        if (destino == null) return;
        lanzarEjecucion(tipo, validada.request(),
                comoXlsx ? ModoSalida.EXPORTAR_XLSX : ModoSalida.EXPORTAR_PDF, destino.toPath());
    }

    private enum ModoSalida { VISTA_PREVIA, EXPORTAR_PDF, EXPORTAR_XLSX }

    private void ejecutarDocumento(ModoSalida modo, Path destino) {
        ocultarError();
        TipoReporte tipo = cmbTipoReporte.getValue();
        if (tipo == null) {
            mostrarError("Selecciona un documento del catálogo.");
            return;
        }
        SolicitudValidada validada = validarYConstruirSolicitud(tipo);
        if (validada == null) {
            return;
        }
        lanzarEjecucion(tipo, validada.request(), modo, destino);
    }

    private SolicitudValidada validarYConstruirSolicitud(TipoReporte tipo) {
        LocalDate corte = dpFechaCorte.getValue();
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (usaRangoFechas(tipo) && desde != null && hasta != null && hasta.isBefore(desde)) {
            mostrarError("La fecha final no puede ser anterior a la fecha inicial.");
            return null;
        }

        if (tipo == TipoReporte.ESTADO_CUENTA && cmbContrato.getValue() == null) {
            mostrarError("El estado de cuenta requiere seleccionar un contrato.");
            return null;
        }

        Integer umbralDias = null;
        if (tipo == TipoReporte.CONTRATOS_POR_VENCER && txtUmbralDias.getText() != null
                && !txtUmbralDias.getText().isBlank()) {
            try {
                umbralDias = Integer.parseInt(txtUmbralDias.getText().strip());
                if (umbralDias < 0) {
                    mostrarError("El umbral de días no puede ser negativo.");
                    return null;
                }
            } catch (NumberFormatException e) {
                mostrarError("El umbral de días debe ser un número entero.");
                return null;
            }
        }

        String pagoId = null;
        Long numeroRecibo = null;
        Long numeroReciboSugerido = null;
        if (tipo == TipoReporte.RECIBO) {
            ReciboBusquedaItemResponse seleccionado = tblRecibos.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                pagoId = seleccionado.pagoId();
                numeroReciboSugerido = seleccionado.numeroRecibo();
            } else {
                Long escrito = leerNumeroReciboOpcional(true);
                if (escrito != null && escrito < 0) {
                    return null;
                }
                if (escrito == null) {
                    mostrarError("Selecciona un recibo de la tabla o escribe su número.");
                    return null;
                }
                numeroRecibo = escrito;
                numeroReciboSugerido = escrito;
            }
        }

        String propiedadId = cmbPropiedad.getValue() != null ? cmbPropiedad.getValue().id() : null;
        String unidadId = cmbUnidad.getValue() != null ? cmbUnidad.getValue().id() : null;
        String contratoId = cmbContrato.getValue() != null ? cmbContrato.getValue().id() : null;
        String inquilinoId = cmbInquilino.getValue() != null ? cmbInquilino.getValue().id() : null;
        String periodo = tipo == TipoReporte.DASHBOARD_RESUMEN
                && cmbPeriodo.getValue() != null && !PERIODO_ACTUAL.equals(cmbPeriodo.getValue())
                ? cmbPeriodo.getValue() : null;
        String moneda = TODAS.equals(cmbMoneda.getValue()) ? null : cmbMoneda.getValue();
        String tipoUnidad = TODOS.equals(cmbTipoUnidad.getValue()) ? null : cmbTipoUnidad.getValue();
        String estado = resolverEstado(tipo);
        String prioMant = TODAS.equals(cmbPrioridadMantenimiento.getValue()) ? null : cmbPrioridadMantenimiento.getValue();
        String origenTasa = limpiarTexto(txtOrigenTasa.getText());
        String texto = limpiarTexto(txtTextoBusqueda.getText());
        Boolean activo = resolverActivo(tipo);

        GenerarReporteRequest solicitud = new GenerarReporteRequest(
                tipo.codigoApi(),
                isoFecha(corte),
                isoFecha(desde),
                isoFecha(hasta),
                umbralDias,
                propiedadId,
                unidadId,
                contratoId,
                inquilinoId,
                pagoId,
                numeroRecibo,
                periodo,
                moneda,
                tipoUnidad,
                estado,
                prioMant,
                origenTasa,
                texto,
                activo);
        return new SolicitudValidada(solicitud, numeroReciboSugerido);
    }

    private String resolverEstado(TipoReporte tipo) {
        return switch (tipo) {
            case UNIDADES_CATALOGO -> TODOS.equals(cmbEstadoUnidad.getValue()) ? null : cmbEstadoUnidad.getValue();
            case CONTRATOS_OCUPACION -> TODOS.equals(cmbEstadoContrato.getValue()) ? null : cmbEstadoContrato.getValue();
            case MANTENIMIENTO -> TODOS.equals(cmbEstadoMantenimiento.getValue()) ? null : cmbEstadoMantenimiento.getValue();
            default -> null;
        };
    }

    private Boolean resolverActivo(TipoReporte tipo) {
        if (tipo != TipoReporte.INQUILINOS_CATALOGO) {
            return null;
        }
        String seleccion = cmbEstadoInquilino.getValue();
        if ("Activos".equals(seleccion)) return Boolean.TRUE;
        if ("Inactivos".equals(seleccion)) return Boolean.FALSE;
        return null;
    }

    private static boolean usaRangoFechas(TipoReporte tipo) {
        return tipo == TipoReporte.HISTORIAL_PAGOS
                || tipo == TipoReporte.INGRESOS_GASTOS
                || tipo == TipoReporte.MANTENIMIENTO
                || tipo == TipoReporte.TASAS_CATALOGO
                || tipo == TipoReporte.TASAS_UTILIZADAS;
    }

    /** Devuelve null si está vacío, > 0 si es válido, o -1L si se mostró error de formato. */
    private Long leerNumeroReciboOpcional(boolean enfocarSiInvalido) {
        if (txtNumeroRecibo == null || txtNumeroRecibo.getText() == null || txtNumeroRecibo.getText().isBlank()) {
            return null;
        }
        String raw = txtNumeroRecibo.getText().strip();
        if (raw.startsWith("#")) {
            raw = raw.substring(1).strip();
        }
        try {
            long n = Long.parseLong(raw);
            if (n <= 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            mostrarError("El número de recibo debe ser un entero positivo.");
            if (enfocarSiInvalido) {
                txtNumeroRecibo.requestFocus();
            }
            return -1L;
        }
    }

    private void lanzarEjecucion(TipoReporte tipo,
                                 GenerarReporteRequest solicitud,
                                 ModoSalida modo,
                                 Path destino) {
        final GestorH5Reportes.EjecucionH5 ejecucion;
        try {
            ejecucion = GestorH5Reportes.iniciar(tipo.etiqueta());
        } catch (IllegalStateException ocupada) {
            AlertaUtil.mostrarAdvertencia("Documento en curso", ocupada.getMessage());
            return;
        }

        ejecucionActiva = ejecucion;
        setOcupado(true, "Generando " + tipo.etiqueta() + " en segundo plano…", tipo.admiteXlsx());

        Task<ResultadoGeneracion> tarea = new Task<>() {
            @Override
            protected ResultadoGeneracion call() throws Exception {
                try {
                    BooleanSupplier cancelado = ejecucion::estaCancelada;
                    DocumentoReporteResponse doc = generarDocumento(solicitud, cancelado);
                    if (modo == ModoSalida.VISTA_PREVIA) {
                        return new ResultadoGeneracion(doc, null);
                    }
                    try {
                        VistaPreviaReporte.ResultadoPublicacion pub = modo == ModoSalida.EXPORTAR_XLSX
                                ? VistaPreviaReporte.exportarXlsxAtomico(doc.documentoId(), destino, cancelado)
                                : VistaPreviaReporte.exportarPdfAtomico(doc.documentoId(), destino, cancelado);
                        return new ResultadoGeneracion(doc, pub);
                    } finally {
                        eliminarRemotoSilencioso(doc.documentoId());
                    }
                } finally {
                    ejecucion.close();
                }
            }
        };

        TareasJavaFx.ejecutar(
                LatchApplication.instancia().getTareas(),
                tarea,
                res -> {
                    ejecucion.close();
                    ejecucionActiva = null;
                    setOcupado(false, null, tipo.admiteXlsx());
                    if (modo == ModoSalida.VISTA_PREVIA) {
                        VistaPreviaReporte.mostrar(ventanaActual(), res.documento());
                    } else if (res.publicacion() != null) {
                        String msg = res.publicacion().publicadoAntesDeCancelar()
                                ? "El archivo concluyó antes de aplicar la cancelación: "
                                + res.publicacion().rutaFinal().getFileName()
                                : "El documento se guardó correctamente en "
                                + res.publicacion().rutaFinal().getFileName() + ".";
                        AlertaUtil.mostrarExito("Exportación completada", msg);
                    }
                },
                error -> {
                    ejecucion.close();
                    ejecucionActiva = null;
                    setOcupado(false, null, tipo.admiteXlsx());
                    Throwable causa = extraerCausa(error);
                    if (causa instanceof OperacionCanceladaException) {
                        AlertaUtil.mostrarExito("Operación cancelada", OperacionCanceladaException.MSG_089);
                    } else {
                        mostrarError(MensajesUi.traducir(causa, "No se pudo completar la generación del documento."));
                    }
                }
        );
    }

    private DocumentoReporteResponse generarDocumento(GenerarReporteRequest solicitud,
                                                      BooleanSupplier cancelado) {
        if (cancelado != null && cancelado.getAsBoolean()) {
            throw new OperacionCanceladaException();
        }
        DocumentoReporteResponse doc = reportes.generar(solicitud);
        if (cancelado != null && cancelado.getAsBoolean()) {
            eliminarRemotoSilencioso(doc != null ? doc.documentoId() : null);
            throw new OperacionCanceladaException();
        }
        return doc;
    }

    public static String nombreArchivoSugerido(TipoReporte tipo,
                                               boolean comoXlsx,
                                               LocalDate corte,
                                               LocalDate desde,
                                               LocalDate hasta,
                                               Long numeroRecibo) {
        if (tipo == null) {
            return comoXlsx ? "Reporte-Latch.xlsx" : "Reporte-Latch.pdf";
        }
        String extension = comoXlsx && tipo.admiteXlsx() ? ".xlsx" : ".pdf";
        String base = switch (tipo) {
            case RECIBO -> numeroRecibo != null ? "Recibo-" + numeroRecibo : "Recibo-Pago";
            case ESTADO_CUENTA, CARTERA_VENCIDA, LIQUIDACION_GARANTIAS, CONTRATOS_POR_VENCER ->
                    corte != null ? tipo.baseArchivo() + "-" + corte : tipo.baseArchivo();
            case INGRESOS_GASTOS -> (desde != null && hasta != null)
                    ? tipo.baseArchivo() + "-" + desde + "_" + hasta
                    : tipo.baseArchivo();
            default -> tipo.baseArchivo();
        };
        return base + extension;
    }

    private void eliminarRemotoSilencioso(String documentoId) {
        if (documentoId == null || documentoId.isBlank()) {
            return;
        }
        try {
            reportes.eliminar(documentoId);
        } catch (RuntimeException ignored) {
            // La expiración TTL limpiará documentos huérfanos en el servidor.
        }
    }

    private static String isoFecha(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static String limpiarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private void setOcupado(boolean ocupado, String mensaje, boolean admiteXlsx) {
        btnVistaPrevia.setDisable(ocupado);
        btnExportarPdf.setDisable(ocupado);
        btnExportarXlsx.setDisable(ocupado || !admiteXlsx);
        btnCancelarH5.setVisible(ocupado);
        btnCancelarH5.setManaged(ocupado);
        btnCancelarH5.setDisable(!ocupado);
        btnCancelarH5.setText("Cancelar tarea");
        if (ocupado && mensaje != null) {
            mostrarEstadoProceso(mensaje);
        } else {
            ocultarEstadoProceso();
        }
    }

    private void mostrarEstadoProceso(String mensaje) {
        lblEstadoH5.setText(mensaje);
        lblEstadoH5.setVisible(true);
        lblEstadoH5.setManaged(true);
    }

    private void ocultarEstadoProceso() {
        lblEstadoH5.setText("");
        lblEstadoH5.setVisible(false);
        lblEstadoH5.setManaged(false);
    }

    private void mostrarError(String mensaje) {
        barraError.mostrar(mensaje, null);
    }

    private void ocultarError() {
        barraError.ocultar();
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

    private record ResultadoGeneracion(DocumentoReporteResponse documento,
                                       VistaPreviaReporte.ResultadoPublicacion publicacion) { }
}
