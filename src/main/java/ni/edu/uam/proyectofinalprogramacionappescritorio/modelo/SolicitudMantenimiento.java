package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TransicionInvalidaException;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Solicitud de mantenimiento asociada a una unidad (M08 / RN-27).
 */
@Entity
@Table(name = "solicitud_mantenimiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolicitudMantenimiento {
    public static final String PRIORIDAD_BAJA = "Baja";
    public static final String PRIORIDAD_MEDIA = "Media";
    public static final String PRIORIDAD_ALTA = "Alta";
    public static final Set<String> PRIORIDADES = Set.of(PRIORIDAD_BAJA, PRIORIDAD_MEDIA, PRIORIDAD_ALTA);

    public static final String ESTADO_ABIERTA = "Abierta";
    public static final String ESTADO_EN_PROCESO = "En proceso";
    public static final String ESTADO_RESUELTA = "Resuelta";
    public static final String ESTADO_CANCELADA = "Cancelada";
    public static final Set<String> ESTADOS = Set.of(
            ESTADO_ABIERTA, ESTADO_EN_PROCESO, ESTADO_RESUELTA, ESTADO_CANCELADA);
    private static final Set<String> TERMINALES = Set.of(ESTADO_RESUELTA, ESTADO_CANCELADA);
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            ESTADO_ABIERTA, Set.of(ESTADO_EN_PROCESO, ESTADO_CANCELADA),
            ESTADO_EN_PROCESO, Set.of(ESTADO_RESUELTA, ESTADO_CANCELADA),
            ESTADO_RESUELTA, Set.of(),
            ESTADO_CANCELADA, Set.of()
    );

    @Id
    private UUID id;

    @Column(name = "unidad_id", nullable = false, updatable = false)
    private UUID unidadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", insertable = false, updatable = false)
    private Unidad unidad;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false, length = 24)
    private String prioridad;

    @Column(nullable = false, length = 24)
    private String estado;

    @Column(length = 120)
    private String responsable;

    @Column(name = "abierta_el", nullable = false, updatable = false)
    private LocalDate abiertaEl;

    @Column(name = "cerrada_el")
    private LocalDate cerradaEl;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "detalle_resolucion", length = 1000)
    private String detalleResolucion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false, updatable = false)
    private Usuario creadoPor;

    @Version
    private long version;

    public SolicitudMantenimiento(UUID unidadId, String descripcion, String prioridad, String responsable,
                                  LocalDate abiertaEl, Instant creadoEn, Usuario creadoPor) {
        this.id = UUID.randomUUID();
        this.unidadId = Objects.requireNonNull(unidadId, "Selecciona la unidad afectada.");
        this.descripcion = textoRequerido(descripcion, 1000,
                "La descripción de la solicitud es obligatoria (máximo 1000 caracteres).");
        this.prioridad = validarPrioridad(prioridad);
        this.estado = ESTADO_ABIERTA;
        this.responsable = textoOpcional(responsable, 120,
                "El responsable no puede superar 120 caracteres.");
        this.abiertaEl = Objects.requireNonNull(abiertaEl, "La fecha de apertura es obligatoria.");
        this.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        this.creadoPor = Objects.requireNonNull(creadoPor, "Falta el usuario autor.");
    }

    public boolean esTerminal() {
        return TERMINALES.contains(estado);
    }

    public void actualizarDetalles(String descripcion, String prioridad, String responsable) {
        if (esTerminal()) {
            throw new TransicionInvalidaException(
                    "ERR-066: La solicitud está cerrada y no admite modificaciones.");
        }
        this.descripcion = textoRequerido(descripcion, 1000,
                "La descripción de la solicitud es obligatoria (máximo 1000 caracteres).");
        this.prioridad = validarPrioridad(prioridad);
        this.responsable = textoOpcional(responsable, 120,
                "El responsable no puede superar 120 caracteres.");
    }

    /**
     * Aplica una transición RN-27. Si el destino coincide con el estado actual devuelve {@code false}
     * de forma idempotente sin alterar el estado ni los campos de cierre.
     */
    public boolean transicionarA(String destino, LocalDate fechaCierre, String motivoOResolucion) {
        if (destino == null || !ESTADOS.contains(destino.strip())) {
            throw new TransicionInvalidaException("ERR-066: El estado destino de mantenimiento no es válido.");
        }
        String estadoDestino = destino.strip();
        if (Objects.equals(estado, estadoDestino)) {
            return false;
        }
        if (!TRANSICIONES.getOrDefault(estado, Set.of()).contains(estadoDestino)) {
            throw new TransicionInvalidaException(
                    "ERR-066: No se permite pasar la solicitud de " + estado + " a " + estadoDestino + ".");
        }
        if (ESTADO_EN_PROCESO.equals(estadoDestino)) {
            if (motivoOResolucion != null && !motivoOResolucion.isBlank()) {
                this.responsable = textoOpcional(motivoOResolucion, 120,
                        "El responsable no puede superar 120 caracteres.");
            }
            this.estado = ESTADO_EN_PROCESO;
            return true;
        }
        if (fechaCierre == null) {
            throw new IllegalArgumentException("La fecha de cierre es obligatoria para resolver o cancelar.");
        }
        if (fechaCierre.isBefore(abiertaEl)) {
            throw new IllegalArgumentException("La fecha de cierre no puede ser anterior a la apertura.");
        }
        if (ESTADO_RESUELTA.equals(estadoDestino)) {
            String detalle = textoRequerido(motivoOResolucion, 1000,
                    "Describe el trabajo realizado para resolver la solicitud (máximo 1000 caracteres).");
            this.detalleResolucion = detalle;
            this.motivoCancelacion = null;
            this.cerradaEl = fechaCierre;
            this.estado = ESTADO_RESUELTA;
            return true;
        }
        String motivo = textoRequerido(motivoOResolucion, 500,
                "El motivo de cancelación es obligatorio (máximo 500 caracteres).");
        this.motivoCancelacion = motivo;
        this.cerradaEl = fechaCierre;
        this.estado = ESTADO_CANCELADA;
        return true;
    }

    private static String validarPrioridad(String prioridad) {
        if (prioridad == null || prioridad.isBlank()) {
            return PRIORIDAD_MEDIA;
        }
        String limpia = prioridad.strip();
        if (!PRIORIDADES.contains(limpia)) {
            throw new IllegalArgumentException("La prioridad debe ser Baja, Media o Alta.");
        }
        return limpia;
    }

    private static String textoRequerido(String valor, int maximo, String mensaje) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }

    private static String textoOpcional(String valor, int maximo, String mensaje) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.strip();
        if (limpio.length() > maximo) throw new IllegalArgumentException(mensaje);
        return limpio;
    }
}
