package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlIntentosLoginTest {
    @Test
    void bloqueaAlAlcanzarElMaximoYSeLiberaConExito() {
        ControlIntentosLogin control = new ControlIntentosLogin(
                2, Duration.ofMinutes(15), Duration.ofMinutes(15));

        assertFalse(control.estaBloqueado("usuario"));
        control.registrarFallo("usuario");
        assertFalse(control.estaBloqueado("usuario"));
        control.registrarFallo("usuario");
        assertTrue(control.estaBloqueado("usuario"));

        control.registrarExito("usuario");
        assertFalse(control.estaBloqueado("usuario"));
    }

    @Test
    void losIntentosSeCuentanPorUsuario() {
        ControlIntentosLogin control = new ControlIntentosLogin(
                1, Duration.ofMinutes(15), Duration.ofMinutes(15));

        control.registrarFallo("uno");

        assertTrue(control.estaBloqueado("uno"));
        assertFalse(control.estaBloqueado("dos"));
    }
}