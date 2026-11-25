package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ItemPrevisualizacionLoteDTO(
        UUID contratoId,
        String contratoInfo,
        String moneda,
        BigDecimal canon,
        LocalDate periodo,
        LocalDate venceEl,
        boolean yaExistente,
        BigDecimal creditoDisponible,
        BigDecimal creditoAplicable,
        BigDecimal saldoResultante) {
}
