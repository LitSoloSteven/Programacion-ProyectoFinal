package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** Mensajes y confirmaciones JavaFX con tema y textos de acción explícitos. */
public final class AlertaUtil {
    private AlertaUtil() { }

    public static void mostrarExito(String titulo, String mensaje) {
        ejecutarEnFx(() -> {
            if (!Notificaciones.mostrarExito(mensaje)) mostrar(Alert.AlertType.INFORMATION, titulo, mensaje);
        });
    }

    public static void mostrarAdvertencia(String titulo, String mensaje) {
        ejecutarEnFx(() -> mostrar(Alert.AlertType.WARNING, titulo, mensaje));
    }

    public static void mostrarError(String titulo, String mensaje) {
        ejecutarEnFx(() -> mostrar(Alert.AlertType.ERROR, titulo, mensaje));
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        return mostrarConfirmacion(titulo, mensaje, "Continuar");
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje, String accion) {
        if (!Platform.isFxApplicationThread()) return false;
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, mensaje);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        alerta.getButtonTypes().setAll(cancelar, confirmar);
        TemaAplicacion.aplicar(alerta.getDialogPane());
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == confirmar;
    }

    public static boolean mostrarConfirmacionDestructiva(String titulo, String registro,
                                                         String consecuencia, String accion) {
        if (!Platform.isFxApplicationThread()) return false;
        return mostrarConfirmacionDestructiva(titulo, registro + System.lineSeparator()
                + System.lineSeparator() + consecuencia, accion);
    }

    public static boolean mostrarConfirmacionDestructiva(String titulo, String detalle, String accion) {
        if (!Platform.isFxApplicationThread()) return false;
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, detalle);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        alerta.getButtonTypes().setAll(cancelar, confirmar);
        TemaAplicacion.aplicar(alerta.getDialogPane());
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == confirmar;
    }

    public static boolean confirmarDescartarCambios() {
        if (!Platform.isFxApplicationThread()) return false;
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, "Descartar cambios",
                "Los cambios del formulario se perderán.");
        ButtonType cancelar = new ButtonType("Seguir editando", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType descartar = new ButtonType("Descartar cambios", ButtonBar.ButtonData.OK_DONE);
        alerta.getButtonTypes().setAll(cancelar, descartar);
        TemaAplicacion.aplicar(alerta.getDialogPane());
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == descartar;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        crear(tipo, titulo, mensaje).showAndWait();
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo == null ? "Latch" : titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje == null ? "" : mensaje);
        TemaAplicacion.aplicar(alerta.getDialogPane());
        return alerta;
    }

    private static void ejecutarEnFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) accion.run();
        else Platform.runLater(accion);
    }
}

