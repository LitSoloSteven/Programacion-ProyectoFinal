package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Transiciones breves que se pueden interrumpir y omitir con movimiento reducido. */
public final class TransicionesUI {
    private static final Map<Node, Animation> ACTIVAS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Node, Double> DESTINOS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Node, Runnable> AL_FINALIZAR = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Duration ENTRADA = Duration.millis(170);
    private static final Duration SALIDA = Duration.millis(110);

    private TransicionesUI() { }

    public static void reemplazar(StackPane contenedor, Node vista) {
        if (contenedor == null || vista == null) return;
        for (Node anterior : contenedor.getChildren()) cancelar(anterior, 1);
        contenedor.getChildren().setAll(vista);
        entrada(vista, true);
    }

    public static void entrada(Node nodo) { entrada(nodo, false); }

    private static void entrada(Node nodo, boolean nuevaVista) {
        if (nodo == null) return;
        Animation anterior = extraer(nodo);
        boolean interrumpida = anterior != null && anterior.getStatus() == Animation.Status.RUNNING;
        if (anterior != null) anterior.stop();
        if (TemaAplicacion.isMovimientoReducido()) {
            finalizar(nodo, 1);
            return;
        }
        if (!interrumpida || nuevaVista) {
            nodo.setOpacity(0);
            nodo.setScaleX(0.98);
            nodo.setScaleY(0.98);
        }
        FadeTransition fundido = new FadeTransition(ENTRADA, nodo);
        fundido.setToValue(1);
        ScaleTransition escala = new ScaleTransition(ENTRADA, nodo);
        escala.setToX(1);
        escala.setToY(1);
        ParallelTransition transicion = new ParallelTransition(fundido, escala);
        registrar(nodo, transicion, 1, null);
        transicion.setOnFinished(evento -> {
            quitar(nodo, transicion);
            finalizar(nodo, 1);
        });
        transicion.play();
    }

    public static void salida(Node nodo, Runnable alFinalizar) {
        if (nodo == null || TemaAplicacion.isMovimientoReducido() || nodo.getScene() == null) {
            if (nodo != null) finalizar(nodo, 0);
            if (alFinalizar != null) alFinalizar.run();
            return;
        }
        Animation anterior = extraer(nodo);
        if (anterior != null) anterior.stop();
        FadeTransition fundido = new FadeTransition(SALIDA, nodo);
        fundido.setToValue(0);
        ScaleTransition escala = new ScaleTransition(SALIDA, nodo);
        escala.setToX(0.98);
        escala.setToY(0.98);
        ParallelTransition transicion = new ParallelTransition(fundido, escala);
        registrar(nodo, transicion, 0, alFinalizar);
        transicion.setOnFinished(evento -> {
            quitar(nodo, transicion);
            finalizar(nodo, 0);
            if (alFinalizar != null) alFinalizar.run();
        });
        transicion.play();
    }

    public static void finalizarActivas() {
        Map<Node, Animation> activas;
        Map<Node, Double> destinos;
        Map<Node, Runnable> callbacks;
        synchronized (ACTIVAS) {
            activas = Map.copyOf(ACTIVAS);
            destinos = Map.copyOf(DESTINOS);
            callbacks = Map.copyOf(AL_FINALIZAR);
            ACTIVAS.clear();
            DESTINOS.clear();
            AL_FINALIZAR.clear();
        }
        activas.forEach((nodo, animacion) -> animacion.stop());
        destinos.forEach(TransicionesUI::finalizar);
        callbacks.values().forEach(Runnable::run);
    }

    private static void registrar(Node nodo, Animation animacion, double destino, Runnable callback) {
        synchronized (ACTIVAS) {
            ACTIVAS.put(nodo, animacion);
            DESTINOS.put(nodo, destino);
            if (callback != null) AL_FINALIZAR.put(nodo, callback);
        }
    }

    private static Animation extraer(Node nodo) {
        synchronized (ACTIVAS) {
            DESTINOS.remove(nodo);
            AL_FINALIZAR.remove(nodo);
            return ACTIVAS.remove(nodo);
        }
    }

    private static void quitar(Node nodo, Animation animacion) {
        synchronized (ACTIVAS) {
            if (ACTIVAS.get(nodo) == animacion) {
                ACTIVAS.remove(nodo);
                DESTINOS.remove(nodo);
                AL_FINALIZAR.remove(nodo);
            }
        }
    }

    private static void cancelar(Node nodo, double destino) {
        Animation anterior = extraer(nodo);
        if (anterior != null) anterior.stop();
        finalizar(nodo, destino);
    }

    private static void finalizar(Node nodo, double opacidad) {
        nodo.setOpacity(opacidad);
        nodo.setScaleX(1);
        nodo.setScaleY(1);
    }
}
