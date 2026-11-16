package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import javafx.application.Platform;
import java.util.function.BooleanSupplier;


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
        Node botonAccion = pane.lookupButton(pane.getButtonTypes().getFirst());
        Node botonCancelar = pane.lookupButton(pane.getButtonTypes().get(1));
        if (botonAccion instanceof ButtonBase boton) Iconos.asignar(boton, Iconos.Nombre.CONFIRMACION, 14);
        if (botonCancelar instanceof ButtonBase boton) Iconos.asignar(boton, Iconos.Nombre.CERRAR, 14);
        TemaAplicacion.aplicar(pane);
        dialogo.setOnShown(evento -> TransicionesUI.entrada(pane));
        return dialogo;
    }

    public static void protegerCambiosSinGuardar(Dialog<?> dialogo, Node disparador,
                                                 BooleanSupplier modificado) {
        boolean[] cerrando = {false};
        dialogo.setOnCloseRequest(evento -> {
            if (cerrando[0]) {
                evento.consume();
                return;
            }
            if (modificado.getAsBoolean() && !AlertaUtil.confirmarDescartarCambios()) {
                evento.consume();
                return;
            }
            evento.consume();
            cerrando[0] = true;
            TransicionesUI.salida(dialogo.getDialogPane(), () -> {
                dialogo.setOnCloseRequest(null);
                dialogo.close();
            });
        });
        dialogo.setOnHidden(evento -> {
            if (disparador != null) Platform.runLater(disparador::requestFocus);
        });
    }

    public static void cerrar(Dialog<?> dialogo) {
        if (dialogo == null) return;
        dialogo.setOnCloseRequest(null);
        TransicionesUI.salida(dialogo.getDialogPane(), dialogo::close);
    }
}

