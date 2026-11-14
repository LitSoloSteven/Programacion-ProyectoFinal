package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Cuerpo para crear o reemplazar un inquilino. */
public record InquilinoRequest(
        String nombre,
        String identificacion,
        String telefono,
        String correo,
        String contactoEmergencia,
        Boolean activo,
        Long version) {
}