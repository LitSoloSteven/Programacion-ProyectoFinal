package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilidad uniforme para la presentación de mensajes y confirmaciones visuales en JavaFX.
 * Garantiza ejecución segura en el hilo de aplicación de JavaFX (FX Application Thread).
 */
public class AlertaUtil {

    public static void mostrarExito(String titulo, String mensaje) {
        ejecutarEnFx(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo != null ? titulo : "Éxito");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public static void mostrarAdvertencia(String titulo, String mensaje) {
        ejecutarEnFx(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo != null ? titulo : "Advertencia");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public static void mostrarError(String titulo, String mensaje) {
        ejecutarEnFx(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo != null ? titulo : "Error");
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        if (!Platform.isFxApplicationThread()) {
            return false;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo != null ? titulo : "Confirmación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    private static void ejecutarEnFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
        } else {
            Platform.runLater(accion);
        }
    }
}
