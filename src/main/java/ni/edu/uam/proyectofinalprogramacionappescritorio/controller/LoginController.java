package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.concurrent.Task;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.io.IOException;

/**
 * Inicio de sesión contra la API local. La identidad resultante se establece
 * como sesión de JavaFX para que los servicios existentes sigan funcionando.
 */
public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensajeError;
    @FXML private Button btnIngresar;

    @FXML
    public void initialize() {
        lblMensajeError.setVisible(false);
        lblMensajeError.setManaged(false);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String nombre = txtUsuario.getText();
        String password = txtPassword.getText();
        if (nombre == null || nombre.isBlank()) {
            mostrarError("Ingrese su nombre de usuario.");
            txtUsuario.requestFocus();
            return;
        }
        if (password == null || password.isEmpty()) {
            mostrarError("Ingrese su contraseña.");
            txtPassword.requestFocus();
            return;
        }

        ocultarError();
        btnIngresar.setDisable(true);
        Task<LoginResponse> tarea = new Task<>() {
            @Override
            protected LoginResponse call() {
                return new ClienteAuth(ClienteLatch.api()).login(nombre, password);
            }
        };
        tarea.setOnSucceeded(evento -> {
            try {
                LoginResponse respuesta = tarea.getValue();
                SesionUsuario.establecer(SesionUsuario.paraSesionAutenticada(
                        respuesta.usuarioId(), respuesta.nombre(), respuesta.rol()));
                abrirDashboard(event);
            } catch (RuntimeException e) {
                SesionUsuario.cerrarSesion();
                mostrarError("La respuesta de sesión del servicio local no es válida.");
            } finally {
                finalizarIntento();
            }
        });
        tarea.setOnFailed(evento -> {
            mostrarError(mensaje(tarea.getException()));
            finalizarIntento();
        });
        try {
            HelloApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException e) {
            btnIngresar.setDisable(false);
            mostrarError("No se pudo iniciar la operación. Inténtelo nuevamente.");
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
            Parent root = new FXMLLoader(HelloApplication.class.getResource("dashboard-view.fxml")).load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1100, 720);
            var css = HelloApplication.class.getResource("estilos.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("Latch — Sistema de Gestión Inmobiliaria");
            stage.setScene(scene);
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            SesionUsuario.cerrarSesion();
            mostrarError("No se pudo cargar la vista principal del sistema.");
        }
    }

    private void mostrarError(String mensaje) {
        lblMensajeError.setText(mensaje == null ? "No se pudo iniciar sesión." : mensaje);
        lblMensajeError.setVisible(true);
        lblMensajeError.setManaged(true);
    }

    private void ocultarError() {
        lblMensajeError.setVisible(false);
        lblMensajeError.setManaged(false);
    }

    private void finalizarIntento() {
        txtPassword.clear();
        btnIngresar.setDisable(false);
    }
}