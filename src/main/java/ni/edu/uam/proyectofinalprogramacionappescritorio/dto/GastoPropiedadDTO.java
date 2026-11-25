package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Vista inmutable de un gasto efectivo de mantenimiento a cargo del propietario. */
public record GastoPropiedadDTO(
        UUID id,
        UUID propiedadId,
        String propiedadNombre,
        UUID solicitudId,
        String solicitudDescripcion,
        LocalDate fechaGasto,
        BigDecimal importe,
        String moneda,
        String concepto,
        Instant creadoEn,
        String creadoPor
) { }
