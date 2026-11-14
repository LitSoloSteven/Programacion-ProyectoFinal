package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

/**
 * Error controlado de la capa HTTP. No reemplaza las excepciones de negocio
 * existentes: solo traduce una respuesta concreta a un código y un mensaje
 * seguro para el cliente.
 */
public final class ExcepcionApi extends RuntimeException {
    private final int estado;
    private final String codigo;

    public ExcepcionApi(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() { return estado; }
    public String getCodigo() { return codigo; }

    public static ExcepcionApi solicitudInvalida(String mensaje) {
        return new ExcepcionApi(400, "SOLICITUD_INVALIDA", mensaje);
    }

    public static ExcepcionApi jsonInvalido() {
        return new ExcepcionApi(400, "JSON_INVALIDO", "El cuerpo JSON no tiene un formato válido.");
    }

    public static ExcepcionApi contenidoNoSoportado() {
        return new ExcepcionApi(415, "CONTENIDO_NO_SOPORTADO",
                "El Content-Type debe ser application/json.");
    }

    public static ExcepcionApi cuerpoDemasiadoGrande() {
        return new ExcepcionApi(413, "CUERPO_DEMASIADO_GRANDE",
                "El cuerpo supera el límite de 256 KiB.");
    }

    public static ExcepcionApi noAutorizado() {
        return new ExcepcionApi(401, "NO_AUTENTICADO",
                "Debes iniciar sesión y enviar un token Bearer válido.");
    }

    public static ExcepcionApi credencialesInvalidas() {
        return new ExcepcionApi(401, "CREDENCIALES_INVALIDAS",
                "Usuario o contraseña incorrectos.");
    }

    public static ExcepcionApi demasiadosIntentos() {
        return new ExcepcionApi(429, "DEMASIADOS_INTENTOS",
                "Demasiados intentos fallidos. Espera unos minutos antes de reintentar.");
    }

    public static ExcepcionApi sinPermiso() {
        return new ExcepcionApi(403, "SIN_PERMISO",
                "No tienes permiso para realizar esta operación.");
    }

    public static ExcepcionApi noEncontrado() {
        return new ExcepcionApi(404, "NO_ENCONTRADO", "El recurso solicitado no existe.");
    }

    public static ExcepcionApi conflicto(String mensaje) {
        return new ExcepcionApi(409, "CONFLICTO", mensaje);
    }

    public static ExcepcionApi entidadNoProcesable(String mensaje) {
        return new ExcepcionApi(422, "ENTIDAD_NO_PROCESABLE", mensaje);
    }

    public static ExcepcionApi interno() {
        return new ExcepcionApi(500, "ERROR_INTERNO",
                "No se pudo completar la operación. Inténtalo nuevamente.");
    }
}