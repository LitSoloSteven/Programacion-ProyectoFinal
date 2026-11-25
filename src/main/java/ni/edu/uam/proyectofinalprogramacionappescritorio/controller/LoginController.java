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
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;

import java.io.IOException;

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
        ocultarError(lblErrorUsuario);
        ocultarError(lblErrorPassword);
        boolean invalido = false;
        if (nombre == null || nombre.isBlank()) {
            mostrarError(lblErrorUsuario, "Escribe tu nombre de usuario.");
            invalido = true;
        }
        if (password == null || password.isEmpty()) {
            mostrarError(lblErrorPassword, "Escribe tu contraseña.");
            invalido = true;
        }
        if (invalido) {
            ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.mostrar(
                    lblErrorUsuario.isVisible() ? lblErrorUsuario : lblErrorPassword,
                    lblErrorUsuario, lblErrorPassword);
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
                SesionUsuario.establecer(SesionUsuario.paraSesionAutenticada(
                        respuesta.usuarioId(), respuesta.nombre(), respuesta.rol()));
                TemaAplicacion.cargarPreferenciasUsuario(respuesta.usuarioId());
                abrirDashboard(event);
            } catch (RuntimeException e) {
                SesionUsuario.cerrarSesion();
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
        PasswordField confirmacion = new PasswordField();
        Label etiquetaNueva = new Label("Nueva contraseña");
        etiquetaNueva.setLabelFor(nueva);
        Label etiquetaConfirmacion = new Label("Confirmar contraseña");
        etiquetaConfirmacion.setLabelFor(confirmacion);
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(((Node) event.getSource()).getScene().getWindow());
        dialogo.setTitle("Cambiar contraseña");
        dialogo.setHeaderText(indicacion);
        dialogo.getDialogPane().setContent(new VBox(8, etiquetaNueva, nueva,
                etiquetaConfirmacion, confirmacion));
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Node aceptar = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        aceptar.disableProperty().bind(nueva.textProperty().length().lessThan(12)
                .or(nueva.textProperty().length().greaterThan(128))
                .or(nueva.textProperty().isNotEqualTo(confirmacion.textProperty())));
        TemaAplicacion.aplicar(dialogo.getDialogPane());
        dialogo.setOnShown(e -> nueva.requestFocus());
        try {
            return dialogo.showAndWait().filter(ButtonType.OK::equals)
                    .map(opcion -> nueva.getText()).orElse(null);
        } finally {
            nueva.clear();
            confirmacion.clear();
        }
    }

    private String mensaje(Throwable error) {
        if (error instanceof ErrorClienteException cliente) {
            if (cliente.esSinConexion()) {
                return "No se pudo conectar con el servicio local de Latch. "
                        + "Verifique que la aplicación siga iniciada.";
            }
            return cliente.getMessage();
        }
        if (error instanceof IllegalArgumentException e) {
            return e.getMessage();
        }
        return "No se pudo iniciar sesión. Inténtelo nuevamente.";
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
            SesionUsuario.cerrarSesion();
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
