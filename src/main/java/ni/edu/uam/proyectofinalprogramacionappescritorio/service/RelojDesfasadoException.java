package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

/** Se lanza cuando el reloj del equipo se separa demasiado del reloj de la BD. */
public final class RelojDesfasadoException extends RuntimeException {
    public static final String MENSAJE = "La fecha del equipo difiere de la base de datos. "
            + "Revise el reloj antes de registrar operaciones.";

    public RelojDesfasadoException() {
        super(MENSAJE);
    }
}
