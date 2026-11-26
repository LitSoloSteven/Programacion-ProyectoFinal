package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas en cliente para el coordinador de cierre ordenado H6 sobre tareas JavaFX/H5. */
class CoordinadorCierreH6Test {

    @Test
    void cierraRecursosCuandoTodasLasTareasTerminanAntesDelLimite() {
        AtomicBoolean cancelacionSolicitada = new AtomicBoolean(false);
        AtomicBoolean activa = new AtomicBoolean(true);
        CountDownLatch liberada = new CountDownLatch(1);
        AtomicInteger cierresDeRecursos = new AtomicInteger(0);

        CoordinadorCierreH6.ParticipanteCierre participante = new CoordinadorCierreH6.ParticipanteCierre() {
            @Override public String nombre() { return "H5: Reportes y exportación"; }
            @Override public boolean hayActividad() { return activa.get(); }
            @Override public void solicitarCancelacion() {
                cancelacionSolicitada.set(true);
                activa.set(false);
                liberada.countDown();
            }
            @Override public boolean esperarTerminacion(Duration limite) throws InterruptedException {
                return liberada.await(limite.toMillis(), TimeUnit.MILLISECONDS);
            }
        };

        CoordinadorCierreH6 h6 = new CoordinadorCierreH6(
                List.of(participante),
                () -> { },
                () -> { },
                cierresDeRecursos::incrementAndGet);

        var resultado = h6.ejecutarCierre(Duration.ofSeconds(2));
        assertTrue(resultado.completado());
        assertTrue(cancelacionSolicitada.get());
        assertTrue(resultado.tareasPendientes().isEmpty());
        assertEquals(1, cierresDeRecursos.get());
    }

    @Test
    void devuelveErr056YMantieneRecursosAbiertosCuandoTareaSuperaTiempoLimite() {
        AtomicInteger cierresDeRecursos = new AtomicInteger(0);
        AtomicBoolean bloqueoLiberadoTrasFallo = new AtomicBoolean(false);

        CoordinadorCierreH6.ParticipanteCierre bloqueada = new CoordinadorCierreH6.ParticipanteCierre() {
            @Override public String nombre() { return "H5: Reportes y exportación"; }
            @Override public boolean hayActividad() { return true; }
            @Override public void solicitarCancelacion() { }
            @Override public boolean esperarTerminacion(Duration limite) { return false; }
        };

        CoordinadorCierreH6 h6 = new CoordinadorCierreH6(
                List.of(bloqueada),
                () -> { },
                () -> bloqueoLiberadoTrasFallo.set(true),
                cierresDeRecursos::incrementAndGet);

        var resultado = h6.ejecutarCierre(Duration.ofMillis(50));
        assertFalse(resultado.completado());
        assertEquals(TareasActivasException.CODIGO, resultado.codigoError());
        assertEquals(TareasActivasException.MENSAJE_MSG_061, resultado.mensaje());
        assertEquals(List.of("H5: Reportes y exportación"), resultado.tareasPendientes());
        assertEquals(0, cierresDeRecursos.get(), "Nunca se deben cerrar conexiones si hay tareas activas");
        assertTrue(bloqueoLiberadoTrasFallo.get());
    }

    @Test
    void admiteTareasDeInterfazTrasUnCierreBloqueadoYPuedeVolverACerrar() throws InterruptedException {
        var ejecutor = new TareasSegundoPlano();
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicInteger cierres = new AtomicInteger();
        var h6 = CoordinadorCierreH6.paraAplicacion(ejecutor, cierres::incrementAndGet);
        Task<Void> activa = new Task<>() {
            @Override protected Void call() { return null; }
            @Override public void run() {
                iniciada.countDown();
                try {
                    liberar.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        Task<Void> posterior = new Task<>() {
            @Override protected Void call() { return null; }
            @Override public void run() { }
        };
        try {
            ejecutor.ejecutar(activa);
            assertTrue(iniciada.await(2, TimeUnit.SECONDS));
            var bloqueado = h6.ejecutarCierre(Duration.ZERO);
            assertFalse(bloqueado.completado());
            assertEquals(List.of("H4/Tareas de interfaz en curso"), bloqueado.tareasPendientes());
            assertEquals(0, cierres.get());
            ejecutor.ejecutar(posterior);
            liberar.countDown();
            assertTrue(h6.ejecutarCierre(Duration.ofSeconds(2)).completado());
            assertEquals(1, cierres.get());
            assertThrows(RejectedExecutionException.class, () -> ejecutor.ejecutar(posterior));
        } finally {
            liberar.countDown();
            ejecutor.esperarTareas(Duration.ofSeconds(2));
            ejecutor.cerrar();
            GestorH5Reportes.bloquearNuevas(false);
        }
    }
}
