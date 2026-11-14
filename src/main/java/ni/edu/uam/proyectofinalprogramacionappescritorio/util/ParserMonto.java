package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valida la magnitud y la escala de importes antes de normalizarlos. */
public final class ParserMonto {
    private static final BigDecimal MAXIMO = new BigDecimal("1000000000000");
    private static final long DIGITOS_ENTEROS_MAXIMOS =
            (long) MAXIMO.precision() - MAXIMO.scale();

    private ParserMonto() { }

    public static BigDecimal parsear(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }

        BigDecimal valor = new BigDecimal(texto.strip());
        validar(valor, 2, campo);
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Valida tasas conservando la escala, que puede llegar a ocho decimales. */
    public static BigDecimal validarTasa(BigDecimal valor, String campo) {
        return validar(valor, 8, campo);
    }

    private static BigDecimal validar(BigDecimal valor, int escalaMaxima, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }

        BigDecimal normalizado = valor.stripTrailingZeros();
        long digitosEnteros = (long) normalizado.precision() - normalizado.scale();
        long precisionMaxima = DIGITOS_ENTEROS_MAXIMOS + escalaMaxima;
        if (normalizado.scale() > escalaMaxima
                || normalizado.precision() > precisionMaxima
                || digitosEnteros > DIGITOS_ENTEROS_MAXIMOS
                || valor.abs().compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "El campo " + campo + " excede el máximo o la precisión permitida.");
        }
        return valor;
    }
}
