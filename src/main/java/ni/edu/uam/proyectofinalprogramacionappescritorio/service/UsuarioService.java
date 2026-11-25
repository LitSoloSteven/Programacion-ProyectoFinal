package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import lombok.RequiredArgsConstructor;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ControlIntentosLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UsuarioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.UsuarioResumenDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public final class UsuarioService {
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final PasswordHasher passwords = new PasswordHasher();
    private final UsuarioDao usuarios = new UsuarioDao();

    public List<UsuarioResumenDTO> listar(SesionUsuario identidad) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            return usuarios.listarTodos(session).stream()
                    .map(u -> new UsuarioResumenDTO(
                            u.getId(), u.getNombre(), u.getRol(),
                            u.isActivo(), u.isDebeCambiarPassword(), u.getVersion()))
                    .toList();
        });
    }

    public Long crear(SesionUsuario identidad, String nombre, char[] password, String rol) {
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            Usuario nuevo = new Usuario(nombre, passwords.crear(password), rol);
            usuarios.guardar(session, nuevo);
            auditoria.registrar(session, actor, UUID.randomUUID(), "CREAR_USUARIO", "usuario:" + nuevo.getId());
            return nuevo.getId();
        });
    }

    public void activar(SesionUsuario identidad, Long id) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            Usuario usuario = session.find(Usuario.class, id);
            if (usuario == null) throw new IllegalArgumentException("El usuario no existe.");
            if (!usuario.isActivo()) {
                usuario.activar();
                auditoria.registrar(session, actor, UUID.randomUUID(), "ACTIVAR_USUARIO", "usuario:" + id);
            }
            return null;
        });
    }

    public void desactivar(SesionUsuario identidad, Long id) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            if (actor.getId().equals(id)) throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
            Usuario usuario = session.find(Usuario.class, id);
            if (usuario == null) throw new IllegalArgumentException("El usuario no existe.");
            if (Rol.PROPIETARIO.equals(usuario.getRol()) && usuario.isActivo()
                    && usuarios.contarPropietariosActivosExcluyendo(session, id) == 0) {
                throw new IllegalArgumentException("Debe permanecer al menos un Propietario activo.");
            }
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
            if (Rol.PROPIETARIO.equals(rolAnterior) && usuario.isActivo()
                    && !Rol.PROPIETARIO.equals(rol)
                    && usuarios.contarPropietariosActivosExcluyendo(session, id) == 0) {
                throw new IllegalArgumentException("Debe permanecer al menos un Propietario activo.");
            }
            usuario.cambiarRol(rol);
            if (!rolAnterior.equals(usuario.getRol())) {
                auditoria.registrar(session, actor, UUID.randomUUID(), "CAMBIAR_ROL_USUARIO",
                        "usuario:" + id + ";rol:" + rolAnterior + "->" + usuario.getRol());
            }
            return null;
        });
    }

    public void restablecerPassword(SesionUsuario identidad, Long id, char[] nuevaPassword) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            if (actor.getId().equals(id)) {
                throw new IllegalArgumentException("No puedes restablecer tu propia contraseña por esta vía.");
            }
            Usuario usuario = session.find(Usuario.class, id);
            if (usuario == null) throw new IllegalArgumentException("El usuario no existe.");
            usuario.actualizarPasswordHash(passwords.crear(nuevaPassword));
            usuario.marcarCambioPasswordPendiente();
            auditoria.registrar(session, actor, UUID.randomUUID(), "RESTABLECER_PASSWORD_USUARIO", "usuario:" + id);
            return null;
        });
    }

    public long cambiarPasswordPropia(SesionUsuario identidad, char[] passwordActual, char[] nuevaPassword) {
        if (passwordActual == null || passwordActual.length == 0) {
            throw new IllegalArgumentException("Ingresa tu contraseña actual.");
        }
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            if (!passwords.verificar(passwordActual, actor.getPasswordHash())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta.");
            }
            if (nuevaPassword != null && Arrays.equals(passwordActual, nuevaPassword)) {
                throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
            }
            actor.completarCambioPassword(passwords.crear(nuevaPassword));
            session.flush();
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "CAMBIAR_PASSWORD_PROPIA", "usuario:" + actor.getId());
            return actor.getVersion();
        });
    }

    public void desbloquearCuenta(SesionUsuario identidad, Long id, ControlIntentosLogin control) {
        if (control == null) throw new IllegalArgumentException("Falta el control de intentos.");
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.GESTIONAR_USUARIOS);
            Usuario usuario = session.find(Usuario.class, id);
            if (usuario == null) throw new IllegalArgumentException("El usuario no existe.");
            control.desbloquear(usuario.getNombre());
            auditoria.registrar(session, actor, UUID.randomUUID(), "DESBLOQUEAR_USUARIO", "usuario:" + id);
            return null;
        });
    }
}
