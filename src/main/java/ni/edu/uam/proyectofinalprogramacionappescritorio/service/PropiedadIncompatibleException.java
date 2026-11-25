package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

/** Error de negocio cuando la solicitud referenciada pertenece a otra propiedad (ERR-065). */
public class PropiedadIncompatibleException extends IllegalArgumentException {
    public static final String CODIGO = "ERR-065";

    public PropiedadIncompatibleException(String mensaje) {
        super(mensaje);
    }
}
