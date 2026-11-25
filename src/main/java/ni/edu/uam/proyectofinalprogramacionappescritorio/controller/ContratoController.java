package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import lombok.RequiredArgsConstructor;

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
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRenovarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoTerminarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaLiquidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaResumenResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.GarantiaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogosAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Lista, filtros, ciclo de vida contractual y liquidación de garantías. */
@RequiredArgsConstructor
public class ContratoController {
    private static final String ESTADO_TODOS = "Todos los estados";

    @FXML private VBox rootView;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnReporteGarantias;
    @FXML private HBox eliminarTooltipHost;
    @FXML private HBox garantiaTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnActivar;
    @FXML private Button btnCancelarBorrador;
    @FXML private Button btnRenovar;
    @FXML private Button btnTerminar;
    @FXML private Button btnEliminar;
    @FXML private Button btnAjustarGarantia;
    @FXML private Button btnEstadoCuenta;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Unidad> cmbFiltroUnidad;
    @FXML private ComboBox<Inquilino> cmbFiltroInquilino;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private TableView<Contrato> tblContratos;
    @FXML private TableColumn<Contrato, String> colInquilino;
    @FXML private TableColumn<Contrato, String> colUnidad;
    @FXML private TableColumn<Contrato, LocalDate> colInicio;
    @FXML private TableColumn<Contrato, LocalDate> colFin;
    @FXML private TableColumn<Contrato, String> colCanon;
    @FXML private TableColumn<Contrato, Integer> colDiaPago;
    @FXML private TableColumn<Contrato, String> colDeposito;
    @FXML private TableColumn<Contrato, String> colSaldoGarantia;
    @FXML private TableColumn<Contrato, String> colEstado;
    @FXML private TableColumn<Contrato, Void> colAcciones;

    private final ReporteService reporteService = new ReporteService();
    private final ClienteCatalogos clienteCatalogos = new ClienteCatalogos(ClienteLatch.api());
    private final ClientePagos clientePagos = new ClientePagos();
    private final ContratoService contratoService;
    private final GarantiaService garantiaService;
    private final ObservableList<Contrato> registros = FXCollections.observableArrayList();
    private final FilteredList<Contrato> filtrados = new FilteredList<>(registros, item -> true);
    private List<Unidad> unidadesDisponibles = List.of();
    private List<Inquilino> inquilinosDisponibles = List.of();
    private Contrato seleccionado;
    private final boolean permisoEliminar = SesionUsuario.esPropietario();
    private final boolean permisoLiquidarGarantia = SesionUsuario.esPropietario();
    private long versionConsulta;
    private boolean actualizandoFiltros;

    private record Catalogos(List<Unidad> unidades, List<Inquilino> inquilinos) { }
    private record DatosTabla(Catalogos catalogos, List<Contrato> contratos, Map<UUID, Garantia> garantias) { }
    private record DatosGarantiaDialogo(GarantiaResumenResponse resumen,
                                        List<EstadoCuentaResponse.CargoEstadoCuenta> cargosAbiertos) { }

    private Map<UUID, Garantia> garantias = Map.of();

