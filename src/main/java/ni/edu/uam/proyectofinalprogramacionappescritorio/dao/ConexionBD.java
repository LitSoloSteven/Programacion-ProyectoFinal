package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexiones JDBC para PostgreSQL 18.
 * Permite configuración externa vía DB_URL, DB_USER y DB_PASSWORD.
 * Posee valores predeterminados seguros para desarrollo local en la UAM.
 */
public class ConexionBD {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/latch";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASS = "CALEBZITOW";

    @FunctionalInterface
    public interface ProveedorConexion {
        Connection obtener() throws SQLException;
    }

    private static ProveedorConexion proveedorPersonalizado = null;

    public static void setProveedorPersonalizado(ProveedorConexion proveedor) {
        proveedorPersonalizado = proveedor;
    }

    public static String getUrl() {
        String env = System.getenv("DB_URL");
        return (env != null && !env.isBlank()) ? env : DEFAULT_URL;
    }

    public static String getUser() {
        String env = System.getenv("DB_USER");
        return (env != null && !env.isBlank()) ? env : DEFAULT_USER;
    }

    public static String getPassword() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null && !env.isBlank()) {
            return env;
        }
        String alt = System.getenv("DB_PASS");
        if (alt != null && !alt.isBlank()) {
            return alt;
        }
        return DEFAULT_PASS;
    }

    public static Connection obtenerConexion() throws SQLException {
        if (proveedorPersonalizado != null) {
            return proveedorPersonalizado.obtener();
        }
        try {
            return DriverManager.getConnection(getUrl(), getUser(), getPassword());
        } catch (SQLException e) {
            String urlSegura = sanitizarUrl(getUrl());
            throw new SQLException(
                    "No se pudo conectar a PostgreSQL (" + urlSegura + ", usuario: " + getUser() + "): " + e.getMessage(),
                    e.getSQLState(),
                    e.getErrorCode()
            );
        }
    }

    public static String sanitizarUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("(?i)(password=)[^&;]*", "$1*****")
                  .replaceAll("(://[^:]+:)[^@]+@", "$1*****@");
    }
}
