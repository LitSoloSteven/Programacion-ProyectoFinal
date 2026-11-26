package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;

import java.io.IOException;
import java.util.List;

/**
 * Inicio de sesión contra la API local. La identidad resultante se establece
 * como sesión de JavaFX para que los servicios existentes sigan funcionando.
 */
public class LoginController {
    @FXML private Region auroraFondo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblErrorUsuario;
    @FXML private Label lblErrorPassword;
    @FXML private Label lblAvisoPreferencia;
    @FXML private Button btnIngresar;
    @FXML private Button btnTemaLogin;
    @FXML private CheckBox chkSinEfectosLogin;

    @FXML
    public void initialize() {
        Iconos.asignar(btnIngresar, Iconos.Nombre.ENTRAR, 16);
        ocultarError(lblErrorUsuario);
        ocultarError(lblErrorPassword);
        if (lblAvisoPreferencia != null) {
            lblAvisoPreferencia.setVisible(false);
            lblAvisoPreferencia.setManaged(false);
        }
        if (btnTemaLogin != null) {
            Iconos.asignar(btnTemaLogin, Iconos.Nombre.TEMA, 14);
        }
        TemaAplicacion.registrarObservadorAviso(mensaje -> {
            if (lblAvisoPreferencia != null) {
                lblAvisoPreferencia.setText(mensaje);
                lblAvisoPreferencia.setVisible(true);
                lblAvisoPreferencia.setManaged(true);
            }
        });
        ValidacionFormulario.vincularTexto(txtUsuario, () -> {
            if (lblErrorUsuario.isVisible() || txtUsuario.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                String u = txtUsuario.getText();
                ValidacionFormulario.marcarCampo(txtUsuario, lblErrorUsuario,
                        u == null || u.isBlank() ? "Escribe tu nombre de usuario." : null);
            }
        });
        ValidacionFormulario.vincularTexto(txtPassword, () -> {
            if (lblErrorPassword.isVisible() || txtPassword.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                String p = txtPassword.getText();
                ValidacionFormulario.marcarCampo(txtPassword, lblErrorPassword,
                        p == null || p.isEmpty() ? "Escribe tu contraseña." : null);
            }
        });
        actualizarControlesTema();
    }

    @FXML
    public void handleCambiarTemaLogin() {
        TemaAplicacion.Tema nuevo = TemaAplicacion.getTema() == TemaAplicacion.Tema.OSCURO
                ? TemaAplicacion.Tema.CLARO : TemaAplicacion.Tema.OSCURO;
        TemaAplicacion.establecerTema(nuevo);
        actualizarControlesTema();
    }

    @FXML
    public void handleSinEfectosLogin() {
        if (chkSinEfectosLogin == null) return;
        TemaAplicacion.establecerSinEfectos(chkSinEfectosLogin.isSelected());
        actualizarControlesTema();
    }

