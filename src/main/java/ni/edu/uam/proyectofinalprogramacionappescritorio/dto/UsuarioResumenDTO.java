package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

/** Proyección de solo lectura de una cuenta sin exponer passwordHash. */
public record UsuarioResumenDTO(
        Long id,
        String nombre,
        String rol,
        boolean activo,
        boolean debeCambiarPassword,
        long version) {
}
