package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Vista inmutable de un movimiento de garantía para consulta en interfaz y reportes. */
public record MovimientoGarantiaDTO(
        UUID id,
        UUID garantiaId,
        UUID cargoId,
        String cargoResumen,
        UUID liquidacionId,
        short orden,
        String tipo,
        BigDecimal importe,
        LocalDate fechaMovimiento,
        String metodo,
        String concepto,
        Instant creadoEn,
        Long creadoPor) {
}
