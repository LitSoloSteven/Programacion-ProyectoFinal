package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.lang.ref.WeakReference;

/** Toast breve y cerrable para éxitos de operación. */
public final class Notificaciones {
    private static WeakReference<StackPane> anfitrion = new WeakReference<>(null);

    private Notificaciones() { }

    public static void registrarAnfitrion(StackPane nodo) {
        if (nodo != null) anfitrion = new WeakReference<>(nodo);
    }

    /** Devuelve false si no hay un anfitrión visible y debe usarse el diálogo alternativo. */
    public static boolean mostrarExito(String mensaje) {
        StackPane root = anfitrion.get();
        if (root == null || root.getScene() == null) return false;
        HBox toast = new HBox(10);
        toast.getStyleClass().add("toast");
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMaxWidth(440);
        toast.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        toast.setAccessibleText(mensaje);

        javafx.scene.Node icono = Iconos.crear(Iconos.Nombre.CONFIRMACION, 16);
        icono.getStyleClass().add("toast-icon");
        Label texto = new Label(mensaje);
        texto.setWrapText(true);
        texto.setMaxWidth(350);
        HBox.setHgrow(texto, javafx.scene.layout.Priority.ALWAYS);
        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().add("toast-close");
        cerrar.setAccessibleText("Cerrar notificación");
        toast.getChildren().addAll(icono, texto, cerrar);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(16));
        root.getChildren().add(toast);

        PauseTransition tiempo = new PauseTransition(Duration.seconds(4));
        Runnable quitar = () -> {
            if (root.getChildren().contains(toast)) root.getChildren().remove(toast);
        };
        cerrar.setOnAction(event -> {
            tiempo.stop();
            quitar.run();
        });
        tiempo.setOnFinished(event -> {
            if (TemaAplicacion.isMovimientoReducido()) quitar.run();
            else {
                FadeTransition fade = new FadeTransition(Duration.millis(120), toast);
                fade.setToValue(0);
                fade.setOnFinished(fin -> quitar.run());
                fade.play();
            }
        });
        tiempo.play();
        return true;
    }
}

