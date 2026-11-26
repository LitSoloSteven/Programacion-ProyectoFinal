package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/**
 * Cuerpo para crear o reemplazar una propiedad. Los datos del propietario son
 * obligatorios al crear; al actualizar pueden omitirse y se reutilizan los del
 * usuario autenticado.
 */
public record PropiedadRequest(
        String nombre,
        String direccion,
        Boolean activa,
        String propietarioNombre,
        String propietarioCorreo,
        String propietarioTelefono,
        Long version) {
}