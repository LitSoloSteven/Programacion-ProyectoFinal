package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro persistente de una escritura financiera protegida por
 * Idempotency-Key. Guarda una huella canónica de la petición y la respuesta
 * original para poder repetirla sin duplicar el pago.
 */
@Entity
@Table(name = "solicitud_idempotente",
        uniqueConstraints = @UniqueConstraint(name = "uk_solicitud_idempotente_clave",
                columnNames = "clave"))
public class SolicitudIdempotente {

    public static final String EN_PROCESO = "EN_PROCESO";
    public static final String COMPLETADA = "COMPLETADA";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 36, updatable = false)
    private String clave;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 60, updatable = false)
    private String recurso;

    @Column(name = "hash_peticion", nullable = false, length = 64, updatable = false)
    private String hashPeticion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "codigo_estado")
    private Integer codigoEstado;

    @Lob
    @Column(name = "respuesta_json")
    private String respuestaJson;

    @Column(name = "completado_en")
    private Instant completadoEn;

    protected SolicitudIdempotente() { }

    public SolicitudIdempotente(String clave, Long usuarioId, String recurso,
                                String hashPeticion, Instant creadoEn) {
        this.clave = Objects.requireNonNull(clave, "Falta la Idempotency-Key.");
        this.usuarioId = Objects.requireNonNull(usuarioId, "Falta el usuario.");
        this.recurso = Objects.requireNonNull(recurso, "Falta el recurso.");
        this.hashPeticion = Objects.requireNonNull(hashPeticion, "Falta la huella de la petición.");
        this.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        this.estado = EN_PROCESO;
    }

    public void completar(int codigoEstado, String respuestaJson, Instant completadoEn) {
        this.estado = COMPLETADA;
        this.codigoEstado = codigoEstado;
        this.respuestaJson = Objects.requireNonNull(respuestaJson);
        this.completadoEn = Objects.requireNonNull(completadoEn);
    }

    public UUID getId() {
        return id;
    }

    public String getClave() {
        return clave;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getHashPeticion() {
        return hashPeticion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public String getEstado() {
        return estado;
    }

    public Integer getCodigoEstado() {
        return codigoEstado;
    }

    public String getRespuestaJson() {
        return respuestaJson;
    }

    public Instant getCompletadoEn() {
        return completadoEn;
    }

    public boolean isCompletada() {
        return COMPLETADA.equals(estado) && respuestaJson != null;
    }
}