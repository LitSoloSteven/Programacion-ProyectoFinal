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
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Historial paginado y formulario de revisión para registrar pagos. */
public class PagoController {
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private VBox rootView;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnVerRecibo;
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
    @FXML private TableView<PagoListaResponse> tblPagos;
    @FXML private TableColumn<PagoListaResponse, String> colContrato;
    @FXML private TableColumn<PagoListaResponse, LocalDate> colFecha;
    @FXML private TableColumn<PagoListaResponse, String> colMonto;
    @FXML private TableColumn<PagoListaResponse, String> colMetodo;
    @FXML private TableColumn<PagoListaResponse, String> colReferencia;
    @FXML private TableColumn<PagoListaResponse, String> colEstado;
    @FXML private TableColumn<PagoListaResponse, Void> colAcciones;

    private final ReporteService reporteService = new ReporteService();
    private final ClienteCatalogos catalogos = new ClienteCatalogos(ClienteLatch.api());
    private final ClientePagos clientePagos = HelloApplication.instancia().getPagos();
    private final ObservableList<PagoListaResponse> listaPagos = FXCollections.observableArrayList();
    private List<Contrato> contratosDisponibles = List.of();
    private PagoListaResponse pagoSeleccionado;
    private int paginaActual = 1;
    private long versionConsulta;
    private final boolean permisoAnular = SesionUsuario.esPropietario();

