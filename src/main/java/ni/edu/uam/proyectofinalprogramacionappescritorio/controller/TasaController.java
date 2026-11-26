package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ReferenciaTasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.FormatoTasa;
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
import java.util.function.Supplier;

/** Registro informativo y autorización de tasas NIO por USD vía HTTP. */
public class TasaController {
    private static final DateTimeFormatter FECHA_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INSTANTE_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ConstantesLatch.ZONA_OPERATIVA);

    @FXML private VBox rootView;
    @FXML private HBox autorizacionTooltipHost;
    @FXML private Button btnRegistrar;
    @FXML private Button btnAutorizar;
    @FXML private Button btnEditar;
    @FXML private CheckBox chkHistorial;
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
    private final ClienteTasas cliente;
    private TasaResponse seleccionada;
    private final boolean permisoAutorizar;
    private RecargadorVista recargadorTasas;
    private RecargadorVista recargadorReferencia;
    private BarraErrorContextual barraError;

    public TasaController() { this(ClienteLatch.tasas()); }

    public TasaController(ClienteTasas cliente) {
        this.cliente = cliente;
        this.permisoAutorizar = SesionCliente.getActual().esPropietario();
    }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.TASA, 20));
        recargadorTasas = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        recargadorReferencia = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
        Iconos.asignar(btnRegistrar, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnAutorizar, Iconos.Nombre.CONFIRMACION, 16);
        Tooltip.install(autorizacionTooltipHost, new Tooltip(permisoAutorizar
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
                estadoVisible(cell.getValue())));
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
        chkHistorial.selectedProperty().addListener((obs, anterior, actual) -> {
            tasasCargadas.clear();
            tasasVisibles.clear();
            actualizarSeleccion(null);
            tblTasas.setPlaceholder(PanelEstadoTabla.crear("Cargando tasas", null, null, null));
            cargarTasas();
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleRegistrar);
        cargarTasas();
        recargadorTasas.activarConsultaPeriodica(Duration.seconds(60), () -> {
            recargadorTasas.recargarSiInactivo(
                    consultaTasas(),
                    this::aplicarTasas,
                    this::manejarErrorTasas);
            consultarReferenciaSiInactivo();
        });
    }

    public void cargarTasas() {
        recargadorTasas.recargar(
                consultaTasas(),
                this::aplicarTasas,
                this::manejarErrorTasas);
        consultarReferencia();
    }

    private Supplier<List<TasaResponse>> consultaTasas() {
        boolean operativa = !chkHistorial.isSelected();
        return () -> cliente.listar(null, null, 200, operativa);
    }

    private String estadoVisible(TasaResponse tasa) {
        if (tasa.autorizada()) return "Autorizada";
        return chkHistorial.isSelected() ? "Propuesta" : "Pendiente";
    }

    private void aplicarTasas(List<TasaResponse> lista) {
        tasasCargadas.clear();
        tasasCargadas.addAll(lista);
        aplicarFiltro();
        barraError.ocultar();
    }

    private void manejarErrorTasas(Throwable error) {
        mostrarErrorConsulta(error);
        if (tasasCargadas.isEmpty()) {
            tblTasas.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar las tasas",
                    "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarTasas));
        }
    }

    private void consultarReferencia() {
        recargadorReferencia.recargar(
                cliente::referencia,
                this::aplicarReferencia,
                error -> lblReferencia.setText("No se pudo consultar la referencia comercial."));
    }

    private void consultarReferenciaSiInactivo() {
        recargadorReferencia.recargarSiInactivo(
                cliente::referencia,
                this::aplicarReferencia,
                error -> lblReferencia.setText("No se pudo consultar la referencia comercial."));
    }

    private void aplicarReferencia(ReferenciaTasaResponse dato) {
        lblReferencia.setText(textoReferencia(dato));
        if (dato != null && "CONSULTANDO".equals(dato.estado())) {
            recargadorReferencia.recargarAgrupado(
                    Duration.seconds(5),
                    cliente::referencia,
                    this::aplicarReferencia,
                    error -> lblReferencia.setText("No se pudo consultar la referencia comercial."));
        }
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

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }

    private void aplicarFiltro() {
        String filtro = txtBuscar.getText() == null ? "" : txtBuscar.getText().strip().toLowerCase(Locale.ROOT);
        List<TasaResponse> resultado = tasasCargadas.stream().filter(tasa -> {
            if (filtro.isEmpty()) return true;
            return (tasa.fecha() + " " + tasa.nioPorUsd() + " " + tasa.fuente() + " "
                    + estadoVisible(tasa) + " "
                    + tasa.autorizadoPorNombre() + " " + tasa.motivo()).toLowerCase(Locale.ROOT).contains(filtro);
        }).toList();
        RecargadorVista.conservarSeleccionPorId(tblTasas,
                chkHistorial.isSelected() ? TasaResponse::id : TasaResponse::fecha,
                () -> tasasVisibles.setAll(resultado));
        lblContador.setText(resultado.size() + (resultado.size() == 1 ? " tasa" : " tasas"));
        if (tblTasas.getSelectionModel().getSelectedItem() == null && seleccionada != null) {
            actualizarSeleccion(null);
        }
        boolean puedeRegistrar = filtro.isEmpty() && !chkHistorial.isSelected();
        tblTasas.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay tasas" : "No hay coincidencias",
                filtro.isEmpty() ? "Las tasas registradas aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                puedeRegistrar ? "Registrar tasa" : filtro.isEmpty() ? null : "Limpiar búsqueda",
                puedeRegistrar ? this::handleRegistrar : filtro.isEmpty() ? null : this::limpiarFiltros));
    }

    @FXML public void handleRegistrar() {
        if (!chkHistorial.isSelected()) abrirFormularioPropuesta(null);
    }

    @FXML public void handleEditar() {
        if (puedeEditarSeleccion()) abrirFormularioPropuesta(seleccionada);
    }

    private void abrirFormularioPropuesta(TasaResponse propuesta) {
        boolean editando = propuesta != null;
        String titulo = editando ? "Editar propuesta" : "Registrar propuesta";
        String textoAccion = editando ? "Guardar cambios" : "Registrar tasa";
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("tasa-form.fxml"));
            Node contenido = loader.load();
            TasaFormController formulario = loader.getController();
            if (editando) formulario.mostrarEdicion(propuesta);
            else formulario.mostrarRegistro();
            Window propietario = tblTasas.getScene() == null ? null : tblTasas.getScene().getWindow();
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(titulo, contenido,
                    propietario, 720, 620, textoAccion);
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, editando ? btnEditar : btnRegistrar,
                    formulario::hayCambios);
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
                        return editando
                                ? cliente.editar(propuesta.id(), datos.fecha(), datos.tasa(), datos.fuente())
                                : cliente.registrar(datos.fecha(), datos.tasa(), datos.fuente());
                    }
                };
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, registro -> {
                    formulario.marcarLimpio();
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito(editando ? "Propuesta actualizada" : "Propuesta registrada", "Tasa del "
                            + fechaVisible(registro.fecha()) + ": "
                            + FormatoTasa.formatear(new BigDecimal(registro.nioPorUsd())) + " NIO/USD."
                            + " Fuente: " + registro.fuente() + ". Pendiente de autorización.");
                    cargarTasas();
                }, error -> manejarErrorFormulario(error, formulario, accion, textoAccion));
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleAutorizar() {
        TasaResponse tasa = seleccionada;
        if (!permisoAutorizar || !puedeEditarSeleccion()) return;
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
                boolean fueraDeBanda = valor.compareTo(ConstantesLatch.BANDA_TASA_MINIMA) < 0
                        || valor.compareTo(ConstantesLatch.BANDA_TASA_MAXIMA) > 0;
                if (fueraDeBanda) {
                    String banda = FormatoTasa.formatear(ConstantesLatch.BANDA_TASA_MINIMA) + "–"
                            + FormatoTasa.formatear(ConstantesLatch.BANDA_TASA_MAXIMA) + " NIO/USD";
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
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, autorizada -> {
                    formulario.marcarLimpio();
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito("Tasa autorizada", "La tasa del "
                            + fechaVisible(autorizada.fecha()) + " quedó autorizada con valor "
                            + FormatoTasa.formatear(new BigDecimal(autorizada.nioPorUsd())) + " NIO/USD.");
                    cargarTasas();
                }, error -> manejarErrorFormulario(error, formulario, accion, "Autorizar tasa"));
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    private void actualizarSeleccion(TasaResponse tasa) {
        seleccionada = tasa;
        if (tasa == null) {
            lblSeleccion.setText("");
            lblSeleccion.setVisible(false);
            lblSeleccion.setManaged(false);
        } else {
            lblSeleccion.setText(estadoVisible(tasa) + " · " + fechaVisible(tasa.fecha()) + " · fuente " + tasa.fuente());
            lblSeleccion.setVisible(true);
            lblSeleccion.setManaged(true);
        }
        actualizarControles();
    }

    private boolean puedeEditarSeleccion() {
        return !chkHistorial.isSelected() && seleccionada != null && !seleccionada.autorizada();
    }

    private void actualizarControles() {
        btnRegistrar.setDisable(chkHistorial.isSelected());
        btnEditar.setDisable(!puedeEditarSeleccion());
        btnAutorizar.setDisable(!puedeEditarSeleccion() || !permisoAutorizar);
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
        if (error instanceof ErrorClienteException clienteError && clienteError.getEstado() == 409) cargarTasas();
    }

    private void mostrarErrorConsulta(Throwable error) {
        barraError.mostrar(mensajeError(error), this::cargarTasas);
    }

    private String mensajeError(Throwable error) {
        return MensajesUi.traducir(error, "No se pudo completar la solicitud. Revisa los datos e inténtalo de nuevo.");
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
