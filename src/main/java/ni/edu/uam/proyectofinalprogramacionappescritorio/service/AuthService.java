package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UsuarioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;

import java.sql.SQLException;

/**
 * Servicio de negocio para autenticación de usuarios.
 */
public class AuthService {

    private final UsuarioDao usuarioDao;

    public AuthService() {
        this.usuarioDao = new UsuarioDao();
    }

    public AuthService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    /**
     * Autentica credenciales del usuario con validaciones de negocio.
     *
     * @param nombreUsuario Nombre de usuario
     * @param password Contraseña
     * @return Usuario autenticado
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException si ocurre un problema de base de datos
     */
    public Usuario iniciarSesion(String nombreUsuario, String password) throws SQLException {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar su nombre de usuario.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar su contraseña.");
        }

        Usuario usuario = usuarioDao.autenticar(nombreUsuario.trim(), password.trim());
        if (usuario == null) {
            throw new IllegalArgumentException("Credenciales incorrectas o usuario inactivo.");
        }

        SesionUsuario.setUsuarioActual(usuario);
        return usuario;
    }

    public void cerrarSesion() {
        SesionUsuario.cerrarSesion();
    }
}
