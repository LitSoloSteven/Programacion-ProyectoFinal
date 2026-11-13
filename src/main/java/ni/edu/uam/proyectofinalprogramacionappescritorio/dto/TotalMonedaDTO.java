package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TotalMonedaDTO(String moneda, long cantidad, BigDecimal total) {
    public TotalMonedaDTO {
        if (!"USD".equals(moneda) && !"NIO".equals(moneda)) {
            throw new IllegalArgumentException("La moneda debe ser USD o NIO.");
        }
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        if (total == null) {
            throw new IllegalArgumentException("El total es obligatorio.");
        }
        total = total.setScale(2, RoundingMode.HALF_EVEN);
    }
}
