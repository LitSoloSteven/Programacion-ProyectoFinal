package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

/**
 * Mantiene la sesión del usuario autenticado en la aplicación.
 */
public class SesionUsuario {

    private static Usuario usuarioActual = null;

    private SesionUsuario() {
    }

    public static void setUsuarioActual(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static boolean haySesionActiva() {
        return usuarioActual != null;
    }

    public static boolean esPropietario() {
        return usuarioActual != null && usuarioActual.esPropietario();
    }

    public static void cerrarSesion() {
        usuarioActual = null;
    }
}
