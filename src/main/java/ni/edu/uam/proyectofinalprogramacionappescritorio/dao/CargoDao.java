package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO para acceso a datos de la entidad Cargo contra latch.cargo en PostgreSQL 18.
 */
public class CargoDao {

    public List<Cargo> listarTodos() throws SQLException {
        List<Cargo> lista = new ArrayList<>();
        String sql = """
            SELECT c.id, c.contrato_id, i.nombre || ' — ' || p.nombre || ' (' || u.codigo || ')' AS contrato_info,
                   c.tipo, c.periodo, c.fecha_emision, c.vence_el, c.importe, c.concepto, c.estado, c.version
            FROM latch.cargo c
            JOIN latch.contrato ct ON c.contrato_id = ct.id
            JOIN latch.inquilino i ON ct.inquilino_id = i.id
            JOIN latch.unidad u ON ct.unidad_id = u.id
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            ORDER BY c.vence_el DESC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearCargo(rs));
            }
        }
        return lista;
    }

    public void insertar(Cargo c) throws SQLException {
        String sql = """
            INSERT INTO latch.cargo (
                id, contrato_id, tipo, periodo, fecha_emision, vence_el, importe, concepto, estado, creado_en, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0);
        """;

        if (c.getId() == null) {
            c.setId(UUID.randomUUID());
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, c.getId());
            ps.setObject(2, c.getContratoId());
            ps.setString(3, c.getTipo());
            ps.setDate(4, Date.valueOf(c.getPeriodo()));
            ps.setDate(5, Date.valueOf(c.getFechaEmision()));
            ps.setDate(6, Date.valueOf(c.getVenceEl()));
            ps.setBigDecimal(7, c.getImporte());
            ps.setString(8, c.getConcepto().trim());
            ps.setString(9, c.getEstado());

            ps.executeUpdate();
        }
    }

    public void actualizar(Cargo c) throws SQLException {
        String sql = """
            UPDATE latch.cargo
            SET contrato_id = ?, tipo = ?, periodo = ?, fecha_emision = ?, vence_el = ?,
                importe = ?, concepto = ?, estado = ?, version = version + 1
            WHERE id = ?;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, c.getContratoId());
            ps.setString(2, c.getTipo());
            ps.setDate(3, Date.valueOf(c.getPeriodo()));
            ps.setDate(4, Date.valueOf(c.getFechaEmision()));
            ps.setDate(5, Date.valueOf(c.getVenceEl()));
            ps.setBigDecimal(6, c.getImporte());
            ps.setString(7, c.getConcepto().trim());
            ps.setString(8, c.getEstado());
            ps.setObject(9, c.getId());

            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el cargo para actualizar.");
            }
        }
    }

    public void eliminar(UUID id) throws SQLException {
        String sql = "DELETE FROM latch.cargo WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el cargo para eliminar.");
            }
        }
    }

    private Cargo mapearCargo(ResultSet rs) throws SQLException {
        Date periodo = rs.getDate("periodo");
        Date fechaEmision = rs.getDate("fecha_emision");
        Date venceEl = rs.getDate("vence_el");

        return new Cargo(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("contrato_id"),
                rs.getString("contrato_info"),
                rs.getString("tipo"),
                periodo != null ? periodo.toLocalDate() : null,
                fechaEmision != null ? fechaEmision.toLocalDate() : null,
                venceEl != null ? venceEl.toLocalDate() : null,
                rs.getBigDecimal("importe"),
                rs.getString("concepto"),
                rs.getString("estado"),
                rs.getLong("version")
        );
    }
}
