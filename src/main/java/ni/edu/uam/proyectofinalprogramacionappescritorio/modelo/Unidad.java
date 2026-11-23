package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.Getter;
import lombok.Setter;

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
    @Getter
    @Setter
    private UUID id;
    @Column(name = "propiedad_id", nullable = false)
    @Getter
    @Setter
    private UUID propiedadId;
    @Transient
    @Setter
    private String propiedadNombre;
    @Column(length = 30, nullable = false)
    @Getter
    @Setter
    private String codigo;
    @Column(length = 255)
    @Getter
    @Setter
    private String descripcion;
    @Column(length = 24, nullable = false)
    @Getter
    @Setter
    private String tipo;
    @Column(length = 24, nullable = false)
    @Getter
    @Setter
    private String estado;
    @Column(name = "canon_referencia", precision = 19, scale = 2, nullable = false)
    @Getter
    @Setter
    private BigDecimal canonReferencia;
    @Column(length = 3, nullable = false)
    @Getter
    @Setter
    private String moneda;
    @Version
    @Getter
    @Setter
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

    public void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
        this.propiedadId = propiedad == null ? null : propiedad.getId();
    }

    public String getPropiedadNombre() {
        return propiedad == null ? propiedadNombre : propiedad.getNombre();
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
