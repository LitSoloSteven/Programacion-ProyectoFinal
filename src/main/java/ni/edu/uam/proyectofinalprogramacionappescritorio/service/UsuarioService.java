package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import lombok.RequiredArgsConstructor;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UsuarioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import java.util.UUID;

@RequiredArgsConstructor
public final class UsuarioService {
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final PasswordHasher passwords = new PasswordHasher();
    private final UsuarioDao usuarios = new UsuarioDao();

    public Long crear(SesionUsuario identidad, String nombre, char[] password, String rol) {
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            Usuario nuevo = new Usuario(nombre, passwords.crear(password), rol);
            usuarios.guardar(session, nuevo);
            auditoria.registrar(session, actor, UUID.randomUUID(), "CREAR_USUARIO", "usuario:" + nuevo.getId());
            return nuevo.getId();
        });
    }

    public void desactivar(SesionUsuario identidad, Long id) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            if (actor.getId().equals(id)) throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
            Usuario usuario = session.find(Usuario.class, id);
            if (usuario == null) throw new IllegalArgumentException("El usuario no existe.");
            usuario.desactivar();
            auditoria.registrar(session, actor, UUID.randomUUID(), "DESACTIVAR_USUARIO", "usuario:" + id);
            return null;
        });
    }

    public void cambiarRol(SesionUsuario identidad, Long id, String rol) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            if (actor.getId().equals(id)) {
                throw new IllegalArgumentException("No puedes cambiar tu propio rol.");
            }
            Usuario usuario = session.find(Usuario.class, id);
            if (usuario == null) throw new IllegalArgumentException("El usuario no existe.");
            String rolAnterior = usuario.getRol();
            usuario.cambiarRol(rol);
            if (!rolAnterior.equals(usuario.getRol())) {
                auditoria.registrar(session, actor, UUID.randomUUID(), "CAMBIAR_ROL_USUARIO",
                        "usuario:" + id + ";rol:" + rolAnterior + "->" + usuario.getRol());
            }
            return null;
        });
    }
}
