package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
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
    private UUID id;
    @Column(name = "contrato_id", updatable = false, nullable = false)
    private UUID contratoId;
    @Transient
    private String contratoInfo;
    @Column(name = "fecha_pago", updatable = false, nullable = false)
    private LocalDate fechaPago;
    @Column(name = "moneda_original", length = 3, updatable = false, nullable = false)
    private String monedaOriginal;
    @Column(name = "monto_original", precision = 19, scale = 2, updatable = false, nullable = false)
    private BigDecimal montoOriginal;
    @Column(name = "monto_contrato", precision = 19, scale = 2, updatable = false, nullable = false)
    private BigDecimal montoContrato;
    @Column(length = 24, updatable = false, nullable = false)
    private String metodo;
    @Column(length = 100, updatable = false)
    private String referencia;
    @Column(length = 255, updatable = false)
    private String notas;
    @Column(length = 24, nullable = false)
    private String estado;

    // Campos de anulación: se escriben cuando el pago cambia a "Anulado",
    // por eso no pueden marcarse como updatable = false.
    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;
    @Column(name = "anulado_por")
    private Long anuladoPor;
    @Column(name = "anulado_en")
    private Instant anuladoEn;


    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id", insertable = false, updatable = false)
    private Contrato contrato;

    @Column(name = "moneda_contrato", length = 3, nullable = false, updatable = false)
    private String monedaContrato;

    @ManyToOne
    @JoinColumn(name = "tipo_cambio_id", updatable = false)
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public void setContratoId(UUID contratoId) {
        this.contratoId = contratoId;
    }

    public String getContratoInfo() {
        return contrato == null ? contratoInfo : contrato.toString();
    }

    public void setContratoInfo(String contratoInfo) {
        this.contratoInfo = contratoInfo;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMonedaOriginal() {
        return monedaOriginal;
    }

    public void setMonedaOriginal(String monedaOriginal) {
        this.monedaOriginal = monedaOriginal;
    }

    public BigDecimal getMontoOriginal() {
        return montoOriginal;
    }

    public void setMontoOriginal(BigDecimal montoOriginal) {
        this.montoOriginal = montoOriginal;
    }

    public BigDecimal getMontoContrato() {
        return montoContrato;
    }

    public void setMontoContrato(BigDecimal montoContrato) {
        this.montoContrato = montoContrato;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public Long getAnuladoPor() {
        return anuladoPor;
    }

    public Instant getAnuladoEn() {
        return anuladoEn;
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

    public String getMonedaContrato() {
        return monedaContrato;
    }

    public TipoCambio getTipoCambio() {
        return tipoCambio;
    }

    public void aplicarConversion(String monedaContrato, BigDecimal montoContrato, TipoCambio tipoCambio) {
        this.monedaContrato = monedaContrato;
        this.montoContrato = montoContrato;
        this.tipoCambio = tipoCambio;
    }
}
