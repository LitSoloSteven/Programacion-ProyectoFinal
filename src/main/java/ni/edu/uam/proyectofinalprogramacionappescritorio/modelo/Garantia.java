package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Seguimiento del depósito: monto inicial pactado y saldo aún retenido. */
@Entity
@Table(name = "garantia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public void actualizarMontoInicial(BigDecimal nuevoMontoInicial) {
        BigDecimal monto = validarNoNegativo(nuevoMontoInicial,
                "El monto inicial no puede ser negativo.");
        if (saldoActual.compareTo(monto) > 0) {
            throw new IllegalArgumentException(
                    "El monto inicial de la garantía no puede ser menor que el saldo actual.");
        }
        montoInicial = monto;
    }

    public void ajustarSaldo(BigDecimal nuevoSaldo) {
        BigDecimal saldo = validarNoNegativo(nuevoSaldo, "El saldo actual no puede ser negativo.");
        if (saldo.compareTo(montoInicial) > 0) {
            throw new IllegalArgumentException("El saldo de la garantía no puede superar el monto inicial.");
        }
        saldoActual = saldo;
    }

    /**
     * Deriva el estado de la garantía según RN-26: importe cero es Retenido;
     * con remanente y devoluciones es Devuelto parcial; con remanente sin
     * devolución es Retenido; agotada con devolución es Devuelto; agotada solo
     * con aplicaciones/deducciones es Aplicado.
     */
    public static String derivarEstado(BigDecimal montoInicial, BigDecimal saldoActual,
                                       BigDecimal totalDevuelto) {
        if (montoInicial == null || montoInicial.signum() == 0) {
            return "Retenido";
        }
        BigDecimal saldo = saldoActual == null ? BigDecimal.ZERO : saldoActual;
        BigDecimal devuelto = totalDevuelto == null ? BigDecimal.ZERO : totalDevuelto;
        if (saldo.signum() > 0 && devuelto.signum() > 0) {
            return "Devuelto parcial";
        }
        if (saldo.signum() > 0) {
            return "Retenido";
        }
        if (devuelto.signum() > 0) {
            return "Devuelto";
        }
        return "Aplicado";
    }
}
