package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenTasaDashboardDTO(
        boolean disponible,
        String estado,
        BigDecimal valor,
        String origen,
        LocalDate fechaEfectiva,
        Long antiguedadSegundos,
        boolean autorizadaParaPagos) {
    public static ResumenTasaDashboardDTO sinTasa() {
        return new ResumenTasaDashboardDTO(false, "SIN_TASA", null, "Sin tasa disponible", null, null, false);
    }
}
