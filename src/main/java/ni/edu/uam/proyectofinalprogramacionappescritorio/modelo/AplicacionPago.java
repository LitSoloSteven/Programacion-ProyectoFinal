package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aplicación de un pago o garantía a un cargo del mismo contrato. El monto se
 * expresa siempre en la moneda del contrato y nunca puede superar el saldo del
 * cargo. Las aplicaciones de crédito pueden revertirse de forma trazable.
 */
@Entity
@Table(name = "aplicacion_pago")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AplicacionPago {

    public static final String ORIGEN_INICIAL = "Inicial";
    public static final String ORIGEN_CREDITO = "Credito";
    public static final String ORIGEN_GARANTIA = "Garantia";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pago_id", nullable = true, updatable = false)
    @Setter
    private UUID pagoId;

    @Column(name = "movimiento_id", updatable = false)
    private UUID movimientoId;

    @Column(name = "cargo_id", nullable = false, updatable = false)
    private UUID cargoId;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal monto;

    @Column(nullable = false, length = 24, updatable = false)
    private String origen = ORIGEN_INICIAL;

    @Column(name = "fecha_aplicacion", updatable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @Column(name = "creado_por", updatable = false)
    private Long creadoPor;

    @Column(name = "revertida_en")
    private Instant revertidaEn;

    @Column(name = "revertida_por")
    private Long revertidaPor;

    @Column(name = "motivo_reversion", length = 500)
    private String motivoReversion;

    public AplicacionPago(UUID pagoId, UUID cargoId, BigDecimal monto) {
        this.pagoId = pagoId;
        this.movimientoId = null;
        this.cargoId = Objects.requireNonNull(cargoId, "Falta el cargo.");
        this.monto = validarMonto(monto);
        this.origen = ORIGEN_INICIAL;
    }

    public static AplicacionPago inicial(UUID pagoId, UUID cargoId, BigDecimal monto,
                                         LocalDate fechaAplicacion, Instant creadoEn, Long creadoPor) {
        AplicacionPago aplicacion = new AplicacionPago(pagoId, cargoId, monto);
        aplicacion.fechaAplicacion = Objects.requireNonNull(fechaAplicacion, "Falta la fecha de aplicación.");
        aplicacion.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        aplicacion.creadoPor = Objects.requireNonNull(creadoPor, "Falta el actor de creación.");
        return aplicacion;
    }

    public static AplicacionPago desdeCredito(UUID pagoId, UUID cargoId, BigDecimal monto,
                                              LocalDate fechaAplicacion, Instant creadoEn, Long creadoPor) {
        AplicacionPago aplicacion = new AplicacionPago();
        aplicacion.pagoId = Objects.requireNonNull(pagoId, "Falta el pago origen del crédito.");
        aplicacion.movimientoId = null;
        aplicacion.cargoId = Objects.requireNonNull(cargoId, "Falta el cargo.");
        aplicacion.monto = validarMonto(monto);
        aplicacion.origen = ORIGEN_CREDITO;
        aplicacion.fechaAplicacion = Objects.requireNonNull(fechaAplicacion, "Falta la fecha de aplicación.");
        aplicacion.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        aplicacion.creadoPor = Objects.requireNonNull(creadoPor, "Falta el actor de creación.");
        return aplicacion;
    }

    public static AplicacionPago desdeGarantia(UUID movimientoId, UUID cargoId, BigDecimal monto) {
        AplicacionPago aplicacion = new AplicacionPago();
        aplicacion.pagoId = null;
        aplicacion.movimientoId = Objects.requireNonNull(movimientoId, "Falta el movimiento de garantía.");
        aplicacion.cargoId = Objects.requireNonNull(cargoId, "Falta el cargo.");
        aplicacion.monto = validarMonto(monto);
        aplicacion.origen = ORIGEN_GARANTIA;
        return aplicacion;
    }

    public boolean isRevertida() {
        return revertidaEn != null;
    }

    public void revertir(String motivo, Long usuarioId, Instant instante) {
        if (revertidaEn != null) {
            throw new IllegalStateException("La aplicación ya fue revertida.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de reversión es obligatorio.");
        }
        String limpio = motivo.strip();
        if (limpio.length() > 500) {
            throw new IllegalArgumentException("El motivo de reversión no puede superar 500 caracteres.");
        }
        this.revertidaPor = Objects.requireNonNull(usuarioId, "Falta el usuario que revierte.");
        this.revertidaEn = Objects.requireNonNull(instante, "Falta el instante de reversión.");
        this.motivoReversion = limpio;
    }

    private static BigDecimal validarMonto(BigDecimal monto) {
        Objects.requireNonNull(monto, "Falta el monto aplicado.");
        BigDecimal normalizado = monto.setScale(2, RoundingMode.HALF_EVEN);
        if (normalizado.signum() <= 0) {
            throw new IllegalArgumentException("El monto aplicado debe ser mayor que cero.");
        }
        return normalizado;
    }

}