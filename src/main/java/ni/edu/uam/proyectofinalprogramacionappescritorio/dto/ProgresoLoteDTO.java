package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

public record ProgresoLoteDTO(
        int procesados,
        int total,
        int confirmados,
        int omitidos,
        int fallidos,
        int pendientes,
        boolean cancelado,
        String mensaje) {
}
