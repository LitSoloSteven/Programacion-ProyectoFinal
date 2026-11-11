package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.util.Map;
import java.util.Properties;

public final class DatabaseConfig {

    private DatabaseConfig() {
        // Constructor privado para ocultar el implícito público
    }

    public static Properties desdeEntorno(Map<String, String> env) {
        Properties properties = new Properties();

        // 1. Configuración del driver y base de datos (PostgreSQL 18 / pgJDBC 42.7.14)
        properties.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // 2. URL de conexión (Busca variable de entorno, si no existe usa la local)
        // Asegúrate de haber creado la base de datos "latch_db" en pgAdmin primero.
        String dbUrl = env.getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/latch_db");
        properties.put("hibernate.connection.url", dbUrl);

        // 3. Credenciales (Busca variables de entorno, si no usa las tuyas locales)
        // Cambia "postgres" por tu usuario y contraseña real de tu PostgreSQL local.
        properties.put("hibernate.connection.username", env.getOrDefault("DB_USER", "postgres"));
        properties.put("hibernate.connection.password", env.getOrDefault("DB_PASSWORD", "1234"));

        // 4. ¡INSTRUCCIÓN DEL PROFESOR!: Autogeneración de tablas
        // "update" crea las tablas si no existen y las actualiza si añades nuevos campos a los modelos.
        properties.put("hibernate.hbm2ddl.auto", "update");

        // 5. Opciones de depuración (Muy útiles para ver qué hace Hibernate por debajo)
        properties.put("hibernate.show_sql", "true"); // Imprime el SQL generado en la consola
        properties.put("hibernate.format_sql", "true"); // Lo formatea para que sea fácil de leer

        // 6. Contexto de hilo para transacciones
        properties.put("hibernate.current_session_context_class", "thread");

        return properties;
    }
}