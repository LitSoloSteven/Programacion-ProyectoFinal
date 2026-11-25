package ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto;

/** Resumen de la tasa disponible para el panel principal (valor como texto decimal). */
public record TasaResumenResponse(
        boolean disponible,
        String estado,
        String valor,
        String origen,
        String fechaEfectiva,
        Long antiguedadSegundos,
        boolean autorizadaParaPagos) {
    public static TasaResumenResponse sinTasa() {
        return new TasaResumenResponse(false, "SIN_TASA", null, "Sin tasa disponible", null, null, false);
    }
}
