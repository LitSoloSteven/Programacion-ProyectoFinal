package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad de dominio que representa una Unidad dentro de una Propiedad en Latch.
 */
@Entity
@Table(name = "unidad", uniqueConstraints = @UniqueConstraint(columnNames = {"propiedad_id", "codigo"}))
public class Unidad {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "propiedad_id", nullable = false)
    private UUID propiedadId;
    @Transient
    private String propiedadNombre;
    @Column(length = 30, nullable = false)
    private String codigo;
    @Column(length = 255)
    private String descripcion;
    @Column(length = 24, nullable = false)
    private String tipo;
    @Column(length = 24, nullable = false)
    private String estado;
    @Column(name = "canon_referencia", precision = 19, scale = 2, nullable = false)
    private BigDecimal canonReferencia;
    @Column(length = 3, nullable = false)
    private String moneda;
    @Version
    private long version;


    @ManyToOne(optional = false)
    @JoinColumn(name = "propiedad_id", insertable = false, updatable = false)
    private Propiedad propiedad;

    public Unidad() {
        this.tipo = "Apartamento";
        this.estado = "Disponible";
        this.moneda = "USD";
        this.canonReferencia = BigDecimal.ZERO;
        this.version = 0;
    }

    public Unidad(UUID id, UUID propiedadId, String propiedadNombre, String codigo,
                  String tipo, String estado, BigDecimal canonReferencia, String moneda, long version) {
        this.id = id;
        this.propiedadId = propiedadId;
        this.propiedadNombre = propiedadNombre;
        this.codigo = codigo;
        this.tipo = tipo;
        this.estado = estado;
        this.canonReferencia = canonReferencia;
        this.moneda = moneda;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPropiedadId() {
        return propiedadId;
    }

    public void setPropiedadId(UUID propiedadId) {
        this.propiedadId = propiedadId;
    }

    public String getPropiedadNombre() {
        return propiedad == null ? propiedadNombre : propiedad.getNombre();
    }

    public void setPropiedadNombre(String propiedadNombre) {
        this.propiedadNombre = propiedadNombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() { return descripcion; }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getCanonReferencia() {
        return canonReferencia;
    }

    public void setCanonReferencia(BigDecimal canonReferencia) {
        this.canonReferencia = canonReferencia;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getCanonFormateado() {
        if (canonReferencia == null) return "0.00 " + moneda;
        return String.format("%.2f %s", canonReferencia, moneda);
    }

    @Override
    public String toString() {
        String nombrePropiedad = getPropiedadNombre();
        return (nombrePropiedad != null ? nombrePropiedad + " — " : "") + codigo + " (" + tipo + ")";
    }
}
