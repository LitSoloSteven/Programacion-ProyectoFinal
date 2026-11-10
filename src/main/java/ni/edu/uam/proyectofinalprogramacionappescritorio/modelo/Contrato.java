package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Contrato de arrendamiento en Latch.
 */
public class Contrato {

    private UUID id;
    private UUID unidadId;
    private String unidadInfo;
    private UUID inquilinoId;
    private String inquilinoNombre;
    private LocalDate inicio;
    private LocalDate fin;
    private BigDecimal canon;
    private String moneda;
    private int diaPago;
    private BigDecimal depositoPactado;
    private String estado;
    private long version;

    public Contrato() {
        this.moneda = "USD";
        this.diaPago = 5;
        this.canon = BigDecimal.ZERO;
        this.depositoPactado = BigDecimal.ZERO;
        this.estado = "Vigente";
        this.version = 0;
    }

    public Contrato(UUID id, UUID unidadId, String unidadInfo, UUID inquilinoId, String inquilinoNombre,
                    LocalDate inicio, LocalDate fin, BigDecimal canon, String moneda,
                    int diaPago, BigDecimal depositoPactado, String estado, long version) {
        this.id = id;
        this.unidadId = unidadId;
        this.unidadInfo = unidadInfo;
        this.inquilinoId = inquilinoId;
        this.inquilinoNombre = inquilinoNombre;
        this.inicio = inicio;
        this.fin = fin;
        this.canon = canon;
        this.moneda = moneda;
        this.diaPago = diaPago;
        this.depositoPactado = depositoPactado;
        this.estado = estado;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUnidadId() {
        return unidadId;
    }

    public void setUnidadId(UUID unidadId) {
        this.unidadId = unidadId;
    }

    public String getUnidadInfo() {
        return unidadInfo;
    }

    public void setUnidadInfo(String unidadInfo) {
        this.unidadInfo = unidadInfo;
    }

    public UUID getInquilinoId() {
        return inquilinoId;
    }

    public void setInquilinoId(UUID inquilinoId) {
        this.inquilinoId = inquilinoId;
    }

    public String getInquilinoNombre() {
        return inquilinoNombre;
    }

    public void setInquilinoNombre(String inquilinoNombre) {
        this.inquilinoNombre = inquilinoNombre;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public void setFin(LocalDate fin) {
        this.fin = fin;
    }

    public BigDecimal getCanon() {
        return canon;
    }

    public void setCanon(BigDecimal canon) {
        this.canon = canon;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public int getDiaPago() {
        return diaPago;
    }

    public void setDiaPago(int diaPago) {
        this.diaPago = diaPago;
    }

    public BigDecimal getDepositoPactado() {
        return depositoPactado;
    }

    public void setDepositoPactado(BigDecimal depositoPactado) {
        this.depositoPactado = depositoPactado;
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

    public String getCanonFormateado() {
        if (canon == null) return "0.00 " + moneda;
        return String.format("%.2f %s", canon, moneda);
    }

    public String getDepositoFormateado() {
        if (depositoPactado == null) return "0.00 " + moneda;
        return String.format("%.2f %s", depositoPactado, moneda);
    }

    @Override
    public String toString() {
        return (inquilinoNombre != null ? inquilinoNombre : "Contrato") + " — " + (unidadInfo != null ? unidadInfo : "");
    }
}
