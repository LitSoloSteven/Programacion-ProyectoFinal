package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public final class DatabaseConfig {
    private DatabaseConfig() {}

    /**
     * Lee las variables del archivo local y las combina con las del entorno.
     * El entorno tiene prioridad. Es la base común para la configuración de
     * base de datos y para la configuración de la API.
     */
    public static Map<String, String> leerVariables(Path archivo, Map<String, String> entorno) {
        Map<String, String> valores = new HashMap<>();
        try {
            int numero = 0;
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                numero++;
                if (numero == 1 && linea.startsWith("\uFEFF")) linea = linea.substring(1);
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                int separador = linea.indexOf('=');
                if (separador < 1) throw lineaInvalida(numero);
                String clave = linea.substring(0, separador).strip();
                String valor = linea.substring(separador + 1).strip();
                if (!clave.matches("[A-Za-z_][A-Za-z0-9_]*")) throw lineaInvalida(numero);
                if (valor.startsWith("\"") || valor.startsWith("'")) {
                    if (valor.length() < 2 || valor.charAt(valor.length() - 1) != valor.charAt(0)) {
                        throw lineaInvalida(numero);
                    }
                    valor = valor.substring(1, valor.length() - 1);
                }
                valores.put(clave, valor);
            }
        } catch (NoSuchFileException e) {
            // Sin archivo local, se conserva la configuración mediante variables del sistema.
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo .env. Revisa sus permisos.");
        }
        valores.putAll(entorno);
        return valores;
    }

    public static Properties desdeArchivo(Path archivo, Map<String, String> entorno) {
        return desdeEntorno(leerVariables(archivo, entorno));
    }

    private static IllegalArgumentException lineaInvalida(int numero) {
        return new IllegalArgumentException("Formato inválido en .env, línea " + numero + ". Usa NOMBRE=valor.");
    }

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
