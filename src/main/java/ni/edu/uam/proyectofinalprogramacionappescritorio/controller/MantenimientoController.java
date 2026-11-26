package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.CambioEstadoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ResumenGastosMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoUpdateRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.BarraErrorContextual;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RecargadorVista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Controlador de solicitudes de mantenimiento y gastos de propiedad vía HTTP (M08). */
public class MantenimientoController {
    private static final String TODOS = "Todos";
    private static final String TODAS = "Todas";

    @FXML private VBox rootView;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private Button btnRegistrarGasto;
    @FXML private Button btnNuevaSolicitud;
    @FXML private Button btnLimpiar;
    @FXML private Button btnEditarSolicitud;
    @FXML private Button btnIniciarAtencion;
    @FXML private Button btnResolver;
    @FXML private Button btnCancelarSolicitud;
    @FXML private Button btnGastoSolicitud;

    @FXML private ComboBox<PropiedadResponse> cmbFiltroPropiedad;
    @FXML private ComboBox<UnidadResponse> cmbFiltroUnidad;
    @FXML private ComboBox<String> cmbFiltroPrioridad;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private TextField txtBuscar;

    @FXML private TableView<SolicitudMantenimientoResponse> tblSolicitudes;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colPropiedad;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colUnidad;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colDescripcion;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colPrioridad;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colEstado;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colResponsable;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colAbiertaEl;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colGastoUsd;
    @FXML private TableColumn<SolicitudMantenimientoResponse, String> colGastoNio;

    @FXML private DatePicker dpGastosDesde;
    @FXML private DatePicker dpGastosHasta;
    @FXML private ComboBox<String> cmbMonedaGasto;
    @FXML private Button btnConsultarGastos;
    @FXML private Label lblTotalGastosUsd;
    @FXML private Label lblTotalGastosNio;
    @FXML private TableView<GastoMantenimientoResponse> tblGastos;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoFecha;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoPropiedad;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoSolicitud;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoConcepto;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoMoneda;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoImporte;
    @FXML private TableColumn<GastoMantenimientoResponse, String> colGastoCreador;

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ClienteMantenimiento cliente = ClienteLatch.mantenimiento();
    private final ObservableList<SolicitudMantenimientoResponse> solicitudes = FXCollections.observableArrayList();
    private final FilteredList<SolicitudMantenimientoResponse> filtradas = new FilteredList<>(solicitudes, item -> true);
    private final ObservableList<GastoMantenimientoResponse> gastos = FXCollections.observableArrayList();
    private List<PropiedadResponse> propiedadesDisponibles = List.of();
    private List<UnidadResponse> unidadesDisponibles = List.of();
    private boolean actualizandoFiltros;
    private RecargadorVista recargadorGeneral;
    private RecargadorVista recargadorGastos;
    private BarraErrorContextual barraError;

    private record DatosCargados(List<PropiedadResponse> propiedades, List<UnidadResponse> unidades,
                                 List<SolicitudMantenimientoResponse> solicitudes,
                                 ResumenGastosMantenimientoResponse gastos) { }

