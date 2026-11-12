package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.io.IOException;
import java.util.Arrays;

public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensajeError;

    @FXML
    public void initialize() {
        lblMensajeError.setVisible(false);
        lblMensajeError.setManaged(false);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String nombre = txtUsuario.getText();
        char[] password = txtPassword.getText().toCharArray();
        try {
            SesionUsuario sesion = new AutenticacionService(
                    new HibernateTransactions(HibernateUtil.getSessionFactory()))
                    .autenticar(nombre, password);
            abrirDashboard(event);
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (IllegalStateException e) {
            mostrarError("No se pudo conectar con la base de datos. Compruebe la configuración local.");
        } finally {
            Arrays.fill(password, '\0');
            txtPassword.clear();
        }
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
}
