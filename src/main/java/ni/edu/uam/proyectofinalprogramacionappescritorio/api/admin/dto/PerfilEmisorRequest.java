package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record PerfilEmisorRequest(
        String nombre,
        String identificacion,
        String direccion,
        String telefono,
        String correo,
        String motivo) { }
