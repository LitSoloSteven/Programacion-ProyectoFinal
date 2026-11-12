package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class FormatoMoneda {
    private FormatoMoneda() {}

    public static String formatear(String moneda, BigDecimal monto) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
        formato.setGroupingUsed(true);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        formato.setRoundingMode(RoundingMode.HALF_EVEN);
        BigDecimal valor = monto == null ? BigDecimal.ZERO : monto;
        return formato.format(valor) + " " + moneda;
    }
}
