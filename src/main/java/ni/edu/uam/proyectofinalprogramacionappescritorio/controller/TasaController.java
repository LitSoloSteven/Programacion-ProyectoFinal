package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTasa;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla de tasas NIO por USD. Toda la comunicación pasa por la API local y
 * se ejecuta en segundo plano para no bloquear el hilo de JavaFX.
 */
public class TasaController {
    private static final DateTimeFormatter FECHA_VISIBLE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INSTANTE_VISIBLE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());

    @FXML private DatePicker dpFecha;
    @FXML private TextField txtTasa;
    @FXML private TextField txtFuente;
    @FXML private TextField txtMotivo;
    @FXML private Button btnRegistrar;
    @FXML private Button btnAutorizar;
    @FXML private Button btnRefrescar;
    @FXML private Label lblSeleccion;
    @FXML private TableView<TasaResponse> tblTasas;
    @FXML private TableColumn<TasaResponse, String> colFecha;
    @FXML private TableColumn<TasaResponse, String> colTasa;
    @FXML private TableColumn<TasaResponse, String> colFuente;
    @FXML private TableColumn<TasaResponse, String> colEstado;
    @FXML private TableColumn<TasaResponse, String> colAutorizadoPor;
    @FXML private TableColumn<TasaResponse, String> colRegistrado;
    @FXML private TableColumn<TasaResponse, String> colMotivo;

    private final ObservableList<TasaResponse> tasas = FXCollections.observableArrayList();
    private final ClienteTasas cliente = new ClienteTasas(ClienteLatch.api());
    private TasaResponse seleccionada;
    private int operacionesEnCurso;

    @FXML
    public void initialize() {
        configurarTabla();
        dpFecha.setValue(LocalDate.now());
        boolean propietario = SesionUsuario.esPropietario();
        btnAutorizar.setVisible(propietario);
        btnAutorizar.setManaged(propietario);
        actualizarSeleccion(null);
        cargarTasas();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(cd ->
                new SimpleStringProperty(fechaVisible(cd.getValue().fecha())));
        colTasa.setCellValueFactory(cd ->
                new SimpleStringProperty(FormatoTasa.formatear(
                        new BigDecimal(cd.getValue().nioPorUsd()))));
        colFuente.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().fuente()));
        colEstado.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().autorizada()
                        ? "AUTORIZADA" : "INFORMATIVA"));
        colAutorizadoPor.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().autorizadoPorNombre() == null
                        ? "—" : cd.getValue().autorizadoPorNombre()));
        colRegistrado.setCellValueFactory(cd ->
                new SimpleStringProperty(instanteVisible(cd.getValue().registradoEn())));
        colMotivo.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().motivo() == null ? "—" : cd.getValue().motivo()));

        tblTasas.setItems(tasas);
        tblTasas.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, actual) -> actualizarSeleccion(actual));
    }

    @FXML
    public void cargarTasas() {
        Task<List<TasaResponse>> tarea = new Task<>() {
            @Override
            protected List<TasaResponse> call() {
                return cliente.listar(null, null, 200);
            }
        };
        ejecutar(tarea, lista -> {
            tasas.setAll(lista);
            actualizarSeleccion(null);
        });
    }

    @FXML
    public void refrescarTabla() {
        cargarTasas();
    }

    @FXML
    public void limpiarFormulario() {
        dpFecha.setValue(LocalDate.now());
        txtTasa.clear();
        txtFuente.clear();
        txtMotivo.clear();
        tblTasas.getSelectionModel().clearSelection();
        actualizarSeleccion(null);
    }

    @FXML
    public void handleRegistrar() {
        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            AlertaUtil.mostrarAdvertencia("Fecha requerida",
                    "Seleccione la fecha a la que corresponde la tasa.");
            dpFecha.requestFocus();
            return;
        }
        if (fecha.isAfter(LocalDate.now())) {
            AlertaUtil.mostrarAdvertencia("Fecha inválida",
                    "La tasa no puede corresponder a una fecha futura.");
            return;
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(txtTasa.getText().strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            AlertaUtil.mostrarAdvertencia("Tasa inválida",
                    "Escriba la tasa como un número decimal, por ejemplo 36.75000000.");
            txtTasa.requestFocus();
            return;
        }
        if (valor.signum() <= 0) {
            AlertaUtil.mostrarAdvertencia("Tasa inválida",
                    "La tasa debe ser mayor que cero.");
            txtTasa.requestFocus();
            return;
        }
        String fuente = txtFuente.getText();
        if (fuente == null || fuente.isBlank()) {
            AlertaUtil.mostrarAdvertencia("Fuente requerida",
                    "Indique la fuente de la tasa, por ejemplo BCN.");
            txtFuente.requestFocus();
            return;
        }

        Task<TasaResponse> tarea = new Task<>() {
            @Override
            protected TasaResponse call() {
                return cliente.registrar(fecha, valor, fuente.strip());
            }
        };
        ejecutar(tarea, registro -> {
            AlertaUtil.mostrarExito("Tasa registrada",
                    "La tasa informativa quedó registrada. Un propietario debe autorizarla "
                            + "antes de usarla en pagos.");
            limpiarFormulario();
            cargarTasas();
        });
    }

    @FXML
    public void handleAutorizar() {
        if (seleccionada == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida",
                    "Seleccione una tasa informativa de la tabla.");
            return;
        }
        if (seleccionada.autorizada()) {
            AlertaUtil.mostrarAdvertencia("Tasa ya autorizada",
                    "La tasa seleccionada ya está autorizada.");
            return;
        }
        String motivo = txtMotivo.getText();
        if (motivo == null || motivo.isBlank()) {
            AlertaUtil.mostrarAdvertencia("Motivo requerido",
                    "Explique brevemente por qué autoriza esta tasa.");
            txtMotivo.requestFocus();
            return;
        }
        boolean confirmar = AlertaUtil.mostrarConfirmacion("Autorizar tasa",
                "La tasa del " + fechaVisible(seleccionada.fecha())
                        + " quedará disponible para convertir pagos. ¿Desea continuar?");
        if (!confirmar) return;

        Task<TasaResponse> tarea = new Task<>() {
            @Override
            protected TasaResponse call() {
                return cliente.autorizar(seleccionada.id(), motivo.strip());
            }
        };
        ejecutar(tarea, autorizada -> {
            AlertaUtil.mostrarExito("Tasa autorizada",
                    "La tasa quedó autorizada y disponible para pagos.");
            txtMotivo.clear();
            cargarTasas();
        });
    }

    private <T> void ejecutar(Task<T> tarea, Consumer<T> alExito) {
        iniciarOperacion();
        tarea.setOnSucceeded(evento -> {
            try {
                alExito.accept(tarea.getValue());
            } finally {
                terminarOperacion();
            }
        });
        tarea.setOnFailed(evento -> {
            terminarOperacion();
            mostrarError(tarea.getException());
        });
        tarea.setOnCancelled(evento -> terminarOperacion());
        try {
            HelloApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException e) {
            terminarOperacion();
            AlertaUtil.mostrarError("Servicio ocupado",
                    "No se pudo iniciar la operación. Inténtelo nuevamente.");
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

    private void actualizarSeleccion(TasaResponse actual) {
        seleccionada = actual;
        if (actual == null) {
            lblSeleccion.setText(
                    "Seleccione una tasa informativa de la tabla para autorizarla.");
        } else if (actual.autorizada()) {
            lblSeleccion.setText("Tasa autorizada el " + instanteVisible(actual.registradoEn())
                    + " por " + (actual.autorizadoPorNombre() == null
                    ? "un propietario" : actual.autorizadoPorNombre()) + ".");
        } else {
            lblSeleccion.setText("Tasa informativa del " + fechaVisible(actual.fecha())
                    + ". Escriba un motivo y autorícela.");
        }
        actualizarControles();
    }

    private void actualizarControles() {
        boolean ocupado = operacionesEnCurso > 0;
        boolean puedeAutorizar = !ocupado && seleccionada != null
                && !seleccionada.autorizada() && SesionUsuario.esPropietario();
        btnRegistrar.setDisable(ocupado);
        btnRefrescar.setDisable(ocupado);
        btnAutorizar.setDisable(!puedeAutorizar);
    }

    private void mostrarError(Throwable error) {
        String mensaje;
        if (error instanceof ErrorClienteException clienteError) {
            if (clienteError.esSinConexion()) {
                mensaje = "No se pudo conectar con el servicio local de Latch. "
                        + "Verifique que la aplicación siga iniciada.";
            } else if (clienteError.esNoAutenticado()) {
                mensaje = "La sesión expiró. Cierre sesión y vuelva a iniciar.";
            } else {
                mensaje = clienteError.getMessage();
            }
        } else {
            mensaje = "Ocurrió un error inesperado al comunicarse con el servicio local.";
        }
        AlertaUtil.mostrarError("No se pudo completar la operación", mensaje);
    }

    private static String fechaVisible(String valor) {
        try {
            return LocalDate.parse(valor).format(FECHA_VISIBLE);
        } catch (DateTimeParseException e) {
            return valor;
        }
    }

    private static String instanteVisible(String valor) {
        try {
            return INSTANTE_VISIBLE.format(Instant.parse(valor));
        } catch (DateTimeParseException e) {
            return valor;
        }
    }

    }