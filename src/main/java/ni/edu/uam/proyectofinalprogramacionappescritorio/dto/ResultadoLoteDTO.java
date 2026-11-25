package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.util.List;
import java.util.UUID;

public record ResultadoLoteDTO(
        String periodo,
        int totalElegibles,
        int confirmados,
        int omitidos,
        int fallidos,
        int pendientes,
        boolean cancelado,
        String mensaje,
        List<UUID> cargosGenerados) {
}
