package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Motor H2 para pruebas reproducibles de respaldo y restauración sin requerir PostgreSQL externo.
 */
public final class MotorH2Respaldo implements MotorBaseRespaldo {
    private final ConexionJdbc origen;
    private final IntSupplier sesionesActivasProveedor;
    private volatile boolean simularFalloRestauracion;

    public MotorH2Respaldo(ConexionJdbc origen) {
        this(origen, () -> 0);
    }

    public MotorH2Respaldo(ConexionJdbc origen, IntSupplier sesionesActivasProveedor) {
        this.origen = MotorBaseRespaldo.validarNoNula(origen);
        this.sesionesActivasProveedor = Objects.requireNonNull(sesionesActivasProveedor);
    }

    public void setSimularFalloRestauracion(boolean simularFalloRestauracion) {
        this.simularFalloRestauracion = simularFalloRestauracion;
    }

    @Override
    public String identificadorMotor() {
        return "h2";
    }

    @Override
    public void exportarBase(Path archivoDump) {
        exportarBaseDestino(origen, archivoDump);
    }

    @Override
    public void exportarBaseDestino(ConexionJdbc destino, Path archivoDump) {
        MotorBaseRespaldo.validarNoNula(destino);
        Objects.requireNonNull(archivoDump, "Falta la ruta del dump H2.");
        try {
            if (archivoDump.getParent() != null) {
                Files.createDirectories(archivoDump.getParent());
            }
            String rutaSql = escaparRutaSql(archivoDump);
            try (Connection conn = abrirConexion(destino);
                 Statement st = conn.createStatement()) {
                st.execute("SCRIPT TO '" + rutaSql + "'");
            }
        } catch (Exception e) {
            throw RespaldoException.operativo("ERR-055: No se pudo exportar la base H2 de prueba.", e);
        }
    }

    @Override
    public void restaurarEnBaseAislada(Path archivoDump, ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        Objects.requireNonNull(archivoDump, "Falta el archivo dump H2.");
        if (simularFalloRestauracion) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: Fallo simulado al restaurar la base de destino.",
                    null);
        }
        String rutaSql = escaparRutaSql(archivoDump);
        try (Connection conn = abrirConexion(destino);
             Statement st = conn.createStatement()) {
            st.execute("DROP ALL OBJECTS");
            st.execute("RUNSCRIPT FROM '" + rutaSql + "'");
        } catch (SQLException e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudo restaurar el script H2 en la base destino.", e);
        }
    }

    @Override
    public EstadoRecibosBase inspeccionarRecibos(ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        try (Connection conn = abrirConexion(destino)) {
            Set<String> tablas = listarTablasExistentes(conn);
            long ultimoRecibo = 0L;
            if (tablas.contains("recibo")) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(numero), 0) FROM recibo")) {
                    if (rs.next()) ultimoRecibo = rs.getLong(1);
                }
            }
            long siguiente = 1L;
            if (tablas.contains("consecutivo_recibo")) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(siguiente_numero), 1) FROM consecutivo_recibo")) {
                    if (rs.next()) siguiente = Math.max(1L, rs.getLong(1));
                }
            }
            return new EstadoRecibosBase(ultimoRecibo, siguiente);
        } catch (SQLException e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudo inspeccionar el estado de recibos en H2.", e);
        }
    }

    @Override
    public Map<String, Long> contarTablas(ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        try (Connection conn = abrirConexion(destino)) {
            Set<String> existentes = listarTablasExistentes(conn);
            Map<String, Long> conteos = new LinkedHashMap<>();
            for (String tabla : TABLAS_VERIFICADAS) {
                if (!existentes.contains(tabla)) {
                    conteos.put(tabla, 0L);
                    continue;
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                    rs.next();
                    conteos.put(tabla, rs.getLong(1));
                }
            }
            return Map.copyOf(conteos);
        } catch (SQLException e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudieron contar las tablas en H2.", e);
        }
    }

    @Override
    public int contarSesionesOperativasActivas(ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        return Math.max(0, sesionesActivasProveedor.getAsInt());
    }

    @Override
    public void validarEsquemaRestaurado(ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        try (Connection conn = abrirConexion(destino)) {
            Set<String> existentes = listarTablasExistentes(conn);
            List<String> faltantes = new ArrayList<>();
            for (String tabla : TABLAS_ESQUEMA_MINIMO) {
                if (!existentes.contains(tabla)) {
                    faltantes.add(tabla);
                }
            }
            if (!faltantes.isEmpty()) {
                throw RestauracionException.integridadInvalida(
                        "Faltan tablas esperadas en la base restaurada: " + String.join(", ", faltantes) + ".");
            }
        } catch (SQLException e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudo validar el esquema restaurado en H2.", e);
        }
    }

    private static Connection abrirConexion(ConexionJdbc jdbc) throws SQLException {
        return DriverManager.getConnection(jdbc.url(), jdbc.usuario(), jdbc.claveTexto());
    }

    private static Set<String> listarTablasExistentes(Connection conn) throws SQLException {
        Set<String> tablas = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = CURRENT_SCHEMA()
                  AND TABLE_TYPE IN ('BASE TABLE', 'TABLE')
                """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tablas.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        return tablas;
    }

    private static String escaparRutaSql(Path archivo) {
        return archivo.toAbsolutePath().normalize().toString()
                .replace("\\", "/")
                .replace("'", "''");
    }
}
