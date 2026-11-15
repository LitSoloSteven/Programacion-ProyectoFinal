package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserMontoTest {
    @Test
    void normalizaExponenteModeradoSinExpandirUnaMagnitudExcesiva() {
        assertEquals(new BigDecimal("1000000.00"), ParserMonto.parsear("1E+6", "monto"));
    }

    @Test
    void rechazaExponenteExtremoAntesDeNormalizarlo() {
        long inicio = System.nanoTime();

        assertThrows(IllegalArgumentException.class,
                () -> ParserMonto.parsear("1E+999999999", "monto"));

        assertTrue(System.nanoTime() - inicio < 100_000_000L,
                "El exponente extremo debe rechazarse en menos de 100 ms.");
    }

    @Test
    void aceptaCeroNegativosYEscalaMonetariaValidaParaQueElDominioDecidaElSigno() {
        assertEquals(new BigDecimal("0.00"), ParserMonto.parsear("0", "monto"));
        assertEquals(new BigDecimal("-12.30"), ParserMonto.parsear("-12.3", "monto"));
    }

    @Test
    void rechazaMagnitudYDecimalesFueraDelLimite() {
        assertThrows(IllegalArgumentException.class,
                () -> ParserMonto.parsear("1000000000000.01", "monto"));
        assertThrows(IllegalArgumentException.class,
                () -> ParserMonto.parsear("1.001", "monto"));
    }

    @Test
    void aceptaCerosDecimalesAlFinalSinConsiderarlosPrecisionAdicional() {
        assertEquals(new BigDecimal("1.20"), ParserMonto.parsear("1.2000", "monto"));
    }
}
