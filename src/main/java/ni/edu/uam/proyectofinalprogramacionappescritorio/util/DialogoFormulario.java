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
        return crear(titulo, contenido, propietario, ancho, altoMaximo, textoAccion, false);
    }

    public static Dialog<ButtonType> crear(String titulo, Node contenido, Window propietario,
                                           double ancho, double altoMaximo,
                                           String textoAccion, boolean destructiva) {
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
        if (botonAccion instanceof ButtonBase boton) {
            boton.getStyleClass().add(destructiva ? "button-destructive" : "button-primary");
            Iconos.asignar(boton, destructiva ? Iconos.Nombre.ELIMINAR : Iconos.Nombre.CONFIRMACION, 14);
        }
        if (botonCancelar instanceof ButtonBase boton) {
            boton.getStyleClass().add("button-secondary");
            Iconos.asignar(boton, Iconos.Nombre.CERRAR, 14);
        }
        TemaAplicacion.aplicar(pane);
        dialogo.setOnShown(evento -> {
            TransicionesUI.entrada(pane);
            if (propietario != null && pane.getScene() != null && pane.getScene().getWindow() != null) {
                Window ventanaDialogo = pane.getScene().getWindow();
                Platform.runLater(() -> {
                    double yCentrada = propietario.getY()
                            + (propietario.getHeight() - ventanaDialogo.getHeight()) / 2.0;
                    ventanaDialogo.setY(Math.max(propietario.getY() + 20.0, yCentrada + 36.0));
                });
            }
        });
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
            Runnable cerrarDefinitivo = () -> {
                dialogo.setOnCloseRequest(null);
                Platform.runLater(() -> {
                    if (dialogo instanceof Dialog<?> d) {
                        @SuppressWarnings("unchecked")
                        Dialog<Object> objDialog = (Dialog<Object>) d;
                        objDialog.setResult(ButtonType.CANCEL);
                    }
                    if (dialogo.isShowing()) {
                        dialogo.close();
                    }
                    if (dialogo.getDialogPane() != null
                            && dialogo.getDialogPane().getScene() != null
                            && dialogo.getDialogPane().getScene().getWindow() != null
                            && dialogo.getDialogPane().getScene().getWindow().isShowing()) {
                        dialogo.getDialogPane().getScene().getWindow().hide();
                    }
                });
            };
            try {
                TransicionesUI.salida(dialogo.getDialogPane(), cerrarDefinitivo);
            } catch (RuntimeException ex) {
                cerrarDefinitivo.run();
            }
        });
        dialogo.setOnHidden(evento -> {
            if (disparador != null) Platform.runLater(disparador::requestFocus);
        });
    }

    public static void cerrar(Dialog<?> dialogo) {
        if (dialogo == null) return;
        dialogo.setOnCloseRequest(null);
        Runnable cerrarDefinitivo = () -> Platform.runLater(() -> {
            if (dialogo.isShowing()) {
                dialogo.close();
            }
            if (dialogo.getDialogPane() != null
                    && dialogo.getDialogPane().getScene() != null
                    && dialogo.getDialogPane().getScene().getWindow() != null
                    && dialogo.getDialogPane().getScene().getWindow().isShowing()) {
                dialogo.getDialogPane().getScene().getWindow().hide();
            }
        });
        try {
            TransicionesUI.salida(dialogo.getDialogPane(), cerrarDefinitivo);
        } catch (RuntimeException ex) {
            cerrarDefinitivo.run();
        }
    }
}
