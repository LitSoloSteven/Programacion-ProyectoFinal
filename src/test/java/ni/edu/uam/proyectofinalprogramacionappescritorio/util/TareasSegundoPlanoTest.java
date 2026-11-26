package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TareasSegundoPlanoTest {
    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException yaIniciado) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS), "JavaFX debe iniciar");
    }

    @Test
    void ejecutaLaOperacionEnUnHiloDeTrabajo() throws InterruptedException {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch ejecutada = new CountDownLatch(1);
        CountDownLatch actualizada = new CountDownLatch(1);
        AtomicReference<Thread> hiloOperacion = new AtomicReference<>();
        AtomicReference<Boolean> enHiloJavaFx = new AtomicReference<>(false);
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                hiloOperacion.set(Thread.currentThread());
                ejecutada.countDown();
                return null;
            }
        };

        try {
            TareasJavaFx.ejecutar(tareas, tarea, ignorado -> {
                enHiloJavaFx.set(Platform.isFxApplicationThread());
                actualizada.countDown();
            }, error -> { throw new AssertionError(error); });

            assertTrue(ejecutada.await(5, TimeUnit.SECONDS), "la tarea debe ejecutarse");
            assertTrue(actualizada.await(5, TimeUnit.SECONDS), "el resultado debe volver a JavaFX");
            assertNotSame(Thread.currentThread(), hiloOperacion.get());
            assertTrue(hiloOperacion.get().getName().startsWith("latch-worker-"));
            assertTrue(enHiloJavaFx.get(), "la interfaz debe actualizarse en el hilo JavaFX");
        } finally {
            tareas.cerrar();
            tareas.esperarCierre(Duration.ofSeconds(1));
        }
    }

    @Test
    void entregaLosFallosAlManejadorEnElHiloDeJavaFx() throws InterruptedException {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch manejado = new CountDownLatch(1);
        AtomicReference<Throwable> errorRecibido = new AtomicReference<>();
        AtomicReference<Boolean> enHiloJavaFx = new AtomicReference<>(false);
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                throw new IllegalStateException("fallo esperado");
            }
        };

        try {
            TareasJavaFx.ejecutar(tareas, tarea,
                    ignorado -> { throw new AssertionError("la tarea debía fallar"); }, error -> {
                        errorRecibido.set(error);
                        enHiloJavaFx.set(Platform.isFxApplicationThread());
                        manejado.countDown();
                    });

            assertTrue(manejado.await(5, TimeUnit.SECONDS), "el fallo debe llegar al manejador");
            assertInstanceOf(IllegalStateException.class, errorRecibido.get());
            assertTrue(enHiloJavaFx.get(), "el manejador debe ejecutarse en el hilo JavaFX");
        } finally {
            tareas.cerrar();
            tareas.esperarCierre(Duration.ofSeconds(1));
        }
    }
}
