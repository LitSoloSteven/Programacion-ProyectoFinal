package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class DatabaseConfig {
    private DatabaseConfig() {}

    public static Properties desdeEntorno(Map<String, String> entorno) {
        String url = requerida(entorno, "LATCH_DB_URL");
        // Impide introducir credenciales en la URL, que Hibernate puede escribir en logs.
        if (!url.matches("jdbc:postgresql://[^?@]+/[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("LATCH_DB_URL debe ser jdbc:postgresql://host:puerto/base, sin credenciales ni parámetros.");
        }
        String schema = entorno.getOrDefault("LATCH_DB_SCHEMA_MODE", "validate");
        if (!Set.of("validate", "update", "none").contains(schema)) {
            throw new IllegalArgumentException("LATCH_DB_SCHEMA_MODE debe ser validate, update o none.");
        }
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        p.setProperty("hibernate.connection.url", url);
        p.setProperty("hibernate.connection.username", requerida(entorno, "LATCH_DB_USER"));
        p.setProperty("hibernate.connection.password", requerida(entorno, "LATCH_DB_PASSWORD"));
        p.setProperty("hibernate.hbm2ddl.auto", schema);
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");
        p.setProperty("hibernate.connection.pool_size", "5");
        p.setProperty("hibernate.connection.autocommit", "false");
        p.setProperty("hibernate.connection.connectTimeout", "5");
        p.setProperty("hibernate.connection.socketTimeout", "10");
        return p;
    }

    private static String requerida(Map<String, String> entorno, String clave) {
        String valor = entorno.get(clave);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta configurar " + clave + ".");
        return valor;
    }
}
