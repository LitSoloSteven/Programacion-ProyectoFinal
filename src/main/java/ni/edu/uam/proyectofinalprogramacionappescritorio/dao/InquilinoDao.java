package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO para la persistencia de Inquilino contra la tabla latch.inquilino en PostgreSQL 18.
 */
public class InquilinoDao {

    public List<Inquilino> listarTodos() throws SQLException {
        List<Inquilino> lista = new ArrayList<>();
        String sql = """
            SELECT id, nombre, identificacion, telefono, correo, contacto_emergencia, activo, creado_en, version
            FROM latch.inquilino
            ORDER BY nombre ASC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearInquilino(rs));
            }
        }
        return lista;
    }

    public List<Inquilino> listarActivos() throws SQLException {
        List<Inquilino> lista = new ArrayList<>();
        String sql = """
            SELECT id, nombre, identificacion, telefono, correo, contacto_emergencia, activo, creado_en, version
            FROM latch.inquilino
            WHERE activo = TRUE
            ORDER BY nombre ASC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearInquilino(rs));
            }
        }
        return lista;
    }

    public Inquilino buscarPorId(UUID id) throws SQLException {
        String sql = "SELECT id, nombre, identificacion, telefono, correo, contacto_emergencia, activo, creado_en, version FROM latch.inquilino WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearInquilino(rs);
                }
            }
        }
        return null;
    }

    public void insertar(Inquilino inq) throws SQLException {
        String sql = """
            INSERT INTO latch.inquilino (
                id, nombre, identificacion, telefono, correo, contacto_emergencia, activo, creado_en, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0);
        """;

        if (inq.getId() == null) {
            inq.setId(UUID.randomUUID());
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, inq.getId());
            ps.setString(2, inq.getNombre().trim());
            setNullableString(ps, 3, inq.getIdentificacion());
            setNullableString(ps, 4, inq.getTelefono());
            setNullableString(ps, 5, inq.getCorreo());
            setNullableString(ps, 6, inq.getContactoEmergencia());
            ps.setBoolean(7, inq.isActivo());

            ps.executeUpdate();
        }
    }

    public void actualizar(Inquilino inq) throws SQLException {
        String sql = """
            UPDATE latch.inquilino
            SET nombre = ?, identificacion = ?, telefono = ?, correo = ?, contacto_emergencia = ?, activo = ?, version = version + 1
            WHERE id = ?;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, inq.getNombre().trim());
            setNullableString(ps, 2, inq.getIdentificacion());
            setNullableString(ps, 3, inq.getTelefono());
            setNullableString(ps, 4, inq.getCorreo());
            setNullableString(ps, 5, inq.getContactoEmergencia());
            ps.setBoolean(6, inq.isActivo());
            ps.setObject(7, inq.getId());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el inquilino para actualizar.");
            }
        }
    }

    public void darDeBaja(UUID id) throws SQLException {
        String sql = "UPDATE latch.inquilino SET activo = FALSE, version = version + 1 WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    public void eliminar(UUID id) throws SQLException {
        String sql = "DELETE FROM latch.inquilino WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el inquilino para eliminar.");
            }
        }
    }

    private void setNullableString(PreparedStatement ps, int paramIndex, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            ps.setNull(paramIndex, Types.VARCHAR);
        } else {
            ps.setString(paramIndex, value.trim());
        }
    }

    private Inquilino mapearInquilino(ResultSet rs) throws SQLException {
        return new Inquilino(
                (UUID) rs.getObject("id"),
                rs.getString("nombre"),
                rs.getString("identificacion"),
                rs.getString("telefono"),
                rs.getString("correo"),
                rs.getString("contacto_emergencia"),
                rs.getBoolean("activo"),
                rs.getTimestamp("creado_en"),
                rs.getLong("version")
        );
    }
}
