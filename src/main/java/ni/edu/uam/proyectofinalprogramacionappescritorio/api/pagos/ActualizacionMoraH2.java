package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CobranzaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RegistroTareaUnica;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Programador periódico para la revisión H2 de vencimientos y mora.
 * Garantiza instancia única vía RegistroTareaUnica.H2 y detención ordenada para H6.
 */
public final class ActualizacionMoraH2 implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger(ActualizacionMoraH2.class.getName());
    private static final long RETRASO_INICIAL_SEGUNDOS = 30L;
    private static final long INTERVALO_SEGUNDOS = 900L;

    private final CobranzaService cobranza;
    private final AtomicBoolean cerrando = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public ActualizacionMoraH2(CobranzaService cobranza) {
        this.cobranza = Objects.requireNonNull(cobranza, "Falta el servicio de cobranza.");
    }

    public synchronized void iniciar() {
        if (cerrando.get() || scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "latch-h2");
            hilo.setDaemon(true);
            return hilo;
        });
        scheduler.scheduleWithFixedDelay(
                this::ejecutarProgramada,
                RETRASO_INICIAL_SEGUNDOS,
                INTERVALO_SEGUNDOS,
                TimeUnit.SECONDS);
    }

    private void ejecutarProgramada() {
        if (cerrando.get() || Thread.currentThread().isInterrupted()) return;
        try (RegistroTareaUnica.Ejecucion ejecucion =
                     RegistroTareaUnica.H2.iniciar("Revisión periódica de vencimientos y mora (H2)")) {
            cobranza.revisarMoraYVencimientosSistema(null, ejecucion::estaCancelada);
        } catch (IllegalStateException yaEnCurso) {
            // Instancia única (RNF-023) o cierre en progreso: se omite este disparo sin fallo.
        } catch (RuntimeException error) {
            LOG.log(System.Logger.Level.WARNING, "H2 no pudo completar la revisión programada.");
        }
    }

    public boolean hayActividad() {
        return RegistroTareaUnica.H2.hayActividad();
    }

    public void solicitarDetencion() {
        cerrando.set(true);
        RegistroTareaUnica.H2.solicitarCancelacionActiva();
        ScheduledExecutorService actual;
        synchronized (this) {
            actual = scheduler;
        }
        if (actual != null) {
            actual.shutdownNow();
        }
    }

    public boolean esperarTerminacion(Duration limite) throws InterruptedException {
        Objects.requireNonNull(limite, "Falta el límite de espera.");
        long inicio = System.nanoTime();
        boolean registroLibre = RegistroTareaUnica.H2.esperarTerminacion(limite);
        ScheduledExecutorService actual;
        synchronized (this) {
            actual = scheduler;
        }
        if (actual == null) {
            return registroLibre;
        }
        long transcurridoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        long restanteMs = Math.max(0L, limite.toMillis() - transcurridoMs);
        boolean poolTerminado = actual.awaitTermination(restanteMs, TimeUnit.MILLISECONDS);
        return registroLibre && poolTerminado;
    }

    @Override
    public void close() {
        solicitarDetencion();
        try {
            esperarTerminacion(Duration.ofSeconds(3));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
