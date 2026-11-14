package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboDTO;

@Entity
@Table(name = "recibo")
public class Recibo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long numero;

    @OneToOne(optional = false)
    @JoinColumn(name = "pago_id", nullable = false, unique = true, updatable = false)
    private Pago pago;

    @Column(name = "contrato_info", nullable = false, length = 300, updatable = false)
    private String contratoInfo;

    protected Recibo() {}

    public Recibo(Pago pago, String contratoInfo) {
        this.pago = pago;
        this.contratoInfo = contratoInfo;
    }

    public Long getNumero() {
        return numero;
    }

    public Pago getPago() {
        return pago;
    }

    public String getContratoInfo() {
        return contratoInfo;
    }

    public ReciboDTO presentar() {
        return new ReciboDTO(numero, pago.getId(), pago.getContratoId(), pago.getFechaPago(),
                contratoInfo, pago.getMontoOriginal(), pago.getMonedaOriginal(),
                pago.getMontoContrato(), pago.getMonedaContrato(), pago.getMetodo(),
                pago.getReferencia(), "Anulado".equals(pago.getEstado()));
    }
}
