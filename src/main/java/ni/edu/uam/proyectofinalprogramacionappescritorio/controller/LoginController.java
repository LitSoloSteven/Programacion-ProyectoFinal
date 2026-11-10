package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AuthService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controlador de la vista de inicio de sesión de Latch.
 * Gestiona autenticación contra PostgreSQL y transición fluida al Dashboard.
 */
public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMensajeError;

    @FXML
    private Button btnIngresar;

    private final AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        ocultarError();
        txtUsuario.textProperty().addListener((obs, oldVal, newVal) -> ocultarError());
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> ocultarError());
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();

        if (usuario == null || usuario.trim().isEmpty()) {
            mostrarError("Por favor ingrese su nombre de usuario.");
            txtUsuario.requestFocus();
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            mostrarError("Por favor ingrese su contraseña.");
            txtPassword.requestFocus();
            return;
        }

        try {
            Usuario usuarioAutenticado = authService.iniciarSesion(usuario, password);
            if (usuarioAutenticado != null) {
                abrirDashboard(event);
            }
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            mostrarError("Error al conectar con la base de datos PostgreSQL. Verifique el servicio.");
        } catch (Exception e) {
            mostrarError("Ocurrió un error inesperado al procesar la solicitud.");
        }
    }

    private void abrirDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1100, 720);

            // Cargar estilos CSS
            String css = HelloApplication.class.getResource("estilos.css") != null 
                    ? HelloApplication.class.getResource("estilos.css").toExternalForm() 
                    : null;
            if (css != null) {
                scene.getStylesheets().add(css);
            }

            stage.setTitle("Latch — Sistema de Gestión Inmobiliaria");
            stage.setScene(scene);
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            mostrarError("No se pudo cargar la vista principal del sistema.");
        }
    }

    private void mostrarError(String mensaje) {
        lblMensajeError.setText(mensaje);
        lblMensajeError.setVisible(true);
        lblMensajeError.setManaged(true);
    }

    private void ocultarError() {
        lblMensajeError.setText("");
        lblMensajeError.setVisible(false);
        lblMensajeError.setManaged(false);
    }
}
