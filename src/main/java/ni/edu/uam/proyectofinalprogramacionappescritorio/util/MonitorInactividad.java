package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Supervisa la inactividad local del usuario en JavaFX (RF-M01-07 / FLU-05)
 * con 0 hilos adicionales (Timeline en hilo FX) y sin extender la caducidad
 * fija de 15 minutos del token Bearer (D-P0-2).
 */
public final class MonitorInactividad {
    public enum EstadoEvaluacion { ACTIVO, AVISAR, EXPIRADO }

    public static final Duration UMBRAL_POR_DEFECTO = Duration.ofMinutes(15);
    public static final Duration AVISO_PREVIO_POR_DEFECTO = Duration.ofSeconds(60);

    private final Clock reloj;
    private final Duration umbral;
    private final Duration avisoPrevio;
    private final Runnable alAvisar;
    private final Runnable alExpirar;

    private Instant ultimaActividad;
    private boolean avisoEmitido;
    private boolean expirado;
    private Timeline temporizador;
    private Scene escenaVinculada;
    private EventHandler<InputEvent> filtroActividad;

    public MonitorInactividad(Duration umbral, Duration avisoPrevio, Clock reloj,
                              Runnable alAvisar, Runnable alExpirar) {
        this.reloj = Objects.requireNonNull(reloj, "Falta el reloj.");
        this.umbral = Objects.requireNonNull(umbral, "Falta el umbral de inactividad.");
        if (this.umbral.isZero() || this.umbral.isNegative()) {
            throw new IllegalArgumentException("El umbral de inactividad debe ser positivo.");
        }
        Duration previo = Objects.requireNonNull(avisoPrevio, "Falta el tiempo de aviso previo.");
        this.avisoPrevio = previo.compareTo(this.umbral) >= 0
                ? Duration.ofMillis(Math.max(1L, this.umbral.toMillis() / 2L))
                : previo;
        this.alAvisar = alAvisar == null ? () -> { } : alAvisar;
        this.alExpirar = alExpirar == null ? () -> { } : alExpirar;
        this.ultimaActividad = reloj.instant();
    }

    public static MonitorInactividad crear(int minutosInactividad, Runnable alAvisar, Runnable alExpirar) {
        int minutos = (minutosInactividad >= 5 && minutosInactividad <= 60) ? minutosInactividad : 15;
        return new MonitorInactividad(
                Duration.ofMinutes(minutos),
                AVISO_PREVIO_POR_DEFECTO,
                Clock.systemUTC(),
                alAvisar,
                alExpirar);
    }

    public synchronized void registrarActividad() {
        registrarActividad(reloj.instant());
    }

    public synchronized void registrarActividad(Instant instante) {
        if (expirado) return;
        this.ultimaActividad = Objects.requireNonNull(instante);
        this.avisoEmitido = false;
    }

    public synchronized EstadoEvaluacion evaluar() {
        return evaluar(reloj.instant());
    }

    public synchronized EstadoEvaluacion evaluar(Instant ahora) {
        if (expirado) {
            return EstadoEvaluacion.EXPIRADO;
        }
        Duration inactivo = Duration.between(ultimaActividad, ahora);
        if (inactivo.compareTo(umbral) >= 0) {
            expirado = true;
            return EstadoEvaluacion.EXPIRADO;
        }
        Duration inicioAviso = umbral.minus(avisoPrevio);
        if (!avisoEmitido && inactivo.compareTo(inicioAviso) >= 0) {
            avisoEmitido = true;
            return EstadoEvaluacion.AVISAR;
        }
        return EstadoEvaluacion.ACTIVO;
    }

    public synchronized Duration tiempoRestante(Instant ahora) {
        Duration restante = umbral.minus(Duration.between(ultimaActividad, ahora));
        return restante.isNegative() ? Duration.ZERO : restante;
    }

    public void vincularEscena(Scene escena) {
        detener();
        if (escena == null) return;
        this.escenaVinculada = escena;
        this.filtroActividad = evento -> registrarActividad();
        escena.addEventFilter(InputEvent.ANY, filtroActividad);
        registrarActividad();
        temporizador = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> verificarPulso()));
        temporizador.setCycleCount(Animation.INDEFINITE);
        temporizador.play();
    }

    private void verificarPulso() {
        EstadoEvaluacion estado = evaluar();
        if (estado == EstadoEvaluacion.AVISAR) {
            alAvisar.run();
        } else if (estado == EstadoEvaluacion.EXPIRADO) {
            detener();
            alExpirar.run();
        }
    }

    public void pausarEvaluacion() {
        if (temporizador != null) temporizador.pause();
    }

    public void reanudarEvaluacion() {
        if (temporizador != null) temporizador.play();
    }

    public void detener() {
        if (temporizador != null) {
            temporizador.stop();
            temporizador = null;
        }
        if (escenaVinculada != null && filtroActividad != null) {
            escenaVinculada.removeEventFilter(InputEvent.ANY, filtroActividad);
        }
        escenaVinculada = null;
        filtroActividad = null;
    }
}
