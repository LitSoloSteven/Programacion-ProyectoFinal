package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Cargo abierto del contrato elegible para recibir una aplicación desde garantía. */
public record CargoAplicableGarantiaDTO(
        UUID cargoId,
        String tipo,
        String concepto,
        LocalDate venceEl,
        BigDecimal importe,
        BigDecimal saldoPendiente,
        String moneda) {
}
