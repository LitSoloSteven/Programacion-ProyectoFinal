package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO para la persistencia de Contrato contra latch.contrato en PostgreSQL 18.
 */
public class ContratoDao {

    public List<Contrato> listarTodos() throws SQLException {
        List<Contrato> lista = new ArrayList<>();
        String sql = """
            SELECT c.id, c.unidad_id, p.nombre || ' — ' || u.codigo AS unidad_info,
                   c.inquilino_id, i.nombre AS inquilino_nombre,
                   c.inicio, c.fin, c.canon, c.moneda, c.dia_pago, c.deposito_pactado,
                   c.estado, c.version
            FROM latch.contrato c
            JOIN latch.unidad u ON c.unidad_id = u.id
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            JOIN latch.inquilino i ON c.inquilino_id = i.id
            ORDER BY c.inicio DESC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearContrato(rs));
            }
        }
        return lista;
    }

    public List<Contrato> listarVigentes() throws SQLException {
        List<Contrato> lista = new ArrayList<>();
        String sql = """
            SELECT c.id, c.unidad_id, p.nombre || ' — ' || u.codigo AS unidad_info,
                   c.inquilino_id, i.nombre AS inquilino_nombre,
                   c.inicio, c.fin, c.canon, c.moneda, c.dia_pago, c.deposito_pactado,
                   c.estado, c.version
            FROM latch.contrato c
            JOIN latch.unidad u ON c.unidad_id = u.id
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            JOIN latch.inquilino i ON c.inquilino_id = i.id
            WHERE c.estado = 'Vigente'
            ORDER BY c.inicio DESC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearContrato(rs));
            }
        }
        return lista;
    }

    public Contrato buscarPorId(UUID id) throws SQLException {
        String sql = """
            SELECT c.id, c.unidad_id, p.nombre || ' — ' || u.codigo AS unidad_info,
                   c.inquilino_id, i.nombre AS inquilino_nombre,
                   c.inicio, c.fin, c.canon, c.moneda, c.dia_pago, c.deposito_pactado,
                   c.estado, c.version
            FROM latch.contrato c
            JOIN latch.unidad u ON c.unidad_id = u.id
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            JOIN latch.inquilino i ON c.inquilino_id = i.id
            WHERE c.id = ?;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearContrato(rs);
                }
            }
        }
        return null;
    }

    public void insertar(Contrato c) throws SQLException {
        String sql = """
            INSERT INTO latch.contrato (
                id, unidad_id, inquilino_id, inicio, fin, canon, moneda, dia_pago,
                deposito_pactado, estado, creado_en, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0);
        """;

        if (c.getId() == null) {
            c.setId(UUID.randomUUID());
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, c.getId());
            ps.setObject(2, c.getUnidadId());
            ps.setObject(3, c.getInquilinoId());
            ps.setDate(4, Date.valueOf(c.getInicio()));
            ps.setDate(5, Date.valueOf(c.getFin()));
            ps.setBigDecimal(6, c.getCanon());
            ps.setString(7, c.getMoneda());
            ps.setInt(8, c.getDiaPago());
            ps.setBigDecimal(9, c.getDepositoPactado());
            ps.setString(10, c.getEstado());

            ps.executeUpdate();
        }
    }

    public void actualizar(Contrato c) throws SQLException {
        String sql = """
            UPDATE latch.contrato
            SET unidad_id = ?, inquilino_id = ?, inicio = ?, fin = ?, canon = ?,
                moneda = ?, dia_pago = ?, deposito_pactado = ?, estado = ?, version = version + 1
            WHERE id = ?;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, c.getUnidadId());
            ps.setObject(2, c.getInquilinoId());
            ps.setDate(3, Date.valueOf(c.getInicio()));
            ps.setDate(4, Date.valueOf(c.getFin()));
            ps.setBigDecimal(5, c.getCanon());
            ps.setString(6, c.getMoneda());
            ps.setInt(7, c.getDiaPago());
            ps.setBigDecimal(8, c.getDepositoPactado());
            ps.setString(9, c.getEstado());
            ps.setObject(10, c.getId());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el contrato para actualizar.");
            }
        }
    }

    public void cambiarEstado(UUID id, String estado) throws SQLException {
        String sql = "UPDATE latch.contrato SET estado = ?, version = version + 1 WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setObject(2, id);
            ps.executeUpdate();
        }
    }

    public void eliminar(UUID id) throws SQLException {
        String sql = "DELETE FROM latch.contrato WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el contrato para eliminar.");
            }
        }
    }

    private Contrato mapearContrato(ResultSet rs) throws SQLException {
        Date inicio = rs.getDate("inicio");
        Date fin = rs.getDate("fin");

        return new Contrato(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("unidad_id"),
                rs.getString("unidad_info"),
                (UUID) rs.getObject("inquilino_id"),
                rs.getString("inquilino_nombre"),
                inicio != null ? inicio.toLocalDate() : null,
                fin != null ? fin.toLocalDate() : null,
                rs.getBigDecimal("canon"),
                rs.getString("moneda"),
                rs.getInt("dia_pago"),
                rs.getBigDecimal("deposito_pactado"),
                rs.getString("estado"),
                rs.getLong("version")
        );
    }
}
