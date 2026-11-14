package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

/**
 * Estado de cuenta de un contrato a una fecha de corte. Todos los importes
 * están en la moneda del contrato y se devuelven como texto decimal.
 */
public record EstadoCuentaResponse(
        String contratoId,
        String corte,
        String moneda,
        String canon,
        List<CargoEstadoCuenta> cargos,
        List<PagoEstadoCuenta> pagos,
        TotalesEstadoCuenta totales,
        GarantiaEstadoCuenta garantia) {

    public record CargoEstadoCuenta(
            String cargoId,
            String tipo,
            String periodo,
            String fechaEmision,
            String venceEl,
            String concepto,
            String importe,
            String aplicado,
            String saldo,
            String estado) { }

    public record PagoEstadoCuenta(
            String pagoId,
            Long numeroRecibo,
            String fechaPago,
            String montoOriginal,
            String monedaOriginal,
            String montoContrato,
            String metodo,
            String referencia,
            String estado,
            String aplicado,
            String credito) { }

    public record TotalesEstadoCuenta(
            String cargos,
            String pagado,
            String saldo,
            String credito) { }

    public record GarantiaEstadoCuenta(
            String pactada,
            String saldoActual) { }
}