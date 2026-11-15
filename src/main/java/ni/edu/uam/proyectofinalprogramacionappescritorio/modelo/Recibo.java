package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboDTO;

import java.util.Objects;

@Entity
@Table(name = "recibo")
public class Recibo {
    @Id
    private Long numero;

    @OneToOne(optional = false)
    @JoinColumn(name = "pago_id", nullable = false, unique = true, updatable = false)
    private Pago pago;

    @Column(name = "contrato_info", nullable = false, columnDefinition = "text", updatable = false)
    private String contratoInfo;

    protected Recibo() {}

    public Recibo(long numero, Pago pago, String contratoInfo) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El número del recibo debe ser positivo.");
        }
        this.numero = numero;
        this.pago = Objects.requireNonNull(pago, "Falta el pago del recibo.");
        this.contratoInfo = Objects.requireNonNull(contratoInfo,
                "Falta el resumen del contrato.");
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
