package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitorInactividadTest {
    private static final class RelojFijo extends Clock {
        private Instant ahora;

        RelojFijo(Instant inicial) {
            this.ahora = inicial;
        }

        void avanzar(Duration delta) {
            this.ahora = this.ahora.plus(delta);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Test
    void avisaSesentaSegundosAntesYExpiraTrasQuinceMinutosDeInactividad() {
        RelojFijo reloj = new RelojFijo(Instant.parse("2026-11-25T10:00:00Z"));
        MonitorInactividad monitor = new MonitorInactividad(
                Duration.ofMinutes(15),
                Duration.ofSeconds(60),
                reloj,
                () -> { },
                () -> { });

        assertEquals(MonitorInactividad.EstadoEvaluacion.ACTIVO, monitor.evaluar());

        reloj.avanzar(Duration.ofMinutes(13).plusSeconds(59));
        assertEquals(MonitorInactividad.EstadoEvaluacion.ACTIVO, monitor.evaluar());

        // A los 14 min (60 s antes de vencer): avisa una sola vez
        reloj.avanzar(Duration.ofSeconds(1));
        assertEquals(MonitorInactividad.EstadoEvaluacion.AVISAR, monitor.evaluar());
        assertEquals(MonitorInactividad.EstadoEvaluacion.ACTIVO, monitor.evaluar());

        // Interacción del usuario reinicia el reloj de inactividad local
        monitor.registrarActividad();
        reloj.avanzar(Duration.ofMinutes(14));
        assertEquals(MonitorInactividad.EstadoEvaluacion.AVISAR, monitor.evaluar());

        // A los 15 min exactos sin interacción: expira
        reloj.avanzar(Duration.ofMinutes(1));
        assertEquals(MonitorInactividad.EstadoEvaluacion.EXPIRADO, monitor.evaluar());
    }

    @Test
    void crearAcotaLimitesFueraDeRangoYConservaQuincePorDefecto() {
        Instant ahora = Instant.now();
        MonitorInactividad bajo = MonitorInactividad.crear(4, () -> { }, () -> { });
        MonitorInactividad alto = MonitorInactividad.crear(61, () -> { }, () -> { });
        MonitorInactividad valido = MonitorInactividad.crear(30, () -> { }, () -> { });

        assertEquals(15L, bajo.tiempoRestante(ahora).toMinutes());
        assertEquals(15L, alto.tiempoRestante(ahora).toMinutes());
        assertEquals(30L, valido.tiempoRestante(ahora).toMinutes());
    }
}
