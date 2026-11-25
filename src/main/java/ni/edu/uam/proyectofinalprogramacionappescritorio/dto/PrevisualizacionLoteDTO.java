package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.util.List;

public record PrevisualizacionLoteDTO(
        String periodo,
        int totalElegibles,
        int porGenerar,
        int omitidos,
        List<ItemPrevisualizacionLoteDTO> items) {
}
