package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Resumen de garantía por contrato para listados sin entidades JPA en cliente. */
public record GarantiaListadoItemResponse(
        String id,
        String contratoId,
        String moneda,
        String montoInicial,
        String saldoActual,
        String estado) { }
