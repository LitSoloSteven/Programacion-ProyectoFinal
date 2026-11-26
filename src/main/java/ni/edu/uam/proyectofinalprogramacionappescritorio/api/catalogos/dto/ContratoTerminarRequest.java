package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cuerpo para terminar un contrato activo, liquidar garantía y activar sucesor opcional. */
public record ContratoTerminarRequest(
        Long version,
        String fechaTerminacion,
        String motivo,
        String activarSucesorId,
        GarantiaLiquidacionRequest liquidacion) {
}
