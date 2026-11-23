package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoMoneda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Cargo u obligación financiera en Latch.
 */
@Entity
@Table(name = "cargo")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    private UUID id;
    @Column(name = "contrato_id", nullable = false)
    @Getter
    @Setter
    private UUID contratoId;
    @Transient
    @Setter
    private String contratoInfo;
    @Column(length = 24, nullable = false)
    @Getter
    @Setter
    private String tipo;
    @Column(nullable = false)
    @Getter
    @Setter
    private LocalDate periodo;
    @Column(name = "fecha_emision", nullable = false)
    @Getter
    @Setter
    private LocalDate fechaEmision;
    @Column(name = "vence_el", nullable = false)
    @Getter
    @Setter
    private LocalDate venceEl;
    @Column(precision = 19, scale = 2, nullable = false)
    @Getter
    @Setter
    private BigDecimal importe;
    @Column(length = 300, nullable = false)
    @Getter
    @Setter
    private String concepto;
    @Column(length = 24, nullable = false)
    private String estado;
    @Version
    @Getter
    @Setter
    private long version;
    @Transient
    private String estadoDerivado;


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

    public String getContratoInfo() {
        return contrato == null ? contratoInfo : contrato.toString();
    }

    public String getEstado() {
        return estadoDerivado == null ? estado : estadoDerivado;
    }

    public String getEstadoPersistido() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
        this.estadoDerivado = null;
    }

    public void establecerEstadoDerivado(BigDecimal montoAplicado, LocalDate fechaCorte) {
        if ("Cancelado".equals(estado) || "Anulado".equals(estado)) {
            estadoDerivado = "Anulado";
            return;
        }
        BigDecimal aplicado = montoAplicado == null ? BigDecimal.ZERO : montoAplicado;
        BigDecimal saldo = saldoPendiente(aplicado);
        if (saldo.signum() <= 0) {
            estadoDerivado = "Pagado";
        } else if (venceEl != null && fechaCorte != null && venceEl.isBefore(fechaCorte)) {
            estadoDerivado = "Vencido";
        } else if (aplicado.signum() > 0) {
            estadoDerivado = "Parcial";
        } else {
            estadoDerivado = "Pendiente";
        }
    }

    public BigDecimal saldoPendiente(BigDecimal montoAplicado) {
        if ("Cancelado".equals(estado) || "Anulado".equals(estado)) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal monto = importe == null ? BigDecimal.ZERO : importe;
        BigDecimal aplicado = montoAplicado == null ? BigDecimal.ZERO : montoAplicado;
        BigDecimal saldo = monto.subtract(aplicado);
        return saldo.signum() <= 0
                ? BigDecimal.ZERO.setScale(2)
                : saldo.setScale(2, RoundingMode.HALF_EVEN);
    }

    public String getImporteFormateado() {
        if (contrato != null) {
            return FormatoMoneda.formatear(contrato.getMoneda(), importe);
        }
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(importe == null ? BigDecimal.ZERO : importe);
    }

    @Override
    public String toString() {
        return tipo + " - " + concepto + " (" + getImporteFormateado() + ")";
    }
}
