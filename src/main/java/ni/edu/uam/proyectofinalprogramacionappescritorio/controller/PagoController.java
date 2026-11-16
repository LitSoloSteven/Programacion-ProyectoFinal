package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PagoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.SesionExpiradaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Historial paginado y formulario de revisión para registrar pagos. */
public class PagoController {
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private VBox rootView;
    @FXML private HBox anularTooltipHost;
    @FXML private Button btnAnular;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
    @FXML private Button btnRefrescar;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private Label lblPaginacion;
    @FXML private Label lblTitulo;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Pago> tblPagos;
    @FXML private TableColumn<Pago, String> colContrato;
    @FXML private TableColumn<Pago, LocalDate> colFecha;
    @FXML private TableColumn<Pago, String> colMonto;
    @FXML private TableColumn<Pago, String> colMetodo;
    @FXML private TableColumn<Pago, String> colReferencia;
    @FXML private TableColumn<Pago, String> colEstado;
    @FXML private TableColumn<Pago, Void> colAcciones;

    private final PagoService pagoService;
    private final ContratoService contratoService;
    private final ClientePagos clientePagos;
    private final Map<PagoRequest, UUID> clavesPagosPendientes = new HashMap<>();
    private final ObservableList<Pago> listaPagos = FXCollections.observableArrayList();
    private List<Contrato> contratosDisponibles = List.of();
    private Pago pagoSeleccionado;
    private int paginaActual = 1;
    private long versionConsulta;
    private final boolean permisoAnular = SesionUsuario.esPropietario();

    private record DatosTabla(List<Contrato> contratos, PaginaResponse<Pago> pagos) { }

    public PagoController() {
        this(HelloApplication.instancia().getComposicion().pagos(),
                HelloApplication.instancia().getComposicion().contratos(), new ClientePagos());
    }

    public PagoController(PagoService pagoService, ContratoService contratoService) {
        this(pagoService, contratoService, new ClientePagos());
    }

