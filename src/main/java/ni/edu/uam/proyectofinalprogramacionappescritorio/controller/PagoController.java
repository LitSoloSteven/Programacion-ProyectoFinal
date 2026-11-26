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
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ReciboResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Historial paginado y formulario de revisión para registrar pagos vía HTTP. */
public class PagoController {
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private VBox rootView;
    @FXML private HBox anularTooltipHost;
    @FXML private Button btnAnular;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private Label lblPaginacion;
    @FXML private Label lblTitulo;
    @FXML private TextField txtBuscar;
    @FXML private TableView<PagoListaResponse> tblPagos;
    @FXML private TableColumn<PagoListaResponse, String> colContrato;
    @FXML private TableColumn<PagoListaResponse, LocalDate> colFecha;
    @FXML private TableColumn<PagoListaResponse, String> colMonto;
    @FXML private TableColumn<PagoListaResponse, String> colMetodo;
    @FXML private TableColumn<PagoListaResponse, String> colReferencia;
    @FXML private TableColumn<PagoListaResponse, String> colEstado;
    @FXML private TableColumn<PagoListaResponse, Void> colAcciones;

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ClientePagos clientePagos = LatchApplication.instancia().getPagos();
    private final ObservableList<PagoListaResponse> listaPagos = FXCollections.observableArrayList();
    private List<ContratoResponse> contratosDisponibles = List.of();
    private PagoListaResponse pagoSeleccionado;
    private int paginaActual = 1;
    private boolean suprimiendoFiltro;
    private RecargadorVista recargador;
    private BarraErrorContextual barraError;
    private final boolean permisoAnular = SesionCliente.getActual().esPropietario();

