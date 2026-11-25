package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;
import java.util.List;
import java.util.Optional;

public final class UsuarioDao {
    public Optional<Usuario> buscarPorNombre(Session session, String nombre) {
        return session.createQuery("from Usuario where nombre = :nombre", Usuario.class)
                .setParameter("nombre", nombre).uniqueResultOptional();
    }

    public List<Usuario> listarTodos(Session session) {
        return session.createQuery("from Usuario order by nombre", Usuario.class)
                .getResultList();
    }

    public long contarPropietariosActivosExcluyendo(Session session, Long id) {
        return session.createQuery("""
                        select count(u) from Usuario u
                        where u.rol = :rol and u.activo = true and u.id <> :id
                        """, Long.class)
                .setParameter("rol", Rol.PROPIETARIO)
                .setParameter("id", id)
                .getSingleResult();
    }

    public void guardar(Session session, Usuario usuario) { session.persist(usuario); }
}
