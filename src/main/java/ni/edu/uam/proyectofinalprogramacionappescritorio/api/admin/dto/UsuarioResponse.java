package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String rol,
        boolean activo,
        boolean debeCambiarPassword,
        long version) { }
