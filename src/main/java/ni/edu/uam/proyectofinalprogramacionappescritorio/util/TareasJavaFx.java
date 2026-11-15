package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;

import java.util.Objects;
import java.util.function.Consumer;

/** Conecta tareas de segundo plano con sus actualizaciones de interfaz. */
public final class TareasJavaFx {
    private TareasJavaFx() {
    }

    public static <T> void ejecutar(TareasSegundoPlano ejecutor, Task<T> tarea,
                                    Consumer<? super T> alCompletar,
                                    Consumer<? super Throwable> alFallar) {
        Objects.requireNonNull(ejecutor, "ejecutor");
        Objects.requireNonNull(tarea, "tarea");
        Objects.requireNonNull(alCompletar, "alCompletar");
        Objects.requireNonNull(alFallar, "alFallar");

        tarea.setOnSucceeded(evento -> {
            try {
                alCompletar.accept(tarea.getValue());
            } catch (RuntimeException error) {
                alFallar.accept(error);
            }
        });
        tarea.setOnFailed(evento -> alFallar.accept(tarea.getException()));
        try {
            ejecutor.ejecutar(tarea);
        } catch (RuntimeException error) {
            alFallar.accept(error);
        }
    }
}
