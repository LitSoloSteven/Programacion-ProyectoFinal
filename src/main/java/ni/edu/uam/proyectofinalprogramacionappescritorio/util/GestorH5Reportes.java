package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinador de instancia única para las tareas H5 de generación y exportación de reportes.
 * Garantiza que exista a lo sumo una tarea H5 activa, mantiene seguimiento del recurso real
 * y expone cancelación cooperativa y espera determinista de liberación.
 */
public final class GestorH5Reportes {

    private static final AtomicReference<EjecucionH5> ACTIVA = new AtomicReference<>();
    private static final AtomicBoolean CERRANDO = new AtomicBoolean(false);

    private GestorH5Reportes() {
    }

    public static void bloquearNuevas(boolean bloquear) {
        CERRANDO.set(bloquear);
    }

    public static EjecucionH5 iniciar(String descripcion) {
        if (CERRANDO.get()) {
            throw new IllegalStateException(
                    "La aplicación se está cerrando y no admite nuevas generaciones o exportaciones de reportes.");
        }
        String desc = descripcion == null || descripcion.isBlank() ? "Reporte H5" : descripcion.strip();
        EjecucionH5 nueva = new EjecucionH5(desc);
        if (!ACTIVA.compareAndSet(null, nueva)) {
            throw new IllegalStateException(
                    "Ya existe una generación o exportación de reporte en curso. Espera a que termine o cancélala antes de iniciar otra.");
        }
        if (CERRANDO.get()) {
            nueva.close();
            throw new IllegalStateException(
                    "La aplicación se está cerrando y no admite nuevas generaciones o exportaciones de reportes.");
        }
        return nueva;
    }

    public static boolean hayActividad() {
        return ACTIVA.get() != null;
    }

    public static String descripcionActiva() {
        EjecucionH5 actual = ACTIVA.get();
        return actual == null ? null : actual.descripcion();
    }

    public static boolean solicitarCancelacionActiva() {
        EjecucionH5 actual = ACTIVA.get();
        if (actual == null) {
            return false;
        }
        actual.solicitarCancelacion();
        return true;
    }

    public static boolean esperarTerminacion(Duration limite) throws InterruptedException {
        Objects.requireNonNull(limite, "Falta el límite de espera.");
        EjecucionH5 actual = ACTIVA.get();
        if (actual == null) {
            return true;
        }
        return actual.esperar(limite);
    }

    public static final class EjecucionH5 implements AutoCloseable {
        private final String descripcion;
        private final AtomicBoolean cancelada = new AtomicBoolean(false);
        private final CountDownLatch terminada = new CountDownLatch(1);
        private final AtomicBoolean cerrada = new AtomicBoolean(false);

        private EjecucionH5(String descripcion) {
            this.descripcion = descripcion;
        }

        public String descripcion() {
            return descripcion;
        }

        public void solicitarCancelacion() {
            cancelada.set(true);
        }

        public boolean estaCancelada() {
            return cancelada.get();
        }

        public boolean esperar(Duration limite) throws InterruptedException {
            return terminada.await(Math.max(0L, limite.toMillis()), TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            if (cerrada.compareAndSet(false, true)) {
                ACTIVA.compareAndSet(this, null);
                terminada.countDown();
            }
        }
    }
}
