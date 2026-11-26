package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TareasSegundoPlano implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final Set<Task<?>> tareas = new HashSet<>();
    private boolean pausado;
    private boolean cerrado;

    public TareasSegundoPlano() {
        AtomicInteger contador = new AtomicInteger();
        executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
            Thread hilo = new Thread(runnable, "latch-worker-" + contador.incrementAndGet());
            hilo.setDaemon(true);
            return hilo;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized <T> Task<T> ejecutar(Task<T> tarea) {
        if (cerrado || pausado) throw new RejectedExecutionException("La aplicación se está cerrando.");
        tareas.add(tarea);
        try {
            executor.execute(() -> {
                try { tarea.run(); }
                finally {
                    synchronized (this) {
                        tareas.remove(tarea);
                        notifyAll();
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            tareas.remove(tarea);
            tarea.cancel();
            throw e;
        }
        return tarea;
    }

    /** Detiene la admisión de nuevas tareas sin interrumpir abruptamente transacciones en curso (H4/H6). */
    public synchronized void bloquearNuevas(boolean bloquear) {
        pausado = bloquear;
    }

    public synchronized boolean tieneActivas() {
        return !tareas.isEmpty();
    }

    public synchronized void cerrar() {
        if (cerrado) return;
        cerrado = true;
        tareas.forEach(tarea -> tarea.cancel(true));
        tareas.clear();
        executor.shutdownNow();
        notifyAll();
    }

    public synchronized boolean esperarTareas(Duration limite) throws InterruptedException {
        long vencimiento = System.nanoTime() + limite.toNanos();
        long restante;
        while (!tareas.isEmpty() && (restante = vencimiento - System.nanoTime()) > 0) {
            TimeUnit.NANOSECONDS.timedWait(this, restante);
        }
        return tareas.isEmpty();
    }

    public boolean esperarCierre(Duration limite) throws InterruptedException {
        return executor.awaitTermination(Math.max(0L, limite.toNanos()), TimeUnit.NANOSECONDS);
    }

    @Override public void close() { cerrar(); }
}
