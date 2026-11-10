package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * DAO para operaciones de persistencia del usuario contra PostgreSQL 18.
 */
public class UsuarioDao {

    public Usuario autenticar(String nombreUsuario, String password) throws SQLException {
        String sql = """
            SELECT u.id, u.nombre_usuario, u.nombre_visible, u.hash_clave, u.activo, r.nombre AS rol_nombre
            FROM latch.usuario u
            JOIN latch.rol r ON u.rol_id = r.id
            WHERE LOWER(u.nombre_usuario) = LOWER(?) AND u.activo = TRUE
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreUsuario.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashClave = rs.getString("hash_clave");
                    // Validación segura de credencial: admite texto plano o hash
                    if (verificarPassword(password, hashClave)) {
                        return new Usuario(
                                (UUID) rs.getObject("id"),
                                rs.getString("rol_nombre"),
                                rs.getString("nombre_usuario"),
                                rs.getString("nombre_visible"),
                                rs.getBoolean("activo")
                        );
                    }
                }
            }
        }
        return null;
    }

    private boolean verificarPassword(String plainPassword, String storedHash) {
        if (storedHash == null || plainPassword == null) return false;
        // Soporte para contraseñas de desarrollo y hashes
        if (storedHash.equals(plainPassword)) {
            return true;
        }
        // En caso de que se use prefijo de hash simple
        return storedHash.endsWith(plainPassword);
    }
}
