package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ejecuta validaciones remotas con pausa corta tras la edición, evita solicitudes
 * duplicadas para el mismo valor y descarta respuestas obsoletas o fuera de orden.
 */
public final class ValidadorAsincrono {
    private static final Duration PAUSA_DEFECTO = Duration.millis(280);

    private final TareasSegundoPlano tareas;
    private final Executor executorOpcional;
    private final Duration pausa;
    private final AtomicLong secuencia = new AtomicLong();
    private PauseTransition temporizador;
    private String ultimaClaveSolicitada;

    public ValidadorAsincrono() {
        this(null, null, PAUSA_DEFECTO);
    }

    public ValidadorAsincrono(TareasSegundoPlano tareas) {
        this(tareas, null, PAUSA_DEFECTO);
    }

    public ValidadorAsincrono(TareasSegundoPlano tareas, Duration pausa) {
        this(tareas, null, pausa);
    }

    public ValidadorAsincrono(Executor executorOpcional, Duration pausa) {
        this(null, executorOpcional, pausa);
    }

    private ValidadorAsincrono(TareasSegundoPlano tareas, Executor executorOpcional, Duration pausa) {
        this.tareas = tareas;
        this.executorOpcional = executorOpcional;
        this.pausa = pausa == null || pausa.lessThan(Duration.ZERO) ? PAUSA_DEFECTO : pausa;
    }

    public <T> void solicitar(String claveNormalizada,
                              BooleanSupplier sigueVigente,
                              Supplier<T> consulta,
                              Consumer<T> alResponder,
                              Consumer<Throwable> alFallar) {
        Objects.requireNonNull(consulta, "consulta");
        Objects.requireNonNull(alResponder, "alResponder");
        String clave = claveNormalizada == null ? "" : claveNormalizada;
        if (clave.equals(ultimaClaveSolicitada)) {
            return;
        }
        cancelarTemporizador();
        long ticket = secuencia.incrementAndGet();
        ultimaClaveSolicitada = clave;

        Runnable lanzar = () -> {
            if (secuencia.get() != ticket || (sigueVigente != null && !sigueVigente.getAsBoolean())) {
                return;
            }
            ejecutarConsulta(ticket, sigueVigente, consulta, alResponder, alFallar);
        };

        if (pausa.toMillis() <= 0) {
            lanzar.run();
            return;
        }
        temporizador = new PauseTransition(pausa);
        temporizador.setOnFinished(ev -> lanzar.run());
        temporizador.playFromStart();
    }

    public void cancelar() {
        secuencia.incrementAndGet();
        cancelarTemporizador();
    }

    public void reiniciar() {
        ultimaClaveSolicitada = null;
        cancelar();
    }

    private void cancelarTemporizador() {
        if (temporizador != null) {
            temporizador.stop();
            temporizador = null;
        }
    }

    private <T> void ejecutarConsulta(long ticket,
                                      BooleanSupplier sigueVigente,
                                      Supplier<T> consulta,
                                      Consumer<T> alResponder,
                                      Consumer<Throwable> alFallar) {
        if (executorOpcional != null) {
            executorOpcional.execute(() -> {
                try {
                    T resultado = consulta.get();
                    Platform.runLater(() -> {
                        if (secuencia.get() == ticket && (sigueVigente == null || sigueVigente.getAsBoolean())) {
                            alResponder.accept(resultado);
                        }
                    });
                } catch (Throwable error) {
                    Platform.runLater(() -> {
                        if (secuencia.get() == ticket && (sigueVigente == null || sigueVigente.getAsBoolean())) {
                            if (alFallar != null) {
                                alFallar.accept(error);
                            }
                        }
                    });
                }
            });
            return;
        }

        TareasSegundoPlano pool = resolverPool();
        if (pool == null) {
            return;
        }
        Task<T> tarea = new Task<>() {
            @Override protected T call() {
                return consulta.get();
            }
        };
        TareasJavaFx.ejecutar(pool, tarea, resultado -> {
            if (secuencia.get() == ticket && (sigueVigente == null || sigueVigente.getAsBoolean())) {
                alResponder.accept(resultado);
            }
        }, error -> {
            if (secuencia.get() == ticket && (sigueVigente == null || sigueVigente.getAsBoolean())) {
                if (alFallar != null) {
                    alFallar.accept(error);
                }
            }
        });
    }

    private TareasSegundoPlano resolverPool() {
        if (tareas != null) {
            return tareas;
        }
        try {
            return LatchApplication.instancia().getTareas();
        } catch (IllegalStateException sinAplicacion) {
            return null;
        }
    }
}
