package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO para acceso a datos de la entidad Propiedad contra PostgreSQL 18.
 */
public class PropiedadDao {

    public List<Propiedad> listarTodas() throws SQLException {
        List<Propiedad> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, direccion, activa, creado_en, version FROM latch.propiedad ORDER BY nombre ASC;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearPropiedad(rs));
            }
        }
        return lista;
    }

    public List<Propiedad> listarActivas() throws SQLException {
        List<Propiedad> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, direccion, activa, creado_en, version FROM latch.propiedad WHERE activa = TRUE ORDER BY nombre ASC;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearPropiedad(rs));
            }
        }
        return lista;
    }

    public Propiedad buscarPorId(UUID id) throws SQLException {
        String sql = "SELECT id, nombre, direccion, activa, creado_en, version FROM latch.propiedad WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearPropiedad(rs);
                }
            }
        }
        return null;
    }

    public void insertar(Propiedad p) throws SQLException {
        String sql = """
            INSERT INTO latch.propiedad (id, nombre, direccion, activa, creado_en, version)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 0);
        """;

        if (p.getId() == null) {
            p.setId(UUID.randomUUID());
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, p.getId());
            ps.setString(2, p.getNombre().trim());
            ps.setString(3, p.getDireccion().trim());
            ps.setBoolean(4, p.isActiva());

            ps.executeUpdate();
        }
    }

    public void actualizar(Propiedad p) throws SQLException {
        String sql = """
            UPDATE latch.propiedad
            SET nombre = ?, direccion = ?, activa = ?, version = version + 1
            WHERE id = ?;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNombre().trim());
            ps.setString(2, p.getDireccion().trim());
            ps.setBoolean(3, p.isActiva());
            ps.setObject(4, p.getId());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró la propiedad especificada para actualizar.");
            }
        }
    }

    public void eliminar(UUID id) throws SQLException {
        String sql = "DELETE FROM latch.propiedad WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró la propiedad para eliminar.");
            }
        }
    }

    public void cambiarEstado(UUID id, boolean activa) throws SQLException {
        String sql = "UPDATE latch.propiedad SET activa = ?, version = version + 1 WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activa);
            ps.setObject(2, id);
            ps.executeUpdate();
        }
    }

    private Propiedad mapearPropiedad(ResultSet rs) throws SQLException {
        return new Propiedad(
                (UUID) rs.getObject("id"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                rs.getBoolean("activa"),
                rs.getTimestamp("creado_en"),
                rs.getLong("version")
        );
    }
}
