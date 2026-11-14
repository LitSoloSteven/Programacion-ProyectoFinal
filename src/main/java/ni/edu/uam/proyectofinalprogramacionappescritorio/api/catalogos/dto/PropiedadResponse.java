package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

/** Representación de una propiedad en la API. Los importes y fechas son texto. */
public record PropiedadResponse(
        String id,
        String nombre,
        String direccion,
        boolean activa,
        String propietarioNombre,
        String propietarioCorreo,
        String propietarioTelefono,
        String creadoEn,
        long version) {
}