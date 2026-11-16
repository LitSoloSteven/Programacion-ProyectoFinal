package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;


/** Construye la carcasa accesible común para diálogos de formularios. */
public final class DialogoFormulario {
    private DialogoFormulario() { }

    public static Dialog<ButtonType> crear(String titulo, Node contenido, Window propietario,
                                           double ancho, double altoMaximo,
                                           String textoAccion) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        if (propietario != null) dialogo.initOwner(propietario);
        DialogPane pane = dialogo.getDialogPane();
        pane.getStyleClass().add("form-dialog");
        pane.setContent(contenido);
        pane.setPrefWidth(ancho);
        pane.setMaxHeight(altoMaximo);
        pane.getButtonTypes().setAll(
                new ButtonType(textoAccion, ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        TemaAplicacion.aplicar(pane);
        return dialogo;
    }
}

