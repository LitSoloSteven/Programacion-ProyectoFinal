package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Conserva el detalle por campo y anuncia los errores del intento de guardado. */
public final class ValidacionFormulario {
    private ValidacionFormulario() { }

    public static void mostrar(Label resumen, Label... campos) {
        String detalle = Arrays.stream(campos)
                .filter(Label::isVisible)
                .map(Label::getText)
                .filter(texto -> texto != null && !texto.isBlank())
                .collect(Collectors.joining("\n"));
        if (detalle.isBlank()) return;
        resumen.setText(detalle);
        resumen.setVisible(true);
        resumen.setManaged(true);
        resumen.setFocusTraversable(true);
        for (Node nodo = resumen; nodo != null; nodo = nodo.getParent()) {
            if (nodo instanceof ScrollPane scroll) scroll.setVvalue(scroll.getVmin());
        }
        if (resumen.getScene() != null && resumen.getScene().getWindow() != null
                && resumen.getScene().getWindow().isShowing()) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.initOwner(resumen.getScene().getWindow());
            alerta.setTitle("Revisa el formulario");
            alerta.setHeaderText("Hay campos vacíos o incorrectos");
            alerta.setContentText(detalle);
            TemaAplicacion.aplicar(alerta.getDialogPane());
            alerta.showAndWait();
        }
        resumen.requestFocus();
    }
}