    private record DatosTabla(List<ContratoResponse> contratos, PaginaResponse<PagoListaResponse> pagos) { }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PAGO, 20));
        recargador = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
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
        colContrato.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().contratoInfo()));
        colFecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(LocalDate.parse(c.getValue().fechaPago())));
        colFecha.setCellFactory(FormatoTabla.fecha());
        colMonto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().montoFormateado()));
        colMonto.setCellFactory(FormatoTabla.numerica());
        colMetodo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().metodo()));
        colReferencia.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().referencia()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Ver pago", this::mostrarDetalle, Iconos.Nombre.VER)));
        tblPagos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            pagoSeleccionado = actual;
            btnAnular.setDisable(actual == null || !permisoAnular || "Anulado".equalsIgnoreCase(actual.estado()));
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            if (suprimiendoFiltro) return;
            paginaActual = 1;
            int pagina = paginaActual;
            String filtro = nuevo;
            recargador.recargarAgrupado(
                    Duration.millis(250),
                    () -> clientePagos.listarPagos(pagina, filtro),
                    this::mostrarPagina,
                    this::mostrarErrorConsulta);
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        cargarContratosYPagos();
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
        cargarContratosYPagos();
    }

    private void cargarContratosYPagos() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        recargador.recargar(
                () -> new DatosTabla(catalogos.contratos(true), clientePagos.listarPagos(pagina, filtro)),
                datos -> {
                    contratosDisponibles = datos.contratos();
                    btnNuevo.setDisable(contratosDisponibles.isEmpty());
                    mostrarPagina(datos.pagos());
                },
                error -> {
                    mostrarErrorConsulta(error);
                    if (listaPagos.isEmpty()) {
                        tblPagos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los pagos",
                                "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarContratosYPagos));
                    }
                });
    }

    public void cargarDatos() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        recargador.recargar(
                () -> clientePagos.listarPagos(pagina, filtro),
                this::mostrarPagina,
                this::mostrarErrorConsulta);
    }

    private void mostrarPagina(PaginaResponse<PagoListaResponse> pagina) {
        int paginas = (int) Math.max(1, (pagina.total() + pagina.tamano() - 1) / pagina.tamano());
        if (paginaActual > paginas) {
            paginaActual = paginas;
            cargarDatos();
            return;
        }
        RecargadorVista.conservarSeleccionPorId(tblPagos, PagoListaResponse::id,
                () -> listaPagos.setAll(pagina.elementos()));
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
        barraError.ocultar();
    }

    @FXML public void handleNuevo() { abrirFormulario(); }

    private void abrirFormulario() {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("pago-form.fxml"));
            Node contenido = loader.load();
            PagoFormController formulario = loader.getController();
            formulario.establecerClientePagos(clientePagos);
            formulario.establecerValidadorServidor(ClienteLatch.validaciones());
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
                    registrarPago(dialogo, botonAccion, formulario);
                }
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    private void registrarPago(Dialog<ButtonType> dialogo, Button botonAccion,
                               PagoFormController formulario) {
        PagoFormController.Datos datos = formulario.datos();
        PagoRequest solicitud = new PagoRequest(datos.contrato().id(),
                datos.fecha().toString(), datos.moneda(), datos.monto().toPlainString(), datos.metodo(),
                datos.referencia(), datos.distribucion(), datos.notas(), datos.aplicacionAutomatica());
        long usuarioId = SesionCliente.getActual().getUsuarioId();
        botonAccion.setDisable(true);
        botonAccion.setText("Registrando pago…");
        Task<PagoRegistroResponse> tarea = new Task<>() {
            @Override protected PagoRegistroResponse call() { return clientePagos.registrarReintentable(usuarioId, solicitud); }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, registro -> {
            formulario.marcarLimpio();
            DialogoFormulario.cerrar(dialogo);
            AlertaUtil.mostrarExito("Pago registrado", "Pago de "
                    + String.format("%.2f %s", datos.monto(), datos.moneda()) + " registrado el "
                    + datos.fecha().format(FECHA_VISIBLE) + ". Recibo N.º " + registro.numeroRecibo()
                    + ". Puedes consultar y descargar el comprobante en Reportes.");
            paginaActual = 1;
            cargarDatos();
        }, error -> {
            botonAccion.setDisable(false);
            botonAccion.setText("Confirmar pago");
            if (error instanceof ErrorClienteException clienteError && clienteError.esNoAutenticado()) {
                formulario.mostrarError("Tu sesión expiró.");
                return;
            }
            formulario.mostrarError(mensajeErrorPago(error));
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
        cargarDatos();
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
        PagoListaResponse pago = pagoSeleccionado;
        if (pago == null || !permisoAnular || "Anulado".equalsIgnoreCase(pago.estado())) return;
        Window propietario = tblPagos.getScene() == null ? null : tblPagos.getScene().getWindow();
        TextField txtMotivo = new TextField();
        txtMotivo.getStyleClass().add("field");
        txtMotivo.setPromptText("Explica el motivo de la anulación (hasta 500 car.)");
        Label lblError = new Label();
        lblError.getStyleClass().add("field-error");
        lblError.setWrapText(true);
        lblError.setMinHeight(18);
        lblError.setVisible(false);
        lblError.setManaged(true);
        Label lblResumen = new Label(pago.montoFormateado() + " · " + pago.contratoInfo());
        lblResumen.setWrapText(true);
        lblResumen.getStyleClass().add("review-value");
        Label lblAyuda = new Label("El recibo quedará marcado como Anulado y se revertirán las aplicaciones.");
        lblAyuda.setWrapText(true);
        lblAyuda.getStyleClass().add("field-help");
        VBox contenido = new VBox(8, lblResumen, lblAyuda, new Label("Motivo de la anulación *:"), txtMotivo, lblError);
        contenido.setPadding(new Insets(12));

        Runnable validarMotivo = () -> {
            String m = txtMotivo.getText() == null ? "" : txtMotivo.getText().strip();
            String msg = m.isEmpty() ? "Indica por qué se debe anular el pago."
                    : (m.length() > 500 ? "El motivo admite hasta 500 caracteres." : null);
            ValidacionFormulario.marcarCampo(txtMotivo, lblError, msg);
        };
        ValidacionFormulario.vincularTexto(txtMotivo, validarMotivo);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Anular pago", contenido, propietario, 500, 320, "Anular pago", true);
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
                    clientePagos.anular(pago.id(), motivo);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                DialogoFormulario.cerrar(dialogo);
                pagoSeleccionado = null;
                AlertaUtil.mostrarExito("Pago anulado", "El pago quedó marcado como anulado.");
                cargarDatos();
            }, error -> {
                accion.setDisable(false);
                accion.setText("Anular pago");
                if (error instanceof ErrorClienteException api && api.getEstado() == 409) cargarDatos();
                ValidacionFormulario.marcarCampo(txtMotivo, lblError, mensajeErrorPago(error));
            });
        });
        dialogo.showAndWait();
    }

    private record Detalle(PagoDetalleResponse pago, ReciboResponse recibo) { }

    private void mostrarDetalle(PagoListaResponse pago) {
        Task<Detalle> tarea = new Task<>() {
            @Override protected Detalle call() {
                PagoDetalleResponse detalle = clientePagos.detalle(pago.id());
                ReciboResponse recibo = detalle.numeroRecibo() == null ? null
                        : clientePagos.recibo(detalle.numeroRecibo());
                return new Detalle(detalle, recibo);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, detalle -> {
            PagoDetalleResponse datos = detalle.pago();
            ReciboResponse recibo = detalle.recibo();
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.initOwner(tblPagos.getScene().getWindow());
            alerta.setTitle("Detalle del pago");
            alerta.setHeaderText(recibo == null ? pago.montoFormateado()
                    : "Recibo N.º " + recibo.numero() + " · " + pago.montoFormateado());
            alerta.setContentText(datos.contratoInfo()
                    + "\nFecha: " + LocalDate.parse(datos.fechaPago()).format(FECHA_VISIBLE)
                    + "\nMétodo: " + datos.metodo()
                    + "\nReferencia: " + (datos.referencia() == null ? "—" : datos.referencia())
                    + "\nEstado: " + datos.estado());
            TemaAplicacion.aplicar(alerta.getDialogPane());
            alerta.showAndWait();
        }, error -> AlertaUtil.mostrarError("No se pudo consultar el pago", mensajeErrorPago(error)));
    }

    private void mostrarErrorConsulta(Throwable error) {
        barraError.mostrar("No se pudo actualizar el historial. " + mensajeErrorPago(error),
                contratosDisponibles.isEmpty() ? this::cargarContratosYPagos : this::cargarDatos);
    }

    private String mensajeErrorPago(Throwable error) {
        String base = MensajesUi.traducir(error, "Revisa los datos y vuelve a intentarlo.");
        if (error instanceof ErrorClienteException clienteError
                && (clienteError.esSinConexion() || clienteError.getEstado() >= 500)) {
            return base + " Si el resultado es incierto, reintenta sin cambiar los datos.";
        }
        return base;
    }
}
