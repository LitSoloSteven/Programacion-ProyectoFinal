package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

/**
 * Aplicación solicitada de un pago a un cargo. El monto se expresa en la
 * moneda del contrato, que es la misma del campo montoContrato de la respuesta.
 */
public record DistribucionRequest(String cargoId, String monto) { }