    private record DatosTabla(List<Contrato> contratos, PaginaResponse<PagoListaResponse> pagos) { }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PAGO, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnGenerarReporte != null) {
            Iconos.asignar(btnGenerarReporte, Iconos.Nombre.REPORTE, 16);
            btnGenerarReporte.setTooltip(new Tooltip("Vista previa y exportación PDF del reporte de pagos"));
        }
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        if (btnVerRecibo != null) {
            Iconos.asignar(btnVerRecibo, Iconos.Nombre.VER, 16);
            btnVerRecibo.setTooltip(new Tooltip("Vista previa y exportación PDF del recibo del pago seleccionado"));
            btnVerRecibo.setDisable(true);
        }
        Iconos.asignar(btnAnular, Iconos.Nombre.ELIMINAR, 16);
        btnAnular.setDisable(true);
        Tooltip.install(anularTooltipHost, new Tooltip(permisoAnular
                ? "Selecciona un pago para anularlo." : "Solo el propietario puede anular pagos."));
        btnNuevo.setDisable(true);
        tblPagos.setItems(listaPagos);
        tblPagos.setPlaceholder(PanelEstadoTabla.crear("Cargando pagos",
                "El historial aparecerá al terminar la consulta.", null, null));
        colContrato.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().contratoInfo()));
        colFecha.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(LocalDate.parse(c.getValue().fechaPago())));
        colFecha.setCellFactory(FormatoTabla.fecha());
        colMonto.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().montoFormateado()));
        colMonto.setCellFactory(FormatoTabla.numerica());
        colMetodo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().metodo()));
        colReferencia.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().referencia()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().estado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Ver pago", this::mostrarDetalle, Iconos.Nombre.VER),
                new AccionesTabla.Accion<>("Ver recibo", this::abrirRecibo, Iconos.Nombre.REPORTE)));
        tblPagos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            pagoSeleccionado = actual;
            if (btnVerRecibo != null) {
                btnVerRecibo.setDisable(actual == null);
            }
            btnAnular.setDisable(actual == null || !permisoAnular || "Anulado".equalsIgnoreCase(actual.estado()));
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            paginaActual = 1;
            cargarDatos();
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        cargarContratosYPagos();
    }

    public void aplicarFiltroInicial(String texto) {
        if (texto == null || texto.isBlank() || txtBuscar == null) {
            return;
        }
        txtBuscar.setText(texto.strip());
        paginaActual = 1;
        cargarContratosYPagos();
    }

    private void cargarContratosYPagos() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() {
                return new DatosTabla(catalogos.contratos(true),
                        clientePagos.listarPagos(pagina, filtro));
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
        Task<PaginaResponse<PagoListaResponse>> tarea = new Task<>() {
            @Override protected PaginaResponse<PagoListaResponse> call() {
                return clientePagos.listarPagos(pagina, filtro);
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

    private void mostrarPagina(PaginaResponse<PagoListaResponse> pagina) {
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
            formulario.establecerClientePagos(clientePagos);
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
        PagoRequest solicitud = new PagoRequest(datos.contrato().getId().toString(),
                datos.fecha().toString(), datos.moneda(), datos.monto().toPlainString(), datos.metodo(),
                datos.referencia(), datos.distribucion(), datos.notas(), datos.aplicacionAutomatica());
        long usuarioId = SesionUsuario.getActual().getUsuarioId();
        botonAccion.setDisable(true);
        botonAccion.setText("Registrando pago…");
        Task<PagoRegistroResponse> tarea = new Task<>() {
            @Override protected PagoRegistroResponse call() { return clientePagos.registrarReintentable(usuarioId, solicitud); }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, registro -> {
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
            @Override protected DatosTabla call() {
                return new DatosTabla(catalogos.contratos(true),
                        clientePagos.listarPagos(pagina, filtro));
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
        PagoListaResponse pago = pagoSeleccionado;
        if (pago == null || !permisoAnular || "Anulado".equalsIgnoreCase(pago.estado())) return;
        TextInputDialog confirmacion = new TextInputDialog();
        confirmacion.initOwner(tblPagos.getScene().getWindow());
        confirmacion.setTitle("Anular pago");
        confirmacion.setHeaderText(pago.montoFormateado() + " · " + pago.contratoInfo());
        confirmacion.setContentText("Motivo de la anulación");
        TemaAplicacion.aplicar(confirmacion.getDialogPane());
        String motivo = confirmacion.showAndWait().map(String::strip).orElse(null);
        if (motivo == null) return;
        if (motivo.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Motivo requerido", "Indica por qué se debe anular el pago.");
            return;
        }
        btnAnular.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                clientePagos.anular(pago.id(), motivo);
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            pagoSeleccionado = null;
            AlertaUtil.mostrarExito("Pago anulado", "El pago quedó marcado como anulado.");
            cargarDatos();
        }, error -> {
            btnAnular.setDisable(pagoSeleccionado == null || !permisoAnular);
            if (error instanceof ErrorClienteException api && api.getEstado() == 409) cargarDatos();
            AlertaUtil.mostrarError("No se pudo anular el pago", mensajeErrorPago(error));
        });
    }

    @FXML
    public void handleGenerarReporte() {
        List<PagoListaResponse> filasActuales = tblPagos == null
                ? List.of()
                : List.copyOf(tblPagos.getItems());

        if (filasActuales.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos",
                    "No hay pagos en la tabla para generar el reporte.");
            return;
        }

        List<Pago> pagosParaReporte = filasActuales.stream()
                .map(this::mapearPagoDesdeLista)
                .toList();

        Window ventana = tblPagos == null || tblPagos.getScene() == null
                ? null
                : tblPagos.getScene().getWindow();
        VistaPreviaReporte.generarYMostrar(btnGenerarReporte, ventana,
                () -> reporteService.generarDocumentoPagos(pagosParaReporte));
    }

    @FXML
    public void handleVerRecibo() {
        abrirRecibo(pagoSeleccionado);
    }

    private void abrirRecibo(PagoListaResponse pago) {
        if (pago == null || pago.id() == null || pago.id().isBlank()) {
            AlertaUtil.mostrarAdvertencia("Selección requerida",
                    "Selecciona un pago para consultar su recibo.");
            return;
        }
        UUID pagoId;
        try {
            pagoId = UUID.fromString(pago.id());
        } catch (IllegalArgumentException ex) {
            AlertaUtil.mostrarAdvertencia("Identificador inválido",
                    "El pago seleccionado no tiene un identificador válido.");
            return;
        }
        Window ventana = tblPagos == null || tblPagos.getScene() == null
                ? null
                : tblPagos.getScene().getWindow();
        VistaPreviaReporte.generarYMostrar(btnVerRecibo, ventana,
                () -> reporteService.generarDocumentoRecibo(pagoId));
    }

    private Pago mapearPagoDesdeLista(PagoListaResponse item) {
        Pago pago = new Pago();
        if (item.id() != null) {
            try {
                pago.setId(UUID.fromString(item.id()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        pago.setContratoInfo(item.contratoInfo());
        if (item.fechaPago() != null && !item.fechaPago().isBlank()) {
            try {
                pago.setFechaPago(LocalDate.parse(item.fechaPago()));
            } catch (Exception ignored) {
            }
        }
        String formateado = item.montoFormateado();
        if (formateado != null && !formateado.isBlank()) {
            String[] partes = formateado.strip().split("\\s+");
            if (partes.length >= 1) {
                try {
                    BigDecimal monto = parsearMontoTexto(partes[0]);
                    pago.setMontoOriginal(monto);
                    pago.setMontoContrato(monto);
                } catch (NumberFormatException ignored) {
                }
            }
            if (partes.length >= 2) {
                pago.setMonedaOriginal(partes[1]);
            }
        }
        pago.setMetodo(item.metodo());
        pago.setReferencia(item.referencia());
        pago.setEstado(item.estado());
        return pago;
    }

    private static BigDecimal parsearMontoTexto(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String limpio = raw.strip().replaceAll("[^0-9.,-]", "");
        if (limpio.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int ultimaComa = limpio.lastIndexOf(',');
        int ultimoPunto = limpio.lastIndexOf('.');

        if (ultimaComa >= 0 && ultimoPunto >= 0) {
            // Si la coma está después del punto, la coma es decimal (ej. 1.250,00)
            if (ultimaComa > ultimoPunto) {
                limpio = limpio.replace(".", "").replace(",", ".");
            } else {
                // Si el punto está después de la coma, la coma es separador de miles (ej. 1,250.00)
                limpio = limpio.replace(",", "");
            }
        } else if (ultimaComa >= 0) {
            // Solo hay coma: si tiene 1 o 2 dígitos al final es decimal (ej. 1000,00 -> 1000.00)
            int decimales = limpio.length() - ultimaComa - 1;
            if (decimales <= 2) {
                limpio = limpio.replace(",", ".");
            } else {
                limpio = limpio.replace(",", "");
            }
        }
        return new BigDecimal(limpio);
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
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, detalle -> {
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
        lblErrorConsulta.setText("No se pudo actualizar el historial. " + mensajeErrorPago(error));
        lblErrorConsulta.setVisible(true);
        lblErrorConsulta.setManaged(true);
    }

    private String mensajeErrorPago(Throwable error) {
        if (error instanceof ErrorClienteException clienteError) {
            if (clienteError.esSinConexion() || clienteError.getEstado() >= 500) {
                return clienteError.getMessage() + " Si el resultado es incierto, reintenta sin cambiar los datos.";
            }
            return clienteError.getMessage();
        }
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException
                || error instanceof SecurityException) return error.getMessage();
        return "Revisa los datos y vuelve a intentarlo.";
    }
}