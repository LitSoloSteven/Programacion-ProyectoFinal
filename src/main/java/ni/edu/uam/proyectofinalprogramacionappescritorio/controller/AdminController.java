package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.adjuntos.dto.DocumentoAdjuntoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.AuditoriaRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.EstadoRespaldoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.ParametroHistorialResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.PerfilEmisorRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.PerfilEmisorResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.RestaurarRespaldoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.ResultadoRespaldoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.ResultadoRestauracionResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.ResultadoVerificacionRespaldoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.UsuarioResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAdmin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.ValidadorEntrada;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.BarraErrorContextual;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RecargadorVista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/** Controlador de administración de cuentas, parámetros/emisor, auditoría y respaldos vía API HTTP (M01/M12). */
public class AdminController {
    private static final DateTimeFormatter FORMATO_INSTANTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private VBox rootView;
    @FXML private Label lblTitulo;
    @FXML private TabPane tabPaneAdmin;
    @FXML private Tab tabCuentas;
    @FXML private Tab tabParametros;
    @FXML private Tab tabAuditoria;
    @FXML private Tab tabRespaldo;

    @FXML private Button btnCrearUsuario;
    @FXML private Button btnCambiarRol;
    @FXML private Button btnActivarDesactivar;
    @FXML private Button btnRestablecerClave;
    @FXML private Label lblErrorUsuarios;
    @FXML private TableView<UsuarioResponse> tblUsuarios;
    @FXML private TableColumn<UsuarioResponse, String> colUsuarioNombre;
    @FXML private TableColumn<UsuarioResponse, String> colUsuarioRol;
    @FXML private TableColumn<UsuarioResponse, String> colUsuarioEstado;
    @FXML private TableColumn<UsuarioResponse, String> colUsuarioCambioClave;

    @FXML private TextField txtEmisorNombre;
    @FXML private TextField txtEmisorIdentificacion;
    @FXML private TextField txtEmisorDireccion;
    @FXML private TextField txtEmisorTelefono;
    @FXML private TextField txtEmisorCorreo;
    @FXML private TextField txtEmisorMotivo;
    @FXML private Button btnGuardarEmisor;
    @FXML private Label lblEstadoEmisor;
    @FXML private Label lblLogotipoEstado;
    @FXML private Button btnAdjuntarLogotipo;
    @FXML private Button btnQuitarLogotipo;

    @FXML private TextField txtParamClave;
    @FXML private TextField txtParamValor;
    @FXML private TextField txtParamMotivo;
    @FXML private Button btnRegistrarParametro;
    @FXML private Label lblErrorParametro;
    @FXML private TableView<ParametroHistorialResponse> tblParametros;
    @FXML private TableColumn<ParametroHistorialResponse, String> colParamClave;
    @FXML private TableColumn<ParametroHistorialResponse, String> colParamValor;
    @FXML private TableColumn<ParametroHistorialResponse, String> colParamVigenteDesde;
    @FXML private TableColumn<ParametroHistorialResponse, String> colParamMotivo;
    @FXML private TableColumn<ParametroHistorialResponse, String> colParamCreadoPor;

    @FXML private TextField txtAuditoriaUsuario;
    @FXML private TextField txtAuditoriaAccion;
    @FXML private DatePicker dpAuditoriaDesde;
    @FXML private DatePicker dpAuditoriaHasta;
    @FXML private Button btnConsultarAuditoria;
    @FXML private Label lblErrorAuditoria;
    @FXML private TableView<AuditoriaRegistroResponse> tblAuditoria;
    @FXML private TableColumn<AuditoriaRegistroResponse, String> colAuditoriaInstante;
    @FXML private TableColumn<AuditoriaRegistroResponse, String> colAuditoriaActor;
    @FXML private TableColumn<AuditoriaRegistroResponse, String> colAuditoriaAccion;
    @FXML private TableColumn<AuditoriaRegistroResponse, String> colAuditoriaReferencia;
    @FXML private TableColumn<AuditoriaRegistroResponse, String> colAuditoriaCorrelacion;

    @FXML private Label lblEstadoRespaldo;
    @FXML private TextField txtCarpetaRespaldo;
    @FXML private Button btnElegirCarpetaRespaldo;
    @FXML private PasswordField pwdClaveRespaldo;
    @FXML private PasswordField pwdConfirmarClaveRespaldo;
    @FXML private Button btnCrearRespaldo;
    @FXML private TextField txtArchivoRespaldo;
    @FXML private Button btnElegirArchivoRespaldo;
    @FXML private PasswordField pwdClaveRestaurar;
    @FXML private Button btnVerificarRespaldo;
    @FXML private TextField txtDestinoJdbcRestaurar;
    @FXML private TextField txtDestinoUsuarioRestaurar;
    @FXML private PasswordField pwdDestinoClaveRestaurar;
    @FXML private TextField txtDestinoAdjuntosRestaurar;
    @FXML private Button btnElegirDestinoAdjuntos;
    @FXML private CheckBox chkModoMantenimiento;
    @FXML private Button btnRestaurarAislado;
    @FXML private Label lblErrorRespaldo;
    @FXML private Label lblResultadoRespaldo;

    private final ObservableList<UsuarioResponse> usuarios = FXCollections.observableArrayList();
    private final ObservableList<ParametroHistorialResponse> parametros = FXCollections.observableArrayList();
    private final ObservableList<AuditoriaRegistroResponse> auditorias = FXCollections.observableArrayList();

    private final ClienteAdmin admin = ClienteLatch.admin();
    private final ClienteAdjuntos adjuntos = ClienteLatch.adjuntos();

