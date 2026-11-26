package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CarteraResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PrevisualizacionLoteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ResultadoLoteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ResultadoRevisionH2Response;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/** Lista paginada de cargos y operación financiera vía ClientePagos HTTP. */
public class CargoController {
    @FXML private VBox rootView;
    @FXML private HBox anularTooltipHost;
    @FXML private Button btnAnular;
    @FXML private Button btnCartera;
    @FXML private Button btnGenerarRentas;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
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

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ClientePagos cobranza = LatchApplication.instancia().getPagos();
    private final ObservableList<CargoListaResponse> listaCargos = FXCollections.observableArrayList();
    private List<ContratoResponse> contratosDisponibles = List.of();
    private CargoListaResponse cargoSeleccionado;
    private int paginaActual = 1;
    private boolean permisoAnular;
    private boolean suprimiendoFiltro;
    private boolean sinContratosVigentes;
    private RecargadorVista recargador;
    private BarraErrorContextual barraError;

    private record DatosTabla(List<ContratoResponse> contratos, PaginaResponse<CargoListaResponse> cargos) { }

    @FXML
    public void initialize() {
        permisoAnular = SesionCliente.getActual().esPropietario();
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.CARGO, 20));
        recargador = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnAnular, Iconos.Nombre.ELIMINAR, 16);
        btnAnular.setDisable(true);
        Tooltip.install(anularTooltipHost,
                new Tooltip(permisoAnular ? "Selecciona un cargo para anularlo." : "Solo el propietario puede anular cargos."));
        btnNuevo.setDisable(true);
        tblCargos.setItems(listaCargos);
        tblCargos.setPlaceholder(PanelEstadoTabla.crear("Cargando cargos", "La lista se actualizará al terminar la consulta.", null, null));
        colContrato.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().contratoInfo()));
        colTipo.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().tipo()));
        colConcepto.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().concepto()));
        colImporte.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().importeFormateado()));
        colImporte.setCellFactory(FormatoTabla.numerica());
        colVence.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(LocalDate.parse(cell.getValue().venceEl())));
        colVence.setCellFactory(FormatoTabla.fecha());
        colEstado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().estado()));
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
            if (suprimiendoFiltro) return;
            paginaActual = 1;
            int pagina = paginaActual;
            String filtro = nuevo;
            recargador.recargarAgrupado(
                    Duration.millis(250),
                    () -> cobranza.listarCargos(pagina, filtro),
                    this::mostrarPagina,
                    this::mostrarErrorConsulta);
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        cargarContratosYPagina();
        recargador.activarConsultaPeriodica(Duration.seconds(60), () -> {
            int pagina = paginaActual;
            String filtro = txtBuscar.getText();
            recargador.recargarSiInactivo(
                    () -> cobranza.listarCargos(pagina, filtro),
                    this::mostrarPagina,
                    this::mostrarErrorConsulta);
        });
    }

    public void aplicarFiltroInicial(String texto) {
        if (texto == null || texto.isBlank() || txtBuscar == null) {
            return;
        }
        suprimiendoFiltro = true;
        try {
            txtBuscar.setText(texto.strip());
        } finally {
            suprimiendoFiltro = false;
        }
        paginaActual = 1;
        cargarContratosYPagina();
    }

    private void cargarContratosYPagina() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        recargador.recargar(
                () -> new DatosTabla(catalogos.contratos(true), cobranza.listarCargos(pagina, filtro)),
                resultado -> {
                    contratosDisponibles = resultado.contratos();
                    sinContratosVigentes = contratosDisponibles.isEmpty();
                    btnNuevo.setDisable(sinContratosVigentes);
                    mostrarPagina(resultado.cargos());
                },
                error -> {
                    mostrarErrorConsulta(error);
                    if (listaCargos.isEmpty()) {
                        tblCargos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los cargos",
                                "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarContratosYPagina));
                    }
                });
    }

    private void cargarPagina() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        recargador.recargar(
                () -> cobranza.listarCargos(pagina, filtro),
                this::mostrarPagina,
                this::mostrarErrorConsulta);
    }

    private void mostrarPagina(PaginaResponse<CargoListaResponse> resultado) {
        int paginas = (int) Math.max(1, (resultado.total() + resultado.tamano() - 1) / resultado.tamano());
        if (paginaActual > paginas) {
            paginaActual = paginas;
            cargarPagina();
            return;
        }
        RecargadorVista.conservarSeleccionPorId(tblCargos, CargoListaResponse::id,
                () -> listaCargos.setAll(resultado.elementos()));
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
        if (sinContratosVigentes) {
            barraError.mostrar("No hay contratos vigentes para registrar cargos.", null);
        } else {
            barraError.ocultar();
        }
    }

    @FXML public void handleNuevo() { abrirFormulario(null); }

    private void abrirEdicion(CargoListaResponse fila) {
        UUID cargoId = UUID.fromString(fila.id());
        Task<CargoDetalleResponse> tarea = new Task<>() {
            @Override protected CargoDetalleResponse call() {
                return cobranza.obtenerCargo(cargoId);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, this::abrirFormulario,
                error -> AlertaUtil.mostrarError("No se pudo abrir el cargo", mensajeError(error)));
    }

    private void abrirFormulario(CargoDetalleResponse cargo) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("cargo-form.fxml"));
            Node contenido = loader.load();
            CargoFormController formulario = loader.getController();
            formulario.establecerValidadorServidor(ClienteLatch.validaciones());
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
                                  CargoFormController formulario, CargoDetalleResponse cargo) {
        if (!formulario.validar()) return;
        CargoFormController.Datos datos = formulario.datos();
        boolean nuevo = cargo == null;
        CargoRequest solicitud = new CargoRequest(
                datos.contrato().id(),
                datos.tipo(),
                datos.periodo().toString(),
                datos.emision().toString(),
                datos.vencimiento().toString(),
                datos.importe().toPlainString(),
                datos.concepto(),
                datos.estado(),
                nuevo ? null : cargo.version());
        UUID cargoId = nuevo ? null : UUID.fromString(cargo.id());
        botonGuardar.setDisable(true);
        botonGuardar.setText("Guardando cargo…");
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                if (nuevo) {
                    cobranza.crearCargo(solicitud);
                } else {
                    cobranza.actualizarCargo(cargoId, solicitud);
                }
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
            if (!nuevo && error instanceof ErrorClienteException api && api.getEstado() == 409) {
                DialogoFormulario.cerrar(dialogo);
                cargarPagina();
                AlertaUtil.mostrarAdvertencia("No se guardaron los cambios",
                        api.getMessage() + " Abre el cargo actualizado para volver a editarlo.");
                return;
            }
            formulario.mostrarError(mensajeError(error));
        });
    }

    @FXML public void limpiarFiltros() {
        if (txtBuscar.getText().isBlank() && paginaActual == 1) return;
        suprimiendoFiltro = true;
        try {
            txtBuscar.clear();
        } finally {
            suprimiendoFiltro = false;
        }
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
        Window propietario = tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
        TextField txtMotivo = new TextField();
        txtMotivo.getStyleClass().add("field");
        txtMotivo.setPromptText("Explica el motivo de la anulación (hasta 500 car.)");
        Label lblError = new Label();
        lblError.getStyleClass().add("field-error");
        lblError.setWrapText(true);
        lblError.setMinHeight(18);
        lblError.setVisible(false);
        lblError.setManaged(true);
        Label lblResumen = new Label(cargo.concepto() + " · " + cargo.importeFormateado()
                + " · vence " + cargo.venceEl());
        lblResumen.setWrapText(true);
        lblResumen.getStyleClass().add("review-value");
        Label lblAyuda = new Label("El cargo conservará su importe histórico y no podrá revertirse.");
        lblAyuda.setWrapText(true);
        lblAyuda.getStyleClass().add("field-help");
        VBox contenido = new VBox(8, lblResumen, lblAyuda, new Label("Motivo de la anulación *:"), txtMotivo, lblError);
        contenido.setPadding(new Insets(12));

        Runnable validarMotivo = () -> {
            String m = txtMotivo.getText() == null ? "" : txtMotivo.getText().strip();
            String msg = m.isEmpty() ? "Indica el motivo para anular el cargo."
                    : (m.length() > 500 ? "El motivo admite hasta 500 caracteres." : null);
            ValidacionFormulario.marcarCampo(txtMotivo, lblError, msg);
        };
        ValidacionFormulario.vincularTexto(txtMotivo, validarMotivo);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Anular cargo", contenido, propietario, 500, 320, "Anular cargo", true);
        DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnAnular, () -> !txtMotivo.getText().isBlank());
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            validarMotivo.run();
            if (lblError.isVisible()) {
                txtMotivo.requestFocus();
                return;
            }
            String motivo = txtMotivo.getText().strip();
            accion.setDisable(true);
            accion.setText("Anulando…");
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    cobranza.anularCargo(cargo.id(), motivo);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                DialogoFormulario.cerrar(dialogo);
                cargoSeleccionado = null;
                AlertaUtil.mostrarExito("Cargo anulado", "El cargo quedó anulado y conservó su importe histórico.");
                cargarPagina();
            }, error -> {
                accion.setDisable(false);
                accion.setText("Anular cargo");
                ValidacionFormulario.marcarCampo(txtMotivo, lblError, mensajeError(error));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleGenerarRentas() {
        Window ventana = tblCargos == null || tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (ventana != null) dialogo.initOwner(ventana);
        dialogo.setTitle("Generación mensual de rentas");

        TextField txtPeriodo = new TextField(YearMonth.now(ConstantesLatch.ZONA_OPERATIVA).toString());
        txtPeriodo.getStyleClass().add("field");
        txtPeriodo.setPromptText("AAAA-MM");
        Button btnPrevisualizar = new Button("Previsualizar");
        btnPrevisualizar.getStyleClass().add("button-secondary");
        Button btnEjecutar = new Button("Confirmar generación");
        btnEjecutar.getStyleClass().add("button-primary");

        Label lblEstadoLote = new Label("Indica el período (AAAA-MM) y previsualiza los cargos antes de confirmar.");
        lblEstadoLote.setWrapText(true);
        lblEstadoLote.getStyleClass().add("secondary-text");
        Label lblErrorPeriodo = new Label();
        lblErrorPeriodo.getStyleClass().add("field-error");
        lblErrorPeriodo.setWrapText(true);
        lblErrorPeriodo.setVisible(false);
        lblErrorPeriodo.setManaged(false);
        ValidacionFormulario.vincularTexto(txtPeriodo, () -> {
            String raw = txtPeriodo.getText() == null ? "" : txtPeriodo.getText().strip();
            String msg = null;
            if (raw.isEmpty()) {
                msg = "Indica el período en formato AAAA-MM.";
            } else {
                try { YearMonth.parse(raw); }
                catch (DateTimeParseException ex) { msg = "Formato inválido. Usa AAAA-MM (por ejemplo, 2026-11)."; }
            }
            ValidacionFormulario.marcarCampo(txtPeriodo, lblErrorPeriodo, msg);
            lblErrorPeriodo.setManaged(msg != null);
        });
        ProgressBar barra = new ProgressBar(0);
        barra.setMaxWidth(Double.MAX_VALUE);

        TableView<PrevisualizacionLoteResponse.Item> tblPrevia = new TableView<>();
        tblPrevia.setPrefHeight(260);
        TableColumn<PrevisualizacionLoteResponse.Item, String> colPContrato = new TableColumn<>("Contrato");
        colPContrato.setPrefWidth(210);
        colPContrato.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().contratoInfo()));
        TableColumn<PrevisualizacionLoteResponse.Item, String> colPCanon = new TableColumn<>("Canon");
        colPCanon.setPrefWidth(105);
        colPCanon.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().canon() + " " + c.getValue().moneda()));
        TableColumn<PrevisualizacionLoteResponse.Item, String> colPCredito = new TableColumn<>("Crédito aplicado");
        colPCredito.setPrefWidth(120);
        colPCredito.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().creditoAplicable()));
        TableColumn<PrevisualizacionLoteResponse.Item, String> colPEstado = new TableColumn<>("Acción");
        colPEstado.setPrefWidth(120);
        colPEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().yaExistente() ? "Omitir (ya existe)" : "Generar (" + c.getValue().venceEl() + ")"));
        tblPrevia.getColumns().addAll(List.of(colPContrato, colPCanon, colPCredito, colPEstado));

        HBox barraControles = new HBox(10, new Label("Período:"), txtPeriodo, btnPrevisualizar, btnEjecutar);
        barraControles.setAlignment(Pos.CENTER_LEFT);
        VBox contenido = new VBox(12, barraControles, lblErrorPeriodo, barra, lblEstadoLote, tblPrevia);
        contenido.setPadding(new Insets(16));
        contenido.setPrefSize(720, 440);

        btnPrevisualizar.setOnAction(ev -> {
            YearMonth periodo;
            try {
                periodo = YearMonth.parse(txtPeriodo.getText().strip());
            } catch (DateTimeParseException ex) {
                lblEstadoLote.setText("Formato inválido. Usa AAAA-MM (por ejemplo, 2026-11).");
                return;
            }
            btnPrevisualizar.setDisable(true);
            Task<PrevisualizacionLoteResponse> tarea = new Task<>() {
                @Override protected PrevisualizacionLoteResponse call() {
                    return cobranza.previsualizarGeneracionMes(periodo);
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

        btnEjecutar.setOnAction(ev -> {
            YearMonth periodo;
            try {
                periodo = YearMonth.parse(txtPeriodo.getText().strip());
            } catch (DateTimeParseException ex) {
                lblEstadoLote.setText("Formato inválido. Usa AAAA-MM.");
                return;
            }
            btnEjecutar.setDisable(true);
            btnPrevisualizar.setDisable(true);
            barra.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            lblEstadoLote.setText("Generando rentas mensuales en el servidor para " + periodo + "…");
            Task<ResultadoLoteResponse> tarea = new Task<>() {
                @Override protected ResultadoLoteResponse call() {
                    return cobranza.generarMes(periodo);
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
                btnEjecutar.setDisable(false);
                btnPrevisualizar.setDisable(false);
                barra.setProgress(res.totalElegibles() == 0 ? 1.0
                        : (double) (res.totalElegibles() - res.pendientes()) / res.totalElegibles());
                String encabezado = res.mensaje() != null && !res.mensaje().isBlank()
                        ? res.mensaje() + " " : "Generación finalizada. ";
                lblEstadoLote.setText(encabezado
                        + "Confirmados: " + res.confirmados()
                        + ", omitidos: " + res.omitidos()
                        + ", fallidos: " + res.fallidos()
                        + ", pendientes: " + res.pendientes() + ".");
                paginaActual = 1;
                cargarPagina();
            }, error -> {
                btnEjecutar.setDisable(false);
                btnPrevisualizar.setDisable(false);
                barra.setProgress(0);
                lblEstadoLote.setText("Error en generación: " + mensajeError(error));
            });
        });

        DialogPane panel = dialogo.getDialogPane();
        panel.setContent(contenido);
        panel.getButtonTypes().setAll(ButtonType.CLOSE);
        dialogo.setOnCloseRequest(evento -> {
            if (btnEjecutar.isDisabled()) {
                evento.consume();
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
            AlertaUtil.mostrarExito("Revisión de mora y vencimientos completada",
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
        dialogo.setTitle("Cartera por antigüedad");

        DatePicker dpCorte = new DatePicker(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA));
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
        cContrato.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().contratoInfo()));
        TableColumn<CarteraResponse.Item, String> cConcepto = new TableColumn<>("Concepto");
        cConcepto.setPrefWidth(180);
        cConcepto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().concepto()));
        TableColumn<CarteraResponse.Item, String> cVence = new TableColumn<>("Vence");
        cVence.setPrefWidth(95);
        cVence.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().venceEl()));
        TableColumn<CarteraResponse.Item, String> cDias = new TableColumn<>("Días");
        cDias.setPrefWidth(65);
        cDias.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().diasVencidos())));
        TableColumn<CarteraResponse.Item, String> cTramo = new TableColumn<>("Tramo");
        cTramo.setPrefWidth(100);
        cTramo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tramo()));
        TableColumn<CarteraResponse.Item, String> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setPrefWidth(120);
        cSaldo.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().saldo() + " " + c.getValue().moneda()));
        TableColumn<CarteraResponse.Item, String> cEstado = new TableColumn<>("Estado");
        cEstado.setPrefWidth(95);
        cEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));
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
        filtros.setAlignment(Pos.CENTER_LEFT);
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
        barraError.mostrar("No se pudieron actualizar los cargos. " + mensajeError(error),
                contratosDisponibles.isEmpty() ? this::cargarContratosYPagina : this::cargarPagina);
    }

    private static String mensajeError(Throwable error) {
        return MensajesUi.traducir(error, "Revisa los datos y vuelve a intentarlo.");
    }
}