    public ContratoController() {
        this(HelloApplication.instancia().getComposicion().contratos(),
                HelloApplication.instancia().getComposicion().garantias());
    }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.CONTRATO, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnGenerarReporte != null) {
            Iconos.asignar(btnGenerarReporte, Iconos.Nombre.REPORTE, 16);
            btnGenerarReporte.setTooltip(new Tooltip("Vista previa y exportación PDF del resumen de contratos y ocupación"));
        }
        if (btnReporteGarantias != null) {
            Iconos.asignar(btnReporteGarantias, Iconos.Nombre.REPORTE, 16);
            btnReporteGarantias.setTooltip(new Tooltip("Vista previa y exportación PDF de los depósitos de garantía"));
        }
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        btnNuevo.disableProperty().bind(btnRefrescar.disableProperty());
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnActivar, Iconos.Nombre.CONFIRMACION, 16);
        Iconos.asignar(btnCancelarBorrador, Iconos.Nombre.CERRAR, 16);
        Iconos.asignar(btnRenovar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnTerminar, Iconos.Nombre.CONFIRMACION, 16);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Iconos.asignar(btnAjustarGarantia, Iconos.Nombre.EDITAR, 16);
        if (btnEstadoCuenta != null) {
            Iconos.asignar(btnEstadoCuenta, Iconos.Nombre.REPORTE, 16);
            btnEstadoCuenta.setTooltip(new Tooltip("Consultar y exportar el estado de cuenta del contrato seleccionado"));
            btnEstadoCuenta.setDisable(true);
        }
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona un contrato para eliminarlo." : "Solo el propietario puede eliminar contratos."));
        Tooltip.install(garantiaTooltipHost, new Tooltip(permisoLiquidarGarantia
                ? "Consulta movimientos y liquida la garantía del contrato."
                : "Consulta el estado e historial de la garantía (liquidación reservada al propietario)."));

        configurarFiltros();

        tblContratos.setItems(filtrados);
        tblContratos.setPlaceholder(PanelEstadoTabla.crear("Cargando contratos", "El listado aparecerá al terminar la consulta.", null, null));
        colInquilino.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getInquilinoNombre()));
        colUnidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUnidadInfo()));
        colInicio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getInicio()));
        colInicio.setCellFactory(FormatoTabla.fecha());
        colFin.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getFin()));
        colFin.setCellFactory(FormatoTabla.fecha());
        colCanon.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCanonFormateado()));
        colCanon.setCellFactory(FormatoTabla.numerica());
        colDiaPago.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getDiaPago()));
        colDiaPago.setCellFactory(FormatoTabla.entero());
        colDeposito.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDepositoFormateado()));
        colDeposito.setCellFactory(FormatoTabla.numerica());
        colSaldoGarantia.setCellValueFactory(c -> new ReadOnlyStringWrapper(saldoGarantia(c.getValue())));
        colSaldoGarantia.setCellFactory(FormatoTabla.numerica());
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEstado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Editar contrato", this::abrirEdicion, Iconos.Nombre.EDITAR),
                new AccionesTabla.Accion<>("Documento PDF", this::abrirDocumentoPdf, Iconos.Nombre.REPORTE),
                new AccionesTabla.Accion<>("Garantía", this::abrirGarantia, Iconos.Nombre.EDITAR),
                new AccionesTabla.Accion<>("Estado de cuenta", this::abrirEstadoCuenta, Iconos.Nombre.REPORTE)));
        tblContratos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionado = actual;
            actualizarAcciones();
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        actualizarAcciones();
        aplicarFiltro();
        cargarDatos();
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
            @Override public String toString(Unidad unidad) {
                return unidad == null ? "Todas las unidades" : unidad.toString();
            }
            @Override public Unidad fromString(String string) { return null; }
        });
        cmbFiltroInquilino.setConverter(new StringConverter<>() {
            @Override public String toString(Inquilino inquilino) {
                return inquilino == null ? "Todos los inquilinos" : inquilino.toString();
            }
            @Override public Inquilino fromString(String string) { return null; }
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
        Unidad unidadFiltro = cmbFiltroUnidad.getValue();
        Inquilino inquilinoFiltro = cmbFiltroInquilino.getValue();
        String estadoFiltro = cmbFiltroEstado.getValue();
        boolean filtrarEstado = estadoFiltro != null && !ESTADO_TODOS.equals(estadoFiltro);

        filtrados.setPredicate(contrato -> {
            boolean coincideTexto = filtro.isEmpty()
                    || contiene(contrato.getInquilinoNombre(), filtro)
                    || contiene(contrato.getUnidadInfo(), filtro)
                    || contiene(contrato.getEstado(), filtro);
            boolean coincideUnidad = unidadFiltro == null
                    || Objects.equals(contrato.getUnidadId(), unidadFiltro.getId());
            boolean coincideInquilino = inquilinoFiltro == null
                    || Objects.equals(contrato.getInquilinoId(), inquilinoFiltro.getId());
            boolean coincideEstado = !filtrarEstado
                    || Objects.equals(contrato.getEstado(), estadoFiltro);
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
        btnRefrescar.setDisable(true);
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                Catalogos catalogos = new Catalogos(clienteCatalogos.unidades(), clienteCatalogos.inquilinos(true));
                List<Contrato> contratos = clienteCatalogos.contratos(false);
                Map<UUID, Garantia> seguimiento = garantiaService.listarTodas().stream()
                        .collect(Collectors.toMap(g -> g.getContrato().getId(), g -> g));
                return new DatosTabla(catalogos, contratos, seguimiento);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            mostrarCatalogos(datos.catalogos());
            garantias = datos.garantias();
            registros.setAll(datos.contratos());
            aplicarFiltro();
            actualizarAcciones();
            ocultarError();
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) {
                lblErrorConsulta.setText("No se pudieron cargar los contratos y sus opciones. Vuelve a intentarlo.");
                lblErrorConsulta.setVisible(true); lblErrorConsulta.setManaged(true);
                tblContratos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los contratos",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
            btnRefrescar.setDisable(false);
        });
    }

    @FXML public void refrescarTabla() { cargarDatos(); }

    @FXML public void handleNuevo() {
        if (!btnNuevo.isDisabled()) abrirFormulario(null);
    }

    private void abrirEdicion(Contrato contrato) { abrirFormulario(contrato); }

    private void abrirDocumentoPdf(Contrato contrato) {
        if (contrato == null || contrato.getId() == null) return;
        var adjuntos = HelloApplication.instancia().getComposicion().adjuntos();
        SesionUsuario identidad = SesionUsuario.getActual();
        DialogosAdjuntos.mostrarGestorAdjuntos(
                ventanaActual(),
                "Documento contractual — " + contrato.getInquilinoNombre(),
                "Adjunta únicamente archivos PDF de hasta 5 MiB vinculados a esta versión contractual.",
                true,
                adjuntos,
                () -> adjuntos.listarPorContrato(identidad, contrato.getId()),
                archivo -> adjuntos.adjuntarDocumentoContrato(identidad, contrato.getId(), archivo));
    }

    private void abrirFormulario(Contrato existente) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("contrato-form.fxml"));
            Node contenido = loader.load();
            ContratoFormController formulario = loader.getController();
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
                UUID contratoId = nuevo ? null : existente.getId();
                long version = nuevo ? 0 : existente.getVersion();
                UUID unidadId = datos.unidad().getId();
                UUID inquilinoId = datos.inquilino().getId();
                accion.setDisable(true); accion.setText("Guardando contrato…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        clienteCatalogos.guardarContrato(contratoId,
                                new ContratoRequest(unidadId.toString(), inquilinoId.toString(),
                                        datos.inicio().toString(), datos.fin().toString(),
                                        datos.canon().toPlainString(), datos.moneda(), datos.diaPago(),
                                        datos.deposito().toPlainString(), datos.estado(),
                                        nuevo ? null : version));
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
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
        Contrato contrato = seleccionado;
        if (contrato == null || !"Borrador".equals(contrato.getEstado())) return;
        String detalle = "Contrato de " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()
                + "\nVigencia: " + fecha(contrato.getInicio()) + " a " + fecha(contrato.getFin())
                + "\nEl estado pasará a Vigente y se reservará la unidad.";
        if (!AlertaUtil.mostrarConfirmacion("Activar borrador", detalle, "Activar")) return;

        UUID contratoId = contrato.getId();
        long version = contrato.getVersion();
        btnActivar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                clienteCatalogos.activarContrato(contratoId, version);
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Contrato activado", "El contrato quedó Vigente y con su garantía registrada.");
            cargarDatos();
        }, error -> {
            actualizarAcciones();
            AlertaUtil.mostrarError("No se pudo activar el contrato", mensajeError(error));
        });
    }

    @FXML public void handleCancelarBorrador() {
        Contrato contrato = seleccionado;
        if (contrato == null || !"Borrador".equals(contrato.getEstado())) return;

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo de cancelación del borrador");
        txtMotivo.getStyleClass().add("field");
        Label lblError = crearLabelError();
        VBox contenido = new VBox(8,
                new Label("Contrato en borrador: " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()),
                new Label("Motivo obligatorio (sin movimientos financieros):"),
                txtMotivo, lblError);
        contenido.setPadding(new Insets(16));

        Window propietario = ventanaActual();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear("Cancelar borrador", contenido, propietario,
                480, 290, "Cancelar borrador");
        DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnCancelarBorrador,
                () -> !txtMotivo.getText().isBlank());
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().strip();
            if (motivo.isEmpty() || motivo.length() > 500) {
                mostrarErrorInline(lblError, "Escribe un motivo válido (máximo 500 caracteres).");
                return;
            }
            UUID contratoId = contrato.getId();
            long version = contrato.getVersion();
            accion.setDisable(true); accion.setText("Cancelando…");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    clienteCatalogos.cancelarBorrador(contratoId, version, motivo);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
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
        Contrato contrato = seleccionado;
        if (contrato == null || !sePuedeRenovar(contrato)) return;

        LocalDate inicioSugerido = contrato.getFin().plusDays(1);
        LocalDate finSugerido = inicioSugerido.plusYears(1).minusDays(1);
        DatePicker dpInicio = new DatePicker(inicioSugerido);
        dpInicio.getStyleClass().add("field");
        DatePicker dpFin = new DatePicker(finSugerido);
        dpFin.getStyleClass().add("field");
        TextField txtCanon = new TextField(contrato.getCanon().toPlainString());
        txtCanon.getStyleClass().add("field");
        ComboBox<String> cmbMoneda = new ComboBox<>(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.setValue(contrato.getMoneda());
        cmbMoneda.getStyleClass().add("field");
        TextField txtDiaPago = new TextField(String.valueOf(contrato.getDiaPago()));
        txtDiaPago.getStyleClass().add("field");
        TextField txtDeposito = new TextField(contrato.getDepositoPactado().toPlainString());
        txtDeposito.getStyleClass().add("field");
        Label lblError = crearLabelError();

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        grid.addRow(0, new Label("Inicio *"), dpInicio, new Label("Fin *"), dpFin);
        grid.addRow(1, new Label("Canon *"), txtCanon, new Label("Moneda *"), cmbMoneda);
        grid.addRow(2, new Label("Día de pago *"), txtDiaPago, new Label("Depósito"), txtDeposito);
        VBox contenido = new VBox(10,
                new Label("Renovación enlazada de " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()),
                new Label("Se creará un borrador sucesor sin alterar el contrato actual ni rentas pasadas."),
                grid, lblError);
        contenido.setPadding(new Insets(16));

        Window propietario = ventanaActual();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear("Renovar contrato", contenido, propietario,
                620, 400, "Crear borrador sucesor");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();
            try {
                ValidadorEntrada.validarRangoFechas(inicio, fin, 1);
                BigDecimal canon = new BigDecimal(txtCanon.getText().strip());
                if (canon.signum() <= 0 || canon.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("El canon debe ser positivo con hasta dos decimales.");
                }
                int diaPago = Integer.parseInt(txtDiaPago.getText().strip());
                if (diaPago < 1 || diaPago > 31) {
                    throw new IllegalArgumentException("El día de pago debe estar entre 1 y 31.");
                }
                BigDecimal deposito = txtDeposito.getText().isBlank()
                        ? BigDecimal.ZERO : new BigDecimal(txtDeposito.getText().strip());
                if (deposito.signum() < 0 || deposito.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("El depósito debe ser cero o mayor con hasta dos decimales.");
                }
                var solicitud = new ContratoRenovarRequest(
                        inicio.toString(), fin.toString(), canon.toPlainString(),
                        cmbMoneda.getValue(), diaPago, deposito.toPlainString());
                UUID anteriorId = contrato.getId();
                accion.setDisable(true); accion.setText("Creando borrador…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        clienteCatalogos.renovarContrato(anteriorId, solicitud);
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito("Renovación preparada",
                            "Se creó el borrador sucesor enlazado a este contrato.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText("Crear borrador sucesor");
                    mostrarErrorInline(lblError, mensajeError(error));
                });
            } catch (NumberFormatException ex) {
                mostrarErrorInline(lblError, "Verifica que canon, día de pago y depósito sean numéricos válidos.");
            } catch (RuntimeException ex) {
                mostrarErrorInline(lblError, ex.getMessage());
            }
        });
        dialogo.showAndWait();
    }

    @FXML public void handleTerminarContrato() {
        Contrato contrato = seleccionado;
        if (contrato == null || !sePuedeFinalizar(contrato)) return;

        List<Contrato> sucesores = registros.stream()
                .filter(c -> Objects.equals(contrato.getId(), c.getPredecesorId())
                        && "Borrador".equals(c.getEstado()))
                .toList();

        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.getStyleClass().add("field");
        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo de terminación (obligatorio)");
        txtMotivo.getStyleClass().add("field");
        ComboBox<Contrato> cmbSucesor = new ComboBox<>();
        cmbSucesor.getStyleClass().add("field");
        cmbSucesor.setMaxWidth(Double.MAX_VALUE);
        cmbSucesor.setConverter(new StringConverter<>() {
            @Override public String toString(Contrato c) {
                return c == null ? "Sin activar borrador sucesor"
                        : "Activar borrador: " + fecha(c.getInicio()) + " a " + fecha(c.getFin())
                        + " (" + c.getCanonFormateado() + ")";
            }
            @Override public Contrato fromString(String s) { return null; }
        });
        List<Contrato> opcionesSucesor = new ArrayList<>();
        opcionesSucesor.add(null);
        opcionesSucesor.addAll(sucesores);
        cmbSucesor.setItems(FXCollections.observableArrayList(opcionesSucesor));
        cmbSucesor.getSelectionModel().selectFirst();

        Garantia garantia = garantias.get(contrato.getId());
        BigDecimal saldoGarantia = garantia == null ? BigDecimal.ZERO : garantia.getSaldoActual();
        Label lblGarantia = new Label(saldoGarantia.signum() > 0
                ? "Saldo retenido de garantía: " + FormatoMoneda.formatear(contrato.getMoneda(), saldoGarantia)
                + ". Liquídalo desde el botón Garantía antes de terminar."
                : "Garantía sin saldo retenido pendiente.");
        lblGarantia.setWrapText(true);
        Label lblError = crearLabelError();

        VBox contenido = new VBox(8,
                new Label("Contrato de " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()),
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
                540, sucesores.isEmpty() ? 390 : 450, "Finalizar contrato");
        DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnTerminar,
                () -> !txtMotivo.getText().isBlank());
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            LocalDate fechaTerminacion = dpFecha.getValue();
            String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().strip();
            if (fechaTerminacion == null) {
                mostrarErrorInline(lblError, "Selecciona la fecha de terminación.");
                return;
            }
            if (motivo.isEmpty() || motivo.length() > 500) {
                mostrarErrorInline(lblError, "Escribe un motivo válido (máximo 500 caracteres).");
                return;
            }
            Contrato sucesorElegido = cmbSucesor.getValue();
            var solicitud = new ContratoTerminarRequest(
                    contrato.getVersion(),
                    fechaTerminacion.toString(),
                    motivo,
                    sucesorElegido == null ? null : sucesorElegido.getId().toString(),
                    null);
            UUID contratoId = contrato.getId();
            accion.setDisable(true); accion.setText("Finalizando…");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    clienteCatalogos.terminarContratoConCierre(contratoId, solicitud);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
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

    private void abrirGarantia(Contrato contrato) {
        if (contrato == null || contrato.getId() == null) return;
        UUID contratoId = contrato.getId();
        btnAjustarGarantia.setDisable(true);
        Task<DatosGarantiaDialogo> tarea = new Task<>() {
            @Override protected DatosGarantiaDialogo call() {
                GarantiaResumenResponse resumen = clienteCatalogos.obtenerGarantia(contratoId);
                List<EstadoCuentaResponse.CargoEstadoCuenta> cargosAbiertos = List.of();
                if (permisoLiquidarGarantia && new BigDecimal(resumen.saldoActual()).signum() > 0) {
                    EstadoCuentaResponse cuenta = clientePagos.estadoCuenta(contratoId.toString(), null);
                    cargosAbiertos = cuenta.cargos().stream()
                            .filter(c -> !"Anulado".equalsIgnoreCase(c.estado())
                                    && !"Pagado".equalsIgnoreCase(c.estado())
                                    && new BigDecimal(c.saldo()).signum() > 0)
                            .toList();
                }
                return new DatosGarantiaDialogo(resumen, cargosAbiertos);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            actualizarAcciones();
            mostrarDialogoGarantia(contrato, datos);
        }, error -> {
            actualizarAcciones();
            AlertaUtil.mostrarError("No se pudo consultar la garantía", mensajeError(error));
        });
    }

    private void mostrarDialogoGarantia(Contrato contrato, DatosGarantiaDialogo datos) {
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
                new Label("Contrato: " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()),
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
                    MovimientoGarantia.TIPO_APLICACION,
                    MovimientoGarantia.TIPO_DEDUCCION,
                    MovimientoGarantia.TIPO_DEVOLUCION));
            cmbTipo.setValue(MovimientoGarantia.TIPO_DEVOLUCION);
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
                boolean esAplic = MovimientoGarantia.TIPO_APLICACION.equals(t);
                boolean esDev = MovimientoGarantia.TIPO_DEVOLUCION.equals(t);
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

            btnAgregarMov.setOnAction(ev -> {
                lblError.setVisible(false); lblError.setManaged(false);
                String tipo = cmbTipo.getValue();
                String concepto = txtConcepto.getText() == null ? "" : txtConcepto.getText().strip();
                if (concepto.isEmpty()) {
                    mostrarErrorInline(lblError, "El concepto del movimiento es obligatorio.");
                    return;
                }
                BigDecimal importe;
                try {
                    importe = new BigDecimal(txtImporte.getText().strip());
                    if (importe.signum() <= 0 || importe.stripTrailingZeros().scale() > 2) {
                        throw new NumberFormatException();
                    }
                } catch (RuntimeException ex) {
                    mostrarErrorInline(lblError, "Escribe un importe positivo con hasta dos decimales.");
                    return;
                }
                String cargoId = null;
                String metodo = null;
                if (MovimientoGarantia.TIPO_APLICACION.equals(tipo)) {
                    var cargo = cmbCargo.getValue();
                    if (cargo == null) {
                        mostrarErrorInline(lblError, "Selecciona un cargo abierto para aplicar la garantía.");
                        return;
                    }
                    cargoId = cargo.cargoId();
                } else if (MovimientoGarantia.TIPO_DEVOLUCION.equals(tipo)) {
                    metodo = cmbMetodo.getValue();
                }
                borradores.add(new GarantiaLiquidacionRequest.MovimientoItemRequest(
                        tipo, cargoId, importe.toPlainString(), LocalDate.now().toString(), metodo, concepto));
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
                UUID contratoId = contrato.getId();
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        clienteCatalogos.liquidarGarantia(contratoId, liquidacionId, solicitud);
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
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
        Contrato contrato = seleccionado;
        if (contrato == null || !permisoEliminar) return;
        String registro = "Contrato: " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()
                + " · " + fecha(contrato.getInicio()) + " a " + fecha(contrato.getFin());
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar contrato", registro,
                "El registro se eliminará físicamente si no tiene cargos ni pagos asociados.", "Eliminar contrato")) return;
        UUID contratoId = contrato.getId();
        btnEliminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { contratoService.eliminar(contratoId); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Contrato eliminado", "El registro del contrato se eliminó.");
            cargarDatos();
        }, error -> {
            actualizarAcciones();
            if (error instanceof SecurityException) {
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
            Unidad unidadSel = cmbFiltroUnidad.getValue();
            List<Unidad> filtroUnidades = new ArrayList<>();
            filtroUnidades.add(null);
            filtroUnidades.addAll(unidadesDisponibles);
            cmbFiltroUnidad.setItems(FXCollections.observableArrayList(filtroUnidades));
            cmbFiltroUnidad.setValue(unidadSel != null && unidadesDisponibles.stream()
                    .anyMatch(u -> u.getId().equals(unidadSel.getId())) ? unidadSel : null);

            Inquilino inquilinoSel = cmbFiltroInquilino.getValue();
            List<Inquilino> filtroInquilinos = new ArrayList<>();
            filtroInquilinos.add(null);
            filtroInquilinos.addAll(inquilinosDisponibles);
            cmbFiltroInquilino.setItems(FXCollections.observableArrayList(filtroInquilinos));
            cmbFiltroInquilino.setValue(inquilinoSel != null && inquilinosDisponibles.stream()
                    .anyMatch(i -> i.getId().equals(inquilinoSel.getId())) ? inquilinoSel : null);
        } finally {
            actualizandoFiltros = false;
        }
    }

    private String saldoGarantia(Contrato contrato) {
        Garantia garantia = garantias.get(contrato.getId());
        return garantia == null ? contrato.getGarantiaSaldoFormateado()
                : FormatoMoneda.formatear(contrato.getMoneda(), garantia.getSaldoActual());
    }

    private void actualizarAcciones() {
        boolean haySeleccion = seleccionado != null;
        boolean esBorrador = haySeleccion && "Borrador".equals(seleccionado.getEstado());
        btnActivar.setDisable(!esBorrador);
        btnCancelarBorrador.setDisable(!esBorrador);
        btnRenovar.setDisable(!haySeleccion || !sePuedeRenovar(seleccionado));
        btnTerminar.setDisable(!haySeleccion || !sePuedeFinalizar(seleccionado));
        btnEliminar.setDisable(!haySeleccion || !permisoEliminar);
        btnAjustarGarantia.setDisable(!haySeleccion);
        if (btnEstadoCuenta != null) {
            btnEstadoCuenta.setDisable(!haySeleccion);
        }
    }

    private static boolean sePuedeRenovar(Contrato contrato) {
        return "Vigente".equalsIgnoreCase(contrato.getEstado())
                || "Por vencer".equalsIgnoreCase(contrato.getEstado());
    }

    private static boolean sePuedeFinalizar(Contrato contrato) {
        return "Vigente".equalsIgnoreCase(contrato.getEstado())
                || "Por vencer".equalsIgnoreCase(contrato.getEstado())
                || "Vencido".equalsIgnoreCase(contrato.getEstado());
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

    private void ocultarError() { lblErrorConsulta.setText(""); lblErrorConsulta.setVisible(false); lblErrorConsulta.setManaged(false); }
    private static boolean contiene(String texto, String filtro) { return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro); }
    private static String fecha(LocalDate fecha) { return fecha == null ? "—" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    private static String mensajeError(Throwable error) {
        if (error instanceof ErrorClienteException) return error.getMessage();
        if (error instanceof IllegalArgumentException || error instanceof SecurityException || error instanceof IllegalStateException) {
            return error.getMessage();
        }
        return "Revisa los datos e inténtalo de nuevo.";
    }

    @FXML
    public void handleGenerarReporte() {
        List<Contrato> contratosActuales = tblContratos == null
                ? List.of()
                : List.copyOf(tblContratos.getItems());

        if (contratosActuales.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos",
                    "No hay contratos en la tabla para generar el reporte.");
            return;
        }

        VistaPreviaReporte.generarYMostrar(btnGenerarReporte, ventanaActual(),
                () -> reporteService.generarDocumentoContratosOcupacion(contratosActuales));
    }

    @FXML
    public void handleReporteGarantias() {
        VistaPreviaReporte.generarYMostrar(btnReporteGarantias, ventanaActual(),
                reporteService::generarDocumentoGarantias);
    }

    @FXML
    public void handleEstadoCuenta() {
        abrirEstadoCuenta(seleccionado);
    }

    private void abrirEstadoCuenta(Contrato contrato) {
        if (contrato == null || contrato.getId() == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida",
                    "Selecciona un contrato para consultar su estado de cuenta.");
            return;
        }

        Window ventana = ventanaActual();
        String descripcion = contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo();

        Dialog<LocalDate> dialogo = new Dialog<>();
        dialogo.setTitle("Estado de cuenta");
        dialogo.setHeaderText("Contrato: " + descripcion);
        if (ventana != null) {
            dialogo.initOwner(ventana);
        }
        DatePicker selectorCorte = new DatePicker(LocalDate.now());
        selectorCorte.getStyleClass().add("field");
        VBox contenido = new VBox(8, new Label("Fecha de corte"), selectorCorte);
        contenido.setPadding(new Insets(16));
        dialogo.getDialogPane().setContent(contenido);

        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType generar = new ButtonType("Generar estado de cuenta", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(cancelar, generar);
        dialogo.setResultConverter(tipo -> tipo == generar
                ? (selectorCorte.getValue() != null ? selectorCorte.getValue() : LocalDate.now())
                : null);
        TemaAplicacion.aplicar(dialogo.getDialogPane());

        LocalDate fechaCorte = dialogo.showAndWait().orElse(null);
        if (fechaCorte == null) {
            return;
        }

        String contratoId = contrato.getId().toString();
        VistaPreviaReporte.generarYMostrar(btnEstadoCuenta, ventana, () -> {
            var respuesta = clientePagos.estadoCuenta(contratoId, fechaCorte);
            return reporteService.generarDocumentoEstadoCuenta(respuesta, descripcion);
        });
    }
}
