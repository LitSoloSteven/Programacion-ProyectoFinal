package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

public record GastoMantenimientoRequest(
        String propiedadId,
        String solicitudId,
        String fechaGasto,
        String importe,
        String moneda,
        String concepto
) { }
