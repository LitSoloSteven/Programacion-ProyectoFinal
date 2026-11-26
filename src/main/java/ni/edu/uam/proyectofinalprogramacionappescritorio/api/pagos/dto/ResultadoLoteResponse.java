package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

/** Resultado de la ejecución por lotes H1. */
public record ResultadoLoteResponse(
        String periodo,
        int totalElegibles,
        int confirmados,
        int omitidos,
        int fallidos,
        int pendientes,
        boolean cancelado,
        String mensaje,
        List<String> cargosGenerados) {
}
