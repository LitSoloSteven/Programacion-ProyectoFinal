package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.time.LocalDate;
import java.util.List;

public record CarteraResumenDTO(
        LocalDate corte,
        List<CarteraItemDTO> items,
        List<CarteraTramoMonedaDTO> totalesPorMoneda) {
}
