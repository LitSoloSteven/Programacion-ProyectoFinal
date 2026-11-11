package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Comprobante administrativo: conserva una copia de los datos confirmados.
 * El Service de pagos debe asignar el numero y persistir pago/recibo en la
 * misma transaccion. Esta entidad no genera consecutivos ni confirma pagos.
 * No debe eliminarse fisicamente ni modificarse su informacion financiera.
 */
@Entity
@Table(name = "recibo", schema = "latch", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recibo_numero", columnNames = "numero"),
    @UniqueConstraint(name = "uq_recibo_pago", columnNames = "pago_id")
})
public class Recibo {
    @Id
    @Column(updatable = false)
    private UUID id;
    @Column(nullable = false, updatable = false)
    private long numero;
    // FK a latch.pago. Se mantiene UUID hasta integrar el modelo Pago del equipo.
    @Column(name = "pago_id", nullable = false, updatable = false)
    private UUID pagoId;
    @Column(name = "contrato_id", nullable = false, updatable = false)
    private UUID contratoId;
    @Column(name = "fecha_pago", nullable = false, updatable = false)
    private LocalDate fechaPago;
    @Column(name = "contrato_info", nullable = false, updatable = false, length = 500)
    private String contratoInfo;
    @Column(name = "monto_original", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal montoOriginal;
    @Column(name = "moneda_original", nullable = false, updatable = false, length = 3)
    private String monedaOriginal;
    @Column(name = "monto_contrato", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal montoContrato;
    @Column(nullable = false, updatable = false, length = 24)
    private String metodo;
    @Column(updatable = false, length = 100)
    private String referencia;
    @Column(name = "emitido_por", nullable = false, updatable = false)
    private UUID emitidoPor;
    @Column(name = "emitido_en", nullable = false, updatable = false)
    private Instant emitidoEn;
    @Column(nullable = false)
    private boolean anulado;
    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;
    @Column(name = "anulado_por")
    private UUID anuladoPor;
    @Column(name = "anulado_en")
    private Instant anuladoEn;
    @Version
    private long version;

    protected Recibo() { }

    public Recibo(long numero, UUID pagoId, UUID contratoId, LocalDate fechaPago,
                  String contratoInfo, BigDecimal montoOriginal, String monedaOriginal,
                  BigDecimal montoContrato, String metodo, String referencia,
                  UUID emitidoPor, Instant emitidoEn) {
        if (numero <= 0) throw new IllegalArgumentException("El numero debe ser positivo.");
        this.id = UUID.randomUUID();
        this.numero = numero;
        this.pagoId = Objects.requireNonNull(pagoId, "Falta el pago.");
        this.contratoId = Objects.requireNonNull(contratoId, "Falta el contrato.");
        this.fechaPago = Objects.requireNonNull(fechaPago, "Falta la fecha del pago.");
        this.contratoInfo = texto(contratoInfo, "contrato", 500);
        this.montoOriginal = monto(montoOriginal);
        this.monedaOriginal = texto(monedaOriginal, "moneda", 3);
        if (!"USD".equals(this.monedaOriginal) && !"NIO".equals(this.monedaOriginal)) {
            throw new IllegalArgumentException("La moneda debe ser USD o NIO.");
        }
        this.montoContrato = monto(montoContrato);
        this.metodo = texto(metodo, "metodo", 24);
        this.referencia = referencia == null || referencia.isBlank()
                ? "" : texto(referencia, "referencia", 100);
        this.emitidoPor = Objects.requireNonNull(emitidoPor, "Falta el actor.");
        this.emitidoEn = Objects.requireNonNull(emitidoEn, "Falta el instante UTC.");
    }

    /**
     * Solo transicion de estado; conserva numero y montos originales.
     * El Service debe coordinar tambien la anulacion del pago y su auditoria.
     */
    public void anular(Usuario actor, String motivo, Instant instante) {
        if (actor == null || actor.getId() == null || !actor.isActivo() || !actor.esPropietario()) {
            throw new SecurityException("Solo el Propietario activo puede anular recibos.");
        }
        // Un reintento no sustituye el motivo ni el actor de la anulacion original.
        if (anulado) return;
        String motivoValido = texto(motivo, "motivo de anulacion", 500);
        Objects.requireNonNull(instante, "Falta el instante UTC.");
        if (instante.isBefore(emitidoEn)) {
            throw new IllegalArgumentException("La anulacion no puede ser anterior a la emision.");
        }
        motivoAnulacion = motivoValido;
        anuladoPor = actor.getId();
        anuladoEn = instante;
        anulado = true;
    }

    private static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta " + campo + ".");
        }
        String limpio = valor.trim();
        if (limpio.length() > limite) throw new IllegalArgumentException("Longitud invalida: " + campo);
        return limpio;
    }
    private static BigDecimal monto(BigDecimal valor) {
        Objects.requireNonNull(valor, "Falta el monto.");
        BigDecimal redondeado = valor.setScale(2, RoundingMode.HALF_EVEN);
        if (redondeado.signum() <= 0 || redondeado.precision() > 19) {
            throw new IllegalArgumentException("El monto debe ser positivo y caber en NUMERIC(19,2).");
        }
        return redondeado;
    }

    public UUID getId() { return id; }
    public long getNumero() { return numero; }
    public UUID getPagoId() { return pagoId; }
    public UUID getContratoId() { return contratoId; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getContratoInfo() { return contratoInfo; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public String getMonedaOriginal() { return monedaOriginal; }
    public BigDecimal getMontoContrato() { return montoContrato; }
    public String getMetodo() { return metodo; }
    public String getReferencia() { return referencia; }
    public UUID getEmitidoPor() { return emitidoPor; }
    public Instant getEmitidoEn() { return emitidoEn; }
    public boolean isAnulado() { return anulado; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public UUID getAnuladoPor() { return anuladoPor; }
    public Instant getAnuladoEn() { return anuladoEn; }
}