    @FXML
    public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.MANTENIMIENTO, 20));
        recargadorGeneral = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        recargadorGastos = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
        Iconos.asignar(btnNuevaSolicitud, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnEditarSolicitud, Iconos.Nombre.EDITAR, 14);
        Iconos.asignar(btnIniciarAtencion, Iconos.Nombre.ENTRAR, 14);
        Iconos.asignar(btnResolver, Iconos.Nombre.CONFIRMACION, 14);
        Iconos.asignar(btnCancelarSolicitud, Iconos.Nombre.CERRAR, 14);

        configurarFiltros();
        configurarTablas();
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevaSolicitud);
        cargarDatos();
    }

    private void configurarFiltros() {
        cmbFiltroPropiedad.setConverter(new StringConverter<>() {
            @Override public String toString(PropiedadResponse p) { return p == null ? "Todas las propiedades" : p.nombre(); }
            @Override public PropiedadResponse fromString(String s) { return null; }
        });
        cmbFiltroUnidad.setConverter(new StringConverter<>() {
            @Override public String toString(UnidadResponse u) {
                return u == null ? "Todas las unidades" : u.codigo() + " · " + u.propiedadNombre();
            }
            @Override public UnidadResponse fromString(String s) { return null; }
        });
        cmbFiltroPrioridad.setItems(FXCollections.observableArrayList(
                TODAS, ConstantesLatch.MANT_PRIORIDAD_BAJA,
                ConstantesLatch.MANT_PRIORIDAD_MEDIA, ConstantesLatch.MANT_PRIORIDAD_ALTA));
        cmbFiltroPrioridad.setValue(TODAS);
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(
                TODOS, ConstantesLatch.MANT_ESTADO_ABIERTA,
                ConstantesLatch.MANT_ESTADO_EN_PROCESO,
                ConstantesLatch.MANT_ESTADO_RESUELTA,
                ConstantesLatch.MANT_ESTADO_CANCELADA));
        cmbFiltroEstado.setValue(TODOS);
        cmbMonedaGasto.setItems(FXCollections.observableArrayList(TODAS, "USD", "NIO"));
        cmbMonedaGasto.setValue(TODAS);

        cmbFiltroPropiedad.valueProperty().addListener((o, a, b) -> {
            if (!actualizandoFiltros) {
                aplicarFiltro();
                consultarGastos();
            }
        });
        cmbFiltroUnidad.valueProperty().addListener((o, a, b) -> { if (!actualizandoFiltros) aplicarFiltro(); });
        cmbFiltroPrioridad.valueProperty().addListener((o, a, b) -> { if (!actualizandoFiltros) aplicarFiltro(); });
        cmbFiltroEstado.valueProperty().addListener((o, a, b) -> { if (!actualizandoFiltros) aplicarFiltro(); });
        txtBuscar.textProperty().addListener((o, a, b) -> aplicarFiltro());
        dpGastosDesde.valueProperty().addListener((o, a, b) -> { if (!actualizandoFiltros) consultarGastos(); });
        dpGastosHasta.valueProperty().addListener((o, a, b) -> { if (!actualizandoFiltros) consultarGastos(); });
        cmbMonedaGasto.valueProperty().addListener((o, a, b) -> { if (!actualizandoFiltros) consultarGastos(); });
    }

    private void configurarTablas() {
        tblSolicitudes.setItems(filtradas);
        tblSolicitudes.setPlaceholder(PanelEstadoTabla.crear(
                "Cargando solicitudes", "El listado aparecerá al terminar la consulta.", null, null));
        colPropiedad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().propiedadNombre()));
        colUnidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().unidadCodigo()));
        colDescripcion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().descripcion()));
        colPrioridad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().prioridad()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        colResponsable.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().responsable() == null ? "—" : c.getValue().responsable()));
        colAbiertaEl.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().abiertaEl()));
        colGastoUsd.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().totalGastoUsd()));
        colGastoUsd.setCellFactory(FormatoTabla.numerica());
        colGastoNio.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().totalGastoNio()));
        colGastoNio.setCellFactory(FormatoTabla.numerica());

        tblSolicitudes.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> actualizarBotones());

        tblGastos.setItems(gastos);
        tblGastos.setPlaceholder(PanelEstadoTabla.crear(
                "Cargando gastos", "Los gastos aparecerán al terminar la consulta.", null, null));
        colGastoFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fechaGasto()));
        colGastoPropiedad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().propiedadNombre()));
        colGastoSolicitud.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().solicitudDescripcion() == null ? "General de propiedad" : c.getValue().solicitudDescripcion()));
        colGastoConcepto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
        colGastoMoneda.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().moneda()));
        colGastoImporte.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().importe()));
        colGastoImporte.setCellFactory(FormatoTabla.numerica());
        colGastoCreador.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().creadoPor()));
    }

    private void cargarDatos() {
        ocultarError();
        PropiedadResponse propFiltro = cmbFiltroPropiedad.getValue();
        UUID propId = propFiltro == null ? null : UUID.fromString(propFiltro.id());
        LocalDate desde = dpGastosDesde.getValue();
        LocalDate hasta = dpGastosHasta.getValue();
        String moneda = TODAS.equals(cmbMonedaGasto.getValue()) ? null : cmbMonedaGasto.getValue();
        recargadorGeneral.recargar(
                () -> {
                    List<PropiedadResponse> props = catalogos.propiedades(false);
                    List<UnidadResponse> unis = catalogos.unidades();
                    List<SolicitudMantenimientoResponse> sols = cliente.listarSolicitudes(null, null, null, null);
                    ResumenGastosMantenimientoResponse resGastos = cliente.consultarGastos(propId, desde, hasta, moneda);
                    return new DatosCargados(props, unis, sols, resGastos);
                },
                datos -> {
                    propiedadesDisponibles = datos.propiedades();
                    unidadesDisponibles = datos.unidades();
                    actualizandoFiltros = true;
                    try {
                        RecargadorVista.conservarComboPorId(
                                cmbFiltroPropiedad, datos.propiedades(), true, PropiedadResponse::id);
                        RecargadorVista.conservarComboPorId(
                                cmbFiltroUnidad, datos.unidades(), true, UnidadResponse::id);
                    } finally {
                        actualizandoFiltros = false;
                    }
                    RecargadorVista.conservarSeleccionPorId(tblSolicitudes, SolicitudMantenimientoResponse::id, () -> {
                        solicitudes.setAll(datos.solicitudes());
                        aplicarFiltro();
                    });
                    mostrarGastos(datos.gastos());
                    actualizarBotones();
                },
                err -> {
                    barraError.mostrar(
                            mensajeSeguro(err, "No se pudieron cargar los datos de mantenimiento."),
                            this::cargarDatos);
                    if (solicitudes.isEmpty()) {
                        tblSolicitudes.setPlaceholder(PanelEstadoTabla.crear(
                                "No se pudieron cargar las solicitudes",
                                "Revisa la conexión e inténtalo de nuevo.",
                                "Reintentar", this::cargarDatos));
                    }
                    if (gastos.isEmpty()) {
                        tblGastos.setPlaceholder(PanelEstadoTabla.crear(
                                "No se pudieron cargar los gastos",
                                "Revisa la conexión e inténtalo de nuevo.",
                                "Reintentar", this::cargarDatos));
                    }
                });
    }

    @FXML
    public void consultarGastos() {
        ocultarError();
        PropiedadResponse propFiltro = cmbFiltroPropiedad.getValue();
        UUID propId = propFiltro == null ? null : UUID.fromString(propFiltro.id());
        LocalDate desde = dpGastosDesde.getValue();
        LocalDate hasta = dpGastosHasta.getValue();
        String moneda = TODAS.equals(cmbMonedaGasto.getValue()) ? null : cmbMonedaGasto.getValue();
        recargadorGastos.recargar(
                () -> cliente.consultarGastos(propId, desde, hasta, moneda),
                this::mostrarGastos,
                err -> {
                    barraError.mostrar(
                            mensajeSeguro(err, "No se pudieron consultar los gastos de mantenimiento."),
                            this::consultarGastos);
                    if (gastos.isEmpty()) {
                        tblGastos.setPlaceholder(PanelEstadoTabla.crear(
                                "No se pudieron consultar los gastos",
                                "Revisa la conexión e inténtalo de nuevo.",
                                "Reintentar", this::consultarGastos));
                    }
                });
    }

    @FXML
    public void handleNuevaSolicitud() {
        if (unidadesDisponibles.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin unidades", "Registra una unidad antes de abrir una solicitud.");
            return;
        }
        ComboBox<UnidadResponse> cmbUnidad = new ComboBox<>(FXCollections.observableArrayList(unidadesDisponibles));
        cmbUnidad.setConverter(new StringConverter<>() {
            @Override public String toString(UnidadResponse u) {
                return u == null ? "" : u.codigo() + " · " + u.propiedadNombre();
            }
            @Override public UnidadResponse fromString(String s) { return null; }
        });
        cmbUnidad.getSelectionModel().selectFirst();
        cmbUnidad.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> cmbPrioridad = new ComboBox<>(FXCollections.observableArrayList(
                ConstantesLatch.MANT_PRIORIDAD_BAJA,
                ConstantesLatch.MANT_PRIORIDAD_MEDIA,
                ConstantesLatch.MANT_PRIORIDAD_ALTA));
        cmbPrioridad.setValue(ConstantesLatch.MANT_PRIORIDAD_MEDIA);
        TextField txtResponsable = new TextField();
        txtResponsable.setPromptText("Nombre o empresa responsable (opcional)");
        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Describe la incidencia o trabajo requerido");
        txtDescripcion.setWrapText(true);
        txtDescripcion.setPrefRowCount(4);
        Label lblErr = crearLabelError();
        ValidacionFormulario.vincularTexto(txtDescripcion, () -> {
            String d = txtDescripcion.getText();
            ValidacionFormulario.marcarCampo(txtDescripcion, lblErr,
                    (d == null || d.isBlank()) ? "Describe la incidencia o trabajo requerido." : null);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Unidad *:"), cmbUnidad);
        grid.addRow(1, new Label("Prioridad *:"), cmbPrioridad);
        grid.addRow(2, new Label("Responsable:"), txtResponsable);
        grid.addRow(3, new Label("Descripción *:"), txtDescripcion);
        grid.add(lblErr, 0, 4, 2, 1);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Nueva solicitud de mantenimiento", grid, ventanaActual(), 560, 440, "Guardar solicitud");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            UnidadResponse u = cmbUnidad.getValue();
            String desc = txtDescripcion.getText();
            if (u == null || desc == null || desc.isBlank()) {
                ValidacionFormulario.marcarCampo(txtDescripcion, lblErr,
                        "Selecciona la unidad y escribe la descripción de la solicitud.");
                if (u == null) cmbUnidad.requestFocus(); else txtDescripcion.requestFocus();
                return;
            }
            accion.setDisable(true);
            Task<SolicitudMantenimientoResponse> tarea = new Task<>() {
                @Override protected SolicitudMantenimientoResponse call() {
                    return cliente.crearSolicitud(new SolicitudMantenimientoRequest(
                            u.id(), desc.strip(), cmbPrioridad.getValue(),
                            txtResponsable.getText(), null));
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Solicitud creada", "La solicitud quedó registrada como Abierta.");
                cargarDatos();
            }, err -> {
                accion.setDisable(false);
                mostrarErrorEn(lblErr, mensajeSeguro(err, "No se pudo registrar la solicitud."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleEditarSolicitud() {
        SolicitudMantenimientoResponse sel = tblSolicitudes.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        ComboBox<String> cmbPrioridad = new ComboBox<>(FXCollections.observableArrayList(
                ConstantesLatch.MANT_PRIORIDAD_BAJA,
                ConstantesLatch.MANT_PRIORIDAD_MEDIA,
                ConstantesLatch.MANT_PRIORIDAD_ALTA));
        cmbPrioridad.setValue(sel.prioridad());
        TextField txtResponsable = new TextField(sel.responsable() == null ? "" : sel.responsable());
        TextArea txtDescripcion = new TextArea(sel.descripcion());
        txtDescripcion.setWrapText(true);
        txtDescripcion.setPrefRowCount(4);
        Label lblErr = crearLabelError();
        ValidacionFormulario.vincularTexto(txtDescripcion, () -> {
            String d = txtDescripcion.getText();
            ValidacionFormulario.marcarCampo(txtDescripcion, lblErr,
                    (d == null || d.isBlank()) ? "La descripción es obligatoria." : null);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Prioridad *:"), cmbPrioridad);
        grid.addRow(1, new Label("Responsable:"), txtResponsable);
        grid.addRow(2, new Label("Descripción *:"), txtDescripcion);
        grid.add(lblErr, 0, 3, 2, 1);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Editar solicitud", grid, ventanaActual(), 520, 380, "Guardar cambios");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            if (txtDescripcion.getText() == null || txtDescripcion.getText().isBlank()) {
                ValidacionFormulario.marcarCampo(txtDescripcion, lblErr, "La descripción es obligatoria.");
                txtDescripcion.requestFocus();
                return;
            }
            accion.setDisable(true);
            Task<SolicitudMantenimientoResponse> tarea = new Task<>() {
                @Override protected SolicitudMantenimientoResponse call() {
                    return cliente.actualizarSolicitud(UUID.fromString(sel.id()),
                            new SolicitudMantenimientoUpdateRequest(
                                    sel.version(), txtDescripcion.getText(),
                                    cmbPrioridad.getValue(), txtResponsable.getText()));
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Solicitud actualizada", "Se guardaron los cambios de la solicitud.");
                cargarDatos();
            }, err -> {
                accion.setDisable(false);
                mostrarErrorEn(lblErr, mensajeSeguro(err, "No se pudo actualizar la solicitud."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleIniciarAtencion() {
        SolicitudMantenimientoResponse sel = tblSolicitudes.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextField txtResponsable = new TextField(sel.responsable() == null ? "" : sel.responsable());
        txtResponsable.setPromptText("Responsable asignado (opcional)");
        Label lblErr = crearLabelError();
        VBox box = new VBox(10,
                new Label("Solicitud: " + sel.unidadCodigo() + " — " + sel.descripcion()),
                new Label("Responsable:"), txtResponsable, lblErr);
        box.setPadding(new Insets(12));

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Iniciar atención", box, ventanaActual(), 480, 260, "Pasar a En proceso");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            accion.setDisable(true);
            Task<SolicitudMantenimientoResponse> tarea = new Task<>() {
                @Override protected SolicitudMantenimientoResponse call() {
                    return cliente.cambiarEstado(UUID.fromString(sel.id()),
                            new CambioEstadoMantenimientoRequest(
                                    sel.version(), ConstantesLatch.MANT_ESTADO_EN_PROCESO,
                                    null, txtResponsable.getText()));
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Solicitud en proceso", "La solicitud pasó a estado En proceso.");
                cargarDatos();
            }, err -> {
                accion.setDisable(false);
                mostrarErrorEn(lblErr, mensajeSeguro(err, "No se pudo cambiar el estado."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleResolver() {
        SolicitudMantenimientoResponse sel = tblSolicitudes.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        abrirCierreSolicitud(sel, ConstantesLatch.MANT_ESTADO_RESUELTA,
                "Resolver solicitud", "Detalle del trabajo realizado *", "Marcar como Resuelta");
    }

    @FXML
    public void handleCancelarSolicitud() {
        SolicitudMantenimientoResponse sel = tblSolicitudes.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        abrirCierreSolicitud(sel, ConstantesLatch.MANT_ESTADO_CANCELADA,
                "Cancelar solicitud", "Motivo de cancelación *", "Confirmar cancelación");
    }

    private void abrirCierreSolicitud(SolicitudMantenimientoResponse sel, String estadoDestino,
                                      String titulo, String etiquetaTexto, String textoBoton) {
        DatePicker dpFecha = new DatePicker(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA));
        TextArea txtTexto = new TextArea();
        txtTexto.setWrapText(true);
        txtTexto.setPrefRowCount(3);
        Label lblErr = crearLabelError();
        Runnable validarCierre = () -> {
            boolean sinFecha = dpFecha.getValue() == null;
            boolean sinTexto = txtTexto.getText() == null || txtTexto.getText().isBlank();
            ValidacionFormulario.marcarCampo(txtTexto, lblErr,
                    (sinFecha || sinTexto) ? "Completa la fecha y el detalle obligatorio." : null);
            if (sinFecha && !dpFecha.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                dpFecha.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            } else if (!sinFecha) {
                dpFecha.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            }
        };
        ValidacionFormulario.vincularSelector(dpFecha, validarCierre);
        ValidacionFormulario.vincularTexto(txtTexto, validarCierre);

        VBox box = new VBox(8,
                new Label("Solicitud: " + sel.unidadCodigo() + " — " + sel.descripcion()),
                new Label("Fecha *"), dpFecha,
                new Label(etiquetaTexto), txtTexto, lblErr);
        box.setPadding(new Insets(12));

        boolean esCancelacion = ConstantesLatch.MANT_ESTADO_CANCELADA.equals(estadoDestino);
        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                titulo, box, ventanaActual(), 500, 360, textoBoton, esCancelacion);
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            validarCierre.run();
            if (lblErr.isVisible()) {
                if (dpFecha.getValue() == null) dpFecha.requestFocus();
                else txtTexto.requestFocus();
                return;
            }
            accion.setDisable(true);
            Task<SolicitudMantenimientoResponse> tarea = new Task<>() {
                @Override protected SolicitudMantenimientoResponse call() {
                    return cliente.cambiarEstado(UUID.fromString(sel.id()),
                            new CambioEstadoMantenimientoRequest(
                                    sel.version(), estadoDestino,
                                    dpFecha.getValue().toString(), txtTexto.getText().strip()));
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Estado actualizado", "La solicitud quedó en estado " + estadoDestino + ".");
                cargarDatos();
            }, err -> {
                accion.setDisable(false);
                mostrarErrorEn(lblErr, mensajeSeguro(err, "No se pudo cerrar la solicitud."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleRegistrarGasto() {
        abrirDialogoGasto(null);
    }

    @FXML
    public void handleGastoDeSolicitud() {
        SolicitudMantenimientoResponse sel = tblSolicitudes.getSelectionModel().getSelectedItem();
        if (sel != null) abrirDialogoGasto(sel);
    }

    private void abrirDialogoGasto(SolicitudMantenimientoResponse solicitudPrevia) {
        if (propiedadesDisponibles.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin propiedades", "Registra una propiedad antes de cargar gastos.");
            return;
        }
        ComboBox<PropiedadResponse> cmbPropiedad = new ComboBox<>(FXCollections.observableArrayList(propiedadesDisponibles));
        cmbPropiedad.setConverter(new StringConverter<>() {
            @Override public String toString(PropiedadResponse p) { return p == null ? "" : p.nombre(); }
            @Override public PropiedadResponse fromString(String s) { return null; }
        });
        cmbPropiedad.setMaxWidth(Double.MAX_VALUE);
        if (solicitudPrevia != null && solicitudPrevia.propiedadId() != null) {
            propiedadesDisponibles.stream()
                    .filter(p -> Objects.equals(p.id(), solicitudPrevia.propiedadId()))
                    .findFirst().ifPresent(cmbPropiedad::setValue);
        }
        if (cmbPropiedad.getValue() == null) cmbPropiedad.getSelectionModel().selectFirst();

        DatePicker dpFecha = new DatePicker(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA));
        TextField txtImporte = new TextField();
        txtImporte.setPromptText("0.00");
        ComboBox<String> cmbMoneda = new ComboBox<>(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.setValue("USD");
        TextField txtConcepto = new TextField();
        txtConcepto.setPromptText("Concepto del gasto de mantenimiento");
        Label lblErr = crearLabelError();
        Runnable validarGasto = () -> {
            txtImporte.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            txtConcepto.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            dpFecha.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            String msg = null;
            if (cmbPropiedad.getValue() == null || dpFecha.getValue() == null) {
                if (dpFecha.getValue() == null && !dpFecha.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                    dpFecha.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                }
                msg = "Completa propiedad, fecha, importe y concepto.";
            } else {
                try {
                    BigDecimal imp = new BigDecimal(txtImporte.getText().trim());
                    if (imp.signum() <= 0 || imp.stripTrailingZeros().scale() > 2) throw new NumberFormatException();
                } catch (RuntimeException ex) {
                    if (!txtImporte.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                        txtImporte.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                    }
                    msg = "Escribe un importe positivo con hasta dos decimales.";
                }
                if (msg == null && (txtConcepto.getText() == null || txtConcepto.getText().isBlank())) {
                    if (!txtConcepto.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                        txtConcepto.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                    }
                    msg = "Escribe el concepto del gasto.";
                }
            }
            ValidacionFormulario.marcarCampo(null, lblErr, msg);
        };
        ValidacionFormulario.vincularSelector(cmbPropiedad, validarGasto);
        ValidacionFormulario.vincularSelector(dpFecha, validarGasto);
        ValidacionFormulario.vincularTexto(txtImporte, validarGasto);
        ValidacionFormulario.vincularTexto(txtConcepto, validarGasto);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        if (solicitudPrevia != null) {
            grid.addRow(0, new Label("Solicitud:"), new Label(solicitudPrevia.unidadCodigo() + " — " + solicitudPrevia.descripcion()));
        }
        grid.addRow(1, new Label("Propiedad *:"), cmbPropiedad);
        grid.addRow(2, new Label("Fecha *:"), dpFecha);
        grid.addRow(3, new Label("Importe *:"), txtImporte);
        grid.addRow(4, new Label("Moneda *:"), cmbMoneda);
        grid.addRow(5, new Label("Concepto *:"), txtConcepto);
        grid.add(lblErr, 0, 6, 2, 1);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Registrar gasto de propiedad", grid, ventanaActual(), 520, 410, "Registrar gasto");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            validarGasto.run();
            if (lblErr.isVisible()) return;
            PropiedadResponse p = cmbPropiedad.getValue();
            accion.setDisable(true);
            Task<GastoMantenimientoResponse> tarea = new Task<>() {
                @Override protected GastoMantenimientoResponse call() {
                    return cliente.registrarGasto(new GastoMantenimientoRequest(
                            p.id(),
                            solicitudPrevia == null ? null : solicitudPrevia.id(),
                            dpFecha.getValue().toString(),
                            txtImporte.getText().trim(),
                            cmbMoneda.getValue(),
                            txtConcepto.getText().strip()));
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Gasto registrado", "El egreso quedó registrado sin generar cargo al inquilino.");
                cargarDatos();
            }, err -> {
                accion.setDisable(false);
                mostrarErrorEn(lblErr, mensajeSeguro(err, "No se pudo registrar el gasto."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void limpiarFiltros() {
        actualizandoFiltros = true;
        try {
            cmbFiltroPropiedad.setValue(null);
            cmbFiltroUnidad.setValue(null);
            cmbFiltroPrioridad.setValue(TODAS);
            cmbFiltroEstado.setValue(TODOS);
            txtBuscar.clear();
        } finally {
            actualizandoFiltros = false;
        }
        aplicarFiltro();
        consultarGastos();
    }

    private void aplicarFiltro() {
        PropiedadResponse prop = cmbFiltroPropiedad.getValue();
        UnidadResponse uni = cmbFiltroUnidad.getValue();
        String pri = cmbFiltroPrioridad.getValue();
        String est = cmbFiltroEstado.getValue();
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);

        filtradas.setPredicate(s -> {
            if (prop != null && !Objects.equals(s.propiedadId(), prop.id())) return false;
            if (uni != null && !Objects.equals(s.unidadId(), uni.id())) return false;
            if (pri != null && !TODAS.equals(pri) && !pri.equals(s.prioridad())) return false;
            if (est != null && !TODOS.equals(est) && !est.equals(s.estado())) return false;
            return texto.isEmpty()
                    || contiene(s.descripcion(), texto)
                    || contiene(s.responsable(), texto)
                    || contiene(s.unidadCodigo(), texto)
                    || contiene(s.propiedadNombre(), texto);
        });
        lblContador.setText(filtradas.size() + (filtradas.size() == 1 ? " solicitud" : " solicitudes"));
        boolean hayFiltro = prop != null || uni != null
                || (pri != null && !TODAS.equals(pri))
                || (est != null && !TODOS.equals(est))
                || !texto.isEmpty();
        tblSolicitudes.setPlaceholder(PanelEstadoTabla.crear(
                hayFiltro ? "No hay coincidencias" : "No hay solicitudes de mantenimiento",
                hayFiltro ? "Prueba con otros filtros o limpia la búsqueda." : "Las solicitudes registradas aparecerán aquí.",
                hayFiltro ? "Limpiar filtros" : "Nueva solicitud",
                hayFiltro ? this::limpiarFiltros : this::handleNuevaSolicitud));
    }

    private void mostrarGastos(ResumenGastosMantenimientoResponse resumen) {
        RecargadorVista.conservarSeleccionPorId(tblGastos, GastoMantenimientoResponse::id,
                () -> gastos.setAll(resumen.gastos()));
        tblGastos.setPlaceholder(PanelEstadoTabla.crear(
                "No hay gastos registrados",
                "No hay gastos registrados para los filtros seleccionados.",
                null, null));
        lblTotalGastosUsd.setText(FormatoMoneda.formatear("USD", new BigDecimal(resumen.totalUsd())));
        lblTotalGastosNio.setText(FormatoMoneda.formatear("NIO", new BigDecimal(resumen.totalNio())));
    }

    private void actualizarBotones() {
        SolicitudMantenimientoResponse sel = tblSolicitudes.getSelectionModel().getSelectedItem();
        boolean hay = sel != null;
        boolean abierta = hay && ConstantesLatch.MANT_ESTADO_ABIERTA.equals(sel.estado());
        boolean enProceso = hay && ConstantesLatch.MANT_ESTADO_EN_PROCESO.equals(sel.estado());
        boolean terminal = hay && (ConstantesLatch.MANT_ESTADO_RESUELTA.equals(sel.estado())
                || ConstantesLatch.MANT_ESTADO_CANCELADA.equals(sel.estado()));
        btnEditarSolicitud.setDisable(!hay || terminal);
        btnIniciarAtencion.setDisable(!abierta);
        btnResolver.setDisable(!enProceso);
        btnCancelarSolicitud.setDisable(!hay || terminal);
        btnGastoSolicitud.setDisable(!hay);
    }

    private Window ventanaActual() {
        return rootView != null && rootView.getScene() != null ? rootView.getScene().getWindow() : null;
    }

    private void ocultarError() {
        barraError.ocultar();
    }

    private static Label crearLabelError() {
        Label lbl = new Label("");
        lbl.getStyleClass().add("field-error");
        lbl.setVisible(false);
        lbl.setManaged(false);
        lbl.setWrapText(true);
        return lbl;
    }

    private static void mostrarErrorEn(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private static boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private static String mensajeSeguro(Throwable error, String respaldo) {
        return MensajesUi.traducir(error, respaldo);
    }
}
