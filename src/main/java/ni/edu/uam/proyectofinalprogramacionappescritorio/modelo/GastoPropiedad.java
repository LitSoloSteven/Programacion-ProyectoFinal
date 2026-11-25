package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Gasto efectivo de mantenimiento a cargo del propietario (M08 / RN-23).
 * No genera cargos al inquilino ni altera depósitos en garantía.
 */
@Entity
@Immutable
@Table(name = "gasto_propiedad")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GastoPropiedad {
    @Id
    private UUID id;

    @Column(name = "propiedad_id", nullable = false, updatable = false)
    private UUID propiedadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propiedad_id", insertable = false, updatable = false)
    private Propiedad propiedad;

    @Column(name = "solicitud_id", updatable = false)
    private UUID solicitudId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", insertable = false, updatable = false)
    private SolicitudMantenimiento solicitud;

    @Column(name = "fecha_gasto", nullable = false, updatable = false)
    private LocalDate fechaGasto;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal importe;

    @Column(nullable = false, length = 3, updatable = false)
    private String moneda;

    @Column(nullable = false, length = 300, updatable = false)
    private String concepto;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false, updatable = false)
    private Usuario creadoPor;

    public GastoPropiedad(UUID propiedadId, UUID solicitudId, LocalDate fechaGasto, BigDecimal importe,
                          String moneda, String concepto, Instant creadoEn, Usuario creadoPor) {
        this.id = UUID.randomUUID();
        this.propiedadId = Objects.requireNonNull(propiedadId, "Selecciona la propiedad del gasto.");
        this.solicitudId = solicitudId;
        this.fechaGasto = Objects.requireNonNull(fechaGasto, "La fecha del gasto es obligatoria.");
        this.importe = validarImporte(importe);
        this.moneda = validarMoneda(moneda);
        this.concepto = validarConcepto(concepto);
        this.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        this.creadoPor = Objects.requireNonNull(creadoPor, "Falta el usuario autor.");
    }

    private static BigDecimal validarImporte(BigDecimal importe) {
        if (importe == null || importe.signum() <= 0 || importe.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El importe del gasto debe ser mayor que cero y tener hasta dos decimales.");
        }
        return importe.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String validarMoneda(String moneda) {
        if (moneda == null) {
            throw new IllegalArgumentException("La moneda del gasto debe ser USD o NIO.");
        }
        return Moneda.desde(moneda.strip()).name();
    }

    private static String validarConcepto(String concepto) {
        if (concepto == null || concepto.isBlank() || concepto.strip().length() > 300) {
            throw new IllegalArgumentException("El concepto del gasto es obligatorio (máximo 300 caracteres).");
        }
        return concepto.strip();
    }
}
