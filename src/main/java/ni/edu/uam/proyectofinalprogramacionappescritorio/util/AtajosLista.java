package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Atajos comunes que respetan la edición de texto y selección del usuario. */
public final class AtajosLista {
    private AtajosLista() { }

    public static void instalar(Node raiz, Node busqueda, Runnable nuevo) {
        EventHandler<KeyEvent> manejador = evento -> {
            if (evento.getCode() == KeyCode.F && evento.isControlDown()) {
                if (busqueda != null) {
                    busqueda.requestFocus();
                    evento.consume();
                }
            } else if (evento.getCode() == KeyCode.N && evento.isControlDown()) {
                if (!esEditor(evento.getTarget()) && nuevo != null) {
                    nuevo.run();
                    evento.consume();
                }
            } else if (evento.getCode() == KeyCode.DELETE && !esEditor(evento.getTarget())) {
                // Delete queda reservado; ninguna acción destructiva se ejecuta con este atajo.
                evento.consume();
            }
        };
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, manejador);
    }

    private static boolean esEditor(Object objetivo) {
        if (!(objetivo instanceof Node nodo)) return false;
        return nodo instanceof TextInputControl
                || nodo instanceof ComboBoxBase<?>
                || nodo instanceof Spinner<?>
                || nodo.isFocusTraversable() && nodo.getClass().getSimpleName().contains("Text");
    }
}
