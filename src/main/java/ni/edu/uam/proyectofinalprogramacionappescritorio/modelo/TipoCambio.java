package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Instantanea inmutable de una tasa NIO por USD, con su procedencia.
 * Una referencia informativa no puede usarse para convertir pagos.
 * El Service debe comprobar el permiso de Propietario antes de autorizar.
 * Este modelo aun no incluye el mapeo ORM ni acceso a servicios externos.
 */
public final class TipoCambio {
    private final UUID id;
    private final LocalDate fecha;
    private final BigDecimal nioPorUsd;
    private final String fuente;
    private final Instant registradoEn;
    private final UUID autorizadoPor;
    private final String motivo;

    private TipoCambio(LocalDate fecha, BigDecimal tasa, String fuente,
                       Instant registradoEn, UUID autorizadoPor, String motivo) {
        this.id = UUID.randomUUID();
        this.fecha = Objects.requireNonNull(fecha, "Falta la fecha de la tasa.");
        this.registradoEn = Objects.requireNonNull(registradoEn, "Falta el instante de registro.");
        Objects.requireNonNull(tasa, "Falta la tasa.");
        this.nioPorUsd = tasa.setScale(8, RoundingMode.HALF_EVEN);
        if (this.nioPorUsd.signum() <= 0) {
            throw new IllegalArgumentException("La tasa debe ser positiva a ocho decimales.");
        }
        this.fuente = texto(fuente, "fuente");
        this.autorizadoPor = autorizadoPor;
        this.motivo = autorizadoPor == null ? "" : texto(motivo, "motivo de autorizacion");
    }

    public static TipoCambio informativa(LocalDate fecha, BigDecimal tasa,
                                          String fuente, Instant registradoEn) {
        return new TipoCambio(fecha, tasa, fuente, registradoEn, null, null);
    }

    /** Invocar solo desde un Service que haya comprobado el rol del actor. */
    public static TipoCambio autorizada(LocalDate fecha, BigDecimal tasa,
                                         String fuente, Instant registradoEn,
                                         UUID autorizadoPor, String motivo) {
        Objects.requireNonNull(autorizadoPor, "Falta el usuario autorizador.");
        return new TipoCambio(fecha, tasa, fuente, registradoEn, autorizadoPor, motivo);
    }

    public BigDecimal convertir(BigDecimal monto, Moneda origen,
                                 Moneda destino, LocalDate fechaPago) {
        Objects.requireNonNull(monto, "Falta el monto.");
        Objects.requireNonNull(origen, "Falta la moneda de origen.");
        Objects.requireNonNull(destino, "Falta la moneda de destino.");
        Objects.requireNonNull(fechaPago, "Falta la fecha del pago.");
        if (monto.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo.");
        }
        if (origen == destino) {
            return monto.setScale(2, RoundingMode.HALF_EVEN);
        }
        if (!isAutorizada()) {
            throw new IllegalStateException("Una tasa informativa no puede convertir pagos.");
        }
        if (!fecha.equals(fechaPago)) {
            throw new IllegalArgumentException("La tasa debe estar autorizada para la fecha del pago.");
        }
        // No redondear el monto original antes de la conversion.
        return origen == Moneda.USD
                ? monto.multiply(nioPorUsd).setScale(2, RoundingMode.HALF_EVEN)
                : monto.divide(nioPorUsd, 2, RoundingMode.HALF_EVEN);
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta " + campo + ".");
        }
        return valor.trim();
    }

    public UUID getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getNioPorUsd() { return nioPorUsd; }
    public String getFuente() { return fuente; }
    public Instant getRegistradoEn() { return registradoEn; }
    public UUID getAutorizadoPor() { return autorizadoPor; }
    public String getMotivo() { return motivo; }
    public boolean isAutorizada() { return autorizadoPor != null; }
}
