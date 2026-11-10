package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Consulta métricas y totales reales directamente desde PostgreSQL para el Dashboard.
 */
public class DashboardDao {

    public Map<String, Object> obtenerMetricas() throws SQLException {
        Map<String, Object> metricas = new HashMap<>();

        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement()) {

            // Propiedades activas
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM latch.propiedad WHERE activa = TRUE;")) {
                if (rs.next()) metricas.put("totalPropiedades", rs.getInt(1));
            }

            // Unidades registradas
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM latch.unidad;")) {
                if (rs.next()) metricas.put("totalUnidades", rs.getInt(1));
            }

            // Inquilinos activos
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM latch.inquilino WHERE activo = TRUE;")) {
                if (rs.next()) metricas.put("totalInquilinos", rs.getInt(1));
            }

            // Contratos vigentes
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM latch.contrato WHERE estado = 'Vigente';")) {
                if (rs.next()) metricas.put("totalContratosVigentes", rs.getInt(1));
            }

            // Cargos pendientes
            try (ResultSet rs = stmt.executeQuery("SELECT count(*), coalesce(sum(importe), 0) FROM latch.cargo WHERE estado = 'Pendiente';")) {
                if (rs.next()) {
                    metricas.put("cargosPendientesCant", rs.getInt(1));
                    metricas.put("cargosPendientesMonto", rs.getBigDecimal(2));
                }
            }

            // Pagos registrados
            try (ResultSet rs = stmt.executeQuery("SELECT count(*), coalesce(sum(monto_contrato), 0) FROM latch.pago WHERE estado = 'Registrado';")) {
                if (rs.next()) {
                    metricas.put("pagosRegistradosCant", rs.getInt(1));
                    metricas.put("pagosRegistradosMonto", rs.getBigDecimal(2));
                }
            }
        }

        metricas.putIfAbsent("totalPropiedades", 0);
        metricas.putIfAbsent("totalUnidades", 0);
        metricas.putIfAbsent("totalInquilinos", 0);
        metricas.putIfAbsent("totalContratosVigentes", 0);
        metricas.putIfAbsent("cargosPendientesCant", 0);
        metricas.putIfAbsent("cargosPendientesMonto", BigDecimal.ZERO);
        metricas.putIfAbsent("pagosRegistradosCant", 0);
        metricas.putIfAbsent("pagosRegistradosMonto", BigDecimal.ZERO);

        return metricas;
    }
}
