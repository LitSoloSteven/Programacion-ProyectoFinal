package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Seguimiento del depósito: monto inicial pactado y saldo aún retenido. */
@Entity
@Table(name = "garantia")
public class Garantia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false, unique = true)
    private Contrato contrato;

    @Column(name = "monto_inicial", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "saldo_actual", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoActual;

    @Version
    private long version;

    protected Garantia() {}

    public Garantia(Contrato contrato, BigDecimal montoInicial, BigDecimal saldoActual) {
        this.contrato = contrato;
        this.montoInicial = validarNoNegativo(montoInicial, "El monto inicial no puede ser negativo.");
        this.saldoActual = validarNoNegativo(saldoActual, "El saldo actual no puede ser negativo.");
        if (this.saldoActual.compareTo(this.montoInicial) > 0) {
            throw new IllegalArgumentException("El saldo de la garantía no puede superar el monto inicial.");
        }
    }

    private BigDecimal validarNoNegativo(BigDecimal monto, String mensaje) {
        BigDecimal valor = monto == null ? BigDecimal.ZERO : monto;
        if (valor.signum() < 0) throw new IllegalArgumentException(mensaje);
        return valor;
    }

    public UUID getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public BigDecimal getMontoInicial() { return montoInicial; }
    public BigDecimal getSaldoActual() { return saldoActual; }
    public long getVersion() { return version; }

    public void ajustarSaldo(BigDecimal nuevoSaldo) {
        BigDecimal saldo = validarNoNegativo(nuevoSaldo, "El saldo actual no puede ser negativo.");
        if (saldo.compareTo(montoInicial) > 0) {
            throw new IllegalArgumentException("El saldo de la garantía no puede superar el monto inicial.");
        }
        saldoActual = saldo;
    }
}
