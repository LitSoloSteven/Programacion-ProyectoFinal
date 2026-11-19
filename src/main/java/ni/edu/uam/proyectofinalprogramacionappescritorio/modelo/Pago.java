package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Pago registrado en Latch.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    private UUID id;
    @Column(name = "contrato_id", updatable = false, nullable = false)
    @Getter
    @Setter
    private UUID contratoId;
    @Transient
    @Setter
    private String contratoInfo;
    @Column(name = "fecha_pago", updatable = false, nullable = false)
    @Getter
    @Setter
    private LocalDate fechaPago;
    @Generated(event = EventType.INSERT)
    @Column(name = "creado_en", insertable = false, updatable = false, nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    @Getter
    private Instant creadoEn;
    @Column(name = "moneda_original", length = 3, updatable = false, nullable = false)
    @Getter
    @Setter
    private String monedaOriginal;
    @Column(name = "monto_original", precision = 19, scale = 2, updatable = false, nullable = false)
    @Getter
    @Setter
    private BigDecimal montoOriginal;
    @Column(name = "monto_contrato", precision = 19, scale = 2, updatable = false, nullable = false)
    @Getter
    private BigDecimal montoContrato;
    @Column(length = 24, updatable = false, nullable = false)
    @Getter
    @Setter
    private String metodo;
    @Column(length = 100, updatable = false)
    @Getter
    @Setter
    private String referencia;
    @Column(length = 255, updatable = false)
    @Getter
    @Setter
    private String notas;
    @Column(length = 24, nullable = false)
    @Getter
    @Setter
    private String estado;

    // Campos de anulación: se escriben cuando el pago cambia a "Anulado",
    // por eso no pueden marcarse como updatable = false.
    @Column(name = "motivo_anulacion", length = 500)
    @Getter
    private String motivoAnulacion;
    @Column(name = "anulado_por")
    @Getter
    private Long anuladoPor;
    @Column(name = "anulado_en")
    @Getter
    private Instant anuladoEn;


    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id", insertable = false, updatable = false)
    private Contrato contrato;

    @Column(name = "moneda_contrato", length = 3, nullable = false, updatable = false)
    @Getter
    private String monedaContrato;

    @ManyToOne
    @JoinColumn(name = "tipo_cambio_id", updatable = false)
    @Getter
    private TipoCambio tipoCambio;

    @Version
    private long version;

    public Pago() {
        this.fechaPago = LocalDate.now();
        this.monedaOriginal = "USD";
        this.montoOriginal = BigDecimal.ZERO;
        this.montoContrato = BigDecimal.ZERO;
        this.metodo = "Efectivo";
        this.estado = "Registrado";
    }

    public Pago(UUID id, UUID contratoId, String contratoInfo, LocalDate fechaPago,
                String monedaOriginal, BigDecimal montoOriginal, BigDecimal montoContrato,
                String metodo, String referencia, String notas, String estado) {
        this.id = id;
        this.contratoId = contratoId;
        this.contratoInfo = contratoInfo;
        this.fechaPago = fechaPago;
        this.monedaOriginal = monedaOriginal;
        this.montoOriginal = montoOriginal;
        this.montoContrato = montoContrato;
        this.metodo = metodo;
        this.referencia = referencia;
        this.notas = notas;
        this.estado = estado;
    }

    public String getContratoInfo() {
        return contrato == null ? contratoInfo : contrato.toString();
    }


    public void setMontoContrato(BigDecimal montoContrato) {
        this.montoContrato = montoContrato;
    }

    /**
     * Anula el pago conservando el motivo y el actor. La comprobación de
     * permisos y del estado previo corresponde al Service.
     */
    public void anular(String motivo, Long usuarioId, Instant instante) {
        this.estado = "Anulado";
        this.motivoAnulacion = motivo;
        this.anuladoPor = usuarioId;
        this.anuladoEn = instante;
    }

    public String getMontoFormateado() {
        if (montoOriginal == null) return "0.00 " + monedaOriginal;
        return String.format("%.2f %s", montoOriginal, monedaOriginal);
    }

    @Override
    public String toString() {
        return "Pago " + getMontoFormateado() + " (" + fechaPago + ")";
    }

    public void aplicarConversion(String monedaContrato, BigDecimal montoContrato, TipoCambio tipoCambio) {
        this.monedaContrato = monedaContrato;
        this.montoContrato = montoContrato;
        this.tipoCambio = tipoCambio;
    }
}
