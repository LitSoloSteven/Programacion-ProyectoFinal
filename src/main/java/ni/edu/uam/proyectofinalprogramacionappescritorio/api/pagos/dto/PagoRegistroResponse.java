package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

public record PagoRegistroResponse(
        String pagoId,
        long numeroRecibo,
        String montoContrato,
        String credito) { }