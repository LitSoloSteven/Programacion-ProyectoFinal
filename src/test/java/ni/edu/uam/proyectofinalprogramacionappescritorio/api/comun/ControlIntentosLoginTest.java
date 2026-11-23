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

    @Test
    void limitaPorOrigenSinMezclarOrigenesDistintos() {
        ControlIntentosLogin control = new ControlIntentosLogin(
                5, 2, Duration.ofMinutes(15), Duration.ofMinutes(15));

        control.registrarFallo("uno", "192.0.2.10");
        control.registrarFallo("dos", "192.0.2.10");

        assertTrue(control.estaBloqueado("tres", "192.0.2.10"));
        assertFalse(control.estaBloqueado("tres", "192.0.2.11"));
    }

    @Test
    void unLoginCorrectoRestableceElUsuarioYElOrigen() {
        ControlIntentosLogin control = new ControlIntentosLogin(
                1, 1, Duration.ofMinutes(15), Duration.ofMinutes(15));
        control.registrarFallo("usuario", "192.0.2.10");
        assertTrue(control.estaBloqueado("usuario", "192.0.2.10"));

        control.registrarExito("usuario", "192.0.2.10");

        assertFalse(control.estaBloqueado("usuario", "192.0.2.10"));
    }
}
