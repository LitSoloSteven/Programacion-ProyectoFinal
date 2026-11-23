package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Entidad de dominio que representa a un Inquilino / Arrendatario en Latch.
 */
@Entity
@Table(name = "inquilino")
public class Inquilino {

    private static final Pattern CORREO_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    private UUID id;
    @Column(length = 160, nullable = false)
    @Getter
    @Setter
    private String nombre;
    @Column(length = 40, unique = true)
    @Getter
    @Setter
    private String identificacion;
    @Column(length = 30)
    @Getter
    @Setter
    private String telefono;
    @Column(length = 254)
    @Getter
    @Setter
    private String correo;
    @Column(name = "contacto_emergencia", length = 200)
    @Getter
    @Setter
    private String contactoEmergencia;
    @Column(nullable = false)
    @Getter
    @Setter
    private boolean activo;
    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    @Getter
    @Setter
    private Timestamp creadoEn;
    @Version
    @Getter
    @Setter
    private long version;

    public Inquilino() {
        this.activo = true;
        this.version = 0;
    }

    public Inquilino(UUID id, String nombre, String identificacion, String telefono,
                     String correo, String contactoEmergencia, boolean activo,
                     Timestamp creadoEn, long version) {
        this.id = id;
        this.nombre = nombre;
        this.identificacion = identificacion;
        this.telefono = telefono;
        this.correo = correo;
        this.contactoEmergencia = contactoEmergencia;
        this.activo = activo;
        this.creadoEn = creadoEn;
        this.version = version;
    }

    public String getEstadoTexto() {
        return activo ? "Activo" : "Inactivo";
    }

    public boolean esCorreoValido() {
        if (correo == null || correo.trim().isEmpty()) return true;
        return CORREO_PATTERN.matcher(correo.trim()).matches();
    }

    @Override
    public String toString() {
        return nombre + (identificacion != null ? " (" + identificacion + ")" : "");
    }
}
