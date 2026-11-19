package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Entidad de dominio que representa una Propiedad inmobiliaria en Latch.
 */
@Entity
@Table(name = "propiedad")
public class Propiedad {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    private UUID id;
    @Column(length = 120, nullable = false)
    @Getter
    @Setter
    private String nombre;
    @Column(length = 300, nullable = false)
    @Getter
    @Setter
    private String direccion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    @Getter
    @Setter
    private Propietario propietario;
    @OneToMany(mappedBy = "propiedad")
    private java.util.List<Unidad> unidades = new java.util.ArrayList<>();
    @Column(nullable = false)
    @Getter
    @Setter
    private boolean activa;
    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    @Getter
    @Setter
    private Timestamp creadoEn;
    @Version
    @Getter
    @Setter
    private long version;

    public Propiedad() {
        this.activa = true;
        this.version = 0;
    }

    public Propiedad(UUID id, String nombre, String direccion, boolean activa, Timestamp creadoEn, long version) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.activa = activa;
        this.creadoEn = creadoEn;
        this.version = version;
    }

    public String getEstadoTexto() {
        return activa ? "Activa" : "Inactiva";
    }

    @Override
    public String toString() {
        return nombre;
    }
}
