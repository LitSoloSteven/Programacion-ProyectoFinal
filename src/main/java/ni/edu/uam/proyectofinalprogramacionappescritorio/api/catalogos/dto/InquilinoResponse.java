package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Representación de un inquilino en la API. */
public record InquilinoResponse(
        String id,
        String nombre,
        String identificacion,
        String telefono,
        String correo,
        String contactoEmergencia,
        boolean activo,
        String creadoEn,
        long version) {
}