package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.io.IOException;

/** Permite conservar el borrador mientras se informa que el token expiró. */
public final class SesionExpiradaUtil {
    private SesionExpiradaUtil() { }

    public static void mostrar(Window propietario, Dialog<?> formulario) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle("Tu sesión expiró");
        alerta.setHeaderText("Tu sesión expiró");
        alerta.setContentText("Puedes mantener este formulario abierto o cerrar sesión.");
        if (propietario != null) alerta.initOwner(propietario);
        ButtonType seguir = new ButtonType("Seguir aquí", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType cerrar = new ButtonType("Cerrar sesión", ButtonBar.ButtonData.OK_DONE);
        alerta.getButtonTypes().setAll(seguir, cerrar);
        TemaAplicacion.aplicar(alerta.getDialogPane());
        if (alerta.showAndWait().orElse(seguir) == cerrar) {
            cerrarSesion(propietario, formulario);
        }
    }

    private static void cerrarSesion(Window propietario, Dialog<?> formulario) {
        if (formulario != null) {
            formulario.setOnCloseRequest(null);
            formulario.setOnHidden(null);
            formulario.close();
        }
        SesionUsuario.cerrarSesion();
        ClienteApi api;
        String token;
        try {
            api = ClienteLatch.api();
            token = api.tomarToken();
        } catch (RuntimeException ignorado) {
            api = null;
            token = null;
        }
        if (api != null && token != null && !token.isBlank()) {
            ClienteApi apiSesion = api;
            String tokenSesion = token;
            Task<Void> revocar = new Task<>() {
                @Override protected Void call() {
                    try {
                        new ClienteAuth(apiSesion).logout(tokenSesion);
                    } catch (RuntimeException ignorado) {
                        // El token local ya quedó eliminado; su vigencia en el servidor es acotada.
                    }
                    return null;
                }
            };
            try {
                HelloApplication.instancia().getTareas().ejecutar(revocar);
            } catch (RuntimeException ignorado) {
                // El acceso local ya quedó cerrado.
            }
        }
        if (!(propietario instanceof Stage stage)) return;
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("login-view.fxml"));
            Scene scene = new Scene(root, 440, 520);
            TemaAplicacion.aplicar(scene);
            stage.setTitle("Latch — Iniciar sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMinWidth(440);
            stage.setMinHeight(520);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el acceso", "Inténtalo nuevamente para volver a iniciar sesión.");
        }
    }
}
