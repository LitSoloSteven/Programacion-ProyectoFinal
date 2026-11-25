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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class PostgresTestSupport {
    private static final String ESQUEMA_INICIAL = "V0__esquema_inicial.sql";
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
            "V10__instante_operativo_pago.sql",
            "V11__idempotencia_respuesta_text.sql",
            "V12__p1_cuentas_y_parametros_historial.sql",
            "V13__p2_contratos_y_movimientos_garantia.sql",
            "V14__p3_cobranza_mora_y_creditos.sql",
            "V15__p4_referencias_y_control_fx.sql",
            "V16__p5_mantenimiento_y_adjuntos.sql"
    };
    private static final Set<String> BASES_PROTEGIDAS = Set.of("latch_db", "latch_demo");
    private static final String[] TABLAS_LATCH_ORDEN_BORRADO = {
            "puerta_mantenimiento",
            "documento_adjunto",
            "gasto_propiedad",
            "solicitud_mantenimiento",
            "control_fx",
            "referencia_tipo_cambio",
            "consecutivo_recibo",
            "solicitud_idempotente",
            "aplicacion_pago",
            "movimiento_garantia",
            "recibo",
            "pago",
            "tipo_cambio",
            "cargo",
            "garantia",
            "contrato",
            "inquilino",
            "unidad",
            "propiedad",
            "propietario",
            "parametro_sistema",
            "configuracion",
            "auditoria",
            "usuario"
    };

    private PostgresTestSupport() {}

    public static Properties propiedadesHibernate() {
        return propiedadesHibernateBase("create-drop");
    }

    public static Properties propiedadesHibernateValidate() {
        return propiedadesHibernateBase("validate");
    }

    private static Properties propiedadesHibernateBase(String schemaMode) {
        String url = validarBaseDesechable(requerida("LATCH_TEST_DB_URL"));
        Map<String, String> entorno = Map.of(
                "LATCH_DB_URL", url,
                "LATCH_DB_USER", requerida("LATCH_TEST_DB_USER"),
                "LATCH_DB_PASSWORD", requerida("LATCH_TEST_DB_PASSWORD"),
                "LATCH_DB_SCHEMA_MODE", "validate");
        Properties propiedades = DatabaseConfig.desdeEntorno(entorno);
        propiedades.setProperty("hibernate.hbm2ddl.auto", schemaMode);
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
        propiedades.setProperty("hibernate.show_sql", "false");
        return propiedades;
    }

    public static void aplicarEsquemaInicial(Properties propiedades) {
        String url = validarBaseDesechable(propiedades.getProperty("hibernate.connection.url"));
        String usuario = propiedades.getProperty("hibernate.connection.username");
        String clave = propiedades.getProperty("hibernate.connection.password");
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            ejecutarScript(conexion, ESQUEMA_INICIAL);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("No se pudo aplicar el esquema inicial PostgreSQL de prueba.", e);
        }
    }

    public static void aplicarMigraciones(Properties propiedades) {
        String url = validarBaseDesechable(propiedades.getProperty("hibernate.connection.url"));
        String usuario = propiedades.getProperty("hibernate.connection.username");
        String clave = propiedades.getProperty("hibernate.connection.password");
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            for (String migracion : MIGRACIONES) {
                ejecutarScript(conexion, migracion);
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("No se pudieron aplicar las migraciones PostgreSQL de prueba.", e);
        }
    }

    public static void limpiarEsquemaPrueba(Properties propiedades) {
        String urlEsperada = validarBaseDesechable(requerida("LATCH_TEST_DB_URL"));
        String url = validarBaseDesechable(propiedades.getProperty("hibernate.connection.url"));
        if (!urlEsperada.equals(url)) {
            throw new IllegalStateException("La URL de limpieza no coincide con LATCH_TEST_DB_URL.");
        }
        String usuario = propiedades.getProperty("hibernate.connection.username");
        String clave = propiedades.getProperty("hibernate.connection.password");
        try (Connection conexion = DriverManager.getConnection(url, usuario, clave);
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("drop function if exists public.fn_reservar_numero()");
            for (String tabla : TABLAS_LATCH_ORDEN_BORRADO) {
                sentencia.execute("drop table if exists public." + tabla);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo limpiar el esquema PostgreSQL de prueba.", e);
        }
    }

    private static void ejecutarScript(Connection conexion, String archivo) throws IOException, SQLException {
        String script = Files.readString(Path.of("docs", "migrations", archivo));
        for (String sql : separarSentencias(script)) {
            try (Statement sentencia = conexion.createStatement()) {
                sentencia.execute(sql);
            }
        }
    }

    private static String validarBaseDesechable(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Falta configurar la URL PostgreSQL de prueba.");
        }
        int inicioParametros = url.indexOf('?');
        String baseSinParametros = inicioParametros < 0 ? url : url.substring(0, inicioParametros);
        int ultimaBarra = baseSinParametros.lastIndexOf('/');
        String nombreBase = ultimaBarra >= 0 ? baseSinParametros.substring(ultimaBarra + 1) : "";
        if (BASES_PROTEGIDAS.contains(nombreBase.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "LATCH_TEST_DB_URL debe apuntar a una base desechable, nunca a latch_db ni a latch_demo.");
        }
        return url;
    }

    public static void eliminarFuncionRecibo() {
        Properties propiedades = propiedadesHibernate();
        try (Connection conexion = DriverManager.getConnection(
                propiedades.getProperty("hibernate.connection.url"),
                propiedades.getProperty("hibernate.connection.username"),
                propiedades.getProperty("hibernate.connection.password"));
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("drop function if exists public.fn_reservar_numero()");
            sentencia.execute("drop table if exists public.consecutivo_recibo");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron limpiar los objetos auxiliares de recibos.", e);
        }
    }

    public static void impedirAuditoriaFinanciera(HibernateTransactions tx) {
        tx.ejecutar(sesion -> {
            sesion.createNativeMutationQuery("""
                    create or replace function fallar_auditoria_latch_prueba()
                    returns trigger language plpgsql as
                    'begin raise exception ''Fallo de auditoria de prueba''; end;'
                    """).executeUpdate();
            sesion.createNativeMutationQuery("""
                    create trigger fallar_auditoria_latch_prueba
                    before insert on auditoria for each row
                    when (new.accion in ('TASA_AUTORIZADA', 'PAGO_REGISTRADO'))
                    execute function fallar_auditoria_latch_prueba()
                    """).executeUpdate();
            return null;
        });
    }

    public static void permitirAuditoriaFinanciera(HibernateTransactions tx) {
        tx.ejecutar(sesion -> {
            sesion.createNativeMutationQuery(
                    "alter table auditoria disable trigger fallar_auditoria_latch_prueba").executeUpdate();
            return null;
        });
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
