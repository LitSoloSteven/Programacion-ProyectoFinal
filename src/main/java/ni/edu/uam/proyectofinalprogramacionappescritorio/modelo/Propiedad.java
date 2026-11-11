package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

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
    private UUID id;
    @Column(length = 120, nullable = false)
    private String nombre;
    @Column(length = 300, nullable = false)
    private String direccion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    private Propietario propietario;
    @OneToMany(mappedBy = "propiedad")
    private java.util.List<Unidad> unidades = new java.util.ArrayList<>();
    @Column(nullable = false)
    private boolean activa;
    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    private Timestamp creadoEn;
    @Version
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Propietario getPropietario() {
        return propietario;
    }

    public void setPropietario(Propietario propietario) {
        this.propietario = propietario;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public String getEstadoTexto() {
        return activa ? "Activa" : "Inactiva";
    }

    public Timestamp getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Timestamp creadoEn) {
        this.creadoEn = creadoEn;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
