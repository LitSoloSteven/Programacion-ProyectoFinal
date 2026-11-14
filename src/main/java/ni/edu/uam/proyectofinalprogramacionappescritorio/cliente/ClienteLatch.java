package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

/**
 * Punto único de acceso del cliente al servicio local. La aplicación JavaFX lo
 * inicia después de levantar el servidor y lo cierra al salir.
 */
public final class ClienteLatch {
    private static volatile ClienteApi api;

    private ClienteLatch() { }

    public static synchronized void iniciar(int puerto) {
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto del servicio local no es válido.");
        }
        api = new ClienteApi("http://127.0.0.1:" + puerto);
    }

    public static ClienteApi api() {
        ClienteApi actual = api;
        if (actual == null) {
            throw new IllegalStateException("El servicio local de Latch no está iniciado.");
        }
        return actual;
    }

    public static synchronized void cerrar() {
        if (api != null) api.limpiarToken();
        api = null;
    }
}