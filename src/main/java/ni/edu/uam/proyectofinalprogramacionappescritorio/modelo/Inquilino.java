package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

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
    private UUID id;
    @Column(length = 160, nullable = false)
    private String nombre;
    @Column(length = 40, unique = true)
    private String identificacion;
    @Column(length = 30)
    private String telefono;
    @Column(length = 254)
    private String correo;
    @Column(name = "contacto_emergencia", length = 200)
    private String contactoEmergencia;
    @Column(nullable = false)
    private boolean activo;
    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    private Timestamp creadoEn;
    @Version
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

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContactoEmergencia() {
        return contactoEmergencia;
    }

    public void setContactoEmergencia(String contactoEmergencia) {
        this.contactoEmergencia = contactoEmergencia;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getEstadoTexto() {
        return activo ? "Activo" : "Inactivo";
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

    public boolean esCorreoValido() {
        if (correo == null || correo.trim().isEmpty()) return true;
        return CORREO_PATTERN.matcher(correo.trim()).matches();
    }

    @Override
    public String toString() {
        return nombre + (identificacion != null ? " (" + identificacion + ")" : "");
    }
}
