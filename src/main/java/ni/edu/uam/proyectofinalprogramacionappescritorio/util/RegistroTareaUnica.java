package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registro de instancia única con cancelación cooperativa, bloqueo durante cierre (H6)
 * y espera determinista de terminación (RNF-022 / RNF-023).
 */
public final class RegistroTareaUnica {
    public static final RegistroTareaUnica H1 = new RegistroTareaUnica("H1 (Generación de rentas)");
    public static final RegistroTareaUnica H2 = new RegistroTareaUnica("H2 (Revisión de vencimientos y mora)");

    private final String nombreVisible;
    private final AtomicReference<Ejecucion> activa = new AtomicReference<>();
    private final AtomicBoolean cerrando = new AtomicBoolean(false);

    public RegistroTareaUnica(String nombreVisible) {
        this.nombreVisible = Objects.requireNonNull(nombreVisible);
    }

    public String nombreVisible() {
        return nombreVisible;
    }

    public Ejecucion iniciar(String descripcion) {
        if (cerrando.get()) {
            throw new IllegalStateException(
                    "La aplicación se está cerrando y no admite nuevas ejecuciones de " + nombreVisible + ".");
        }
        String desc = descripcion == null || descripcion.isBlank() ? nombreVisible : descripcion.strip();
        Ejecucion nueva = new Ejecucion(desc);
        if (!activa.compareAndSet(null, nueva)) {
            throw new IllegalStateException(
                    "Ya existe una ejecución activa de " + nombreVisible + ". Espera a que termine antes de iniciar otra.");
        }
        if (cerrando.get()) {
            nueva.close();
            throw new IllegalStateException(
                    "La aplicación se está cerrando y no admite nuevas ejecuciones de " + nombreVisible + ".");
        }
        return nueva;
    }

    public void bloquearNuevas(boolean bloquear) {
        cerrando.set(bloquear);
    }

    public boolean estaBloqueadoParaNuevas() {
        return cerrando.get();
    }

    public boolean hayActividad() {
        return activa.get() != null;
    }

    public String descripcionActiva() {
        Ejecucion actual = activa.get();
        return actual == null ? null : actual.descripcion();
    }

    public boolean solicitarCancelacionActiva() {
        Ejecucion actual = activa.get();
        if (actual == null) {
            return false;
        }
        actual.solicitarCancelacion();
        return true;
    }

    public boolean esperarTerminacion(Duration limite) throws InterruptedException {
        Objects.requireNonNull(limite, "Falta el límite de espera.");
        Ejecucion actual = activa.get();
        if (actual == null) {
            return true;
        }
        return actual.esperar(limite);
    }

    public final class Ejecucion implements AutoCloseable {
        private final String descripcion;
        private final AtomicBoolean cancelada = new AtomicBoolean(false);
        private final CountDownLatch terminada = new CountDownLatch(1);
        private final AtomicBoolean cerrada = new AtomicBoolean(false);

        private Ejecucion(String descripcion) {
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
                activa.compareAndSet(this, null);
                terminada.countDown();
            }
        }
    }
}
