package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;
import java.util.Optional;

public final class UsuarioDao {
    public Optional<Usuario> buscarPorNombre(Session session, String nombre) {
        return session.createQuery("from Usuario where nombre = :nombre", Usuario.class)
                .setParameter("nombre", nombre).uniqueResultOptional();
    }
    public void guardar(Session session, Usuario usuario) { session.persist(usuario); }
}
