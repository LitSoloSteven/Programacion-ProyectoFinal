package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

/** Monedas admitidas por Latch. Los totales se mantienen separados por moneda. */
public enum Moneda {
    USD, NIO;

    public static Moneda desde(String valor) {
        if ("USD".equals(valor)) return USD;
        if ("NIO".equals(valor)) return NIO;
        throw new IllegalArgumentException("Selecciona una moneda válida (USD o NIO).");
    }
}
