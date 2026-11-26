package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto;

public record GastoMantenimientoResponse(
        String id,
        String propiedadId,
        String propiedadNombre,
        String solicitudId,
        String solicitudDescripcion,
        String fechaGasto,
        String importe,
        String moneda,
        String concepto,
        String creadoEn,
        String creadoPor
) { }
