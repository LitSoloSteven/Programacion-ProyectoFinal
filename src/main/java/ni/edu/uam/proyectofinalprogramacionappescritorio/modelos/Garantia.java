package ni.edu.uam.proyectofinalprogramacionappescritorio.modelos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "garantia")
@Getter
@Setter
@NoArgsConstructor
public class Garantia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false, unique = true)
    private Contrato contrato;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoActual;
}