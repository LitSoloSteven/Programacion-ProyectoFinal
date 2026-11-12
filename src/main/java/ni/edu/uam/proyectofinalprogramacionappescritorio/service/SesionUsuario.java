package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SesionUsuario implements AutoCloseable {
    private final Long usuarioId;
    private final String nombre;
    private final String rol;
    private static volatile SesionUsuario actual;
    private final AtomicBoolean vigente = new AtomicBoolean(true);

    SesionUsuario(Long usuarioId, String nombre, String rol) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.rol = rol;
    }
    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public boolean isVigente() { return vigente.get(); }
    public static SesionUsuario getActual() { return actual != null && actual.isVigente() ? actual : null; }
    public static void establecer(SesionUsuario sesion) { cerrarSesion(); actual = sesion; }
    public static void cerrarSesion() { SesionUsuario anterior = actual; actual = null; if (anterior != null) anterior.close(); }
    public static boolean esPropietario() {
        SesionUsuario sesion = getActual();
        return sesion != null && ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol.PROPIETARIO.equals(sesion.getRol());
    }
    @Override public void close() { vigente.set(false); }
}
