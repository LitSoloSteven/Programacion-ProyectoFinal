package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import lombok.RequiredArgsConstructor;

import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CarteraResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ResultadoRevisionH2Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ItemPrevisualizacionLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PrevisualizacionLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CargoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CobranzaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lista paginada de cargos y responsable de las llamadas del diálogo financiero. */
@RequiredArgsConstructor
public class CargoController {
    @FXML private VBox rootView;
    @FXML private HBox anularTooltipHost;
    @FXML private Button btnAnular;
    @FXML private Button btnCartera;
    @FXML private Button btnGenerarRentas;
    @FXML private Button btnNuevo;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnLimpiar;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
    @FXML private Button btnRefrescar;
    @FXML private Button btnRevisarMora;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private Label lblPaginacion;
    @FXML private Label lblTitulo;
    @FXML private TextField txtBuscar;
    @FXML private TableView<CargoListaResponse> tblCargos;
    @FXML private TableColumn<CargoListaResponse, String> colContrato;
    @FXML private TableColumn<CargoListaResponse, String> colTipo;
    @FXML private TableColumn<CargoListaResponse, String> colConcepto;
    @FXML private TableColumn<CargoListaResponse, String> colImporte;
    @FXML private TableColumn<CargoListaResponse, LocalDate> colVence;
    @FXML private TableColumn<CargoListaResponse, String> colEstado;
    @FXML private TableColumn<CargoListaResponse, Void> colAcciones;

    private final ReporteService reporteService = new ReporteService();
    private final CargoService cargoService;
    private final ClienteCatalogos catalogos = new ClienteCatalogos(ClienteLatch.api());
    private final ClientePagos cobranza = new ClientePagos();
    private final ObservableList<CargoListaResponse> listaCargos = FXCollections.observableArrayList();
    private List<Contrato> contratosDisponibles = List.of();
    private CargoListaResponse cargoSeleccionado;
    private int paginaActual = 1;
    private long versionConsulta;
    private boolean permisoAnular;

    private record DatosTabla(List<Contrato> contratos, PaginaResponse<CargoListaResponse> cargos) { }

    public CargoController() {
        this(LatchApplication.instancia().getComposicion().cargos());
    }

