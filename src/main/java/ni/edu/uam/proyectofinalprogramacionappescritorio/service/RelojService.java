package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import org.hibernate.Session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

/** Reloj financiero respaldado por el instante de inicio de la transacción SQL. */
public final class RelojService {
    public static final ZoneId ZONA_OPERATIVA = ZoneId.of("America/Managua");
    public static final Duration DESFASE_MAXIMO = Duration.ofMinutes(5);

    private final Clock relojLocal;

    public RelojService() {
        this(Clock.systemUTC());
    }

    public RelojService(Clock relojLocal) {
        this.relojLocal = Objects.requireNonNull(relojLocal, "Falta el reloj local.");
    }

    /** Obtiene la hora operativa de la BD sin bloquear consultas ni tareas no financieras. */
    public TiempoOperativo leer(Session session) {
        Objects.requireNonNull(session, "Falta la sesión de base de datos.");
        OffsetDateTime fechaHora = session.createNativeQuery(
                "select current_timestamp", OffsetDateTime.class).getSingleResult();
        Instant instante = fechaHora.toInstant();
        return new TiempoOperativo(instante, fechaOperativa(instante));
    }

    /** Debe invocarse al inicio de una transacción financiera, antes de escribir. */
    public TiempoOperativo comprobar(Session session) {
        TiempoOperativo tiempo = leer(session);
        exigirDesfasePermitido(tiempo.instante(), relojLocal.instant());
        return tiempo;
    }

    public static void exigirDesfasePermitido(Instant instanteBd, Instant instanteLocal) {
        Objects.requireNonNull(instanteBd, "Falta el instante de la base de datos.");
        Objects.requireNonNull(instanteLocal, "Falta el instante local.");
        if (Duration.between(instanteBd, instanteLocal).abs().compareTo(DESFASE_MAXIMO) > 0) {
            throw new RelojDesfasadoException();
        }
    }

    public static LocalDate fechaOperativa(Instant instante) {
        return Objects.requireNonNull(instante, "Falta el instante operativo.")
                .atZone(ZONA_OPERATIVA).toLocalDate();
    }

    /** Formato ISO-8601 con el desplazamiento local, conservando el instante. */
    public static String presentar(Instant instante) {
        return instante == null ? null
                : instante.atZone(ZONA_OPERATIVA).toOffsetDateTime().toString();
    }

    public record TiempoOperativo(Instant instante, LocalDate fecha) {
        public TiempoOperativo {
            Objects.requireNonNull(instante, "Falta el instante operativo.");
            Objects.requireNonNull(fecha, "Falta la fecha operativa.");
        }
    }
}
