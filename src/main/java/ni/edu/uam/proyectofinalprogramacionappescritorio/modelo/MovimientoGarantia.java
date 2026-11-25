package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Movimiento inmutable de salida sobre una garantía: aplicación a un cargo
 * del mismo contrato, deducción o devolución al inquilino.
 */
@Entity
@Table(name = "movimiento_garantia",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_movimiento_garantia_liquidacion_orden",
                columnNames = {"liquidacion_id", "orden"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoGarantia {
    public static final String TIPO_APLICACION = "Aplicacion";
    public static final String TIPO_DEDUCCION = "Deduccion";
    public static final String TIPO_DEVOLUCION = "Devolucion";
    public static final Set<String> TIPOS = Set.of(TIPO_APLICACION, TIPO_DEDUCCION, TIPO_DEVOLUCION);
    public static final Set<String> METODOS_DEVOLUCION = Set.of(
            "Efectivo", "Transferencia", "Depósito bancario", "Otro");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "garantia_id", nullable = false, updatable = false)
    private UUID garantiaId;

    @Column(name = "cargo_id", updatable = false)
    private UUID cargoId;

    @Column(name = "liquidacion_id", nullable = false, updatable = false)
    private UUID liquidacionId;

    @Column(name = "liquidacion_hash", nullable = false, length = 64, updatable = false)
    private String liquidacionHash;

    @Column(nullable = false, updatable = false)
    private short orden;

    @Column(nullable = false, length = 24, updatable = false)
    private String tipo;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal importe;

    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDate fechaMovimiento;

    @Column(length = 24, updatable = false)
    private String metodo;

    @Column(nullable = false, length = 300, updatable = false)
    private String concepto;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private Long creadoPor;

    private MovimientoGarantia(UUID garantiaId, UUID cargoId, UUID liquidacionId, String liquidacionHash,
                               short orden, String tipo, BigDecimal importe, LocalDate fechaMovimiento,
                               String metodo, String concepto, Instant creadoEn, Long creadoPor) {
        this.garantiaId = Objects.requireNonNull(garantiaId, "Falta la garantía.");
        this.liquidacionId = Objects.requireNonNull(liquidacionId, "Falta el identificador de liquidación.");
        this.liquidacionHash = validarHash(liquidacionHash);
        if (orden < 1) {
            throw new IllegalArgumentException("El orden del movimiento debe ser positivo.");
        }
        this.orden = orden;
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("El tipo de movimiento de garantía no es válido.");
        }
        this.tipo = tipo;
        this.importe = validarImporte(importe);
        this.fechaMovimiento = Objects.requireNonNull(fechaMovimiento, "La fecha del movimiento es obligatoria.");
        this.concepto = validarConcepto(concepto);
        this.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        this.creadoPor = Objects.requireNonNull(creadoPor, "Falta el usuario autor.");

        if (TIPO_APLICACION.equals(tipo)) {
            if (cargoId == null) {
                throw new IllegalArgumentException("La aplicación de garantía requiere seleccionar un cargo.");
            }
            if (metodo != null) {
                throw new IllegalArgumentException("La aplicación de garantía no admite método de devolución.");
            }
            this.cargoId = cargoId;
            this.metodo = null;
        } else if (TIPO_DEDUCCION.equals(tipo)) {
            if (cargoId != null) {
                throw new IllegalArgumentException("La deducción no debe asociarse a un cargo.");
            }
            if (metodo != null) {
                throw new IllegalArgumentException("La deducción no admite método de devolución.");
            }
            this.cargoId = null;
            this.metodo = null;
        } else {
            if (cargoId != null) {
                throw new IllegalArgumentException("La devolución no debe asociarse a un cargo.");
            }
            if (metodo == null || !METODOS_DEVOLUCION.contains(metodo.strip())) {
                throw new IllegalArgumentException("El método de devolución debe ser Efectivo, Transferencia, Depósito bancario u Otro.");
            }
            this.cargoId = null;
            this.metodo = metodo.strip();
        }
    }

    public static MovimientoGarantia aplicacion(UUID garantiaId, UUID cargoId, UUID liquidacionId,
                                                String liquidacionHash, short orden, BigDecimal importe,
                                                LocalDate fechaMovimiento, String concepto,
                                                Instant creadoEn, Long creadoPor) {
        return new MovimientoGarantia(garantiaId, cargoId, liquidacionId, liquidacionHash, orden,
                TIPO_APLICACION, importe, fechaMovimiento, null, concepto, creadoEn, creadoPor);
    }

    public static MovimientoGarantia deduccion(UUID garantiaId, UUID liquidacionId, String liquidacionHash,
                                               short orden, BigDecimal importe, LocalDate fechaMovimiento,
                                               String concepto, Instant creadoEn, Long creadoPor) {
        return new MovimientoGarantia(garantiaId, null, liquidacionId, liquidacionHash, orden,
                TIPO_DEDUCCION, importe, fechaMovimiento, null, concepto, creadoEn, creadoPor);
    }

    public static MovimientoGarantia devolucion(UUID garantiaId, UUID liquidacionId, String liquidacionHash,
                                                short orden, BigDecimal importe, LocalDate fechaMovimiento,
                                                String metodo, String concepto,
                                                Instant creadoEn, Long creadoPor) {
        return new MovimientoGarantia(garantiaId, null, liquidacionId, liquidacionHash, orden,
                TIPO_DEVOLUCION, importe, fechaMovimiento, metodo, concepto, creadoEn, creadoPor);
    }

    private static BigDecimal validarImporte(BigDecimal importe) {
        if (importe == null || importe.signum() <= 0 || importe.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El importe del movimiento debe ser positivo y tener hasta dos decimales.");
        }
        return importe.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String validarConcepto(String concepto) {
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto del movimiento de garantía es obligatorio.");
        }
        String limpio = concepto.strip();
        if (limpio.length() > 300) {
            throw new IllegalArgumentException("El concepto no puede superar 300 caracteres.");
        }
        return limpio;
    }

    private static String validarHash(String hash) {
        if (hash == null || !hash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("La huella de liquidación es inválida.");
        }
        return hash;
    }
}
