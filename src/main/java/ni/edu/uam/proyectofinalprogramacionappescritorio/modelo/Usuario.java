package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import java.util.UUID;

/**
 * Entidad de dominio que representa a un usuario del sistema Latch.
 */
public class Usuario {
    private UUID id;
    private String rolNombre;
    private String nombreUsuario;
    private String nombreVisible;
    private boolean activo;

    public Usuario() {
    }

    public Usuario(UUID id, String rolNombre, String nombreUsuario, String nombreVisible, boolean activo) {
        this.id = id;
        this.rolNombre = rolNombre;
        this.nombreUsuario = nombreUsuario;
        this.nombreVisible = nombreVisible;
        this.activo = activo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public void setRolNombre(String rolNombre) {
        this.rolNombre = rolNombre;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public void setNombreVisible(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean esPropietario() {
        return "Propietario".equalsIgnoreCase(rolNombre);
    }

    @Override
    public String toString() {
        return nombreVisible != null ? nombreVisible : nombreUsuario;
    }
}
