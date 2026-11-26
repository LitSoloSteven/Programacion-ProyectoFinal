package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class EstilosCssTest {
    private static final Set<String> PALETA = Set.of(
            "#0B1F3A", "#1F3B5C", "#A7B1BE", "#E6E9EF", "#2B2F36");
    private static final Set<String> CANALES_RGB = Set.of(
            "11,31,58", "31,59,92", "167,177,190", "230,233,239", "43,47,54");

    @Test
    void soloDeclaraLaPaletaYVelosDerivadosEnLaRaiz() throws IOException {
        String css;
        try (var input = getClass().getResourceAsStream(
                "/ni/edu/uam/proyectofinalprogramacionappescritorio/estilos.css")) {
            assertNotNull(input, "No se encontró estilos.css");
            css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        Matcher hex = Pattern.compile("#[0-9A-Fa-f]{6}").matcher(css);
        while (hex.find()) assertTrue(PALETA.contains(hex.group()), "Color fuera de paleta: " + hex.group());
        assertTrue(PALETA.stream().allMatch(css::contains), "Falta uno de los cinco colores base");

        Matcher rgba = Pattern.compile("rgba\\((\\d+),(\\d+),(\\d+),([0-9.]+)\\)").matcher(css);
        while (rgba.find()) {
            String canales = rgba.group(1) + "," + rgba.group(2) + "," + rgba.group(3);
            assertTrue(CANALES_RGB.contains(canales), "Velo no derivado de la paleta: " + rgba.group());
        }

        String raiz = Pattern.compile("(?s)\\.root\\s*\\{.*?}").matcher(css).results()
                .map(java.util.regex.MatchResult::group).findFirst().orElse("");
        assertFalse(raiz.isBlank(), "No se encontró la raíz CSS");
        String fueraDeRaiz = css.replace(raiz, "");
        assertFalse(Pattern.compile("#[0-9A-Fa-f]{6}").matcher(fueraDeRaiz).find(),
                "Los hex de color solo se permiten en la raíz");
        assertFalse(fueraDeRaiz.contains("rgba("), "Los velos rgba solo se permiten en la raíz");
    }
}
