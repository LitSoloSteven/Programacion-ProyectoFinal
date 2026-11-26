package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRenovarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoTerminarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaLiquidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaListadoItemResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaResumenResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.ValidadorEntrada;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.BarraErrorContextual;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogosAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RecargadorVista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Lista, filtros, ciclo de vida contractual y liquidación de garantías vía HTTP. */
public class ContratoController {
    private static final String ESTADO_TODOS = "Todos los estados";
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private HBox garantiaTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnActivar;
    @FXML private Button btnCancelarBorrador;
    @FXML private Button btnRenovar;
    @FXML private Button btnTerminar;
    @FXML private Button btnEliminar;
    @FXML private Button btnAjustarGarantia;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<UnidadResponse> cmbFiltroUnidad;
    @FXML private ComboBox<InquilinoResponse> cmbFiltroInquilino;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private TableView<ContratoResponse> tblContratos;
    @FXML private TableColumn<ContratoResponse, String> colInquilino;
    @FXML private TableColumn<ContratoResponse, String> colUnidad;
    @FXML private TableColumn<ContratoResponse, String> colInicio;
    @FXML private TableColumn<ContratoResponse, String> colFin;
    @FXML private TableColumn<ContratoResponse, String> colCanon;
    @FXML private TableColumn<ContratoResponse, Integer> colDiaPago;
    @FXML private TableColumn<ContratoResponse, String> colDeposito;
    @FXML private TableColumn<ContratoResponse, String> colSaldoGarantia;
    @FXML private TableColumn<ContratoResponse, String> colEstado;
    @FXML private TableColumn<ContratoResponse, Void> colAcciones;

    private final ClienteCatalogos clienteCatalogos = ClienteLatch.catalogos();
    private final ClientePagos clientePagos = ClienteLatch.pagos();
    private final ObservableList<ContratoResponse> registros = FXCollections.observableArrayList();
    private final FilteredList<ContratoResponse> filtrados = new FilteredList<>(registros, item -> true);
    private List<UnidadResponse> unidadesDisponibles = List.of();
    private List<InquilinoResponse> inquilinosDisponibles = List.of();
    private ContratoResponse seleccionado;
    private final boolean permisoEliminar = SesionCliente.getActual().esPropietario();
    private final boolean permisoLiquidarGarantia = SesionCliente.getActual().esPropietario();
    private RecargadorVista recargador;
    private BarraErrorContextual barraError;
    private boolean actualizandoFiltros;

    private record Catalogos(List<UnidadResponse> unidades, List<InquilinoResponse> inquilinos) { }
    private record DatosTabla(Catalogos catalogos, List<ContratoResponse> contratos,
                              Map<String, GarantiaListadoItemResponse> garantias) { }
    private record DatosGarantiaDialogo(GarantiaResumenResponse resumen,
                                        List<EstadoCuentaResponse.CargoEstadoCuenta> cargosAbiertos) { }

