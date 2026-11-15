package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@org.hibernate.annotations.Immutable
@Table(name = "auditoria", indexes = @Index(name = "ix_auditoria_actor_instante", columnList = "usuario_id,instante"))
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_id", nullable = true, updatable = false)
    private Usuario actor;
    @Column(nullable = false, updatable = false)
    private Instant instante;
    @Column(nullable = false, updatable = false)
    private UUID correlacion;
    @Column(nullable = false, length = 80, updatable = false)
    private String accion;
    @Column(nullable = false, length = 160, updatable = false)
    private String referencia;

    protected Auditoria() {}

    public Auditoria(Usuario actor, UUID correlacion, String accion, String referencia) {
        this(actor, Instant.now(), correlacion, accion, referencia);
    }

    public Auditoria(Usuario actor, Instant instante, UUID correlacion,
                     String accion, String referencia) {
        this(actor, instante, correlacion, accion, referencia, false);
    }

    private Auditoria(Usuario actor, Instant instante, UUID correlacion,
                      String accion, String referencia, boolean permitirActorNulo) {
        this.actor = permitirActorNulo ? actor : Objects.requireNonNull(actor);
        this.instante = Objects.requireNonNull(instante);
        this.correlacion = Objects.requireNonNull(correlacion);
        this.accion = texto(accion, 80);
        this.referencia = texto(referencia, 160);
    }

    public static Auditoria intentoLoginFallido(UUID correlacion, String referencia) {
        return new Auditoria(null, Instant.now(), correlacion, "LOGIN_FALLIDO", referencia, true);
    }

    public static Auditoria intentoLoginFallido(Instant instante, UUID correlacion,
                                                String referencia) {
        return new Auditoria(null, instante, correlacion, "LOGIN_FALLIDO", referencia, true);
    }

    private static String texto(String valor, int maximo) {
        if (valor == null || valor.isBlank() || valor.length() > maximo) {
            throw new IllegalArgumentException("Acción o referencia de auditoría inválida.");
        }
        return valor;
    }

    public Long getId() { return id; }
    public Usuario getActor() { return actor; }
    public Instant getInstante() { return instante; }
    public UUID getCorrelacion() { return correlacion; }
    public String getAccion() { return accion; }
    public String getReferencia() { return referencia; }
}
