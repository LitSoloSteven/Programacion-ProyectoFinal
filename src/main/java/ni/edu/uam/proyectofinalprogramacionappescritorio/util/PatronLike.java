package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.util.Objects;

public final class PatronLike {
    public static final char CARACTER_ESCAPE = '!';

    private PatronLike() { }

    public static String contieneLiteral(String texto) {
        Objects.requireNonNull(texto, "Falta el texto de búsqueda.");
        String escape = String.valueOf(CARACTER_ESCAPE);
        String escapado = texto.replace(escape, escape + escape)
                .replace("%", escape + "%")
                .replace("_", escape + "_");
        return "%" + escapado + "%";
    }
}
