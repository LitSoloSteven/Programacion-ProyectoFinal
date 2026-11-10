package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Pago registrado en Latch.
 */
public class Pago {

    private UUID id;
    private UUID contratoId;
    private String contratoInfo;
    private LocalDate fechaPago;
    private String monedaOriginal;
    private BigDecimal montoOriginal;
    private BigDecimal montoContrato;
    private String metodo;
    private String referencia;
    private String notas;
    private String estado;

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
        return contratoInfo;
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

    public String getMontoFormateado() {
        if (montoOriginal == null) return "0.00 " + monedaOriginal;
        return String.format("%.2f %s", montoOriginal, monedaOriginal);
    }

    @Override
    public String toString() {
        return "Pago " + getMontoFormateado() + " (" + fechaPago + ")";
    }
}
