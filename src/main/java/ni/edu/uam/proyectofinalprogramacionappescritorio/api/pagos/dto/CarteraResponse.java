package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

/**
 * Cartera por contrato, período y antigüedad con totales separados por moneda.
 * Los importes y fechas se serializan como texto.
 */
public record CarteraResponse(
        String corte,
        List<Item> items,
        List<TotalMoneda> totalesPorMoneda) {

    public record Item(
            String cargoId,
            String contratoId,
            String contratoInfo,
            String moneda,
            String tipo,
            String periodo,
            String fechaEmision,
            String venceEl,
            String concepto,
            String importe,
            String aplicado,
            String saldo,
            String estado,
            long diasVencidos,
            String tramo) { }

    public record TotalMoneda(
            String moneda,
            long cantidadCargos,
            String alDia,
            String tramo1a30,
            String tramo31a60,
            String tramo61a90,
            String tramoMasDe90,
            String totalSaldo) { }
}
