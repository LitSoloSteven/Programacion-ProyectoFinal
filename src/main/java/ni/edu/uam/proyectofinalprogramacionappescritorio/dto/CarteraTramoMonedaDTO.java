package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;

public record CarteraTramoMonedaDTO(
        String moneda,
        long cantidadCargos,
        BigDecimal alDia,
        BigDecimal tramo1a30,
        BigDecimal tramo31a60,
        BigDecimal tramo61a90,
        BigDecimal tramoMasDe90,
        BigDecimal totalSaldo) {
}
