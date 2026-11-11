package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Copia inmutable de los datos de un recibo para presentacion y reportes.
 * El Service debe obtener el numero de una transaccion de pago confirmada;
 * construir este DTO no registra pagos ni genera consecutivos.
 */
public final class ReciboDTO {
    private final long numero;
    private final UUID pagoId;
    private final UUID contratoId;
    private final LocalDate fechaPago;
    private final String contratoInfo;
    private final BigDecimal montoOriginal;
    private final String monedaOriginal;
    private final BigDecimal montoContrato;
    private final String monedaContrato;
    private final String metodo;
    private final String referencia;
    private final boolean anulado;

    public ReciboDTO(long numero, UUID pagoId, UUID contratoId,
                     LocalDate fechaPago, String contratoInfo,
                     BigDecimal montoOriginal, String monedaOriginal,
                     BigDecimal montoContrato, String monedaContrato, String metodo,
                     String referencia, boolean anulado) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El numero debe ser positivo.");
        }
        this.numero = numero;
        this.pagoId = Objects.requireNonNull(pagoId, "Falta el pago.");
        this.contratoId = Objects.requireNonNull(contratoId, "Falta el contrato.");
        this.fechaPago = Objects.requireNonNull(fechaPago, "Falta la fecha.");
        this.contratoInfo = texto(contratoInfo, "contrato");
        this.montoOriginal = monto(montoOriginal);
        this.monedaOriginal = texto(monedaOriginal, "moneda");
        if (!"USD".equals(this.monedaOriginal)
                && !"NIO".equals(this.monedaOriginal)) {
            throw new IllegalArgumentException("La moneda debe ser USD o NIO.");
        }
        this.montoContrato = monto(montoContrato);
        this.monedaContrato = monedaContrato;
        this.metodo = texto(metodo, "metodo de pago");
        this.referencia = referencia == null ? "" : referencia.trim();
        this.anulado = anulado;
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta " + campo + ".");
        }
        return valor.trim();
    }

    private static BigDecimal monto(BigDecimal valor) {
        Objects.requireNonNull(valor, "Falta el monto.");
        BigDecimal resultado = valor.setScale(2, RoundingMode.HALF_EVEN);
        if (resultado.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo.");
        }
        return resultado;
    }

    public long getNumero() { return numero; }
    public UUID getPagoId() { return pagoId; }
    public UUID getContratoId() { return contratoId; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getContratoInfo() { return contratoInfo; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public String getMonedaOriginal() { return monedaOriginal; }
    public BigDecimal getMontoContrato() { return montoContrato; }
    public String getMonedaContrato() { return monedaContrato; }
    public String getMetodo() { return metodo; }
    public String getReferencia() { return referencia; }
    public boolean isAnulado() { return anulado; }
}
