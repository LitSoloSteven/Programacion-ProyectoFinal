package ni.edu.uam.proyectofinalprogramacionappescritorio.entity;

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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
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
        this.actor = Objects.requireNonNull(actor);
        this.instante = Instant.now();
        this.correlacion = Objects.requireNonNull(correlacion);
        this.accion = texto(accion, 80);
        this.referencia = texto(referencia, 160);
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
