package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {
    private static final SesionApi SESION = new SesionApi(7L, "usuario", "PROPIETARIO");

    @Test
    void emiteValidaYRevocaUnToken() {
        TokenService tokens = new TokenService();

        TokenService.TokenEmitido emitido = tokens.emitir(SESION);

        assertFalse(emitido.token().isBlank());
        assertTrue(tokens.validar(emitido.token()).isPresent());
        assertEquals(SESION, tokens.validar(emitido.token()).orElseThrow());
        assertEquals(1, tokens.cantidadActiva());

        tokens.revocar(emitido.token());
        assertTrue(tokens.validar(emitido.token()).isEmpty());
    }

    @Test
    void rechazaTokenAusenteOVacido() {
        TokenService tokens = new TokenService();

        assertTrue(tokens.validar(null).isEmpty());
        assertTrue(tokens.validar(" ").isEmpty());
        assertTrue(tokens.validar("x".repeat(600)).isEmpty());
    }

    @Test
    void unTokenVencidoYaNoEsValido() throws Exception {
        TokenService tokens = new TokenService(Duration.ofMillis(1));
        TokenService.TokenEmitido emitido = tokens.emitir(SESION);

        Thread.sleep(20);
        tokens.limpiarExpirados();

        assertTrue(tokens.validar(emitido.token()).isEmpty());
    }
}