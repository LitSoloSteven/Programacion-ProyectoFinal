package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** Mensajes y confirmaciones JavaFX con tema y textos de acción explícitos. */
public final class AlertaUtil {
    private AlertaUtil() { }

    public static void mostrarExito(String titulo, String mensaje) {
        String limpio = sanitizar(mensaje, "Operación completada correctamente.");
        ejecutarEnFx(() -> {
            if (!Notificaciones.mostrarExito(limpio)) mostrar(Alert.AlertType.INFORMATION, titulo, limpio);
        });
    }

    public static void mostrarAdvertencia(String titulo, String mensaje) {
        String limpio = sanitizar(mensaje, "Revisa la información e inténtalo de nuevo.");
        ejecutarEnFx(() -> mostrar(Alert.AlertType.WARNING, titulo, limpio));
    }

    public static void mostrarError(String titulo, String mensaje) {
        String limpio = sanitizar(mensaje, "No se pudo completar la operación. Vuelve a intentarlo.");
        ejecutarEnFx(() -> mostrar(Alert.AlertType.ERROR, titulo, limpio));
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        return mostrarConfirmacion(titulo, mensaje, "Continuar");
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje, String accion) {
        if (!Platform.isFxApplicationThread()) return false;
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, sanitizar(mensaje, ""));
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        alerta.getButtonTypes().setAll(cancelar, confirmar);
        estilizarBotones(alerta, confirmar, cancelar, false);
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
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, titulo, sanitizar(detalle, ""));
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        alerta.getButtonTypes().setAll(cancelar, confirmar);
        estilizarBotones(alerta, confirmar, cancelar, true);
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
        estilizarBotones(alerta, descartar, cancelar, true);
        TemaAplicacion.aplicar(alerta.getDialogPane());
        Optional<ButtonType> resultado = alerta.showAndWait();
        return resultado.isPresent() && resultado.get() == descartar;
    }

    private static void estilizarBotones(Alert alerta, ButtonType principal, ButtonType secundario, boolean destructiva) {
        Node btnPrincipal = alerta.getDialogPane().lookupButton(principal);
        if (btnPrincipal instanceof ButtonBase boton) {
            boton.getStyleClass().add(destructiva ? "button-destructive" : "button-primary");
        }
        Node btnSecundario = alerta.getDialogPane().lookupButton(secundario);
        if (btnSecundario instanceof ButtonBase boton) {
            boton.getStyleClass().add("button-secondary");
        }
    }

    private static String sanitizar(String mensaje, String respaldo) {
        String limpio = MensajesUi.limpiarParaUsuario(mensaje);
        return limpio.isBlank() ? respaldo : limpio;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        crear(tipo, titulo, mensaje).showAndWait();
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo == null ? "Latch" : MensajesUi.limpiarParaUsuario(titulo));
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
