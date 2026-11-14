package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Presentación de tasas con los ocho decimales exigidos por el dominio. */
public final class FormatoTasa {
    private FormatoTasa() { }

    public static String formatear(BigDecimal tasa) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
        formato.setGroupingUsed(true);
        formato.setMinimumFractionDigits(8);
        formato.setMaximumFractionDigits(8);
        formato.setRoundingMode(RoundingMode.HALF_EVEN);
        return formato.format(tasa == null ? BigDecimal.ZERO : tasa);
    }
}