    private void actualizarControlesTema() {
        boolean oscuro = TemaAplicacion.getTema() == TemaAplicacion.Tema.OSCURO;
        if (btnTemaLogin != null) {
            btnTemaLogin.setText(oscuro ? "Tema claro" : "Tema oscuro");
            btnTemaLogin.setTooltip(new Tooltip("Cambiar a tema " + (oscuro ? "claro" : "oscuro")));
        }
        if (chkSinEfectosLogin != null) {
            chkSinEfectosLogin.setSelected(TemaAplicacion.isSinEfectos());
            chkSinEfectosLogin.setTooltip(new Tooltip("Desactiva animaciones y luces decorativas en ambos temas"));
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String nombre = txtUsuario.getText();
        String password = txtPassword.getText();
        ValidacionFormulario.marcarCampo(txtUsuario, lblErrorUsuario, null);
        ValidacionFormulario.marcarCampo(txtPassword, lblErrorPassword, null);
        boolean invalido = false;
        if (nombre == null || nombre.isBlank()) {
            ValidacionFormulario.marcarCampo(txtUsuario, lblErrorUsuario, "Escribe tu nombre de usuario.");
            invalido = true;
        }
        if (password == null || password.isEmpty()) {
            ValidacionFormulario.marcarCampo(txtPassword, lblErrorPassword, "Escribe tu contraseña.");
            invalido = true;
        }
        if (invalido) {
            if (lblErrorUsuario.isVisible()) {
                txtUsuario.requestFocus();
            } else {
                txtPassword.requestFocus();
            }
            return;
        }

        iniciarSesion(event, nombre, password, null);
    }

    private void iniciarSesion(ActionEvent event, String nombre, String password, String nuevaPassword) {
        btnIngresar.setDisable(true);
        btnIngresar.setText("Ingresando…");
        Task<LoginResponse> tarea = new Task<>() {
            @Override
            protected LoginResponse call() {
                return new ClienteAuth(ClienteLatch.api()).login(nombre, password, nuevaPassword);
            }
        };
        tarea.setOnSucceeded(evento -> {
            try {
                LoginResponse respuesta = tarea.getValue();
                SesionCliente.iniciar(respuesta.usuarioId(), respuesta.nombre(), respuesta.rol());
                TemaAplicacion.cargarPreferenciasUsuario(respuesta.usuarioId());
                abrirDashboard(event);
            } catch (RuntimeException e) {
                SesionCliente.cerrarSesion();
                TemaAplicacion.limpiarUsuarioActivo();
                mostrarError(lblErrorPassword, "No fue posible validar la sesión. Inténtalo nuevamente.");
            } finally {
                finalizarIntento();
            }
        });
        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            if (error instanceof ErrorClienteException cliente
                    && "CAMBIO_PASSWORD_REQUERIDO".equals(cliente.getCodigo())) {
                String nueva = pedirNuevaPassword(event, cliente.getMessage());
                if (nueva != null) {
                    iniciarSesion(event, nombre, password, nueva);
                    return;
                }
            }
            mostrarError(lblErrorPassword, mensaje(error));
            finalizarIntento();
            txtPassword.requestFocus();
        });
        try {
            LatchApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException e) {
            btnIngresar.setDisable(false);
            btnIngresar.setText("Ingresar");
            mostrarError(lblErrorPassword, "No se pudo iniciar sesión. Inténtalo nuevamente.");
        }
    }

    private String pedirNuevaPassword(ActionEvent event, String indicacion) {
        PasswordField nueva = new PasswordField();
        nueva.setPromptText("Mínimo 12 y máximo 128 caracteres");
        nueva.getStyleClass().add("field");
        PasswordField confirmacion = new PasswordField();
        confirmacion.setPromptText("Repite la nueva contraseña");
        confirmacion.getStyleClass().add("field");

        Label lblIndicacion = new Label(MensajesUi.limpiarParaUsuario(indicacion));
        lblIndicacion.getStyleClass().add("field-help");
        lblIndicacion.setWrapText(true);
        Label etiquetaNueva = new Label("Nueva contraseña *");
        etiquetaNueva.getStyleClass().add("field-label");
        etiquetaNueva.setLabelFor(nueva);
        Label lblErrNueva = crearErrorInline();
        Label etiquetaConfirmacion = new Label("Confirmar contraseña *");
        etiquetaConfirmacion.getStyleClass().add("field-label");
        etiquetaConfirmacion.setLabelFor(confirmacion);
        Label lblErrConfirmacion = crearErrorInline();
        Label lblResumen = new Label();
        lblResumen.getStyleClass().add("field-error");
        lblResumen.setWrapText(true);
        lblResumen.setVisible(false);
        lblResumen.setManaged(false);

        VBox contenido = new VBox(8,
                lblIndicacion,
                lblResumen,
                etiquetaNueva, nueva, lblErrNueva,
                etiquetaConfirmacion, confirmacion, lblErrConfirmacion);
        contenido.setStyle("-fx-padding: 8px;");

        Runnable validarNueva = () -> {
            String v = nueva.getText();
            String msg = (v == null || v.length() < 12 || v.length() > 128)
                    ? "La nueva contraseña debe tener entre 12 y 128 caracteres." : null;
            ValidacionFormulario.marcarCampo(nueva, lblErrNueva, msg);
        };
        Runnable validarConf = () -> {
            String v = nueva.getText();
            String c = confirmacion.getText();
            String msg = (c == null || c.isEmpty() || !c.equals(v))
                    ? "La confirmación debe coincidir con la nueva contraseña." : null;
            ValidacionFormulario.marcarCampo(confirmacion, lblErrConfirmacion, msg);
        };
        ValidacionFormulario.vincularTexto(nueva, () -> {
            validarNueva.run();
            if (lblErrConfirmacion.isVisible() || (confirmacion.getText() != null && !confirmacion.getText().isEmpty())) {
                validarConf.run();
            }
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumen, lblErrNueva, lblErrConfirmacion);
        });
        ValidacionFormulario.vincularTexto(confirmacion, () -> {
            validarConf.run();
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumen, lblErrNueva, lblErrConfirmacion);
        });

        Window owner = ((Node) event.getSource()).getScene().getWindow();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Cambiar contraseña", contenido, owner, 460, 380, "Actualizar contraseña");
        ButtonType tipoAceptar = dialogo.getDialogPane().getButtonTypes().getFirst();
        Button aceptar = (Button) dialogo.getDialogPane().lookupButton(tipoAceptar);
        aceptar.addEventFilter(ActionEvent.ACTION, ev -> {
            validarNueva.run();
            validarConf.run();
            if (lblErrNueva.isVisible() || lblErrConfirmacion.isVisible()) {
                ev.consume();
                ValidacionFormulario.mostrarYEnfocar(lblResumen, List.of(
                        new CampoValidado(nueva, lblErrNueva),
                        new CampoValidado(confirmacion, lblErrConfirmacion)));
            }
        });
        dialogo.setOnShown(e -> nueva.requestFocus());
        try {
            return dialogo.showAndWait().filter(tipoAceptar::equals)
                    .map(opcion -> nueva.getText()).orElse(null);
        } finally {
            nueva.clear();
            confirmacion.clear();
        }
    }

    private static Label crearErrorInline() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(true);
        label.setMinHeight(18);
        return label;
    }

    private String mensaje(Throwable error) {
        return MensajesUi.traducir(error, "No se pudo iniciar sesión. Inténtalo nuevamente.");
    }

    private void abrirDashboard(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(LatchApplication.class.getResource("dashboard-view.fxml")).load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1366, 820);
            TemaAplicacion.aplicar(scene);
            stage.setTitle("Latch — Sistema de Gestión Inmobiliaria");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            SesionCliente.cerrarSesion();
            TemaAplicacion.limpiarUsuarioActivo();
            mostrarError(lblErrorPassword, "No se pudo abrir el panel principal. Inténtalo nuevamente.");
        }
    }

    private void mostrarError(Label campo, String mensaje) {
        campo.setText(mensaje == null ? "No se pudo iniciar sesión." : mensaje);
        campo.setVisible(true);
        campo.setManaged(true);
    }

    private void ocultarError(Label campo) {
        campo.setText("");
        campo.setVisible(false);
        campo.setManaged(false);
    }

    private void finalizarIntento() {
        txtPassword.clear();
        btnIngresar.setDisable(false);
        btnIngresar.setText("Ingresar");
    }
}