    private DocumentoAdjuntoResponse logotipoVigente;
    private RecargadorVista recargadorUsuarios;
    private RecargadorVista recargadorParametros;
    private RecargadorVista recargadorAuditoria;
    private RecargadorVista recargadorRespaldo;
    private BarraErrorContextual barraErrorUsuarios;
    private BarraErrorContextual barraErrorParametros;
    private BarraErrorContextual barraErrorAuditoria;
    private BarraErrorContextual barraErrorRespaldo;
    private boolean emisorCargado;
    private boolean emisorEditadoLocalmente;
    private boolean actualizandoEmisor;

    private record DatosParametros(PerfilEmisorResponse emisor,
                                  Optional<DocumentoAdjuntoResponse> logotipo,
                                  List<ParametroHistorialResponse> historial) { }

    public void mostrarParametros() {
        tabPaneAdmin.getSelectionModel().select(tabParametros);
    }

    @FXML
    public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PANEL, 20));
        var tareas = LatchApplication.instancia().getTareas();
        recargadorUsuarios = RecargadorVista.paraNodo(rootView, tareas);
        recargadorParametros = RecargadorVista.paraNodo(rootView, tareas);
        recargadorAuditoria = RecargadorVista.paraNodo(rootView, tareas);
        recargadorRespaldo = RecargadorVista.paraNodo(rootView, tareas);
        barraErrorUsuarios = BarraErrorContextual.envolver(lblErrorUsuarios);
        barraErrorParametros = BarraErrorContextual.envolver(lblErrorParametro);
        barraErrorAuditoria = BarraErrorContextual.envolver(lblErrorAuditoria);
        barraErrorRespaldo = BarraErrorContextual.envolver(lblErrorRespaldo);

        Iconos.asignar(btnCrearUsuario, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnCambiarRol, Iconos.Nombre.EDITAR, 16);
        Iconos.asignar(btnConsultarAuditoria, Iconos.Nombre.BUSCAR, 16);
        Iconos.asignar(btnCrearRespaldo, Iconos.Nombre.DESCARGAR, 16);
        Iconos.asignar(btnVerificarRespaldo, Iconos.Nombre.CONFIRMACION, 16);

        tblUsuarios.setItems(usuarios);
        tblUsuarios.setPlaceholder(PanelEstadoTabla.crear(
                "Cargando cuentas", "Los usuarios aparecerán al terminar la consulta.", null, null));
        colUsuarioNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colUsuarioRol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().rol()));
        colUsuarioEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activo() ? "Activo" : "Inactivo"));
        colUsuarioEstado.setCellFactory(FormatoTabla.estado());
        colUsuarioCambioClave.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().debeCambiarPassword() ? "Pendiente" : "Vigente"));
        colUsuarioCambioClave.setCellFactory(FormatoTabla.estado());
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, ant, actual) -> {
            boolean haySeleccion = actual != null;
            btnCambiarRol.setDisable(!haySeleccion);
            btnActivarDesactivar.setDisable(!haySeleccion);
            btnRestablecerClave.setDisable(!haySeleccion);
            if (haySeleccion) {
                btnActivarDesactivar.setText(actual.activo() ? "Desactivar" : "Activar");
            }
        });

        tblParametros.setItems(parametros);
        tblParametros.setPlaceholder(PanelEstadoTabla.crear(
                "Cargando parámetros", "El historial aparecerá al terminar la consulta.", null, null));
        colParamClave.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().clave()));
        colParamValor.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().valor()));
        colParamVigenteDesde.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearInstante(c.getValue().vigenteDesde())));
        colParamMotivo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().motivo()));
        colParamCreadoPor.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().creadoPor()));

        tblAuditoria.setItems(auditorias);
        tblAuditoria.setPlaceholder(PanelEstadoTabla.crear(
                "Cargando auditoría", "Los eventos aparecerán al terminar la consulta.", null, null));
        colAuditoriaInstante.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearInstante(c.getValue().instante())));
        colAuditoriaActor.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().actor() == null ? "Sistema / Anónimo" : c.getValue().actor()));
        colAuditoriaAccion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().accion()));
        colAuditoriaReferencia.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().referencia()));
        colAuditoriaCorrelacion.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().correlacion() == null ? "" : c.getValue().correlacion()));

        configurarListenersAdmin();
        cargarPestanaActual();
        recargadorRespaldo.activarConsultaPeriodica(Duration.seconds(60), () -> {
            if (tabPaneAdmin != null && tabPaneAdmin.getSelectionModel().getSelectedItem() == tabRespaldo) {
                recargadorRespaldo.recargarSiInactivo(
                        admin::consultarEstadoAlertaRespaldo,
                        this::aplicarEstadoRespaldo,
                        this::manejarErrorRespaldo);
            }
        });
    }

    private void configurarListenersAdmin() {
        for (TextField campo : List.of(
                txtEmisorNombre, txtEmisorIdentificacion, txtEmisorDireccion, txtEmisorTelefono, txtEmisorCorreo)) {
            campo.textProperty().addListener((obs, ant, nuevo) -> {
                if (!actualizandoEmisor && emisorCargado) {
                    emisorEditadoLocalmente = true;
                }
            });
        }
        ValidacionFormulario.vincularTexto(txtEmisorNombre, () -> {
            if (!actualizandoEmisor && emisorCargado) validarEmisorInline();
        });
        ValidacionFormulario.vincularTexto(txtEmisorTelefono, () -> {
            if (!actualizandoEmisor && emisorCargado) validarEmisorInline();
        });
        ValidacionFormulario.vincularTexto(txtEmisorCorreo, () -> {
            if (!actualizandoEmisor && emisorCargado) validarEmisorInline();
        });
        ValidacionFormulario.vincularTexto(txtParamClave, this::validarParametroInlineSiTocado);
        ValidacionFormulario.vincularTexto(txtParamValor, this::validarParametroInlineSiTocado);
        ValidacionFormulario.vincularTexto(pwdClaveRespaldo, this::validarClavesRespaldoEnVivo);
        ValidacionFormulario.vincularTexto(pwdConfirmarClaveRespaldo, this::validarClavesRespaldoEnVivo);
        ValidacionFormulario.vincularTexto(txtDestinoJdbcRestaurar, this::validarJdbcRestauracionEnVivo);
        dpAuditoriaDesde.valueProperty().addListener((obs, ant, nuevo) -> {
            if (tabPaneAdmin != null && tabPaneAdmin.getSelectionModel().getSelectedItem() == tabAuditoria) {
                consultarAuditoria();
            }
        });
        dpAuditoriaHasta.valueProperty().addListener((obs, ant, nuevo) -> {
            if (tabPaneAdmin != null && tabPaneAdmin.getSelectionModel().getSelectedItem() == tabAuditoria) {
                consultarAuditoria();
            }
        });
        txtAuditoriaUsuario.textProperty().addListener((obs, ant, nuevo) -> programarConsultaAuditoria());
        txtAuditoriaAccion.textProperty().addListener((obs, ant, nuevo) -> programarConsultaAuditoria());
        if (tabPaneAdmin != null) {
            tabPaneAdmin.getSelectionModel().selectedItemProperty().addListener((obs, ant, nueva) -> cargarPestana(nueva));
        }
    }

    private void cargarPestanaActual() {
        Tab seleccion = tabPaneAdmin == null ? tabCuentas : tabPaneAdmin.getSelectionModel().getSelectedItem();
        cargarPestana(seleccion);
    }

    private void cargarPestana(Tab pestana) {
        if (pestana == null || pestana == tabCuentas) {
            cargarUsuarios();
        } else if (pestana == tabParametros) {
            cargarParametros(false);
        } else if (pestana == tabAuditoria) {
            consultarAuditoria();
        } else if (pestana == tabRespaldo) {
            cargarEstadoRespaldo();
        }
    }

    private void cargarUsuarios() {
        barraErrorUsuarios.ocultar();
        recargadorUsuarios.recargar(
                admin::listarUsuarios,
                lista -> {
                    RecargadorVista.conservarSeleccionPorId(tblUsuarios, UsuarioResponse::id,
                            () -> usuarios.setAll(lista));
                    tblUsuarios.setPlaceholder(PanelEstadoTabla.crear(
                            "No hay usuarios registrados",
                            "Las cuentas registradas aparecerán aquí.",
                            null, null));
                },
                error -> {
                    barraErrorUsuarios.mostrar(
                            mensajeSeguro(error, "No se pudieron consultar los usuarios."),
                            this::cargarUsuarios);
                    if (usuarios.isEmpty()) {
                        tblUsuarios.setPlaceholder(PanelEstadoTabla.crear(
                                "No se pudieron consultar los usuarios",
                                "Vuelve a intentarlo.",
                                "Reintentar", this::cargarUsuarios));
                    }
                });
    }

    @FXML
    public void handleCrearUsuario() {
        TextField txtNombre = new TextField();
        txtNombre.getStyleClass().add("field");
        txtNombre.setPromptText("usuario.nuevo");
        PasswordField txtClave = new PasswordField();
        txtClave.getStyleClass().add("field");
        txtClave.setPromptText("Entre 12 y 128 caracteres");
        ComboBox<String> cmbRol = new ComboBox<>(
                FXCollections.observableArrayList(ConstantesLatch.ROL_ASISTENTE, ConstantesLatch.ROL_PROPIETARIO));
        cmbRol.getStyleClass().add("field");
        cmbRol.getSelectionModel().selectFirst();
        Label lblErrNombre = crearErrorInline();
        Label lblErrClave = crearErrorInline();
        Label lblErr = new Label();
        lblErr.getStyleClass().add("field-error");
        lblErr.setWrapText(true);
        lblErr.setVisible(false);
        lblErr.setManaged(false);
        Label lblAyuda = new Label("Los campos con * son obligatorios. La contraseña temporal requiere de 12 a 128 caracteres.");
        lblAyuda.getStyleClass().add("field-help");
        lblAyuda.setWrapText(true);

        Runnable validarNombre = () -> {
            String n = txtNombre.getText() == null ? "" : txtNombre.getText().strip();
            String msg = n.isEmpty() ? "Escribe el nombre de usuario."
                    : (n.length() > 60 ? "El nombre admite hasta 60 caracteres." : null);
            ValidacionFormulario.marcarCampo(txtNombre, lblErrNombre, msg);
        };
        Runnable validarClave = () -> {
            String c = txtClave.getText();
            String msg = (c == null || c.length() < 12 || c.length() > 128)
                    ? "La contraseña debe tener entre 12 y 128 caracteres." : null;
            ValidacionFormulario.marcarCampo(txtClave, lblErrClave, msg);
        };
        ValidacionFormulario.vincularTexto(txtNombre, validarNombre);
        ValidacionFormulario.vincularTexto(txtClave, validarClave);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.add(lblAyuda, 0, 0, 2, 1);
        grid.add(lblErr, 0, 1, 2, 1);
        grid.addRow(2, new Label("Usuario *:"), txtNombre);
        grid.add(lblErrNombre, 1, 3);
        grid.addRow(4, new Label("Contraseña *:"), txtClave);
        grid.add(lblErrClave, 1, 5);
        grid.addRow(6, new Label("Rol *:"), cmbRol);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Nuevo usuario", grid, ventanaActual(), 480, 400, "Crear usuario");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            validarNombre.run();
            validarClave.run();
            if (lblErrNombre.isVisible() || lblErrClave.isVisible()) {
                if (lblErrNombre.isVisible()) txtNombre.requestFocus();
                else txtClave.requestFocus();
                return;
            }
            String nombre = txtNombre.getText().strip();
            String claveTexto = txtClave.getText();
            char[] clave = claveTexto.toCharArray();
            String rol = cmbRol.getValue();
            accion.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    try {
                        admin.crearUsuario(nombre, clave, rol);
                        return null;
                    } finally {
                        Arrays.fill(clave, '\0');
                    }
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                txtClave.clear();
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Usuario creado", "La cuenta quedó registrada.");
                cargarUsuarios();
            }, error -> {
                accion.setDisable(false);
                mostrarError(lblErr, mensajeSeguro(error, "No se pudo crear la cuenta."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleCambiarRol() {
        UsuarioResponse sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String nuevoRol = ConstantesLatch.ROL_PROPIETARIO.equals(sel.rol())
                ? ConstantesLatch.ROL_ASISTENTE
                : ConstantesLatch.ROL_PROPIETARIO;
        if (!AlertaUtil.mostrarConfirmacion("Cambiar rol",
                "¿Cambiar el rol de '" + sel.nombre() + "' a " + nuevoRol + "? Las sesiones activas de esa cuenta serán revocadas.")) {
            return;
        }
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                admin.cambiarRol(sel.id(), nuevoRol);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            AlertaUtil.mostrarExito("Rol actualizado", "El rol cambió a " + nuevoRol + ".");
            cargarUsuarios();
        }, error -> AlertaUtil.mostrarError("No se pudo cambiar el rol",
                mensajeSeguro(error, "No fue posible cambiar el rol.")));
    }

    @FXML
    public void handleAlternarActivo() {
        UsuarioResponse sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean activar = !sel.activo();
        String titulo = activar ? "Activar cuenta" : "Desactivar cuenta";
        if (!AlertaUtil.mostrarConfirmacion(titulo,
                "¿Confirmas " + (activar ? "activar" : "desactivar") + " la cuenta '" + sel.nombre() + "'?")) {
            return;
        }
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                if (activar) admin.activarUsuario(sel.id());
                else admin.desactivarUsuario(sel.id());
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            AlertaUtil.mostrarExito("Cuenta actualizada", activar ? "La cuenta quedó activa." : "La cuenta quedó desactivada.");
            cargarUsuarios();
        }, error -> AlertaUtil.mostrarError("No se pudo actualizar la cuenta",
                mensajeSeguro(error, "No fue posible actualizar la cuenta.")));
    }

    @FXML
    public void handleRestablecerPassword() {
        UsuarioResponse sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        PasswordField txtTemporal = new PasswordField();
        txtTemporal.getStyleClass().add("field");
        txtTemporal.setPromptText("Contraseña temporal (12–128 caracteres)");
        Label lblErr = crearErrorInline();

        VBox box = new VBox(8, new Label("Cuenta: " + sel.nombre()),
                new Label("Nueva contraseña temporal *:"), txtTemporal, lblErr);
        box.setPadding(new Insets(8));

        Runnable validarTemporal = () -> {
            String t = txtTemporal.getText();
            String msg = (t == null || t.length() < 12 || t.length() > 128)
                    ? "La contraseña temporal debe tener entre 12 y 128 caracteres." : null;
            ValidacionFormulario.marcarCampo(txtTemporal, lblErr, msg);
        };
        ValidacionFormulario.vincularTexto(txtTemporal, validarTemporal);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Restablecer contraseña", box, ventanaActual(), 460, 290, "Restablecer", true);
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            validarTemporal.run();
            if (lblErr.isVisible()) {
                txtTemporal.requestFocus();
                return;
            }
            char[] temporal = txtTemporal.getText().toCharArray();
            accion.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    try {
                        admin.restablecerPassword(sel.id(), temporal);
                        return null;
                    } finally {
                        Arrays.fill(temporal, '\0');
                    }
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                txtTemporal.clear();
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Contraseña restablecida", "Se marcó cambio de contraseña pendiente.");
                cargarUsuarios();
            }, error -> {
                accion.setDisable(false);
                mostrarError(lblErr, mensajeSeguro(error, "No se pudo restablecer la contraseña."));
            });
        });
        dialogo.showAndWait();
    }

    private void cargarParametros(boolean forzarEmisor) {
        barraErrorParametros.ocultar();
        recargadorParametros.recargar(
                () -> new DatosParametros(
                        admin.obtenerEmisor(),
                        adjuntos.obtenerLogotipoVigente(),
                        admin.listarParametros(null)),
                datos -> {
                    RecargadorVista.conservarSeleccionPorId(tblParametros, ParametroHistorialResponse::id,
                            () -> parametros.setAll(datos.historial()));
                    tblParametros.setPlaceholder(PanelEstadoTabla.crear(
                            "Sin registros históricos de parámetros",
                            "Los cambios de parámetros aparecerán aquí.",
                            null, null));
                    logotipoVigente = datos.logotipo().orElse(null);
                    if (logotipoVigente != null) {
                        lblLogotipoEstado.setText("Logotipo: " + logotipoVigente.nombreOriginal());
                        btnQuitarLogotipo.setDisable(false);
                    } else {
                        lblLogotipoEstado.setText("Sin logotipo configurado");
                        btnQuitarLogotipo.setDisable(true);
                    }
                    PerfilEmisorResponse emisor = datos.emisor();
                    if (emisor != null && emisor.configurado()) {
                        if (forzarEmisor || !emisorCargado || !emisorEditadoLocalmente) {
                            actualizandoEmisor = true;
                            try {
                                txtEmisorNombre.setText(emisor.nombre());
                                txtEmisorIdentificacion.setText(emisor.identificacion());
                                txtEmisorDireccion.setText(emisor.direccion());
                                txtEmisorTelefono.setText(emisor.telefono());
                                txtEmisorCorreo.setText(emisor.correo());
                                emisorCargado = true;
                                emisorEditadoLocalmente = false;
                            } finally {
                                actualizandoEmisor = false;
                            }
                        }
                        lblEstadoEmisor.setText("Emisor vigente para nuevos recibos: " + emisor.resumenVisible());
                    } else {
                        emisorCargado = true;
                        lblEstadoEmisor.setText("Sin perfil emisor configurado; los recibos mostrarán el usuario emisor.");
                    }
                },
                error -> {
                    barraErrorParametros.mostrar(
                            mensajeSeguro(error, "No se pudo cargar la configuración."),
                            () -> cargarParametros(forzarEmisor));
                    if (parametros.isEmpty()) {
                        tblParametros.setPlaceholder(PanelEstadoTabla.crear(
                                "No se pudo cargar la configuración",
                                "Vuelve a intentarlo.",
                                "Reintentar", () -> cargarParametros(forzarEmisor)));
                    }
                });
    }

    @FXML
    public void handleAdjuntarLogotipo() {
        barraErrorParametros.ocultar();
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar logotipo del emisor");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG o JPEG", "*.png", "*.jpg", "*.jpeg"));
        File archivo = fc.showOpenDialog(ventanaActual());
        if (archivo == null) return;
        btnAdjuntarLogotipo.setDisable(true);
        Task<DocumentoAdjuntoResponse> tarea = new Task<>() {
            @Override protected DocumentoAdjuntoResponse call() throws IOException {
                return adjuntos.adjuntarLogotipoEmisor(archivo.toPath());
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            btnAdjuntarLogotipo.setDisable(false);
            AlertaUtil.mostrarExito("Logotipo guardado", "El logotipo quedó vinculado al perfil emisor vigente.");
            cargarParametros(false);
        }, error -> {
            btnAdjuntarLogotipo.setDisable(false);
            barraErrorParametros.mostrar(mensajeSeguro(error, "No se pudo adjuntar el logotipo."), null);
        });
    }

    @FXML
    public void handleQuitarLogotipo() {
        if (logotipoVigente == null) return;
        barraErrorParametros.ocultar();
        UUID adjuntoId = UUID.fromString(logotipoVigente.id());
        btnQuitarLogotipo.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                adjuntos.desactivar(adjuntoId);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            AlertaUtil.mostrarExito("Logotipo quitado", "Se desactivó el logotipo del emisor.");
            cargarParametros(false);
        }, error -> {
            btnQuitarLogotipo.setDisable(false);
            barraErrorParametros.mostrar(mensajeSeguro(error, "No se pudo quitar el logotipo."), null);
        });
    }

    @FXML
    public void handleGuardarEmisor() {
        barraErrorParametros.ocultar();
        if (!validarEmisorInline()) {
            return;
        }
        PerfilEmisorRequest solicitud = new PerfilEmisorRequest(
                txtEmisorNombre.getText(),
                txtEmisorIdentificacion.getText(),
                txtEmisorDireccion.getText(),
                txtEmisorTelefono.getText(),
                txtEmisorCorreo.getText(),
                txtEmisorMotivo.getText());
        btnGuardarEmisor.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                admin.guardarEmisor(solicitud);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            btnGuardarEmisor.setDisable(false);
            txtEmisorMotivo.clear();
            AlertaUtil.mostrarExito("Perfil emisor guardado", "Los nuevos recibos usarán este perfil emisor.");
            cargarParametros(true);
        }, error -> {
            btnGuardarEmisor.setDisable(false);
            barraErrorParametros.mostrar(mensajeSeguro(error, "No se pudo guardar el perfil emisor."), null);
        });
    }

    @FXML
    public void handleRegistrarParametro() {
        barraErrorParametros.ocultar();
        if (!validarParametroInline()) {
            return;
        }
        String clave = txtParamClave.getText();
        String valor = txtParamValor.getText();
        String motivo = txtParamMotivo.getText();
        btnRegistrarParametro.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                admin.registrarParametro(clave, valor, null, motivo);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            btnRegistrarParametro.setDisable(false);
            txtParamClave.clear();
            txtParamValor.clear();
            txtParamMotivo.clear();
            AlertaUtil.mostrarExito("Parámetro registrado", "Se guardó la nueva vigencia en el historial.");
            cargarParametros(false);
        }, error -> {
            btnRegistrarParametro.setDisable(false);
            barraErrorParametros.mostrar(mensajeSeguro(error, "No se pudo registrar el parámetro."), null);
        });
    }

    private void programarConsultaAuditoria() {
        if (tabPaneAdmin == null || tabPaneAdmin.getSelectionModel().getSelectedItem() != tabAuditoria) {
            return;
        }
        barraErrorAuditoria.ocultar();
        String usuarioFiltro = txtAuditoriaUsuario.getText();
        String accionFiltro = txtAuditoriaAccion.getText();
        LocalDate dDesde = dpAuditoriaDesde.getValue();
        LocalDate dHasta = dpAuditoriaHasta.getValue();
        String desde = dDesde == null ? null
                : dDesde.atStartOfDay(ConstantesLatch.ZONA_OPERATIVA).toInstant().toString();
        String hasta = dHasta == null ? null
                : dHasta.plusDays(1).atStartOfDay(ConstantesLatch.ZONA_OPERATIVA).minusNanos(1).toInstant().toString();
        recargadorAuditoria.recargarAgrupado(
                Duration.millis(250),
                () -> admin.consultarAuditoria(usuarioFiltro, accionFiltro, desde, hasta, 200),
                this::aplicarAuditoria,
                this::manejarErrorAuditoria);
    }

    @FXML
    public void consultarAuditoria() {
        barraErrorAuditoria.ocultar();
        String usuarioFiltro = txtAuditoriaUsuario.getText();
        String accionFiltro = txtAuditoriaAccion.getText();
        LocalDate dDesde = dpAuditoriaDesde.getValue();
        LocalDate dHasta = dpAuditoriaHasta.getValue();
        String desde = dDesde == null ? null
                : dDesde.atStartOfDay(ConstantesLatch.ZONA_OPERATIVA).toInstant().toString();
        String hasta = dHasta == null ? null
                : dHasta.plusDays(1).atStartOfDay(ConstantesLatch.ZONA_OPERATIVA).minusNanos(1).toInstant().toString();
        recargadorAuditoria.recargar(
                () -> admin.consultarAuditoria(usuarioFiltro, accionFiltro, desde, hasta, 200),
                this::aplicarAuditoria,
                this::manejarErrorAuditoria);
    }

    private void aplicarAuditoria(List<AuditoriaRegistroResponse> lista) {
        RecargadorVista.conservarSeleccionPorId(tblAuditoria, AuditoriaRegistroResponse::id,
                () -> auditorias.setAll(lista));
        tblAuditoria.setPlaceholder(PanelEstadoTabla.crear(
                "No hay eventos para los filtros indicados",
                "Ajusta usuario, acción o fechas para ver otros registros.",
                null, null));
    }

    private void manejarErrorAuditoria(Throwable error) {
        barraErrorAuditoria.mostrar(
                mensajeSeguro(error, "No se pudo consultar la auditoría."),
                this::consultarAuditoria);
        if (auditorias.isEmpty()) {
            tblAuditoria.setPlaceholder(PanelEstadoTabla.crear(
                    "No se pudo consultar la auditoría",
                    "Vuelve a intentarlo.",
                    "Reintentar", this::consultarAuditoria));
        }
    }

    private void cargarEstadoRespaldo() {
        barraErrorRespaldo.ocultar();
        recargadorRespaldo.recargar(
                admin::consultarEstadoAlertaRespaldo,
                this::aplicarEstadoRespaldo,
                this::manejarErrorRespaldo);
    }

    private void aplicarEstadoRespaldo(EstadoRespaldoResponse estado) {
        barraErrorRespaldo.ocultar();
        lblEstadoRespaldo.setText(estado.mensaje());
    }

    private void manejarErrorRespaldo(Throwable error) {
        lblEstadoRespaldo.setText("No se pudo consultar el estado del último respaldo.");
        barraErrorRespaldo.mostrar(
                mensajeSeguro(error, "No se pudo consultar el estado del respaldo."),
                this::cargarEstadoRespaldo);
    }

    @FXML
    public void handleElegirCarpetaRespaldo() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta de destino del respaldo");
        File carpeta = dc.showDialog(ventanaActual());
        if (carpeta != null) {
            txtCarpetaRespaldo.setText(carpeta.getAbsolutePath());
        }
    }

    @FXML
    public void handleElegirArchivoRespaldo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar paquete de respaldo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Paquete de respaldo Latch (*" + ConstantesLatch.EXTENSION_RESPALDO + ")",
                "*" + ConstantesLatch.EXTENSION_RESPALDO));
        File archivo = fc.showOpenDialog(ventanaActual());
        if (archivo != null) {
            txtArchivoRespaldo.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    public void handleElegirDestinoAdjuntos() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta destino aislada para adjuntos");
        File carpeta = dc.showDialog(ventanaActual());
        if (carpeta != null) {
            txtDestinoAdjuntosRestaurar.setText(carpeta.getAbsolutePath());
        }
    }

    @FXML
    public void handleCrearRespaldo() {
        barraErrorRespaldo.ocultar();
        ocultarError(lblResultadoRespaldo);
        String rutaDestino = txtCarpetaRespaldo.getText();
        if (rutaDestino == null || rutaDestino.isBlank()) {
            barraErrorRespaldo.mostrar("Selecciona la carpeta de destino del respaldo.", null);
            return;
        }
        String claveTexto = pwdClaveRespaldo.getText();
        String confirmacionTexto = pwdConfirmarClaveRespaldo.getText();
        if (claveTexto == null || !claveTexto.equals(confirmacionTexto)) {
            barraErrorRespaldo.mostrar("La contraseña de respaldo y su confirmación no coinciden.", null);
            return;
        }
        char[] clave = claveTexto.toCharArray();
        pwdClaveRespaldo.clear();
        pwdConfirmarClaveRespaldo.clear();

        String directorioDestino = Path.of(rutaDestino.strip()).toString();
        btnCrearRespaldo.setDisable(true);
        Task<ResultadoRespaldoResponse> tarea = new Task<>() {
            @Override protected ResultadoRespaldoResponse call() {
                try {
                    return admin.crearRespaldo(directorioDestino, clave);
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, resultado -> {
            btnCrearRespaldo.setDisable(false);
            txtArchivoRespaldo.setText(resultado.archivo());
            mostrarResultado(lblResultadoRespaldo,
                    "Respaldo generado y verificado: " + Path.of(resultado.archivo()).getFileName()
                            + " (" + resultado.cantidadAdjuntos() + " adjuntos, huella: "
                            + resultado.sha256Paquete().substring(0, 16) + "…, " + resultado.duracionMs() + " ms).");
            cargarEstadoRespaldo();
        }, error -> {
            btnCrearRespaldo.setDisable(false);
            barraErrorRespaldo.mostrar(mensajeSeguro(error, "No se pudo generar el respaldo."), null);
        });
    }

    @FXML
    public void handleVerificarRespaldo() {
        barraErrorRespaldo.ocultar();
        ocultarResultado(lblResultadoRespaldo);
        String rutaArchivo = txtArchivoRespaldo.getText();
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            barraErrorRespaldo.mostrar("Selecciona el archivo de respaldo a verificar.", null);
            return;
        }
        String claveTexto = pwdClaveRestaurar.getText();
        char[] clave = claveTexto == null ? new char[0] : claveTexto.toCharArray();
        pwdClaveRestaurar.clear();

        String archivo = Path.of(rutaArchivo.strip()).toString();
        btnVerificarRespaldo.setDisable(true);
        Task<ResultadoVerificacionRespaldoResponse> tarea = new Task<>() {
            @Override protected ResultadoVerificacionRespaldoResponse call() {
                try {
                    return admin.verificarRespaldo(archivo, clave);
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ver -> {
            btnVerificarRespaldo.setDisable(false);
            mostrarResultado(lblResultadoRespaldo,
                    "Paquete auténtico e íntegro: corte " + ver.corteLocal()
                            + ", esquema " + ver.esquemaVersion()
                            + ", " + ver.adjuntosVerificados() + " adjuntos verificados, último recibo #"
                            + ver.ultimoNumeroRecibo() + ".");
        }, error -> {
            btnVerificarRespaldo.setDisable(false);
            barraErrorRespaldo.mostrar(mensajeSeguro(error, "No se pudo verificar el respaldo."), null);
        });
    }

    @FXML
    public void handleRestaurarAislado() {
        barraErrorRespaldo.ocultar();
        ocultarResultado(lblResultadoRespaldo);
        if (!chkModoMantenimiento.isSelected()) {
            barraErrorRespaldo.mostrar(
                    "Activa el modo mantenimiento sin sesiones operativas antes de restaurar.", null);
            return;
        }
        String rutaArchivo = txtArchivoRespaldo.getText();
        String jdbcUrl = txtDestinoJdbcRestaurar.getText();
        String usuarioDestino = txtDestinoUsuarioRestaurar.getText();
        String rutaAdjuntos = txtDestinoAdjuntosRestaurar.getText();
        if (rutaArchivo == null || rutaArchivo.isBlank()
                || jdbcUrl == null || jdbcUrl.isBlank()
                || usuarioDestino == null || usuarioDestino.isBlank()
                || rutaAdjuntos == null || rutaAdjuntos.isBlank()) {
            barraErrorRespaldo.mostrar(
                    "Completa el paquete de respaldo, la URL JDBC aislada, el usuario destino y la carpeta de adjuntos.", null);
            return;
        }
        if (!jdbcUrl.strip().startsWith("jdbc:postgresql://")) {
            ValidacionFormulario.marcarCampo(txtDestinoJdbcRestaurar, null, "err");
            barraErrorRespaldo.mostrar("La URL de destino debe iniciar con jdbc:postgresql://.", null);
            txtDestinoJdbcRestaurar.requestFocus();
            return;
        }
        if (!AlertaUtil.mostrarConfirmacion("Restaurar en entorno aislado",
                "Se creará una copia previa antes de restaurar el paquete en el destino aislado indicado. ¿Deseas continuar?")) {
            return;
        }

        String claveRespaldoTexto = pwdClaveRestaurar.getText();
        String claveDestinoTexto = pwdDestinoClaveRestaurar.getText();
        pwdClaveRestaurar.clear();
        pwdDestinoClaveRestaurar.clear();

        RestaurarRespaldoRequest solicitud = new RestaurarRespaldoRequest(
                Path.of(rutaArchivo.strip()).toString(),
                claveRespaldoTexto == null ? "" : claveRespaldoTexto,
                jdbcUrl.strip(),
                usuarioDestino.strip(),
                claveDestinoTexto == null ? "" : claveDestinoTexto,
                Path.of(rutaAdjuntos.strip()).toString(),
                true);

        btnRestaurarAislado.setDisable(true);
        Task<ResultadoRestauracionResponse> tarea = new Task<>() {
            @Override protected ResultadoRestauracionResponse call() {
                return admin.restaurarRespaldo(solicitud);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
            btnRestaurarAislado.setDisable(false);
            mostrarResultado(lblResultadoRespaldo,
                    "Restauración aislada completada: " + res.adjuntosVerificados()
                            + " adjuntos verificados, " + res.hashesDiscordantes()
                            + " archivos con diferencias de integridad (" + res.duracionMs() + " ms). Registro: "
                            + Path.of(res.registroExterno()).getFileName());
        }, error -> {
            btnRestaurarAislado.setDisable(false);
            barraErrorRespaldo.mostrar(mensajeSeguro(error, "No se pudo restaurar el respaldo en destino aislado."), null);
        });
    }

    private boolean validarEmisorInline() {
        String nombre = txtEmisorNombre.getText() == null ? "" : txtEmisorNombre.getText().strip();
        String telefono = txtEmisorTelefono.getText();
        String correo = txtEmisorCorreo.getText();
        txtEmisorNombre.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        txtEmisorTelefono.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        txtEmisorCorreo.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        if (nombre.isEmpty()) {
            ValidacionFormulario.marcarCampo(txtEmisorNombre, null, "err");
            barraErrorParametros.mostrar("Escribe el nombre del emisor.", null);
            return false;
        }
        if (telefono != null && !telefono.isBlank() && !ValidadorEntrada.esTelefonoValido(telefono)) {
            ValidacionFormulario.marcarCampo(txtEmisorTelefono, null, "err");
            barraErrorParametros.mostrar("Escribe un teléfono válido para el emisor (7 a 15 dígitos).", null);
            return false;
        }
        if (correo != null && !correo.isBlank() && !ValidadorEntrada.esCorreoValido(correo)) {
            ValidacionFormulario.marcarCampo(txtEmisorCorreo, null, "err");
            barraErrorParametros.mostrar("Escribe un correo electrónico válido para el emisor.", null);
            return false;
        }
        barraErrorParametros.ocultar();
        return true;
    }

    private void validarParametroInlineSiTocado() {
        String clave = txtParamClave.getText() == null ? "" : txtParamClave.getText().strip();
        String valor = txtParamValor.getText() == null ? "" : txtParamValor.getText().strip();
        if (clave.isEmpty() && valor.isEmpty()) {
            txtParamClave.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            txtParamValor.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            barraErrorParametros.ocultar();
            return;
        }
        validarParametroInline();
    }

    private boolean validarParametroInline() {
        String clave = txtParamClave.getText() == null ? "" : txtParamClave.getText().strip();
        String valor = txtParamValor.getText() == null ? "" : txtParamValor.getText().strip();
        txtParamClave.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        txtParamValor.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        if (clave.isEmpty()) {
            ValidacionFormulario.marcarCampo(txtParamClave, null, "err");
            barraErrorParametros.mostrar("Indica la clave del parámetro.", null);
            return false;
        }
        if (valor.isEmpty()) {
            ValidacionFormulario.marcarCampo(txtParamValor, null, "err");
            barraErrorParametros.mostrar("Indica el valor del parámetro.", null);
            return false;
        }
        barraErrorParametros.ocultar();
        return true;
    }

    private void validarClavesRespaldoEnVivo() {
        String clave = pwdClaveRespaldo.getText();
        String conf = pwdConfirmarClaveRespaldo.getText();
        pwdClaveRespaldo.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        pwdConfirmarClaveRespaldo.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        if ((clave == null || clave.isEmpty()) && (conf == null || conf.isEmpty())) {
            barraErrorRespaldo.ocultar();
            return;
        }
        if (clave == null || clave.length() < 12 || clave.length() > 128) {
            ValidacionFormulario.marcarCampo(pwdClaveRespaldo, null, "err");
            barraErrorRespaldo.mostrar("La contraseña del respaldo debe tener entre 12 y 128 caracteres.", null);
            return;
        }
        if (conf != null && !conf.isEmpty() && !clave.equals(conf)) {
            ValidacionFormulario.marcarCampo(pwdConfirmarClaveRespaldo, null, "err");
            barraErrorRespaldo.mostrar("La contraseña de respaldo y su confirmación no coinciden.", null);
            return;
        }
        barraErrorRespaldo.ocultar();
    }

    private void validarJdbcRestauracionEnVivo() {
        String url = txtDestinoJdbcRestaurar.getText() == null ? "" : txtDestinoJdbcRestaurar.getText().strip();
        txtDestinoJdbcRestaurar.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        if (url.isEmpty()) {
            barraErrorRespaldo.ocultar();
            return;
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            ValidacionFormulario.marcarCampo(txtDestinoJdbcRestaurar, null, "err");
            barraErrorRespaldo.mostrar("La URL de destino debe iniciar con jdbc:postgresql://.", null);
            return;
        }
        barraErrorRespaldo.ocultar();
    }

    private static Label crearErrorInline() {
        Label lbl = new Label();
        lbl.getStyleClass().add("field-error");
        lbl.setWrapText(true);
        lbl.setVisible(false);
        lbl.setManaged(true);
        lbl.setMinHeight(18);
        return lbl;
    }

    private static void mostrarResultado(Label label, String mensaje) {
        if (!label.getStyleClass().contains("status-banner")) {
            label.getStyleClass().add("status-banner");
        }
        label.setGraphic(Iconos.crear(Iconos.Nombre.CONFIRMACION, 14));
        label.setText(mensaje);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void ocultarResultado(Label label) {
        label.setGraphic(null);
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private Window ventanaActual() {
        return rootView != null && rootView.getScene() != null ? rootView.getScene().getWindow() : null;
    }

    private static String formatearInstante(String valor) {
        if (valor == null || valor.isBlank()) return "—";
        try {
            Instant instante = Instant.parse(valor);
            return FORMATO_INSTANTE.format(instante.atZone(ConstantesLatch.ZONA_OPERATIVA));
        } catch (DateTimeParseException ex) {
            return valor;
        }
    }

    private static void mostrarError(Label label, String mensaje) {
        label.setText(MensajesUi.limpiarParaUsuario(mensaje));
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void ocultarError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private static String mensajeSeguro(Throwable error, String respaldo) {
        return MensajesUi.traducir(error, respaldo);
    }
}
