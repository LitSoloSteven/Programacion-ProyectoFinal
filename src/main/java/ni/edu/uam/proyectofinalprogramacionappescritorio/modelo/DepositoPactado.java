package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositoPactado {
    @Column(name = "deposito_pactado", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoPactado;

    public DepositoPactado(BigDecimal montoPactado) {
        this.montoPactado = montoPactado == null ? BigDecimal.ZERO : montoPactado;
        if (this.montoPactado.signum() < 0) {
            throw new IllegalArgumentException("El depósito en garantía no puede ser negativo.");
        }
    }
}
