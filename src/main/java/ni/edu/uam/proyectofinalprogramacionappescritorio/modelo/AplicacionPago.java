package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Aplicación de un pago a un cargo del mismo contrato. El monto se expresa
 * siempre en la moneda del contrato y nunca puede superar el saldo del cargo.
 */
@Entity
@Table(name = "aplicacion_pago",
        uniqueConstraints = @UniqueConstraint(name = "uk_aplicacion_pago_pago_cargo",
                columnNames = {"pago_id", "cargo_id"}))
public class AplicacionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pago_id", nullable = false, updatable = false)
    private UUID pagoId;

    @Column(name = "cargo_id", nullable = false, updatable = false)
    private UUID cargoId;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal monto;

    protected AplicacionPago() { }

    public AplicacionPago(UUID pagoId, UUID cargoId, BigDecimal monto) {
        this.pagoId = pagoId;
        this.cargoId = Objects.requireNonNull(cargoId, "Falta el cargo.");
        Objects.requireNonNull(monto, "Falta el monto aplicado.");
        BigDecimal normalizado = monto.setScale(2, RoundingMode.HALF_EVEN);
        if (normalizado.signum() <= 0) {
            throw new IllegalArgumentException("El monto aplicado debe ser mayor que cero.");
        }
        this.monto = normalizado;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPagoId() {
        return pagoId;
    }

    public void setPagoId(UUID pagoId) {
        this.pagoId = pagoId;
    }

    public UUID getCargoId() {
        return cargoId;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}