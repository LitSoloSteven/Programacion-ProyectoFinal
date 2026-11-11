package ni.edu.uam.proyectofinalprogramacionappescritorio.modelos;

public final class Rol {
    public static final String PROPIETARIO = "PROPIETARIO";
    public static final String ASISTENTE = "ASISTENTE";

    private Rol() {}

    public static boolean esValido(String rol) {
        return PROPIETARIO.equals(rol) || ASISTENTE.equals(rol);
    }
}
