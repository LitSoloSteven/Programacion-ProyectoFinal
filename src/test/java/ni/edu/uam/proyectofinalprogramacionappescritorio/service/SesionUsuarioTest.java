package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SesionUsuarioTest {
    @AfterEach
    void limpiar() {
        SesionUsuario.cerrarSesion();
    }

    @Test
    void elContextoDelHiloNoPisaLaSesionGlobal() throws Exception {
        SesionUsuario global = SesionUsuario.paraSesionAutenticada(
                1L, "global", Rol.PROPIETARIO);
        SesionUsuario delHilo = SesionUsuario.paraSesionAutenticada(
                2L, "hilo", Rol.ASISTENTE);
        SesionUsuario.establecer(global);

        AtomicReference<SesionUsuario> visto = new AtomicReference<>();
        Thread hilo = new Thread(() -> SesionUsuario.ejecutarComo(delHilo,
                () -> visto.set(SesionUsuario.getActual())));
        hilo.start();
        hilo.join();

        assertEquals(delHilo, visto.get());
        assertEquals(global, SesionUsuario.getActual());
    }

    @Test
    void unaIdentidadInvalidaNoSeConstruye() {
        assertThrows(IllegalArgumentException.class,
                () -> SesionUsuario.paraSesionAutenticada(null, "nombre", Rol.PROPIETARIO));
        assertThrows(IllegalArgumentException.class,
                () -> SesionUsuario.paraSesionAutenticada(1L, " ", Rol.PROPIETARIO));
        assertThrows(IllegalArgumentException.class,
                () -> SesionUsuario.paraSesionAutenticada(1L, "nombre", "OTRO"));
    }

    @Test
    void unContextoAisladoNoCaeEnLaSesionGlobal() {
        SesionUsuario global = SesionUsuario.paraSesionAutenticada(
                1L, "global", Rol.PROPIETARIO);
        SesionUsuario aislada = SesionUsuario.paraSesionAutenticada(
                2L, "api", Rol.PROPIETARIO);
        SesionUsuario.establecer(global);

        AtomicReference<SesionUsuario> visto = new AtomicReference<>(global);
        SesionUsuario.ejecutarComo(aislada, () -> visto.set(SesionUsuario.getActual()));

        assertEquals(aislada, visto.get());
        assertEquals(global, SesionUsuario.getActual());
    }

    @Test
    void unContextoAisladoConIdentidadCerradaFallaCerrado() {
        SesionUsuario global = SesionUsuario.paraSesionAutenticada(
                1L, "global", Rol.PROPIETARIO);
        SesionUsuario cerrada = SesionUsuario.paraSesionAutenticada(
                2L, "api", Rol.PROPIETARIO);
        cerrada.close();
        SesionUsuario.establecer(global);

        AtomicReference<SesionUsuario> visto = new AtomicReference<>(global);
        SesionUsuario.ejecutarComo(cerrada, () -> visto.set(SesionUsuario.getActual()));

        assertNull(visto.get());
        assertEquals(global, SesionUsuario.getActual());
    }

    @Test
    void elContextoAisladoSeRestauraAunqueLaOperacionFalle() {
        SesionUsuario global = SesionUsuario.paraSesionAutenticada(
                1L, "global", Rol.PROPIETARIO);
        SesionUsuario aislada = SesionUsuario.paraSesionAutenticada(
                2L, "api", Rol.ASISTENTE);
        SesionUsuario.establecer(global);

        assertThrows(IllegalStateException.class, () ->
                SesionUsuario.ejecutarComo(aislada, () -> {
                    throw new IllegalStateException("fallo simulado");
                }));

        assertEquals(global, SesionUsuario.getActual());
    }
}