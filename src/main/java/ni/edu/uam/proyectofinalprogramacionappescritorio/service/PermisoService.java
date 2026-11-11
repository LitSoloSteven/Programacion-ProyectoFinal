package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Usuario;
import org.hibernate.Session;
import java.util.Objects;
import java.util.Set;

public final class PermisoService {
    private static final Set<String> ASISTENTE = Set.of(Permiso.OPERAR_ALQUILERES, Permiso.CONSULTAR_REPORTES);

    public Usuario exigir(Session session, SesionUsuario identidad, String permiso) {
        Objects.requireNonNull(permiso);
        if (!Permiso.esValido(permiso)) throw new IllegalArgumentException("Permiso desconocido.");
        if (identidad == null || !identidad.isVigente()) throw new SecurityException("No tienes permiso para realizar esta operación.");
        Usuario actor = session.find(Usuario.class, identidad.getUsuarioId());
        // Se consulta el rol vigente en BD, no el rol de la pantalla ni el de un login anterior.
        if (actor == null || !actor.isActivo()
                || (!Rol.PROPIETARIO.equals(actor.getRol()) && !ASISTENTE.contains(permiso))) {
            throw new SecurityException("No tienes permiso para realizar esta operación.");
        }
        return actor;
    }
}
