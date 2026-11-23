package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoMonedaTest {
    @Test
    void formateaConMilesDosDecimalesEIndependienteDeLaLocalePredeterminada() {
        Locale localeOriginal = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("1,234.50 USD", FormatoMoneda.formatear("USD", new BigDecimal("1234.5")));
        } finally {
            Locale.setDefault(localeOriginal);
        }
    }

    @Test
    void montoNuloSeFormateaComoCero() {
        assertEquals("0.00 NIO", FormatoMoneda.formatear("NIO", null));
    }
}
