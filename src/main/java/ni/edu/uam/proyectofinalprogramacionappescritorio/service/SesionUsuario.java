package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.entity.Rol;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SesionUsuario implements AutoCloseable {
    private final Long usuarioId;
    private final String nombre;
    private final String rol;
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
    @Override public void close() { vigente.set(false); }
}