    private Map<String, GarantiaListadoItemResponse> garantias = Map.of();

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.CONTRATO, 20));
        recargador = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        btnNuevo.disableProperty().bind(recargador.cargandoProperty());
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnActivar, Iconos.Nombre.CONFIRMACION, 16);
        Iconos.asignar(btnCancelarBorrador, Iconos.Nombre.CERRAR, 16);
        Iconos.asignar(btnRenovar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnTerminar, Iconos.Nombre.CONFIRMACION, 16);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Iconos.asignar(btnAjustarGarantia, Iconos.Nombre.EDITAR, 16);
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona un contrato para eliminarlo." : "Solo el propietario puede eliminar contratos."));
        Tooltip.install(garantiaTooltipHost, new Tooltip(permisoLiquidarGarantia
                ? "Consulta movimientos y liquida la garantía del contrato."
                : "Consulta el estado e historial de la garantía (liquidación reservada al propietario)."));

        configurarFiltros();

        tblContratos.setItems(filtrados);
        tblContratos.setPlaceholder(PanelEstadoTabla.crear("Cargando contratos", "El listado aparecerá al terminar la consulta.", null, null));
        colInquilino.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().inquilinoNombre()));
        colUnidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().unidadInfo()));
        colInicio.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().inicio()));
        colInicio.setCellFactory(FormatoTabla.fechaIso());
        colFin.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fin()));
        colFin.setCellFactory(FormatoTabla.fechaIso());
        colCanon.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearMoneda(c.getValue().moneda(), c.getValue().canon())));
        colCanon.setCellFactory(FormatoTabla.numerica());
        colDiaPago.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().diaPago()));
        colDiaPago.setCellFactory(FormatoTabla.entero());
        colDeposito.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearMoneda(c.getValue().moneda(), c.getValue().depositoPactado())));
        colDeposito.setCellFactory(FormatoTabla.numerica());
        colSaldoGarantia.setCellValueFactory(c -> new ReadOnlyStringWrapper(saldoGarantia(c.getValue())));
        colSaldoGarantia.setCellFactory(FormatoTabla.numerica());
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Editar contrato", this::abrirEdicion, Iconos.Nombre.EDITAR),
                new AccionesTabla.Accion<>("Documento PDF", this::abrirDocumentoPdf, Iconos.Nombre.REPORTE),
                new AccionesTabla.Accion<>("Garantía", this::abrirGarantia, Iconos.Nombre.EDITAR)));
        tblContratos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionado = actual;
            actualizarAcciones();
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        actualizarAcciones();
        aplicarFiltro();
        cargarDatos();
        recargador.activarConsultaPeriodica(Duration.seconds(60), () -> recargador.recargarSiInactivo(
                this::consultarDatosTabla,
                this::aplicarDatosTabla,
                this::manejarErrorConsulta));
    }

    public void aplicarFiltroEstadoInicial(String estado) {
        if (estado == null || estado.isBlank() || cmbFiltroEstado == null) {
            return;
        }
        cmbFiltroEstado.setValue(estado);
        aplicarFiltro();
    }

    private void configurarFiltros() {
        cmbFiltroUnidad.setConverter(new StringConverter<>() {
            @Override public String toString(UnidadResponse unidad) {
                return unidad == null ? "Todas las unidades" : unidad.codigo() + " · " + unidad.propiedadNombre();
            }
            @Override public UnidadResponse fromString(String string) { return null; }
        });
        cmbFiltroInquilino.setConverter(new StringConverter<>() {
            @Override public String toString(InquilinoResponse inquilino) {
                return inquilino == null ? "Todos los inquilinos" : inquilino.nombre();
            }
            @Override public InquilinoResponse fromString(String string) { return null; }
        });
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(
                ESTADO_TODOS, "Borrador", "Vigente", "Por vencer", "Vencido", "Terminado", "Cancelado"));
        cmbFiltroEstado.setValue(ESTADO_TODOS);
        cmbFiltroUnidad.valueProperty().addListener((obs, a, b) -> { if (!actualizandoFiltros) aplicarFiltro(); });
        cmbFiltroInquilino.valueProperty().addListener((obs, a, b) -> { if (!actualizandoFiltros) aplicarFiltro(); });
        cmbFiltroEstado.valueProperty().addListener((obs, a, b) -> { if (!actualizandoFiltros) aplicarFiltro(); });
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText();
        String filtro = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        UnidadResponse unidadFiltro = cmbFiltroUnidad.getValue();
        InquilinoResponse inquilinoFiltro = cmbFiltroInquilino.getValue();
        String estadoFiltro = cmbFiltroEstado.getValue();
        boolean filtrarEstado = estadoFiltro != null && !ESTADO_TODOS.equals(estadoFiltro);

        filtrados.setPredicate(contrato -> {
            boolean coincideTexto = filtro.isEmpty()
                    || contiene(contrato.inquilinoNombre(), filtro)
                    || contiene(contrato.unidadInfo(), filtro)
                    || contiene(contrato.estado(), filtro);
            boolean coincideUnidad = unidadFiltro == null
                    || Objects.equals(contrato.unidadId(), unidadFiltro.id());
            boolean coincideInquilino = inquilinoFiltro == null
                    || Objects.equals(contrato.inquilinoId(), inquilinoFiltro.id());
            boolean coincideEstado = !filtrarEstado
                    || Objects.equals(contrato.estado(), estadoFiltro);
            return coincideTexto && coincideUnidad && coincideInquilino && coincideEstado;
        });

        boolean hayFiltro = !filtro.isEmpty() || unidadFiltro != null || inquilinoFiltro != null || filtrarEstado;
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblContratos.setPlaceholder(PanelEstadoTabla.crear(
                hayFiltro ? "No hay coincidencias" : "Aún no hay contratos",
                hayFiltro ? "Prueba con otros filtros o limpia la búsqueda." : "Los contratos registrados aparecerán aquí.",
                hayFiltro ? "Limpiar filtros" : "Nuevo contrato",
                hayFiltro ? this::limpiarFiltros : this::handleNuevo));
    }

    public void cargarCatalogosReales() { cargarDatos(); }

    public void cargarDatos() {
        recargador.recargar(this::consultarDatosTabla, this::aplicarDatosTabla, this::manejarErrorConsulta);
    }

    private DatosTabla consultarDatosTabla() {
        Catalogos catalogos = new Catalogos(clienteCatalogos.unidades(), clienteCatalogos.inquilinos(true));
        List<ContratoResponse> contratos = clienteCatalogos.contratos(false);
        Map<String, GarantiaListadoItemResponse> seguimiento = clienteCatalogos.listarGarantias().stream()
                .collect(Collectors.toMap(GarantiaListadoItemResponse::contratoId, g -> g, (a, b) -> a));
        return new DatosTabla(catalogos, contratos, seguimiento);
    }

    private void aplicarDatosTabla(DatosTabla datos) {
        mostrarCatalogos(datos.catalogos());
        garantias = datos.garantias();
        RecargadorVista.conservarSeleccionPorId(tblContratos, ContratoResponse::id, () -> {
            registros.setAll(datos.contratos());
            aplicarFiltro();
        });
        actualizarAcciones();
        ocultarError();
    }

    private void manejarErrorConsulta(Throwable error) {
        barraError.mostrar("No se pudieron cargar los contratos y sus opciones. Vuelve a intentarlo.", this::cargarDatos);
        if (registros.isEmpty()) {
            tblContratos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los contratos",
                    "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
        }
    }

    @FXML public void handleNuevo() {
        if (!btnNuevo.isDisabled()) abrirFormulario(null);
    }

    private void abrirEdicion(ContratoResponse contrato) { abrirFormulario(contrato); }

    private void abrirDocumentoPdf(ContratoResponse contrato) {
        if (contrato == null || contrato.id() == null) return;
        ClienteAdjuntos adjuntos = ClienteLatch.adjuntos();
        UUID contratoId = UUID.fromString(contrato.id());
        DialogosAdjuntos.mostrarGestorAdjuntos(
                ventanaActual(),
                "Documento contractual — " + contrato.inquilinoNombre(),
                "Adjunta únicamente archivos PDF de hasta 5 MiB vinculados a esta versión contractual.",
                true,
                adjuntos,
                () -> adjuntos.listarPorContrato(contratoId),
                archivo -> adjuntos.adjuntarDocumentoContrato(contratoId, archivo));
    }

    private void abrirFormulario(ContratoResponse existente) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("contrato-form.fxml"));
            Node contenido = loader.load();
            ContratoFormController formulario = loader.getController();
            formulario.establecerValidadorServidor(ClienteLatch.validaciones());
            formulario.establecerCatalogos(unidadesDisponibles, inquilinosDisponibles);
            if (existente != null) formulario.cargar(existente);
            Window propietario = tblContratos.getScene() == null ? null : tblContratos.getScene().getWindow();
            boolean nuevo = existente == null;
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(nuevo ? "Nuevo contrato" : "Editar contrato",
                    contenido, propietario, 740, 680, nuevo ? "Guardar contrato" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nuevo ? btnNuevo : tblContratos, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                ContratoFormController.Datos datos = formulario.datos();
                UUID contratoId = nuevo ? null : UUID.fromString(existente.id());
                long version = nuevo ? 0 : existente.version();
                String unidadId = datos.unidad().id();
                String inquilinoId = datos.inquilino().id();
                accion.setDisable(true); accion.setText("Guardando contrato…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        clienteCatalogos.guardarContrato(contratoId,
                                new ContratoRequest(unidadId, inquilinoId,
                                        datos.inicio().toString(), datos.fin().toString(),
                                        datos.canon().toPlainString(), datos.moneda(), datos.diaPago(),
                                        datos.deposito().toPlainString(), datos.estado(),
                                        nuevo ? null : version));
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                    formulario.marcarLimpio(); DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito(nuevo ? "Contrato registrado" : "Contrato actualizado",
                            nuevo ? "El contrato quedó registrado." : "Los términos del contrato quedaron guardados.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText(nuevo ? "Guardar contrato" : "Guardar cambios");
                    if (!nuevo && error instanceof ErrorClienteException api && api.getEstado() == 409) {
                        DialogoFormulario.cerrar(dialogo);
                        cargarDatos();
                        AlertaUtil.mostrarAdvertencia("No se guardaron los cambios",
                                api.getMessage() + " Abre el registro actualizado para volver a editarlo.");
                        return;
                    }
                    formulario.mostrarError(mensajeError(error));
                });
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleActivarContrato() {
        ContratoResponse contrato = seleccionado;
        if (contrato == null || !"Borrador".equals(contrato.estado())) return;
        String detalle = "Contrato de " + contrato.inquilinoNombre() + " · " + contrato.unidadInfo()
                + "\nVigencia: " + fecha(contrato.inicio()) + " a " + fecha(contrato.fin())
                + "\nEl estado pasará a Vigente y se reservará la unidad.";
        if (!AlertaUtil.mostrarConfirmacion("Activar borrador", detalle, "Activar")) return;

        UUID contratoId = UUID.fromString(contrato.id());
        long version = contrato.version();
        btnActivar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                clienteCatalogos.activarContrato(contratoId, version);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Contrato activado", "El contrato quedó Vigente y con su garantía registrada.");
            cargarDatos();
        }, error -> {
            actualizarAcciones();
            AlertaUtil.mostrarError("No se pudo activar el contrato", mensajeError(error));
        });
    }

    @FXML public void handleCancelarBorrador() {
        ContratoResponse contrato = seleccionado;
        if (contrato == null || !"Borrador".equals(contrato.estado())) return;

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo de cancelación del borrador");
        txtMotivo.getStyleClass().add("field");
        Label lblError = crearLabelError();
        VBox contenido = new VBox(8,
                new Label("Contrato en borrador: " + contrato.inquilinoNombre() + " · " + contrato.unidadInfo()),
                new Label("Motivo obligatorio (sin movimientos financieros):"),
                txtMotivo, lblError);
        contenido.setPadding(new Insets(16));

        Window propietario = ventanaActual();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear("Cancelar borrador", contenido, propietario,
                480, 290, "Cancelar borrador", true);
        DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnCancelarBorrador,
                () -> !txtMotivo.getText().isBlank());
        Runnable validarMotivo = () -> {
            String m = txtMotivo.getText() == null ? "" : txtMotivo.getText().strip();
            String msg = m.isEmpty() || m.length() > 500 ? "Escribe un motivo válido (máximo 500 caracteres)." : null;
            ValidacionFormulario.marcarCampo(txtMotivo, lblError, msg);
        };
        ValidacionFormulario.vincularTexto(txtMotivo, validarMotivo);
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            validarMotivo.run();
            if (lblError.isVisible()) {
                txtMotivo.requestFocus();
                return;
            }
            String motivo = txtMotivo.getText().strip();
            UUID contratoId = UUID.fromString(contrato.id());
            long version = contrato.version();
            accion.setDisable(true); accion.setText("Cancelando…");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    clienteCatalogos.cancelarBorrador(contratoId, version, motivo);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Borrador cancelado", "El borrador quedó marcado como Cancelado.");
                cargarDatos();
            }, error -> {
                accion.setDisable(false); accion.setText("Cancelar borrador");
                mostrarErrorInline(lblError, mensajeError(error));
            });
        });
        dialogo.showAndWait();
    }

    @FXML public void handleRenovarContrato() {
        ContratoResponse contrato = seleccionado;
        if (contrato == null || !sePuedeRenovar(contrato)) return;

        LocalDate inicioSugerido = LocalDate.parse(contrato.fin()).plusDays(1);
        LocalDate finSugerido = inicioSugerido.plusYears(1).minusDays(1);
        DatePicker dpInicio = new DatePicker(inicioSugerido);
        dpInicio.getStyleClass().add("field");
        DatePicker dpFin = new DatePicker(finSugerido);
        dpFin.getStyleClass().add("field");
        TextField txtCanon = new TextField(contrato.canon());
        txtCanon.getStyleClass().add("field");
        ComboBox<String> cmbMoneda = new ComboBox<>(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.setValue(contrato.moneda());
        cmbMoneda.getStyleClass().add("field");
        TextField txtDiaPago = new TextField(String.valueOf(contrato.diaPago()));
        txtDiaPago.getStyleClass().add("field");
        TextField txtDeposito = new TextField(contrato.depositoPactado());
        txtDeposito.getStyleClass().add("field");
        Label lblError = crearLabelError();

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        grid.addRow(0, new Label("Inicio *"), dpInicio, new Label("Fin *"), dpFin);
        grid.addRow(1, new Label("Canon *"), txtCanon, new Label("Moneda *"), cmbMoneda);
        grid.addRow(2, new Label("Día de pago *"), txtDiaPago, new Label("Depósito"), txtDeposito);
        VBox contenido = new VBox(10,
                new Label("Renovación enlazada de " + contrato.inquilinoNombre() + " · " + contrato.unidadInfo()),
                new Label("Se creará un borrador sucesor sin alterar el contrato actual ni rentas pasadas."),
                grid, lblError);
        contenido.setPadding(new Insets(16));

        Window propietario = ventanaActual();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear("Renovar contrato", contenido, propietario,
                620, 420, "Crear borrador sucesor");
        Runnable validarRenovacion = () -> {
            String msg = null;
            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();
            dpInicio.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            dpFin.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            txtCanon.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            txtDiaPago.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            txtDeposito.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            try {
                ValidadorEntrada.validarRangoFechas(inicio, fin, 1);
            } catch (RuntimeException ex) {
                if (!dpFin.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                    dpFin.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                }
                msg = ex.getMessage();
            }
            if (msg == null) {
                try {
                    BigDecimal canon = new BigDecimal(txtCanon.getText().strip());
                    if (canon.signum() <= 0 || canon.stripTrailingZeros().scale() > 2) {
                        throw new IllegalArgumentException("El canon debe ser positivo con hasta dos decimales.");
                    }
                } catch (RuntimeException ex) {
                    if (!txtCanon.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                        txtCanon.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                    }
                    msg = "El canon debe ser positivo con hasta dos decimales.";
                }
            }
            if (msg == null) {
                try {
                    int diaPago = Integer.parseInt(txtDiaPago.getText().strip());
                    if (diaPago < 1 || diaPago > 31) {
                        throw new IllegalArgumentException("El día de pago debe estar entre 1 y 31.");
                    }
                } catch (RuntimeException ex) {
                    if (!txtDiaPago.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                        txtDiaPago.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                    }
                    msg = "El día de pago debe estar entre 1 y 31.";
                }
            }
            if (msg == null) {
                try {
                    BigDecimal deposito = txtDeposito.getText().isBlank()
                            ? BigDecimal.ZERO : new BigDecimal(txtDeposito.getText().strip());
                    if (deposito.signum() < 0 || deposito.stripTrailingZeros().scale() > 2) {
                        throw new IllegalArgumentException("El depósito debe ser cero o mayor con hasta dos decimales.");
                    }
                } catch (RuntimeException ex) {
                    if (!txtDeposito.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                        txtDeposito.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                    }
                    msg = "El depósito debe ser cero o mayor con hasta dos decimales.";
                }
            }
            ValidacionFormulario.marcarCampo(null, lblError, msg);
        };
        ValidacionFormulario.vincularSelector(dpInicio, validarRenovacion);
        ValidacionFormulario.vincularSelector(dpFin, validarRenovacion);
        ValidacionFormulario.vincularTexto(txtCanon, validarRenovacion);
        ValidacionFormulario.vincularTexto(txtDiaPago, validarRenovacion);
        ValidacionFormulario.vincularTexto(txtDeposito, validarRenovacion);

        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            validarRenovacion.run();
            if (lblError.isVisible()) return;
            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();
            try {
                ValidadorEntrada.validarRangoFechas(inicio, fin, 1);
                BigDecimal canon = new BigDecimal(txtCanon.getText().strip());
                int diaPago = Integer.parseInt(txtDiaPago.getText().strip());
                BigDecimal deposito = txtDeposito.getText().isBlank()
                        ? BigDecimal.ZERO : new BigDecimal(txtDeposito.getText().strip());
                var solicitud = new ContratoRenovarRequest(
                        inicio.toString(), fin.toString(), canon.toPlainString(),
                        cmbMoneda.getValue(), diaPago, deposito.toPlainString());
                UUID anteriorId = UUID.fromString(contrato.id());
                accion.setDisable(true); accion.setText("Creando borrador…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        clienteCatalogos.renovarContrato(anteriorId, solicitud);
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito("Renovación preparada",
                            "Se creó el borrador sucesor enlazado a este contrato.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText("Crear borrador sucesor");
                    mostrarErrorInline(lblError, mensajeError(error));
                });
            } catch (RuntimeException ex) {
                mostrarErrorInline(lblError, ex.getMessage());
            }
        });
        dialogo.showAndWait();
    }

    @FXML public void handleTerminarContrato() {
        ContratoResponse contrato = seleccionado;
        if (contrato == null || !sePuedeFinalizar(contrato)) return;

        List<ContratoResponse> sucesores = registros.stream()
                .filter(c -> Objects.equals(contrato.id(), c.predecesorId())
                        && "Borrador".equals(c.estado()))
                .toList();

        DatePicker dpFecha = new DatePicker(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA));
        dpFecha.getStyleClass().add("field");
        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo de terminación (obligatorio)");
        txtMotivo.getStyleClass().add("field");
        ComboBox<ContratoResponse> cmbSucesor = new ComboBox<>();
        cmbSucesor.getStyleClass().add("field");
        cmbSucesor.setMaxWidth(Double.MAX_VALUE);
        cmbSucesor.setConverter(new StringConverter<>() {
            @Override public String toString(ContratoResponse c) {
                return c == null ? "Sin activar borrador sucesor"
                        : "Activar borrador: " + fecha(c.inicio()) + " a " + fecha(c.fin())
                        + " (" + formatearMoneda(c.moneda(), c.canon()) + ")";
            }
            @Override public ContratoResponse fromString(String s) { return null; }
        });
        List<ContratoResponse> opcionesSucesor = new ArrayList<>();
        opcionesSucesor.add(null);
        opcionesSucesor.addAll(sucesores);
        cmbSucesor.setItems(FXCollections.observableArrayList(opcionesSucesor));
        cmbSucesor.getSelectionModel().selectFirst();

        GarantiaListadoItemResponse garantia = garantias.get(contrato.id());
        BigDecimal saldoGarantia = garantia != null
                ? new BigDecimal(garantia.saldoActual())
                : (contrato.garantiaSaldo() == null ? BigDecimal.ZERO : new BigDecimal(contrato.garantiaSaldo()));
        Label lblGarantia = new Label(saldoGarantia.signum() > 0
                ? "Saldo retenido de garantía: " + FormatoMoneda.formatear(contrato.moneda(), saldoGarantia)
                + ". Liquídalo desde el botón Garantía antes de terminar."
                : "Garantía sin saldo retenido pendiente.");
        lblGarantia.setWrapText(true);
        Label lblError = crearLabelError();

        VBox contenido = new VBox(8,
                new Label("Contrato de " + contrato.inquilinoNombre() + " · " + contrato.unidadInfo()),
                lblGarantia,
                new Label("Fecha de terminación *"), dpFecha,
                new Label("Motivo de terminación *"), txtMotivo);
        if (!sucesores.isEmpty()) {
            contenido.getChildren().addAll(
                    new Label("Cierre atómico con renovación enlazada (opcional):"),
                    cmbSucesor);
        }
        contenido.getChildren().add(lblError);
        contenido.setPadding(new Insets(16));

        Window propietario = ventanaActual();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear("Finalizar contrato", contenido, propietario,
                540, sucesores.isEmpty() ? 400 : 460, "Finalizar contrato", true);
        DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnTerminar,
                () -> !txtMotivo.getText().isBlank());
        Runnable validarTerminacion = () -> {
            LocalDate fechaTerminacion = dpFecha.getValue();
            String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().strip();
            ValidacionFormulario.marcarCampo(dpFecha, null, fechaTerminacion == null ? "err" : null);
            String msg = null;
            if (fechaTerminacion == null) {
                msg = "Selecciona la fecha de terminación.";
            } else if (motivo.isEmpty() || motivo.length() > 500) {
                msg = "Escribe un motivo válido (máximo 500 caracteres).";
            }
            ValidacionFormulario.marcarCampo(txtMotivo, lblError,
                    (motivo.isEmpty() || motivo.length() > 500) ? msg : null);
            if (fechaTerminacion == null) {
                ValidacionFormulario.marcarCampo(null, lblError, msg);
            }
        };
        ValidacionFormulario.vincularSelector(dpFecha, validarTerminacion);
        ValidacionFormulario.vincularTexto(txtMotivo, validarTerminacion);
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            validarTerminacion.run();
            if (lblError.isVisible()) {
                if (dpFecha.getValue() == null) dpFecha.requestFocus();
                else txtMotivo.requestFocus();
                return;
            }
            LocalDate fechaTerminacion = dpFecha.getValue();
            String motivo = txtMotivo.getText().strip();
            ContratoResponse sucesorElegido = cmbSucesor.getValue();
            var solicitud = new ContratoTerminarRequest(
                    contrato.version(),
                    fechaTerminacion.toString(),
                    motivo,
                    sucesorElegido == null ? null : sucesorElegido.id(),
                    null);
            UUID contratoId = UUID.fromString(contrato.id());
            accion.setDisable(true); accion.setText("Finalizando…");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    clienteCatalogos.terminarContratoConCierre(contratoId, solicitud);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Contrato finalizado",
                        sucesorElegido != null
                                ? "El contrato se cerró y el borrador sucesor quedó activado."
                                : "El contrato quedó marcado como Terminado.");
                cargarDatos();
            }, error -> {
                accion.setDisable(false); accion.setText("Finalizar contrato");
                mostrarErrorInline(lblError, mensajeError(error));
            });
        });
        dialogo.showAndWait();
    }

    @FXML public void handleAjustarGarantia() {
        abrirGarantia(seleccionado);
    }

    private void abrirGarantia(ContratoResponse contrato) {
        if (contrato == null || contrato.id() == null) return;
        UUID contratoId = UUID.fromString(contrato.id());
        btnAjustarGarantia.setDisable(true);
        Task<DatosGarantiaDialogo> tarea = new Task<>() {
            @Override protected DatosGarantiaDialogo call() {
                GarantiaResumenResponse resumen = clienteCatalogos.obtenerGarantia(contratoId);
                List<EstadoCuentaResponse.CargoEstadoCuenta> cargosAbiertos = List.of();
                if (permisoLiquidarGarantia && new BigDecimal(resumen.saldoActual()).signum() > 0) {
                    EstadoCuentaResponse cuenta = clientePagos.estadoCuenta(contrato.id(), null);
                    cargosAbiertos = cuenta.cargos().stream()
                            .filter(c -> !"Anulado".equalsIgnoreCase(c.estado())
                                    && !"Pagado".equalsIgnoreCase(c.estado())
                                    && new BigDecimal(c.saldo()).signum() > 0)
                            .toList();
                }
                return new DatosGarantiaDialogo(resumen, cargosAbiertos);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, datos -> {
            actualizarAcciones();
            mostrarDialogoGarantia(contrato, datos);
        }, error -> {
            actualizarAcciones();
            AlertaUtil.mostrarError("No se pudo consultar la garantía", mensajeError(error));
        });
    }

    private void mostrarDialogoGarantia(ContratoResponse contrato, DatosGarantiaDialogo datos) {
        GarantiaResumenResponse resumen = datos.resumen();
        String moneda = resumen.moneda();
        Label lblResumen = new Label(
                "Estado: " + resumen.estado()
                        + "  |  Inicial: " + resumen.montoInicial() + " " + moneda
                        + "  |  Aplicado: " + resumen.totalAplicado() + " " + moneda
                        + "  |  Deducido: " + resumen.totalDeducido() + " " + moneda
                        + "  |  Devuelto: " + resumen.totalDevuelto() + " " + moneda
                        + "  |  Retenido: " + resumen.saldoActual() + " " + moneda
                        + (resumen.conciliado() ? "  (Conciliado)" : "  (Descuadre)"));
        lblResumen.setWrapText(true);

        TableView<GarantiaResumenResponse.MovimientoGarantiaResponse> tblMovimientos = new TableView<>();
        tblMovimientos.setPrefHeight(180);
        tblMovimientos.setPlaceholder(new Label("Sin movimientos registrados."));
        TableColumn<GarantiaResumenResponse.MovimientoGarantiaResponse, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().fechaMovimiento()));
        colFecha.setPrefWidth(95);
        TableColumn<GarantiaResumenResponse.MovimientoGarantiaResponse, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipo()));
        colTipo.setPrefWidth(95);
        TableColumn<GarantiaResumenResponse.MovimientoGarantiaResponse, String> colImporte = new TableColumn<>("Importe");
        colImporte.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().importe() + " " + moneda));
        colImporte.setPrefWidth(115);
        TableColumn<GarantiaResumenResponse.MovimientoGarantiaResponse, String> colDetalle = new TableColumn<>("Destino / Método");
        colDetalle.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().cargoResumen() != null ? c.getValue().cargoResumen()
                        : (c.getValue().metodo() != null ? c.getValue().metodo() : "—")));
        colDetalle.setPrefWidth(180);
        TableColumn<GarantiaResumenResponse.MovimientoGarantiaResponse, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
        colConcepto.setPrefWidth(220);
        tblMovimientos.getColumns().addAll(List.of(colFecha, colTipo, colImporte, colDetalle, colConcepto));
        tblMovimientos.setItems(FXCollections.observableArrayList(resumen.movimientos()));

        VBox contenido = new VBox(10,
                new Label("Contrato: " + contrato.inquilinoNombre() + " · " + contrato.unidadInfo()),
                lblResumen,
                tblMovimientos);
        contenido.setPadding(new Insets(16));

        BigDecimal saldoRetenido = new BigDecimal(resumen.saldoActual());
        boolean puedeLiquidar = permisoLiquidarGarantia && saldoRetenido.signum() > 0;

        ObservableList<GarantiaLiquidacionRequest.MovimientoItemRequest> borradores =
                FXCollections.observableArrayList();
        Label lblError = crearLabelError();
        if (puedeLiquidar) {
            ComboBox<String> cmbTipo = new ComboBox<>(FXCollections.observableArrayList(
                    ConstantesLatch.GARANTIA_TIPO_APLICACION,
                    ConstantesLatch.GARANTIA_TIPO_DEDUCCION,
                    ConstantesLatch.GARANTIA_TIPO_DEVOLUCION));
            cmbTipo.setValue(ConstantesLatch.GARANTIA_TIPO_DEVOLUCION);
            cmbTipo.getStyleClass().add("field");

            ComboBox<EstadoCuentaResponse.CargoEstadoCuenta> cmbCargo = new ComboBox<>(
                    FXCollections.observableArrayList(datos.cargosAbiertos()));
            cmbCargo.setPromptText(datos.cargosAbiertos().isEmpty()
                    ? "Sin cargos abiertos" : "Selecciona cargo");
            cmbCargo.getStyleClass().add("field");
            cmbCargo.setConverter(new StringConverter<>() {
                @Override public String toString(EstadoCuentaResponse.CargoEstadoCuenta c) {
                    return c == null ? "" : c.tipo() + " · " + c.concepto() + " (saldo " + c.saldo() + ")";
                }
                @Override public EstadoCuentaResponse.CargoEstadoCuenta fromString(String s) { return null; }
            });

            ComboBox<String> cmbMetodo = new ComboBox<>(FXCollections.observableArrayList(
                    "Efectivo", "Transferencia", "Depósito bancario", "Otro"));
            cmbMetodo.setValue("Transferencia");
            cmbMetodo.getStyleClass().add("field");

            TextField txtImporte = new TextField(saldoRetenido.toPlainString());
            txtImporte.getStyleClass().add("field");
            TextField txtConcepto = new TextField();
            txtConcepto.setPromptText("Concepto obligatorio");
            txtConcepto.getStyleClass().add("field");
            Button btnAgregarMov = new Button("Agregar línea");
            btnAgregarMov.getStyleClass().add("button-secondary");

            Runnable sincronizarCampos = () -> {
                String t = cmbTipo.getValue();
                boolean esAplic = ConstantesLatch.GARANTIA_TIPO_APLICACION.equals(t);
                boolean esDev = ConstantesLatch.GARANTIA_TIPO_DEVOLUCION.equals(t);
                cmbCargo.setDisable(!esAplic);
                cmbMetodo.setDisable(!esDev);
            };
            cmbTipo.valueProperty().addListener((o, a, b) -> sincronizarCampos.run());
            sincronizarCampos.run();

            TableView<GarantiaLiquidacionRequest.MovimientoItemRequest> tblPaquete = new TableView<>(borradores);
            tblPaquete.setPrefHeight(110);
            tblPaquete.setPlaceholder(new Label("Agrega una o más salidas para liquidar la garantía."));
            TableColumn<GarantiaLiquidacionRequest.MovimientoItemRequest, String> pTipo = new TableColumn<>("Tipo");
            pTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipo()));
            TableColumn<GarantiaLiquidacionRequest.MovimientoItemRequest, String> pImp = new TableColumn<>("Importe");
            pImp.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().importe() + " " + moneda));
            TableColumn<GarantiaLiquidacionRequest.MovimientoItemRequest, String> pConc = new TableColumn<>("Concepto");
            pConc.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
            tblPaquete.getColumns().addAll(List.of(pTipo, pImp, pConc));

            ValidacionFormulario.vincularTexto(txtImporte, () -> {
                String msg = null;
                try {
                    BigDecimal imp = new BigDecimal(txtImporte.getText().strip());
                    if (imp.signum() <= 0 || imp.stripTrailingZeros().scale() > 2) throw new NumberFormatException();
                } catch (RuntimeException ex) {
                    msg = "Escribe un importe positivo con hasta dos decimales.";
                }
                ValidacionFormulario.marcarCampo(txtImporte, lblError, msg);
            });
            ValidacionFormulario.vincularTexto(txtConcepto, () -> {
                String c = txtConcepto.getText() == null ? "" : txtConcepto.getText().strip();
                ValidacionFormulario.marcarCampo(txtConcepto, lblError,
                        c.isEmpty() ? "El concepto del movimiento es obligatorio." : null);
            });

            btnAgregarMov.setOnAction(ev -> {
                lblError.setVisible(false); lblError.setManaged(false);
                txtImporte.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                txtConcepto.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                String tipo = cmbTipo.getValue();
                String concepto = txtConcepto.getText() == null ? "" : txtConcepto.getText().strip();
                if (concepto.isEmpty()) {
                    ValidacionFormulario.marcarCampo(txtConcepto, lblError, "El concepto del movimiento es obligatorio.");
                    txtConcepto.requestFocus();
                    return;
                }
                BigDecimal importe;
                try {
                    importe = new BigDecimal(txtImporte.getText().strip());
                    if (importe.signum() <= 0 || importe.stripTrailingZeros().scale() > 2) {
                        throw new NumberFormatException();
                    }
                } catch (RuntimeException ex) {
                    ValidacionFormulario.marcarCampo(txtImporte, lblError, "Escribe un importe positivo con hasta dos decimales.");
                    txtImporte.requestFocus();
                    return;
                }
                String cargoId = null;
                String metodo = null;
                if (ConstantesLatch.GARANTIA_TIPO_APLICACION.equals(tipo)) {
                    var cargo = cmbCargo.getValue();
                    if (cargo == null) {
                        mostrarErrorInline(lblError, "Selecciona un cargo abierto para aplicar la garantía.");
                        return;
                    }
                    cargoId = cargo.cargoId();
                } else if (ConstantesLatch.GARANTIA_TIPO_DEVOLUCION.equals(tipo)) {
                    metodo = cmbMetodo.getValue();
                }
                borradores.add(new GarantiaLiquidacionRequest.MovimientoItemRequest(
                        tipo, cargoId, importe.toPlainString(),
                        LocalDate.now(ConstantesLatch.ZONA_OPERATIVA).toString(), metodo, concepto));
                txtConcepto.clear();
            });

            HBox fila1 = new HBox(8, cmbTipo, cmbCargo, cmbMetodo);
            HBox.setHgrow(cmbCargo, Priority.ALWAYS);
            HBox fila2 = new HBox(8, txtImporte, txtConcepto, btnAgregarMov);
            HBox.setHgrow(txtConcepto, Priority.ALWAYS);
            contenido.getChildren().addAll(new Separator(),
                    new Label("Registrar liquidación de garantía (Propietario):"),
                    fila1, fila2, tblPaquete, lblError);
        }

        Window propietario = ventanaActual();
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Garantía del contrato");
        if (propietario != null) dialogo.initOwner(propietario);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().setPrefSize(780, puedeLiquidar ? 640 : 410);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        if (puedeLiquidar) {
            ButtonType confirmar = new ButtonType("Confirmar liquidación", ButtonBar.ButtonData.OK_DONE);
            dialogo.getDialogPane().getButtonTypes().setAll(confirmar, cerrar);
            Button accion = (Button) dialogo.getDialogPane().lookupButton(confirmar);
            accion.getStyleClass().add("button-primary");
            UUID liquidacionId = UUID.randomUUID();
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (borradores.isEmpty()) {
                    mostrarErrorInline(lblError, "Agrega al menos un movimiento antes de confirmar.");
                    return;
                }
                accion.setDisable(true); accion.setText("Liquidando…");
                var solicitud = new GarantiaLiquidacionRequest(liquidacionId.toString(), List.copyOf(borradores));
                UUID contratoId = UUID.fromString(contrato.id());
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        clienteCatalogos.liquidarGarantia(contratoId, liquidacionId, solicitud);
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito("Garantía liquidada",
                            "Los movimientos de garantía quedaron registrados.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText("Confirmar liquidación");
                    mostrarErrorInline(lblError, mensajeError(error));
                });
            });
        } else {
            dialogo.getDialogPane().getButtonTypes().setAll(cerrar);
        }
        TemaAplicacion.aplicar(dialogo.getDialogPane());
        dialogo.showAndWait();
    }

    @FXML public void handleEliminar() {
        ContratoResponse contrato = seleccionado;
        if (contrato == null || !permisoEliminar) return;
        String registro = "Contrato: " + contrato.inquilinoNombre() + " · " + contrato.unidadInfo()
                + " · " + fecha(contrato.inicio()) + " a " + fecha(contrato.fin());
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar contrato", registro,
                "El registro se eliminará físicamente si no tiene cargos ni pagos asociados.", "Eliminar contrato")) return;
        UUID contratoId = UUID.fromString(contrato.id());
        btnEliminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                clienteCatalogos.eliminarContrato(contratoId);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Contrato eliminado", "El registro del contrato se eliminó.");
            cargarDatos();
        }, error -> {
            actualizarAcciones();
            if (error instanceof ErrorClienteException api && api.getEstado() == 403) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar contratos.");
            } else {
                AlertaUtil.mostrarError("No se pudo eliminar el contrato",
                        "Revisa si hay cargos o pagos asociados e inténtalo de nuevo.");
            }
        });
    }

    @FXML public void limpiarFiltros() {
        actualizandoFiltros = true;
        try {
            txtBuscar.clear();
            cmbFiltroUnidad.setValue(null);
            cmbFiltroInquilino.setValue(null);
            cmbFiltroEstado.setValue(ESTADO_TODOS);
        } finally {
            actualizandoFiltros = false;
        }
        aplicarFiltro();
    }

    private void mostrarCatalogos(Catalogos catalogos) {
        unidadesDisponibles = catalogos.unidades();
        inquilinosDisponibles = catalogos.inquilinos();
        actualizandoFiltros = true;
        try {
            RecargadorVista.conservarComboPorId(cmbFiltroUnidad, unidadesDisponibles, true, UnidadResponse::id);
            RecargadorVista.conservarComboPorId(cmbFiltroInquilino, inquilinosDisponibles, true, InquilinoResponse::id);
        } finally {
            actualizandoFiltros = false;
        }
    }

    private String saldoGarantia(ContratoResponse contrato) {
        GarantiaListadoItemResponse garantia = garantias.get(contrato.id());
        String valor = garantia != null ? garantia.saldoActual() : contrato.garantiaSaldo();
        return formatearMoneda(contrato.moneda(), valor);
    }

    private static String formatearMoneda(String moneda, String valor) {
        if (valor == null || valor.isBlank()) return FormatoMoneda.formatear(moneda, BigDecimal.ZERO);
        return FormatoMoneda.formatear(moneda, new BigDecimal(valor));
    }

    private void actualizarAcciones() {
        boolean haySeleccion = seleccionado != null;
        boolean esBorrador = haySeleccion && "Borrador".equals(seleccionado.estado());
        btnActivar.setDisable(!esBorrador);
        btnCancelarBorrador.setDisable(!esBorrador);
        btnRenovar.setDisable(!haySeleccion || !sePuedeRenovar(seleccionado));
        btnTerminar.setDisable(!haySeleccion || !sePuedeFinalizar(seleccionado));
        btnEliminar.setDisable(!haySeleccion || !permisoEliminar);
        btnAjustarGarantia.setDisable(!haySeleccion);
    }

    private static boolean sePuedeRenovar(ContratoResponse contrato) {
        return "Vigente".equalsIgnoreCase(contrato.estado())
                || "Por vencer".equalsIgnoreCase(contrato.estado());
    }

    private static boolean sePuedeFinalizar(ContratoResponse contrato) {
        return "Vigente".equalsIgnoreCase(contrato.estado())
                || "Por vencer".equalsIgnoreCase(contrato.estado())
                || "Vencido".equalsIgnoreCase(contrato.estado());
    }

    private Window ventanaActual() {
        return tblContratos == null || tblContratos.getScene() == null ? null : tblContratos.getScene().getWindow();
    }

    private static Label crearLabelError() {
        Label lbl = new Label("");
        lbl.getStyleClass().add("field-error");
        lbl.setMinHeight(18);
        lbl.setVisible(false);
        lbl.setManaged(false);
        lbl.setWrapText(true);
        return lbl;
    }

    private static void mostrarErrorInline(Label lbl, String mensaje) {
        lbl.setText(mensaje);
        lbl.setVisible(true);
        lbl.setManaged(true);
        ValidacionFormulario.mostrar(lbl, lbl);
    }

    private void ocultarError() { barraError.ocultar(); }
    private static boolean contiene(String texto, String filtro) { return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro); }
    private static String fecha(String iso) {
        if (iso == null || iso.isBlank()) return "—";
        try {
            return FECHA_VISIBLE.format(LocalDate.parse(iso));
        } catch (RuntimeException ignored) {
            return iso;
        }
    }
    private static String mensajeError(Throwable error) {
        return MensajesUi.traducir(error, "Revisa los datos e inténtalo de nuevo.");
    }
}