    public PagoController(PagoService pagoService, ContratoService contratoService,
                          ClientePagos clientePagos) {
        this.pagoService = pagoService;
        this.contratoService = contratoService;
        this.clientePagos = clientePagos;
    }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PAGO, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnAnular, Iconos.Nombre.ELIMINAR, 16);
        btnAnular.setDisable(true);
        Tooltip.install(anularTooltipHost, new Tooltip(permisoAnular
                ? "Selecciona un pago para anularlo." : "Solo el propietario puede anular pagos."));
        btnNuevo.setDisable(true);
        tblPagos.setItems(listaPagos);
        tblPagos.setPlaceholder(PanelEstadoTabla.crear("Cargando pagos",
                "El historial aparecerá al terminar la consulta.", null, null));
        colContrato.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getContratoInfo()));
        colFecha.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().getFechaPago()));
        colFecha.setCellFactory(FormatoTabla.fecha());
        colMonto.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getMontoFormateado()));
        colMonto.setCellFactory(FormatoTabla.numerica());
        colMetodo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getMetodo()));
        colReferencia.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getReferencia()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getEstado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>(
                "Ver pago", this::mostrarDetalle, Iconos.Nombre.VER)));
        tblPagos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            pagoSeleccionado = actual;
            btnAnular.setDisable(actual == null || !permisoAnular || "Anulado".equalsIgnoreCase(actual.getEstado()));
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            paginaActual = 1;
            cargarDatos();
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        cargarContratosYPagos();
    }

    private void cargarContratosYPagos() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                return new DatosTabla(contratoService.listarVigentes(),
                        pagoService.listarTodos(pagina, PagoService.TAMANO_PAGINA_MAXIMO, filtro));
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            contratosDisponibles = datos.contratos();
            btnNuevo.setDisable(contratosDisponibles.isEmpty());
            mostrarPagina(datos.pagos());
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) {
                mostrarErrorConsulta(error);
                tblPagos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los pagos",
                        "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarContratosYPagos));
            }
            btnRefrescar.setDisable(false);
        });
    }

    public void cargarDatos() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<PaginaResponse<Pago>> tarea = new Task<>() {
            @Override protected PaginaResponse<Pago> call() {
                return pagoService.listarTodos(pagina, PagoService.TAMANO_PAGINA_MAXIMO, filtro);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, resultado -> {
            if (version == versionConsulta) mostrarPagina(resultado);
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) mostrarErrorConsulta(error);
            btnRefrescar.setDisable(false);
        });
    }

    private void mostrarPagina(PaginaResponse<Pago> pagina) {
        int paginas = (int) Math.max(1, (pagina.total() + pagina.tamano() - 1) / pagina.tamano());
        if (paginaActual > paginas) {
            paginaActual = paginas;
            cargarDatos();
            return;
        }
        listaPagos.setAll(pagina.elementos());
        long desde = pagina.total() == 0 ? 0 : (long) (paginaActual - 1) * pagina.tamano() + 1;
        long hasta = pagina.total() == 0 ? 0 : desde + pagina.elementos().size() - 1;
        lblPaginacion.setText(desde + "–" + hasta + " de " + pagina.total() + " pagos");
        lblContador.setText(pagina.total() + (pagina.total() == 1 ? " pago" : " pagos"));
        btnPaginaAnterior.setDisable(paginaActual <= 1);
        btnPaginaSiguiente.setDisable(paginaActual >= paginas);
        tblPagos.setPlaceholder(PanelEstadoTabla.crear(txtBuscar.getText().isBlank()
                        ? "Aún no hay pagos" : "No hay coincidencias",
                txtBuscar.getText().isBlank() ? "Los pagos registrados aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                txtBuscar.getText().isBlank() && !contratosDisponibles.isEmpty() ? "Registrar pago" : null,
                txtBuscar.getText().isBlank() && !contratosDisponibles.isEmpty() ? this::handleNuevo : null));
        lblErrorConsulta.setText("");
        lblErrorConsulta.setVisible(false);
        lblErrorConsulta.setManaged(false);
    }

    @FXML public void handleNuevo() { abrirFormulario(); }

    private void abrirFormulario() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("pago-form.fxml"));
            Node contenido = loader.load();
            PagoFormController formulario = loader.getController();
            formulario.establecerContratos(contratosDisponibles);
            Window propietario = tblPagos.getScene() == null ? null : tblPagos.getScene().getWindow();
            Dialog<ButtonType> dialogo = DialogoFormulario.crear("Registrar pago", contenido,
                    propietario, 740, 680, "Revisar pago");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnNuevo, formulario::hayCambios);
            ButtonType accion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button botonAccion = (Button) dialogo.getDialogPane().lookupButton(accion);
            botonAccion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if ("Revisar pago".equals(botonAccion.getText())) {
                    if (formulario.validar()) {
                        formulario.mostrarRevision();
                        botonAccion.setText("Confirmar pago");
                    }
                } else {
                    registrarPago(dialogo, botonAccion, formulario, propietario);
                }
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    private void registrarPago(Dialog<ButtonType> dialogo, Button botonAccion,
                               PagoFormController formulario, Window propietario) {
        PagoFormController.Datos datos = formulario.datos();
        PagoRequest solicitud = new PagoRequest(datos.contrato().getId().toString(),
                datos.fecha().toString(), datos.moneda(), datos.monto().toPlainString(), datos.metodo(),
                datos.referencia(), List.of(), datos.notas());
        UUID clave = clavesPagosPendientes.computeIfAbsent(solicitud, ignorada -> UUID.randomUUID());
        botonAccion.setDisable(true);
        botonAccion.setText("Registrando pago…");
        Task<PagoRegistroResponse> tarea = new Task<>() {
            @Override protected PagoRegistroResponse call() { return clientePagos.registrar(solicitud, clave); }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, registro -> {
            clavesPagosPendientes.remove(solicitud, clave);
            formulario.marcarLimpio();
            DialogoFormulario.cerrar(dialogo);
            AlertaUtil.mostrarExito("Pago registrado", "Pago de "
                    + String.format("%.2f %s", datos.monto(), datos.moneda()) + " registrado el "
                    + datos.fecha().format(FECHA_VISIBLE) + ". Recibo N.º " + registro.numeroRecibo() + ".");
            paginaActual = 1;
            cargarDatos();
        }, error -> {
            botonAccion.setDisable(false);
            botonAccion.setText("Confirmar pago");
            if (error instanceof ErrorClienteException clienteError && clienteError.esNoAutenticado()) {
                formulario.mostrarError("Tu sesión expiró.");
                SesionExpiradaUtil.mostrar(propietario, dialogo);
                return;
            }
            formulario.mostrarError(mensajeErrorPago(error));
        });
    }

    @FXML public void refrescarTabla() {
        btnRefrescar.setDisable(true);
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                return new DatosTabla(contratoService.listarVigentes(),
                        pagoService.listarTodos(pagina, PagoService.TAMANO_PAGINA_MAXIMO, filtro));
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            contratosDisponibles = datos.contratos();
            btnNuevo.setDisable(contratosDisponibles.isEmpty());
            mostrarPagina(datos.pagos());
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) mostrarErrorConsulta(error);
            btnRefrescar.setDisable(false);
        });
    }

    @FXML public void limpiarFiltros() {
        if (txtBuscar.getText().isBlank()) return;
        txtBuscar.clear();
    }

    @FXML public void paginaAnterior() {
        if (paginaActual <= 1) return;
        paginaActual--;
        cargarDatos();
    }

    @FXML public void paginaSiguiente() {
        paginaActual++;
        cargarDatos();
    }

    @FXML public void handleAnular() {
        Pago pago = pagoSeleccionado;
        if (pago == null || !permisoAnular || "Anulado".equalsIgnoreCase(pago.getEstado())) return;
        String registro = "Pago: " + pago.getMontoFormateado() + " · "
                + pago.getFechaPago().format(FECHA_VISIBLE) + " · " + pago.getContratoInfo();
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Anular pago", registro,
                "El pago quedará marcado como anulado y ya no se contará en el saldo del cargo.",
                "Anular pago")) return;
        UUID pagoId = pago.getId();
        btnAnular.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException {
                pagoService.anular(pagoId);
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            pagoSeleccionado = null;
            AlertaUtil.mostrarExito("Pago anulado", "El pago quedó marcado como anulado.");
            cargarDatos();
        }, error -> {
            btnAnular.setDisable(pagoSeleccionado == null || !permisoAnular);
            AlertaUtil.mostrarError("No se pudo anular el pago", mensajeErrorPago(error));
        });
    }

    private void mostrarDetalle(Pago pago) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del pago");
        alerta.setHeaderText(pago.getMontoFormateado());
        alerta.setContentText(pago.getContratoInfo() + "\nFecha: "
                + pago.getFechaPago().format(FECHA_VISIBLE) + "\nMétodo: " + pago.getMetodo()
                + "\nReferencia: " + (pago.getReferencia() == null ? "—" : pago.getReferencia())
                + "\nEstado: " + pago.getEstado());
        TemaAplicacion.aplicar(alerta.getDialogPane());
        alerta.showAndWait();
    }

    private void mostrarErrorConsulta(Throwable error) {
        lblErrorConsulta.setText("No se pudo actualizar el historial. " + mensajeErrorPago(error));
        lblErrorConsulta.setVisible(true);
        lblErrorConsulta.setManaged(true);
    }

    private String mensajeErrorPago(Throwable error) {
        if (error instanceof ErrorClienteException clienteError) {
            return clienteError.getMessage() + " Si el resultado es incierto, reintenta sin cambiar los datos.";
        }
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException
                || error instanceof SecurityException) return error.getMessage();
        return "Revisa los datos y vuelve a intentarlo.";
    }
}
