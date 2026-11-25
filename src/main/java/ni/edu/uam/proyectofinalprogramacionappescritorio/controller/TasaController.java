package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.SimpleStringProperty;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ReferenciaTasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTasa;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.InsigniaEstado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Registro informativo y autorización de tasas NIO por USD. */
public class TasaController {
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INSTANTE_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(RelojService.ZONA_OPERATIVA);

    @FXML private VBox rootView;
    @FXML private HBox autorizacionTooltipHost;
    @FXML private Button btnRegistrar;
    @FXML private Button btnAutorizar;
    @FXML private Button btnRefrescar;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnLimpiar;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private Label lblSeleccion;
    @FXML private Label lblTitulo;
    @FXML private Label lblReferencia;
    @FXML private TextField txtBuscar;
    @FXML private TableView<TasaResponse> tblTasas;
    @FXML private TableColumn<TasaResponse, String> colFecha;
    @FXML private TableColumn<TasaResponse, String> colTasa;
    @FXML private TableColumn<TasaResponse, String> colFuente;
    @FXML private TableColumn<TasaResponse, String> colEstado;
    @FXML private TableColumn<TasaResponse, String> colAutorizadoPor;
    @FXML private TableColumn<TasaResponse, String> colRegistrado;
    @FXML private TableColumn<TasaResponse, String> colMotivo;
    @FXML private TableColumn<TasaResponse, Void> colAcciones;

    private final ObservableList<TasaResponse> tasasVisibles = FXCollections.observableArrayList();
    private final List<TasaResponse> tasasCargadas = new ArrayList<>();
    private final ReporteService reporteService = new ReporteService();
    private final ClienteTasas cliente;
    private TasaResponse seleccionada;
    private final boolean permisoAutorizar;
    private int operacionesEnCurso;
    private long versionConsulta;

    public TasaController() { this(new ClienteTasas(ClienteLatch.api())); }

