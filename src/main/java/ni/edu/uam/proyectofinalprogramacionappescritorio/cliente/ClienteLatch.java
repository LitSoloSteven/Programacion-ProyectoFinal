package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

/**
 * Punto único de acceso del cliente HTTP al backend Latch. La aplicación JavaFX lo
 * inicializa usando ClienteConfig (o una URL explícita) y lo cierra al salir.
 */
public final class ClienteLatch {
    private static volatile ClienteApi api;

    private ClienteLatch() { }

    public static synchronized void iniciar(int puerto) {
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto del servicio local no es válido.");
        }
        iniciar("http://127.0.0.1:" + puerto);
    }

    public static synchronized void iniciar(String baseUrl) {
        api = new ClienteApi(ClienteConfig.normalizar(baseUrl));
    }

    public static String baseUrl() {
        return api().baseUrl();
    }

    public static void alExpirar(Runnable accion) {
        api().alExpirar(accion);
    }

    public static ClienteApi api() {
        ClienteApi actual = api;
        if (actual == null) {
            throw new IllegalStateException("El servicio de Latch no está inicializado.");
        }
        return actual;
    }

    public static ClienteAuth auth() {
        return new ClienteAuth(api());
    }

    public static ClienteCatalogos catalogos() {
        return new ClienteCatalogos(api());
    }

    public static ClientePagos pagos() {
        return new ClientePagos(api());
    }

    public static ClienteTasas tasas() {
        return new ClienteTasas(api());
    }

    public static ClienteMantenimiento mantenimiento() {
        return new ClienteMantenimiento(api());
    }

    public static ClienteValidaciones validaciones() {
        return new ClienteValidaciones(api());
    }

    public static ClienteAdmin admin() {
        return new ClienteAdmin(api());
    }

    public static ClienteAdjuntos adjuntos() {
        return new ClienteAdjuntos(api());
    }

    public static ClienteReportes reportes() {
        return new ClienteReportes(api());
    }

    public static synchronized void cerrar() {
        if (api != null) {
            api.limpiarToken();
        }
        SesionCliente.cerrarSesion();
        api = null;
    }
}
