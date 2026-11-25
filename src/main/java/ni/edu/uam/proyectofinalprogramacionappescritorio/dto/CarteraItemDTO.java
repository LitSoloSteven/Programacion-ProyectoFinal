package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CarteraItemDTO(
        UUID cargoId,
        UUID contratoId,
        String contratoInfo,
        String moneda,
        String tipo,
        LocalDate periodo,
        LocalDate fechaEmision,
        LocalDate venceEl,
        String concepto,
        BigDecimal importe,
        BigDecimal aplicado,
        BigDecimal saldo,
        String estado,
        long diasVencidos,
        String tramo) {
}
