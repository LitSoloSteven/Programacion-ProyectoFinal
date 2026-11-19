package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import lombok.Getter;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

/** Aplica de forma centralizada el tema y las preferencias de movimiento. */
public final class TemaAplicacion {
    public enum Tema { CLARO, OSCURO }

    private static final String CSS = "/ni/edu/uam/proyectofinalprogramacionappescritorio/estilos.css";
    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(TemaAplicacion.class);
    private static final Map<Scene, Boolean> ESCENAS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<DialogPane, Boolean> DIALOGOS = Collections.synchronizedMap(new WeakHashMap<>());
    @Getter
    private static volatile Tema tema = leerTema();
    @Getter
    private static volatile boolean movimientoReducido =
            PREFERENCIAS.getBoolean("movimientoReducido", false);

    private TemaAplicacion() { }

    /** Debe llamarse antes de crear la primera Scene. */
    public static void inicializar() {
        cargarFuente("fonts/FiraSans-Regular.ttf");
        cargarFuente("fonts/FiraCode-Regular.ttf");
    }

    private static void cargarFuente(String recurso) {
        try (InputStream fuente = TemaAplicacion.class.getResourceAsStream(recurso)) {
            if (fuente != null) Font.loadFont(fuente, 14);
        } catch (Exception ignored) {
            // La interfaz usa Segoe UI y Consolas si las fuentes no están empaquetadas.
        }
    }

    public static void aplicar(Scene scene) {
        if (scene == null) return;
        if (Platform.isFxApplicationThread()) aplicarEscena(scene);
        else Platform.runLater(() -> aplicarEscena(scene));
    }

    private static void aplicarEscena(Scene scene) {
        var hoja = TemaAplicacion.class.getResource(CSS);
        if (hoja != null && !scene.getStylesheets().contains(hoja.toExternalForm())) {
            scene.getStylesheets().add(hoja.toExternalForm());
        }
        ESCENAS.put(scene, Boolean.TRUE);
        aplicarClase(scene.getRoot());
        if (scene.getRoot() instanceof StackPane stack) Notificaciones.registrarAnfitrion(stack);
    }

    public static void aplicar(DialogPane pane) {
        if (pane == null) return;
        if (Platform.isFxApplicationThread()) aplicarDialogo(pane);
        else Platform.runLater(() -> aplicarDialogo(pane));
    }

    private static void aplicarDialogo(DialogPane pane) {
        var hoja = TemaAplicacion.class.getResource(CSS);
        if (hoja != null && !pane.getStylesheets().contains(hoja.toExternalForm())) {
            pane.getStylesheets().add(hoja.toExternalForm());
        }
        DIALOGOS.put(pane, Boolean.TRUE);
        aplicarClase(pane);
    }

    private static void aplicarClase(Node node) {
        if (node == null) return;
        var clases = node.getStyleClass();
        clases.removeAll("theme-light", "theme-dark", "reduced-motion");
        clases.add(tema == Tema.OSCURO ? "theme-dark" : "theme-light");
        if (movimientoReducido) clases.add("reduced-motion");
    }

    public static void establecerTema(Tema nuevoTema) {
        if (nuevoTema == null) return;
        tema = nuevoTema;
        PREFERENCIAS.put("tema", nuevoTema.name());
        ejecutarFx(() -> {
            synchronized (ESCENAS) {
                ESCENAS.keySet().forEach(scene -> aplicarClase(scene.getRoot()));
            }
            synchronized (DIALOGOS) {
                DIALOGOS.keySet().forEach(TemaAplicacion::aplicarClase);
            }
        });
    }

    public static void establecerMovimientoReducido(boolean reducido) {
        movimientoReducido = reducido;
        PREFERENCIAS.putBoolean("movimientoReducido", reducido);
        ejecutarFx(() -> {
            if (reducido) TransicionesUI.finalizarActivas();
            synchronized (ESCENAS) {
                ESCENAS.keySet().forEach(scene -> aplicarClase(scene.getRoot()));
            }
            synchronized (DIALOGOS) {
                DIALOGOS.keySet().forEach(TemaAplicacion::aplicarClase);
            }
        });
    }

    private static Tema leerTema() {
        try {
            return Tema.valueOf(PREFERENCIAS.get("tema", Tema.CLARO.name()));
        } catch (IllegalArgumentException error) {
            return Tema.CLARO;
        }
    }

    private static void ejecutarFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) accion.run();
        else Platform.runLater(accion);
    }
}

