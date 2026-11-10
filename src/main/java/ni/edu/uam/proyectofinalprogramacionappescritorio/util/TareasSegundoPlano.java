package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TareasSegundoPlano implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final Set<Task<?>> tareas = ConcurrentHashMap.newKeySet();
    private boolean cerrado;

    public TareasSegundoPlano() {
        AtomicInteger contador = new AtomicInteger();
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), runnable -> {
            Thread hilo = new Thread(runnable, "latch-worker-" + contador.incrementAndGet());
            hilo.setDaemon(true);
            return hilo;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized <T> Task<T> ejecutar(Task<T> tarea) {
        if (cerrado) throw new RejectedExecutionException("La aplicación se está cerrando.");
        tareas.add(tarea);
        try {
            executor.execute(() -> {
                try { tarea.run(); }
                finally { tareas.remove(tarea); }
            });
        } catch (RejectedExecutionException e) {
            tareas.remove(tarea);
            tarea.cancel();
            throw e;
        }
        return tarea;
    }

    public synchronized void cerrar() {
        if (cerrado) return;
        cerrado = true;
        tareas.forEach(tarea -> tarea.cancel(true));
        tareas.clear();
        executor.shutdownNow();
    }

    public boolean esperarCierre(Duration limite) throws InterruptedException {
        return executor.awaitTermination(limite.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override public void close() { cerrar(); }
}
