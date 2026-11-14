package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

/**
 * Error recibido del servicio local. Conserva el código y el estado HTTP para
 * que la interfaz pueda reaccionar sin interpretar textos.
 */
public final class ErrorClienteException extends RuntimeException {
    private final int estado;
    private final String codigo;

    public ErrorClienteException(int estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public int getEstado() { return estado; }
    public String getCodigo() { return codigo; }
    public boolean esSinConexion() { return estado == 0; }
    public boolean esNoAutenticado() { return estado == 401; }
    public boolean esNoEncontrado() { return estado == 404; }
}