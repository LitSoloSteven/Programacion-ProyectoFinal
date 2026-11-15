package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class PostgresTestSupport {
    private static final String[] MIGRACIONES = {
            "V1__propietario_perfil.sql",
            "V2__seguimiento_garantias.sql",
            "V3__descripcion_unidad.sql",
            "V4__tasas_autorizadas.sql",
            "V5__pagos_idempotencia_aplicaciones.sql",
            "V6__contratos_activos_por_unidad.sql",
            "V7__retencion_idempotencia_pagos.sql",
            "V8__consecutivo_recibos_transaccional.sql",
            "V9__auditoria_login_fallido.sql",
            "V10__instante_operativo_pago.sql"
    };

    private PostgresTestSupport() {}

    public static Properties propiedadesHibernate() {
        Map<String, String> entorno = Map.of(
                "LATCH_DB_URL", requerida("LATCH_TEST_DB_URL"),
                "LATCH_DB_USER", requerida("LATCH_TEST_DB_USER"),
                "LATCH_DB_PASSWORD", requerida("LATCH_TEST_DB_PASSWORD"),
                "LATCH_DB_SCHEMA_MODE", "validate");
        Properties propiedades = DatabaseConfig.desdeEntorno(entorno);
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
        propiedades.setProperty("hibernate.show_sql", "false");
        return propiedades;
    }

    public static void aplicarMigraciones(Properties propiedades) {
        String url = propiedades.getProperty("hibernate.connection.url");
        String usuario = propiedades.getProperty("hibernate.connection.username");
        String clave = propiedades.getProperty("hibernate.connection.password");
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            for (String migracion : MIGRACIONES) {
                String script = Files.readString(Path.of("docs", "migrations", migracion));
                for (String sql : separarSentencias(script)) {
                    try (Statement sentencia = conexion.createStatement()) {
                        sentencia.execute(sql);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("No se pudieron aplicar las migraciones PostgreSQL de prueba.", e);
        }
    }

    public static void eliminarFuncionRecibo() {
        Properties propiedades = propiedadesHibernate();
        try (Connection conexion = DriverManager.getConnection(
                propiedades.getProperty("hibernate.connection.url"),
                propiedades.getProperty("hibernate.connection.username"),
                propiedades.getProperty("hibernate.connection.password"));
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("drop function if exists public.fn_reservar_numero()");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo limpiar la función PostgreSQL de prueba.", e);
        }
    }

    static List<String> separarSentencias(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        String dollarQuote = null;
        boolean cadena = false;
        boolean identificador = false;
        boolean comentarioLinea = false;
        boolean comentarioBloque = false;

        for (int i = 0; i < script.length(); i++) {
            char caracter = script.charAt(i);
            char siguiente = i + 1 < script.length() ? script.charAt(i + 1) : '\0';
            actual.append(caracter);

            if (comentarioLinea) {
                if (caracter == '\n') comentarioLinea = false;
                continue;
            }
            if (comentarioBloque) {
                if (caracter == '*' && siguiente == '/') {
                    actual.append(siguiente);
                    i++;
                    comentarioBloque = false;
                }
                continue;
            }
            if (dollarQuote != null) {
                if (script.startsWith(dollarQuote, i)) {
                    actual.append(script, i + 1, i + dollarQuote.length());
                    i += dollarQuote.length() - 1;
                    dollarQuote = null;
                }
                continue;
            }
            if (cadena) {
                if (caracter == '\'' && siguiente == '\'') {
                    actual.append(siguiente);
                    i++;
                } else if (caracter == '\'') {
                    cadena = false;
                }
                continue;
            }
            if (identificador) {
                if (caracter == '"' && siguiente == '"') {
                    actual.append(siguiente);
                    i++;
                } else if (caracter == '"') {
                    identificador = false;
                }
                continue;
            }
            if (caracter == '-' && siguiente == '-') {
                actual.append(siguiente);
                i++;
                comentarioLinea = true;
            } else if (caracter == '/' && siguiente == '*') {
                actual.append(siguiente);
                i++;
                comentarioBloque = true;
            } else if (caracter == '\'') {
                cadena = true;
            } else if (caracter == '"') {
                identificador = true;
            } else if (caracter == '$') {
                String etiqueta = etiquetaDollar(script, i);
                if (etiqueta != null) {
                    actual.append(script, i + 1, i + etiqueta.length());
                    i += etiqueta.length() - 1;
                    dollarQuote = etiqueta;
                }
            } else if (caracter == ';') {
                agregar(sentencias, actual);
            }
        }
        agregar(sentencias, actual);
        return sentencias;
    }

    private static String etiquetaDollar(String script, int inicio) {
        int fin = script.indexOf('$', inicio + 1);
        if (fin < 0) return null;
        String etiqueta = script.substring(inicio, fin + 1);
        return etiqueta.matches("\\$\\$|\\$[A-Za-z_][A-Za-z0-9_]*\\$") ? etiqueta : null;
    }

    private static void agregar(List<String> sentencias, StringBuilder actual) {
        String sql = actual.toString().trim();
        if (!sql.isEmpty()) sentencias.add(sql);
        actual.setLength(0);
    }

    private static String requerida(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de prueba " + nombre + ".");
        }
        return valor;
    }
}
