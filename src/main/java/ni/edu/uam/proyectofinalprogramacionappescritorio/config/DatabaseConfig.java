package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class DatabaseConfig {

    private DatabaseConfig() {}

    public static Properties desdeArchivo(Path envPath, Map<String, String> systemEnv) {
        Map<String, String> mergedEnv = new HashMap<>();

        if (Files.exists(envPath)) {
            try {
                Files.readAllLines(envPath).forEach(line -> {
                    // Limpiar BOM (Byte Order Mark) y espacios
                    String cleanLine = line.replace("\uFEFF", "").trim();
                    if (cleanLine.contains("=") && !cleanLine.startsWith("#")) {
                        String[] parts = cleanLine.split("=", 2);
                        // Limpiar comillas iniciales y finales
                        String value = parts[1].trim().replaceAll("^\"|\"$", "");
                        mergedEnv.put(parts[0].trim(), value);
                    }
                });
            } catch (IOException e) {
                System.err.println("Advertencia: No se pudo leer el archivo .env");
            }
        }
        // Las variables del sistema pisan a las del .env (Prioridad correcta)
        mergedEnv.putAll(systemEnv);
        return desdeEntorno(mergedEnv);
    }

    public static Properties desdeEntorno(Map<String, String> env) {
        String url = env.get("DB_URL");
        String user = env.get("DB_USER");
        String password = env.get("DB_PASSWORD");
        String schemaMode = env.getOrDefault("DB_SCHEMA_MODE", "validate");

        // Validaciones estrictas requeridas por las pruebas
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("La variable DB_URL es obligatoria");
        }
        if (user == null || password == null) {
            throw new IllegalArgumentException("Las credenciales (DB_USER, DB_PASSWORD) son obligatorias");
        }
        if (schemaMode.contains("create")) {
            throw new IllegalArgumentException("Modo de esquema peligroso. No se permite 'create' o 'create-drop'");
        }

        Properties properties = new Properties();
        properties.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.connection.url", url);
        properties.put("hibernate.connection.username", user);
        properties.put("hibernate.connection.password", password);
        properties.put("hibernate.hbm2ddl.auto", schemaMode);

        // Restaurar ajustes de rendimiento y seguridad de Steven
        properties.put("hibernate.connection.autocommit", "false");
        properties.put("hibernate.jdbc.time_zone", "UTC");
        properties.put("hibernate.c3p0.timeout", "300");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.current_session_context_class", "thread");

        return properties;
    }
}