    @FXML
    public void initialize() {
        permisoAnular = SesionUsuario.esPropietario();
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.CARGO, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnGenerarReporte != null) {
            Iconos.asignar(btnGenerarReporte, Iconos.Nombre.REPORTE, 16);
            btnGenerarReporte.setTooltip(new Tooltip("Vista previa y exportación PDF de cargos, cartera y mora"));
        }
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnAnular, Iconos.Nombre.ELIMINAR, 16);
        btnAnular.setDisable(true);
        Tooltip.install(anularTooltipHost,
                new Tooltip(permisoAnular ? "Selecciona un cargo para anularlo." : "Solo el propietario puede anular cargos."));
        btnNuevo.setDisable(true);
        tblCargos.setItems(listaCargos);
        tblCargos.setPlaceholder(PanelEstadoTabla.crear("Cargando cargos", "La lista se actualizará al terminar la consulta.", null, null));
        colContrato.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(
                cell.getValue().contratoInfo()));
        colTipo.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().tipo()));
        colConcepto.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().concepto()));
        colImporte.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().importeFormateado()));
        colImporte.setCellFactory(FormatoTabla.numerica());
        colVence.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(LocalDate.parse(cell.getValue().venceEl())));
        colVence.setCellFactory(FormatoTabla.fecha());
        colEstado.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().estado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>(
                "Editar cargo", this::abrirEdicion, Iconos.Nombre.EDITAR)));
        tblCargos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            cargoSeleccionado = nuevo;
            btnAnular.setDisable(nuevo == null || !permisoAnular);
            if (nuevo == null) Tooltip.install(anularTooltipHost,
                    new Tooltip(permisoAnular ? "Selecciona un cargo para anularlo." : "Solo el propietario puede anular cargos."));
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            paginaActual = 1;
            cargarPagina();
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        cargarContratosYPagina();
    }

    public void aplicarFiltroInicial(String texto) {
        if (texto == null || texto.isBlank() || txtBuscar == null) {
            return;
        }
        txtBuscar.setText(texto.strip());
        paginaActual = 1;
        cargarContratosYPagina();
    }

    private void cargarContratosYPagina() {
        long version = ++versionConsulta;
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                return new DatosTabla(catalogos.contratos(true),
                        cobranza.listarCargos(pagina, filtro));
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, resultado -> {
            if (version != versionConsulta) return;
            contratosDisponibles = resultado.contratos();
            btnNuevo.setDisable(contratosDisponibles.isEmpty());
            btnRefrescar.setDisable(false);
            if (contratosDisponibles.isEmpty()) {
                lblErrorConsulta.setText("No hay contratos vigentes para registrar cargos.");
                lblErrorConsulta.setVisible(true);
                lblErrorConsulta.setManaged(true);
            }
            mostrarPagina(resultado.cargos());
        }, error -> {
            if (version != versionConsulta) return;
            mostrarErrorConsulta(error);
            btnRefrescar.setDisable(false);
            tblCargos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los cargos",
                    "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarContratosYPagina));
        });
    }

    private void cargarPagina() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<PaginaResponse<CargoListaResponse>> tarea = new Task<>() {
            @Override protected PaginaResponse<CargoListaResponse> call() {
                return cobranza.listarCargos(pagina, filtro);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, resultado -> {
            if (version == versionConsulta) mostrarPagina(resultado);
        }, error -> {
            if (version == versionConsulta) mostrarErrorConsulta(error);
        });
    }

    private void mostrarPagina(PaginaResponse<CargoListaResponse> resultado) {
        int paginas = (int) Math.max(1, (resultado.total() + resultado.tamano() - 1) / resultado.tamano());
        if (paginaActual > paginas) {
            paginaActual = paginas;
            cargarPagina();
            return;
        }
        listaCargos.setAll(resultado.elementos());
        long desde = resultado.total() == 0 ? 0 : (long) (paginaActual - 1) * resultado.tamano() + 1;
        long hasta = resultado.total() == 0 ? 0 : desde + resultado.elementos().size() - 1;
        lblPaginacion.setText(desde + "–" + hasta + " de " + resultado.total() + " cargos");
        lblContador.setText(resultado.total() + (resultado.total() == 1 ? " cargo" : " cargos"));
        btnPaginaAnterior.setDisable(paginaActual <= 1);
        btnPaginaSiguiente.setDisable(paginaActual >= paginas);
        tblCargos.setPlaceholder(PanelEstadoTabla.crear(
                txtBuscar.getText().isBlank() ? "Aún no hay cargos" : "No hay coincidencias",
                txtBuscar.getText().isBlank() ? "Los cargos registrados aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                txtBuscar.getText().isBlank() && !contratosDisponibles.isEmpty() ? "Nuevo cargo" : null,
                txtBuscar.getText().isBlank() && !contratosDisponibles.isEmpty() ? this::handleNuevo : null));
        lblErrorConsulta.setText("");
        lblErrorConsulta.setVisible(false);
        lblErrorConsulta.setManaged(false);
    }

    @FXML public void handleNuevo() { abrirFormulario(null); }

    private void abrirEdicion(CargoListaResponse fila) {
        Task<Cargo> tarea = new Task<>() {
            @Override protected Cargo call() {
                return cargoService.obtenerParaEdicion(UUID.fromString(fila.id()));
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, this::abrirFormulario,
                error -> AlertaUtil.mostrarError("No se pudo abrir el cargo", mensajeError(error)));
    }

    private void abrirFormulario(Cargo cargo) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("cargo-form.fxml"));
            Node contenido = loader.load();
            CargoFormController formulario = loader.getController();
            formulario.establecerContratos(contratosDisponibles);
            if (cargo != null) formulario.cargar(cargo);
            Window propietario = tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
            String textoAccion = cargo == null ? "Guardar cargo" : "Guardar cambios";
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(cargo == null ? "Nuevo cargo" : "Editar cargo",
                    contenido, propietario, 720, 680, textoAccion);
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, cargo == null ? btnNuevo : tblCargos,
                    formulario::hayCambios);
            ButtonType accion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(accion);
            botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                guardarEnDialogo(dialogo, botonGuardar, formulario, cargo);
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    private void guardarEnDialogo(Dialog<ButtonType> dialogo, Button botonGuardar,
                                  CargoFormController formulario, Cargo cargo) {
        if (!formulario.validar()) return;
        CargoFormController.Datos datos = formulario.datos();
        boolean nuevo = cargo == null;
        CargoService.Solicitud solicitud = new CargoService.Solicitud(
                nuevo ? null : cargo.getId(), nuevo ? 0 : cargo.getVersion(), datos.contrato().getId(),
                datos.tipo(), datos.periodo(), datos.emision(), datos.vencimiento(), datos.importe(),
                datos.concepto(), datos.estado());
        botonGuardar.setDisable(true);
        botonGuardar.setText("Guardando cargo…");
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException {
                cargoService.guardar(solicitud);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            formulario.marcarLimpio();
            DialogoFormulario.cerrar(dialogo);
            AlertaUtil.mostrarExito(nuevo ? "Cargo registrado" : "Cargo actualizado",
                    nuevo ? "El cargo quedó registrado." : "Los cambios del cargo quedaron guardados.");
            paginaActual = 1;
            cargarPagina();
        }, error -> {
            botonGuardar.setDisable(false);
            botonGuardar.setText(nuevo ? "Guardar cargo" : "Guardar cambios");
            formulario.mostrarError(mensajeError(error));
        });
    }

    @FXML public void refrescarTabla() {
        btnRefrescar.setDisable(true);
        cargarContratosYPagina();
    }

    @FXML public void limpiarFiltros() {
        txtBuscar.clear();
        paginaActual = 1;
        cargarPagina();
    }

    @FXML public void paginaAnterior() {
        if (paginaActual <= 1) return;
        paginaActual--;
        cargarPagina();
    }

    @FXML public void paginaSiguiente() {
        paginaActual++;
        cargarPagina();
    }

    @FXML public void handleAnular() {
        CargoListaResponse cargo = cargoSeleccionado;
        if (cargo == null || !permisoAnular) return;
        TextInputDialog confirmacion = new TextInputDialog();
        Window propietario = tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
        if (propietario != null) confirmacion.initOwner(propietario);
        confirmacion.setTitle("Anular cargo");
        confirmacion.setHeaderText(cargo.concepto() + " · " + cargo.importeFormateado()
                + " · vence " + cargo.venceEl());
        confirmacion.setContentText("Motivo de la anulación");
        TemaAplicacion.aplicar(confirmacion.getDialogPane());
        String motivo = confirmacion.showAndWait().map(String::strip).orElse(null);
        if (motivo == null) return;
        if (motivo.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Motivo requerido", "Indica el motivo para anular el cargo.");
            return;
        }
        btnAnular.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                cobranza.anularCargo(cargo.id(), motivo);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            cargoSeleccionado = null;
            AlertaUtil.mostrarExito("Cargo anulado", "El cargo quedó anulado y conservó su importe histórico.");
            cargarPagina();
        }, error -> {
            btnAnular.setDisable(cargoSeleccionado == null || !permisoAnular);
            AlertaUtil.mostrarError("No se pudo anular el cargo", mensajeError(error));
        });
    }

    @FXML
    public void handleGenerarRentas() {
        Window ventana = tblCargos == null || tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana != null) dialogo.initOwner(ventana);
        dialogo.setTitle("Generación mensual de rentas (H1)");

        TextField txtPeriodo = new TextField(YearMonth.now(RelojService.ZONA_OPERATIVA).toString());
        txtPeriodo.getStyleClass().add("field");
        txtPeriodo.setPromptText("AAAA-MM");
        Button btnPrevisualizar = new Button("Previsualizar");
        btnPrevisualizar.getStyleClass().add("button-secondary");
        Button btnEjecutar = new Button("Confirmar generación");
        btnEjecutar.getStyleClass().add("button-primary");
        Button btnCancelarLote = new Button("Cancelar lote");
        btnCancelarLote.getStyleClass().add("button-destructive");
        btnCancelarLote.setDisable(true);

        Label lblEstadoLote = new Label("Indica el período (AAAA-MM) y previsualiza los cargos antes de confirmar.");
        lblEstadoLote.setWrapText(true);
        lblEstadoLote.getStyleClass().add("secondary-text");
        ProgressBar barra = new ProgressBar(0);
        barra.setMaxWidth(Double.MAX_VALUE);

        TableView<ItemPrevisualizacionLoteDTO> tblPrevia = new TableView<>();
        tblPrevia.setPrefHeight(260);
        TableColumn<ItemPrevisualizacionLoteDTO, String> colPContrato = new TableColumn<>("Contrato");
        colPContrato.setPrefWidth(210);
        colPContrato.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().contratoInfo()));
        TableColumn<ItemPrevisualizacionLoteDTO, String> colPCanon = new TableColumn<>("Canon");
        colPCanon.setPrefWidth(105);
        colPCanon.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
                c.getValue().canon() + " " + c.getValue().moneda()));
        TableColumn<ItemPrevisualizacionLoteDTO, String> colPCredito = new TableColumn<>("Crédito aplicado");
        colPCredito.setPrefWidth(120);
        colPCredito.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
                c.getValue().creditoAplicable().toPlainString()));
        TableColumn<ItemPrevisualizacionLoteDTO, String> colPEstado = new TableColumn<>("Acción");
        colPEstado.setPrefWidth(120);
        colPEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
                c.getValue().yaExistente() ? "Omitir (ya existe)" : "Generar (" + c.getValue().venceEl() + ")"));
        tblPrevia.getColumns().addAll(List.of(colPContrato, colPCanon, colPCredito, colPEstado));

        HBox barraControles = new HBox(10, new Label("Período:"), txtPeriodo, btnPrevisualizar, btnEjecutar, btnCancelarLote);
        barraControles.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox contenido = new VBox(12, barraControles, barra, lblEstadoLote, tblPrevia);
        contenido.setPadding(new Insets(16));
        contenido.setPrefSize(720, 440);

        AtomicBoolean cancelado = new AtomicBoolean(false);
        CobranzaService cobranzaDomain = LatchApplication.instancia().getComposicion().cobranza();

        btnPrevisualizar.setOnAction(ev -> {
            YearMonth periodo;
            try {
                periodo = YearMonth.parse(txtPeriodo.getText().strip());
            } catch (DateTimeParseException ex) {
                lblEstadoLote.setText("Formato inválido. Usa AAAA-MM (por ejemplo, 2026-11).");
                return;
            }
            btnPrevisualizar.setDisable(true);
            Task<PrevisualizacionLoteDTO> tarea = new Task<>() {
                @Override protected PrevisualizacionLoteDTO call() {
                    return cobranzaDomain.previsualizarGeneracionMes(periodo);
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, previa -> {
                btnPrevisualizar.setDisable(false);
                tblPrevia.setItems(FXCollections.observableArrayList(previa.items()));
                lblEstadoLote.setText("Vista previa " + previa.periodo() + ": "
                        + previa.totalElegibles() + " elegibles, "
                        + previa.porGenerar() + " por generar, "
                        + previa.omitidos() + " omitidos (sin persistir).");
            }, error -> {
                btnPrevisualizar.setDisable(false);
                lblEstadoLote.setText("No se pudo previsualizar: " + mensajeError(error));
            });
        });

        btnCancelarLote.setOnAction(ev -> {
            cancelado.set(true);
            btnCancelarLote.setDisable(true);
            lblEstadoLote.setText(CobranzaService.MSG_059);
        });

        btnEjecutar.setOnAction(ev -> {
            YearMonth periodo;
            try {
                periodo = YearMonth.parse(txtPeriodo.getText().strip());
            } catch (DateTimeParseException ex) {
                lblEstadoLote.setText("Formato inválido. Usa AAAA-MM.");
                return;
            }
            cancelado.set(false);
            final ni.edu.uam.proyectofinalprogramacionappescritorio.util.RegistroTareaUnica.Ejecucion ejecucionH1;
            try {
                ejecucionH1 = ni.edu.uam.proyectofinalprogramacionappescritorio.util.RegistroTareaUnica.H1
                        .iniciar("Generación mensual de rentas " + periodo);
            } catch (IllegalStateException conflicto) {
                lblEstadoLote.setText(conflicto.getMessage());
                return;
            }
            btnEjecutar.setDisable(true);
            btnPrevisualizar.setDisable(true);
            btnCancelarLote.setDisable(false);
            barra.setProgress(0);
            Task<ResultadoLoteDTO> tarea = new Task<>() {
                @Override protected ResultadoLoteDTO call() {
                    try {
                        return cobranzaDomain.generarMes(
                                periodo,
                                () -> cancelado.get() || ejecucionH1.estaCancelada(),
                                prog -> Platform.runLater(() -> {
                                    barra.setProgress(prog.total() == 0 ? 1.0 : (double) prog.procesados() / prog.total());
                                    lblEstadoLote.setText(prog.mensaje() + " (" + prog.procesados() + "/" + prog.total()
                                            + " · confirmados: " + prog.confirmados()
                                            + ", omitidos: " + prog.omitidos()
                                            + ", pendientes: " + prog.pendientes() + ")");
                                }),
                                null);
                    } finally {
                        ejecucionH1.close();
                    }
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
                btnEjecutar.setDisable(false);
                btnPrevisualizar.setDisable(false);
                btnCancelarLote.setDisable(true);
                barra.setProgress(res.totalElegibles() == 0 ? 1.0
                        : (double) (res.totalElegibles() - res.pendientes()) / res.totalElegibles());
                lblEstadoLote.setText((res.cancelado() ? CobranzaService.MSG_059 + " " : "Generación finalizada. ")
                        + "Confirmados: " + res.confirmados()
                        + ", omitidos: " + res.omitidos()
                        + ", fallidos: " + res.fallidos()
                        + ", pendientes: " + res.pendientes() + ".");
                paginaActual = 1;
                cargarPagina();
            }, error -> {
                btnEjecutar.setDisable(false);
                btnPrevisualizar.setDisable(false);
                btnCancelarLote.setDisable(true);
                lblEstadoLote.setText("Error en generación: " + mensajeError(error));
            });
        });

        DialogPane panel = dialogo.getDialogPane();
        panel.setContent(contenido);
        panel.getButtonTypes().setAll(ButtonType.CLOSE);
        dialogo.setOnCloseRequest(evento -> {
            if (btnEjecutar.isDisabled()) {
                evento.consume();
                btnCancelarLote.fire();
            }
        });
        TemaAplicacion.aplicar(panel);
        dialogo.showAndWait();
    }

    @FXML
    public void handleRevisarMora() {
        btnRevisarMora.setDisable(true);
        Task<ResultadoRevisionH2Response> tarea = new Task<>() {
            @Override protected ResultadoRevisionH2Response call() {
                return cobranza.revisarMora(null);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
            btnRevisarMora.setDisable(false);
            AlertaUtil.mostrarExito("Revisión H2 completada",
                    "Fecha: " + res.fechaEvaluacion()
                            + "\nContratos evaluados: " + res.contratosEvaluados()
                            + "\nMarcados Por vencer: " + res.marcadosPorVencer()
                            + "\nMarcados Vencido: " + res.marcadosVencido()
                            + "\nMoras generadas: " + res.morasGeneradas()
                            + "\nIncidentes: " + res.incidentes());
            cargarPagina();
        }, error -> {
            btnRevisarMora.setDisable(false);
            AlertaUtil.mostrarError("No se pudo ejecutar la revisión de mora", mensajeError(error));
        });
    }

    @FXML
    public void handleVerCartera() {
        Window ventana = tblCargos == null || tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana != null) dialogo.initOwner(ventana);
        dialogo.setTitle("Cartera por antigüedad (M05)");

        DatePicker dpCorte = new DatePicker(LocalDate.now(RelojService.ZONA_OPERATIVA));
        ComboBox<String> cmbMoneda = new ComboBox<>(FXCollections.observableArrayList("Todas", "USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        TextField txtFiltroCartera = new TextField();
        txtFiltroCartera.setPromptText("Contrato, inquilino o concepto");
        txtFiltroCartera.getStyleClass().add("search-field");
        Button btnConsultar = new Button("Consultar");
        btnConsultar.getStyleClass().add("button-primary");

        Label lblTotales = new Label("Consultando cartera…");
        lblTotales.setWrapText(true);
        lblTotales.getStyleClass().add("secondary-text");

        TableView<CarteraResponse.Item> tblCartera = new TableView<>();
        tblCartera.setPrefHeight(320);
        VBox.setVgrow(tblCartera, Priority.ALWAYS);

        TableColumn<CarteraResponse.Item, String> cContrato = new TableColumn<>("Contrato");
        cContrato.setPrefWidth(200);
        cContrato.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().contratoInfo()));
        TableColumn<CarteraResponse.Item, String> cConcepto = new TableColumn<>("Concepto");
        cConcepto.setPrefWidth(180);
        cConcepto.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().concepto()));
        TableColumn<CarteraResponse.Item, String> cVence = new TableColumn<>("Vence");
        cVence.setPrefWidth(95);
        cVence.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().venceEl()));
        TableColumn<CarteraResponse.Item, String> cDias = new TableColumn<>("Días");
        cDias.setPrefWidth(65);
        cDias.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
                String.valueOf(c.getValue().diasVencidos())));
        TableColumn<CarteraResponse.Item, String> cTramo = new TableColumn<>("Tramo");
        cTramo.setPrefWidth(100);
        cTramo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().tramo()));
        TableColumn<CarteraResponse.Item, String> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setPrefWidth(120);
        cSaldo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(
                c.getValue().saldo() + " " + c.getValue().moneda()));
        TableColumn<CarteraResponse.Item, String> cEstado = new TableColumn<>("Estado");
        cEstado.setPrefWidth(95);
        cEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().estado()));
        tblCartera.getColumns().addAll(List.of(cContrato, cConcepto, cVence, cDias, cTramo, cSaldo, cEstado));

        Runnable cargar = () -> {
            btnConsultar.setDisable(true);
            LocalDate corte = dpCorte.getValue();
            String moneda = "Todas".equals(cmbMoneda.getValue()) ? null : cmbMoneda.getValue();
            String texto = txtFiltroCartera.getText();
            Task<CarteraResponse> tarea = new Task<>() {
                @Override protected CarteraResponse call() {
                    return cobranza.consultarCartera(corte, null, null, moneda, texto);
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
                btnConsultar.setDisable(false);
                tblCartera.setItems(FXCollections.observableArrayList(res.items()));
                if (res.totalesPorMoneda().isEmpty()) {
                    lblTotales.setText("Corte " + res.corte() + ": sin saldos pendientes.");
                } else {
                    StringBuilder resumen = new StringBuilder("Corte ").append(res.corte()).append(" — ");
                    for (var tot : res.totalesPorMoneda()) {
                        resumen.append("[").append(tot.moneda()).append("] ")
                                .append("Total: ").append(tot.totalSaldo())
                                .append(" (Al día: ").append(tot.alDia())
                                .append(", 1-30: ").append(tot.tramo1a30())
                                .append(", 31-60: ").append(tot.tramo31a60())
                                .append(", 61-90: ").append(tot.tramo61a90())
                                .append(", >90: ").append(tot.tramoMasDe90()).append(")  ");
                    }
                    lblTotales.setText(resumen.toString().strip());
                }
            }, error -> {
                btnConsultar.setDisable(false);
                lblTotales.setText("No se pudo consultar la cartera: " + mensajeError(error));
            });
        };

        btnConsultar.setOnAction(ev -> cargar.run());
        HBox filtros = new HBox(10, new Label("Corte:"), dpCorte, cmbMoneda, txtFiltroCartera, btnConsultar);
        filtros.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox contenido = new VBox(12, filtros, lblTotales, tblCartera);
        contenido.setPadding(new Insets(16));
        contenido.setPrefSize(880, 500);

        DialogPane panel = dialogo.getDialogPane();
        panel.setContent(contenido);
        panel.getButtonTypes().setAll(ButtonType.CLOSE);
        TemaAplicacion.aplicar(panel);
        cargar.run();
        dialogo.showAndWait();
    }

    private void mostrarErrorConsulta(Throwable error) {
        lblErrorConsulta.setText("No se pudieron actualizar los cargos. " + mensajeError(error));
        lblErrorConsulta.setVisible(true);
        lblErrorConsulta.setManaged(true);
    }

    private static String mensajeError(Throwable error) {
        if (error instanceof ErrorClienteException) return error.getMessage();
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException
                || error instanceof SecurityException) return error.getMessage();
        return "Revisa los datos y vuelve a intentarlo.";
    }

    @FXML
    public void handleGenerarReporte() {
        List<CargoListaResponse> cargosActuales = tblCargos == null
                ? List.of()
                : List.copyOf(tblCargos.getItems());

        if (cargosActuales.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos",
                    "No hay cargos en la tabla para generar el reporte de cartera y mora.");
            return;
        }

        Window ventana = tblCargos == null || tblCargos.getScene() == null
                ? null
                : tblCargos.getScene().getWindow();
        VistaPreviaReporte.generarYMostrar(btnGenerarReporte, ventana,
                () -> reporteService.generarDocumentoCargosCarteraDesdeLista(cargosActuales));
    }



}