    public TasaController(ClienteTasas cliente) {
        this.cliente = cliente;
        this.permisoAutorizar = SesionUsuario.esPropietario();
    }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.TASA, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnGenerarReporte != null) {
            Iconos.asignar(btnGenerarReporte, Iconos.Nombre.REPORTE, 16);
            btnGenerarReporte.setTooltip(new Tooltip("Vista previa y exportación PDF del historial de tasas de cambio"));
        }
        Iconos.asignar(btnRegistrar, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnAutorizar, Iconos.Nombre.CONFIRMACION, 16);
        boolean puedeAutorizar = SesionUsuario.esPropietario();
        Tooltip.install(autorizacionTooltipHost, new Tooltip(puedeAutorizar
                ? "Selecciona una tasa informativa pendiente." : "Solo el propietario puede autorizar tasas."));
        btnAutorizar.setDisable(true);
        tblTasas.setItems(tasasVisibles);
        tblTasas.setPlaceholder(PanelEstadoTabla.crear("Cargando tasas",
                "Las tasas aparecerán al terminar la consulta.", null, null));
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(fechaVisible(cell.getValue().fecha())));
        colTasa.setCellValueFactory(cell -> new SimpleStringProperty(
                FormatoTasa.formatear(new BigDecimal(cell.getValue().nioPorUsd()))));
        colTasa.setCellFactory(FormatoTabla.numerica());
        colFuente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fuente()));
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().autorizada() ? "Autorizada" : "Pendiente de autorización"));
        colEstado.setCellFactory(FormatoTabla.estado());
        colAutorizadoPor.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().autorizadoPorNombre() == null ? "—" : cell.getValue().autorizadoPorNombre()));
        colRegistrado.setCellValueFactory(cell -> new SimpleStringProperty(instanteVisible(cell.getValue().registradoEn())));
        colMotivo.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().motivo() == null ? "—" : cell.getValue().motivo()));
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>("Seleccionar tasa",
                tasa -> tblTasas.getSelectionModel().select(tasa), Iconos.Nombre.CONFIRMACION)));
        tblTasas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> actualizarSeleccion(actual));
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro());
        AtajosLista.instalar(rootView, txtBuscar, this::handleRegistrar);
        cargarTasas();
    }

    @FXML public void cargarTasas() {
        long version = ++versionConsulta;
        Task<List<TasaResponse>> tarea = new Task<>() {
            @Override protected List<TasaResponse> call() { return cliente.listar(null, null, 200); }
        };
        ejecutar(tarea, lista -> {
            if (version != versionConsulta) return;
            tasasCargadas.clear();
            tasasCargadas.addAll(lista);
            aplicarFiltro();
            lblErrorConsulta.setText("");
            lblErrorConsulta.setVisible(false);
            lblErrorConsulta.setManaged(false);
        }, error -> {
            if (version == versionConsulta) {
                mostrarErrorConsulta(error);
                tblTasas.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar las tasas",
                        "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarTasas));
            }
        });
        Task<ReferenciaTasaResponse> referencia = new Task<>() {
            @Override protected ReferenciaTasaResponse call() { return cliente.referencia(); }
        };
        ejecutar(referencia, dato -> {
            if (version == versionConsulta) lblReferencia.setText(textoReferencia(dato));
        }, error -> {
            if (version == versionConsulta) lblReferencia.setText("No se pudo consultar la referencia comercial.");
        });
    }

    private static String textoReferencia(ReferenciaTasaResponse dato) {
        if (dato == null) return "Sin referencia comercial disponible.";
        String aviso = switch (dato.aviso() == null ? "" : dato.aviso()) {
            case "CONFIGURACION" -> " · Actualización suspendida por credenciales";
            case "CUOTA_AGOTADA", "LIMITE_MENSUAL" -> " · Cuota de consultas agotada";
            case "CUOTA_TEMPORAL" -> " · Proveedor limitó las consultas temporalmente";
            case "TRANSITORIO", "RESPUESTA_INVALIDA", "FUERA_DE_BANDA" -> " · Falló la última actualización";
            default -> "";
        };
        if ("CONSULTANDO".equals(dato.estado())) return "Consultando la referencia comercial" + aviso;
        if (dato.nioPorUsd() == null) return "Sin referencia comercial disponible" + aviso;
        long segundos = dato.antiguedadSegundos();
        String antiguedad = segundos < 60 ? "menos de 1 min"
                : segundos < 3600 ? segundos / 60 + " min"
                : segundos < 86400 ? segundos / 3600 + " h" : segundos / 86400 + " d";
        String estado = "HISTORICA".equals(dato.estado()) ? "Referencia informativa histórica" : "Referencia informativa";
        return estado + " · " + FormatoTasa.formatear(new BigDecimal(dato.nioPorUsd())) + " NIO por USD"
                + " · " + dato.fuente() + " · Fecha efectiva " + fechaVisible(dato.fechaEfectiva())
                + " · Consulta " + instanteVisible(dato.consultadaEn()) + " (hace " + antiguedad + ")" + aviso;
    }

    @FXML public void refrescarTabla() { cargarTasas(); }

    @FXML public void handleGenerarReporte() {
        List<TasaResponse> tasasActuales = tblTasas == null
                ? List.of()
                : List.copyOf(tblTasas.getItems());
        if (tasasActuales.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos",
                    "No hay tasas en la tabla para generar el reporte.");
            return;
        }
        Window ventana = tblTasas == null || tblTasas.getScene() == null
                ? null
                : tblTasas.getScene().getWindow();
        VistaPreviaReporte.generarYMostrar(btnGenerarReporte, ventana,
                () -> reporteService.generarDocumentoTasasCambio(tasasActuales));
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }

    private void aplicarFiltro() {
        String filtro = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);
        List<TasaResponse> resultado = tasasCargadas.stream().filter(tasa -> {
            if (filtro.isEmpty()) return true;
            return (tasa.fecha() + " " + tasa.nioPorUsd() + " " + tasa.fuente() + " "
                    + (tasa.autorizada() ? "Autorizada" : "Pendiente de autorización") + " "
                    + tasa.autorizadoPorNombre() + " " + tasa.motivo()).toLowerCase(Locale.ROOT).contains(filtro);
        }).toList();
        tasasVisibles.setAll(resultado);
        lblContador.setText(resultado.size() + (resultado.size() == 1 ? " tasa" : " tasas"));
        if (seleccionada != null && !resultado.contains(seleccionada)) {
            tblTasas.getSelectionModel().clearSelection();
            actualizarSeleccion(null);
        }
        tblTasas.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay tasas" : "No hay coincidencias",
                filtro.isEmpty() ? "Las tasas registradas aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? "Registrar tasa" : "Limpiar búsqueda",
                filtro.isEmpty() ? this::handleRegistrar : this::limpiarFiltros));
    }

    @FXML public void handleRegistrar() { abrirFormularioRegistro(); }

    private void abrirFormularioRegistro() {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("tasa-form.fxml"));
            Node contenido = loader.load();
            TasaFormController formulario = loader.getController();
            formulario.mostrarRegistro();
            Window propietario = tblTasas.getScene() == null ? null : tblTasas.getScene().getWindow();
            Dialog<ButtonType> dialogo = DialogoFormulario.crear("Registrar tasa informativa", contenido,
                    propietario, 720, 620, "Registrar tasa");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnRegistrar, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validarRegistro()) return;
                TasaFormController.Datos datos = formulario.datos();
                accion.setDisable(true);
                accion.setText("Guardando tasa…");
                Task<TasaResponse> tarea = new Task<>() {
                    @Override protected TasaResponse call() {
                        return cliente.registrar(datos.fecha(), datos.tasa(), datos.fuente().strip());
                    }
                };
                ejecutar(tarea, registro -> {
                    formulario.marcarLimpio();
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito("Tasa informativa registrada", "La tasa del "
                            + fechaVisible(registro.fecha()) + " quedó registrada: "
                            + FormatoTasa.formatear(new BigDecimal(registro.nioPorUsd())) + " NIO/USD."
                            + " Fuente: " + registro.fuente() + ". Pendiente de autorización.");
                    cargarTasas();
                }, error -> manejarErrorFormulario(error, formulario, accion,
                        "Registrar tasa"));
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleAutorizar() {
        TasaResponse tasa = seleccionada;
        if (tasa == null || !permisoAutorizar || tasa.autorizada()) return;
        abrirFormularioAutorizacion(tasa);
    }

    private void abrirFormularioAutorizacion(TasaResponse tasa) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("tasa-form.fxml"));
            Node contenido = loader.load();
            TasaFormController formulario = loader.getController();
            formulario.mostrarAutorizacion(tasa);
            Window propietario = tblTasas.getScene() == null ? null : tblTasas.getScene().getWindow();
            Dialog<ButtonType> dialogo = DialogoFormulario.crear("Autorizar tasa", contenido,
                    propietario, 720, 620, "Autorizar tasa");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnAutorizar, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            formulario.enlazarMotivoConAccion(accion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validarMotivo()) return;
                String motivo = formulario.motivo();
                BigDecimal valor = new BigDecimal(tasa.nioPorUsd());
                boolean fueraDeBanda = valor.compareTo(TipoCambio.BANDA_MINIMA) < 0
                        || valor.compareTo(TipoCambio.BANDA_MAXIMA) > 0;
                if (fueraDeBanda) {
                    String banda = FormatoTasa.formatear(TipoCambio.BANDA_MINIMA) + "–"
                            + FormatoTasa.formatear(TipoCambio.BANDA_MAXIMA) + " NIO/USD";
                    String detalle = "Tasa del " + fechaVisible(tasa.fecha()) + ": "
                            + FormatoTasa.formatear(valor) + " NIO/USD. Fuente: " + tasa.fuente()
                            + ". Banda de referencia: " + banda + ".\nMotivo: " + motivo;
                    if (!AlertaUtil.mostrarConfirmacion("Confirmar tasa fuera de banda", detalle,
                            "Autorizar tasa")) return;
                } else if (!AlertaUtil.mostrarConfirmacion("Autorizar tasa",
                        "La tasa del " + fechaVisible(tasa.fecha()) + " de "
                                + FormatoTasa.formatear(valor) + " NIO/USD, fuente " + tasa.fuente()
                                + ", quedará disponible para pagos. Motivo: " + motivo,
                        "Autorizar tasa")) return;

                accion.setDisable(true);
                accion.setText("Autorizando tasa…");
                Task<TasaResponse> tarea = new Task<>() {
                    @Override protected TasaResponse call() {
                        return cliente.autorizar(tasa.id(), motivo.strip(), fueraDeBanda);
                    }
                };
                ejecutar(tarea, autorizada -> {
                    formulario.marcarLimpio();
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito("Tasa autorizada", "La tasa del "
                            + fechaVisible(autorizada.fecha()) + " quedó autorizada con valor "
                            + FormatoTasa.formatear(new BigDecimal(autorizada.nioPorUsd())) + " NIO/USD.");
                    cargarTasas();
                }, error -> manejarErrorFormulario(error, formulario, accion,
                        "Autorizar tasa"));
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    private <T> void ejecutar(Task<T> tarea, Consumer<T> exito, Consumer<Throwable> fallo) {
        iniciarOperacion();
        tarea.setOnSucceeded(evento -> {
            try { exito.accept(tarea.getValue()); }
            finally { terminarOperacion(); }
        });
        tarea.setOnFailed(evento -> {
            terminarOperacion();
            fallo.accept(tarea.getException());
        });
        tarea.setOnCancelled(evento -> terminarOperacion());
        try {
            LatchApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException error) {
            terminarOperacion();
            fallo.accept(error);
        }
    }

    private void iniciarOperacion() {
        operacionesEnCurso++;
        actualizarControles();
    }

    private void terminarOperacion() {
        operacionesEnCurso = Math.max(0, operacionesEnCurso - 1);
        actualizarControles();
    }

    private void actualizarSeleccion(TasaResponse tasa) {
        seleccionada = tasa;
        if (tasa == null) {
            lblSeleccion.setText("");
            lblSeleccion.setVisible(false);
            lblSeleccion.setManaged(false);
        } else {
            lblSeleccion.setText(tasa.autorizada()
                    ? "Autorizada · " + fechaVisible(tasa.fecha()) + " · fuente " + tasa.fuente()
                    : "Pendiente de autorización · " + fechaVisible(tasa.fecha()) + " · fuente " + tasa.fuente());
            lblSeleccion.setVisible(true);
            lblSeleccion.setManaged(true);
        }
        actualizarControles();
    }

    private void actualizarControles() {
        boolean ocupado = operacionesEnCurso > 0;
        btnRegistrar.setDisable(ocupado);
        btnRefrescar.setDisable(ocupado);
        if (btnGenerarReporte != null) {
            btnGenerarReporte.setDisable(ocupado);
        }
        btnLimpiar.setDisable(ocupado);
        btnAutorizar.setDisable(ocupado || seleccionada == null || seleccionada.autorizada() || !permisoAutorizar);
        if (seleccionada != null && !seleccionada.autorizada() && !permisoAutorizar) {
            Tooltip.install(autorizacionTooltipHost, new Tooltip("Solo el propietario puede autorizar tasas."));
        }
    }

    private void manejarErrorFormulario(Throwable error,
                                        TasaFormController formulario, Button accion,
                                        String textoAccion) {
        accion.setDisable(false);
        accion.setText(textoAccion);
        if (error instanceof ErrorClienteException clienteError && clienteError.esNoAutenticado()) {
            formulario.mostrarError("Tu sesión expiró.");
            return;
        }
        formulario.mostrarError(mensajeError(error));
    }

    private void mostrarErrorConsulta(Throwable error) {
        lblErrorConsulta.setText(mensajeError(error));
        lblErrorConsulta.setVisible(true);
        lblErrorConsulta.setManaged(true);
    }

    private String mensajeError(Throwable error) {
        if (error instanceof ErrorClienteException clienteError && clienteError.esSinConexion()) {
            return "No se pudo conectar con el servicio de Latch. Revisa la conexión e inténtalo de nuevo.";
        }
        if (error instanceof ErrorClienteException clienteError && clienteError.esNoAutenticado()) {
            return "Tu sesión expiró.";
        }
        return "No se pudo completar la solicitud. Revisa los datos e inténtalo de nuevo.";
    }

    private static String fechaVisible(String fecha) {
        try { return LocalDate.parse(fecha).format(FECHA_VISIBLE); }
        catch (DateTimeParseException ignorado) { return fecha; }
    }

    private static String instanteVisible(String instante) {
        try { return INSTANTE_VISIBLE.format(OffsetDateTime.parse(instante).toInstant()); }
        catch (DateTimeParseException ignorado) { return instante; }
    }
}
