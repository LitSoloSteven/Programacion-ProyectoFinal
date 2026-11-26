package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

/** Solicitud de creación o actualización de cargo. */
public record CargoRequest(
        String contratoId,
        String tipo,
        String periodo,
        String fechaEmision,
        String venceEl,
        String importe,
        String concepto,
        String estado,
        Long version) { }
