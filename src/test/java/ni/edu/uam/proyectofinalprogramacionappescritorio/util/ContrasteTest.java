package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContrasteTest {
    private record Par(String texto, String fondo, double esperado, boolean cumpleTextoNormal) { }

    private static final List<Par> PARES = List.of(
            new Par("#2B2F36", "#E6E9EF", 11.05, true),
            new Par("#0B1F3A", "#E6E9EF", 13.58, true),
            new Par("#1F3B5C", "#E6E9EF", 9.39, true),
            new Par("#A7B1BE", "#E6E9EF", 1.78, false),
            new Par("#E6E9EF", "#0B1F3A", 13.58, true),
            new Par("#E6E9EF", "#1F3B5C", 9.39, true),
            new Par("#A7B1BE", "#0B1F3A", 7.61, true),
            new Par("#A7B1BE", "#1F3B5C", 5.26, true));

    @Test
    void reproduceLosContrastesDelManualYRespetaLosUmbrales() {
        for (Par par : PARES) {
            double ratio = contraste(par.texto(), par.fondo());
            assertEquals(par.esperado(), ratio, 0.05, par.texto() + " sobre " + par.fondo());
            if (par.cumpleTextoNormal()) assertTrue(ratio >= 4.5, "Texto normal por debajo de 4.5:1");
        }
        assertTrue(contraste("#0B1F3A", "#E6E9EF") >= 3.0, "El foco claro debe superar 3:1");
        assertTrue(contraste("#E6E9EF", "#0B1F3A") >= 3.0, "El foco oscuro debe superar 3:1");
        assertTrue(contraste("#1F3B5C", "#E6E9EF") >= 3.0, "El borde claro debe superar 3:1");
        assertFalse(contraste("#A7B1BE", "#E6E9EF") >= 4.5,
                "El texto pequeño slate sobre fondo claro no es legible");
    }

    private static double contraste(String primerPlano, String fondo) {
        double a = luminancia(primerPlano);
        double b = luminancia(fondo);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminancia(String hex) {
        int rojo = Integer.parseInt(hex.substring(1, 3), 16);
        int verde = Integer.parseInt(hex.substring(3, 5), 16);
        int azul = Integer.parseInt(hex.substring(5, 7), 16);
        return 0.2126 * canal(rojo) + 0.7152 * canal(verde) + 0.0722 * canal(azul);
    }

    private static double canal(int valor) {
        double s = valor / 255.0;
        return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }
}
