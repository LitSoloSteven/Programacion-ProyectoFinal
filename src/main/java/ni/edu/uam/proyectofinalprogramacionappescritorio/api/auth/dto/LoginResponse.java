package ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto;

/**
 * Respuesta de login. expiraEn es un instante ISO-8601 en UTC.
 * usuarioId y nombre permiten que la interfaz establezca su sesión local sin
 * volver a verificar la contraseña.
 */
public record LoginResponse(
        String token,
        String expiraEn,
        String rol,
        Long usuarioId,
        String nombre) { }