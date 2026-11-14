package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

public record PagoDetalleResponse(
        String pagoId,
        String contratoId,
        String contratoInfo,
        String fechaPago,
        String moneda,
        String monto,
        String montoContrato,
        String monedaContrato,
        String metodo,
        String referencia,
        String notas,
        String estado,
        Long numeroRecibo,
        String credito,
        List<AplicacionResponse> aplicaciones,
        String motivoAnulacion,
        String anuladoEn,
        Long anuladoPor) { }