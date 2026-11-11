package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class DatabaseConfig {

    private DatabaseConfig() {
        // Constructor privado
    }

    // 1. Método requerido por HibernateUtil (Lee el archivo .env)
    public static Properties desdeArchivo(Path envPath, Map<String, String> systemEnv) {
        Map<String, String> envMap = new HashMap<>(systemEnv);

        if (Files.exists(envPath)) {
            try {
                Files.readAllLines(envPath).forEach(line -> {
                    // Ignora comentarios y líneas vacías
                    if (line.contains("=") && !line.trim().startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        envMap.put(parts[0].trim(), parts[1].trim());
                    }
                });
            } catch (IOException e) {
                System.err.println("Advertencia: No se pudo leer el archivo .env - " + e.getMessage());
            }
        }
        return desdeEntorno(envMap);
    }

    // 2. Configura las propiedades de Hibernate usando las variables extraídas
    public static Properties desdeEntorno(Map<String, String> env) {
        Properties properties = new Properties();

        properties.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // Ahora busca exactamente los nombres que pusiste en tu .env
        properties.put("hibernate.connection.url", env.getOrDefault("LATCH_DB_URL", "jdbc:postgresql://localhost:5432/latch_db"));
        properties.put("hibernate.connection.username", env.getOrDefault("LATCH_DB_USER", "postgres"));
        properties.put("hibernate.connection.password", env.getOrDefault("LATCH_DB_PASSWORD", "postgres"));

        // Generación automática de tablas usando tu variable
        properties.put("hibernate.hbm2ddl.auto", env.getOrDefault("LATCH_DB_SCHEMA_MODE", "update"));

        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.current_session_context_class", "thread");

        return properties;
    }
}