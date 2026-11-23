package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

/**
 * Datos de un pago. La Idempotency-Key viaja en la cabecera HTTP, no en el
 * cuerpo, y la distribución es opcional.
 */
public record PagoRequest(
        String contratoId,
        String fechaPago,
        String moneda,
        String monto,
        String metodo,
        String referencia,
        List<DistribucionRequest> distribucion,
        String notas) { }