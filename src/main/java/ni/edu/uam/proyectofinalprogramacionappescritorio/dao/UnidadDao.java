package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO para acceso a datos de la entidad Unidad contra PostgreSQL 18.
 */
public class UnidadDao {

    public List<Unidad> listarTodas() throws SQLException {
        List<Unidad> lista = new ArrayList<>();
        String sql = """
            SELECT u.id, u.propiedad_id, p.nombre AS propiedad_nombre, u.codigo, u.tipo,
                   u.estado, u.canon_referencia, u.moneda, u.version
            FROM latch.unidad u
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            ORDER BY p.nombre ASC, u.codigo ASC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearUnidad(rs));
            }
        }
        return lista;
    }

    public List<Unidad> listarDisponibles() throws SQLException {
        List<Unidad> lista = new ArrayList<>();
        String sql = """
            SELECT u.id, u.propiedad_id, p.nombre AS propiedad_nombre, u.codigo, u.tipo,
                   u.estado, u.canon_referencia, u.moneda, u.version
            FROM latch.unidad u
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            WHERE u.estado = 'Disponible' AND p.activa = TRUE
            ORDER BY p.nombre ASC, u.codigo ASC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearUnidad(rs));
            }
        }
        return lista;
    }

    public void insertar(Unidad u) throws SQLException {
        String sql = """
            INSERT INTO latch.unidad (id, propiedad_id, codigo, tipo, estado, canon_referencia, moneda, creado_en, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0);
        """;

        if (u.getId() == null) {
            u.setId(UUID.randomUUID());
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, u.getId());
            ps.setObject(2, u.getPropiedadId());
            ps.setString(3, u.getCodigo().trim());
            ps.setString(4, u.getTipo());
            ps.setString(5, u.getEstado());
            ps.setBigDecimal(6, u.getCanonReferencia());
            ps.setString(7, u.getMoneda());

            ps.executeUpdate();
        }
    }

    public void actualizar(Unidad u) throws SQLException {
        String sql = """
            UPDATE latch.unidad
            SET propiedad_id = ?, codigo = ?, tipo = ?, estado = ?, canon_referencia = ?, moneda = ?, version = version + 1
            WHERE id = ?;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, u.getPropiedadId());
            ps.setString(2, u.getCodigo().trim());
            ps.setString(3, u.getTipo());
            ps.setString(4, u.getEstado());
            ps.setBigDecimal(5, u.getCanonReferencia());
            ps.setString(6, u.getMoneda());
            ps.setObject(7, u.getId());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró la unidad para actualizar.");
            }
        }
    }

    public void eliminar(UUID id) throws SQLException {
        String sql = "DELETE FROM latch.unidad WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró la unidad para eliminar.");
            }
        }
    }

    private Unidad mapearUnidad(ResultSet rs) throws SQLException {
        return new Unidad(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("propiedad_id"),
                rs.getString("propiedad_nombre"),
                rs.getString("codigo"),
                rs.getString("tipo"),
                rs.getString("estado"),
                rs.getBigDecimal("canon_referencia"),
                rs.getString("moneda"),
                rs.getLong("version")
        );
    }
}
