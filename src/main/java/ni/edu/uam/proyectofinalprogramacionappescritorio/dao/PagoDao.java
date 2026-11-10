package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DAO para acceso a datos de la entidad Pago contra latch.pago en PostgreSQL 18.
 */
public class PagoDao {

    public List<Pago> listarTodos() throws SQLException {
        List<Pago> lista = new ArrayList<>();
        String sql = """
            SELECT pg.id, pg.contrato_id, i.nombre || ' — ' || p.nombre || ' (' || u.codigo || ')' AS contrato_info,
                   pg.fecha_pago, pg.moneda_original, pg.monto_original, pg.monto_contrato,
                   pg.metodo, pg.referencia, pg.notas, pg.estado
            FROM latch.pago pg
            JOIN latch.contrato ct ON pg.contrato_id = ct.id
            JOIN latch.inquilino i ON ct.inquilino_id = i.id
            JOIN latch.unidad u ON ct.unidad_id = u.id
            JOIN latch.propiedad p ON u.propiedad_id = p.id
            ORDER BY pg.fecha_pago DESC;
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearPago(rs));
            }
        }
        return lista;
    }

    public void insertar(Pago p) throws SQLException {
        String sql = """
            INSERT INTO latch.pago (
                id, contrato_id, fecha_pago, moneda_original, monto_original, monto_contrato,
                metodo, referencia, notas, estado, creado_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);
        """;

        if (p.getId() == null) {
            p.setId(UUID.randomUUID());
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, p.getId());
            ps.setObject(2, p.getContratoId());
            ps.setDate(3, Date.valueOf(p.getFechaPago()));
            ps.setString(4, p.getMonedaOriginal());
            ps.setBigDecimal(5, p.getMontoOriginal());
            ps.setBigDecimal(6, p.getMontoContrato());
            ps.setString(7, p.getMetodo());

            if (p.getReferencia() != null && !p.getReferencia().isBlank()) {
                ps.setString(8, p.getReferencia().trim());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            if (p.getNotas() != null && !p.getNotas().isBlank()) {
                ps.setString(9, p.getNotas().trim());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }

            ps.setString(10, p.getEstado());

            ps.executeUpdate();
        }
    }

    public void anular(UUID id) throws SQLException {
        String sql = "UPDATE latch.pago SET estado = 'Anulado' WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    public void eliminar(UUID id) throws SQLException {
        String sql = "DELETE FROM latch.pago WHERE id = ?;";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("No se encontró el registro de pago para eliminar.");
            }
        }
    }

    private Pago mapearPago(ResultSet rs) throws SQLException {
        Date fechaPago = rs.getDate("fecha_pago");

        return new Pago(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("contrato_id"),
                rs.getString("contrato_info"),
                fechaPago != null ? fechaPago.toLocalDate() : null,
                rs.getString("moneda_original"),
                rs.getBigDecimal("monto_original"),
                rs.getBigDecimal("monto_contrato"),
                rs.getString("metodo"),
                rs.getString("referencia"),
                rs.getString("notas"),
                rs.getString("estado")
        );
    }
}
