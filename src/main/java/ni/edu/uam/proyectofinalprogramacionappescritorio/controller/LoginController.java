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
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;

import java.io.IOException;

/**
 * Inicio de sesión contra la API local. La identidad resultante se establece
 * como sesión de JavaFX para que los servicios existentes sigan funcionando.
 */
public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblErrorUsuario;
    @FXML private Label lblErrorPassword;
    @FXML private Button btnIngresar;

    @FXML
    public void initialize() {
        ocultarError(lblErrorUsuario);
        ocultarError(lblErrorPassword);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String nombre = txtUsuario.getText();
        String password = txtPassword.getText();
        ocultarError(lblErrorUsuario);
        ocultarError(lblErrorPassword);
        if (nombre == null || nombre.isBlank()) {
            mostrarError(lblErrorUsuario, "Escribe tu nombre de usuario.");
            txtUsuario.requestFocus();
            return;
        }
        if (password == null || password.isEmpty()) {
            mostrarError(lblErrorPassword, "Escribe tu contraseña.");
            txtPassword.requestFocus();
            return;
        }

        btnIngresar.setDisable(true);
        btnIngresar.setText("Ingresando…");
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
                mostrarError(lblErrorPassword, "No fue posible validar la sesión. Inténtalo nuevamente.");
            } finally {
                finalizarIntento();
            }
        });
        tarea.setOnFailed(evento -> {
            mostrarError(lblErrorPassword, mensaje(tarea.getException()));
            finalizarIntento();
            txtPassword.requestFocus();
        });
        try {
            HelloApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException e) {
            btnIngresar.setDisable(false);
            btnIngresar.setText("Ingresar");
            mostrarError(lblErrorPassword, "No se pudo iniciar sesión. Inténtalo nuevamente.");
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
        campo.setManaged(true);
    }

    private void finalizarIntento() {
        txtPassword.clear();
        btnIngresar.setDisable(false);
        btnIngresar.setText("Ingresar");
    }
}
