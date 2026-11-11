package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Cargo u obligación financiera en Latch.
 */
@Entity
@Table(name = "cargo")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "contrato_id", nullable = false)
    private UUID contratoId;
    @Transient
    private String contratoInfo;
    @Column(length = 24, nullable = false)
    private String tipo;
    @Column(nullable = false)
    private LocalDate periodo;
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;
    @Column(name = "vence_el", nullable = false)
    private LocalDate venceEl;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal importe;
    @Column(length = 300, nullable = false)
    private String concepto;
    @Column(length = 24, nullable = false)
    private String estado;
    @Version
    private long version;


    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id", insertable = false, updatable = false)
    private Contrato contrato;

    public Cargo() {
        this.tipo = "Renta";
        this.importe = BigDecimal.ZERO;
        this.estado = "Pendiente";
        this.fechaEmision = LocalDate.now();
        this.venceEl = LocalDate.now().plusDays(5);
        this.periodo = LocalDate.now().withDayOfMonth(1);
        this.version = 0;
    }

    public Cargo(UUID id, UUID contratoId, String contratoInfo, String tipo, LocalDate periodo,
                 LocalDate fechaEmision, LocalDate venceEl, BigDecimal importe,
                 String concepto, String estado, long version) {
        this.id = id;
        this.contratoId = contratoId;
        this.contratoInfo = contratoInfo;
        this.tipo = tipo;
        this.periodo = periodo;
        this.fechaEmision = fechaEmision;
        this.venceEl = venceEl;
        this.importe = importe;
        this.concepto = concepto;
        this.estado = estado;
        this.version = version;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getPeriodo() {
        return periodo;
    }

    public void setPeriodo(LocalDate periodo) {
        this.periodo = periodo;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getVenceEl() {
        return venceEl;
    }

    public void setVenceEl(LocalDate venceEl) {
        this.venceEl = venceEl;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getImporteFormateado() {
        if (importe == null) return "$0.00";
        return String.format("$%.2f", importe);
    }

    @Override
    public String toString() {
        return tipo + " - " + concepto + " (" + getImporteFormateado() + ")";
    }
}
