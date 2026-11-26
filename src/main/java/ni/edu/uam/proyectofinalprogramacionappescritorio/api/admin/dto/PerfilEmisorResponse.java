package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record PerfilEmisorResponse(
        boolean configurado,
        String nombre,
        String identificacion,
        String direccion,
        String telefono,
        String correo,
        String resumenVisible) { }
