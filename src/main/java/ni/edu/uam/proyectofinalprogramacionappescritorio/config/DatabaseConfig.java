package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.InvalidPathException;

public final class DatabaseConfig {
    private static final Pattern JDBC_URL = Pattern.compile(
            "jdbc:postgresql://(?<host>\\[[^\\]]+\\]|[^:/?#@]+)(?::(?<port>[0-9]+))?/(?<database>[a-zA-Z0-9_-]+)");
    private static final Set<String> SSL_MODES = Set.of("disable", "allow", "prefer", "require", "verify-ca", "verify-full");
    private static final Set<String> VERIFIED_SSL_MODES = Set.of("verify-ca", "verify-full");

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
        ConexionPostgres conexion = conexionPostgres(requerida(entorno, "LATCH_DB_URL"));
        String schema = entorno.getOrDefault("LATCH_DB_SCHEMA_MODE", "validate");
        if (!Set.of("validate", "update", "none").contains(schema)) {
            throw new IllegalArgumentException("LATCH_DB_SCHEMA_MODE debe ser validate, update o none.");
        }
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        p.setProperty("hibernate.connection.url", conexion.url());
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

    public static DatosConexionPostgres parsearConexionPostgres(String url) {
        if (url == null || url.isBlank()) throw urlInvalida();
        int inicioParametros = url.indexOf('?');
        String base = inicioParametros < 0 ? url : url.substring(0, inicioParametros);
        Matcher matcher = JDBC_URL.matcher(base);
        if (!matcher.matches() || (inicioParametros >= 0 && url.indexOf('?', inicioParametros + 1) >= 0)) {
            throw urlInvalida();
        }
        String puertoTexto = matcher.group("port");
        int puerto = puertoTexto == null ? 5432 : Integer.parseInt(puertoTexto);
        validarPuerto(puertoTexto);

        String sslmode = null;
        String sslrootcert = null;
        if (inicioParametros >= 0) {
            String parametros = url.substring(inicioParametros + 1);
            if (parametros.isEmpty()) throw urlInvalida();
            for (String parametro : parametros.split("&", -1)) {
                int separador = parametro.indexOf('=');
                if (separador <= 0 || separador == parametro.length() - 1) throw urlInvalida();
                String clave = decodificarParametro(parametro.substring(0, separador));
                String valor = decodificarParametro(parametro.substring(separador + 1));
                if (clave.equals("sslmode") && sslmode == null) {
                    if (!SSL_MODES.contains(valor)) {
                        throw new IllegalArgumentException("sslmode debe ser disable, allow, prefer, require, verify-ca o verify-full.");
                    }
                    sslmode = valor;
                } else if (clave.equals("sslrootcert") && sslrootcert == null) {
                    sslrootcert = valor;
                } else {
                    throw new IllegalArgumentException("LATCH_DB_URL solo admite los parámetros sslmode y sslrootcert, sin duplicados.");
                }
            }
        }

        String host = matcher.group("host");
        if (host.startsWith("[")) host = host.substring(1, host.length() - 1);
        boolean local = esHostLocal(host);
        if (!local && (sslmode == null || !VERIFIED_SSL_MODES.contains(sslmode))) {
            throw new IllegalArgumentException("Para hosts remotos usa sslmode=verify-ca o verify-full con sslrootcert; require no valida el certificado del servidor.");
        }
        if (sslrootcert != null && (sslmode == null || !VERIFIED_SSL_MODES.contains(sslmode))) {
            throw new IllegalArgumentException("sslrootcert solo se permite con sslmode=verify-ca o verify-full.");
        }
        if (sslmode != null && VERIFIED_SSL_MODES.contains(sslmode)) {
            if (sslrootcert == null) {
                throw new IllegalArgumentException("sslmode=verify-ca o verify-full requiere sslrootcert.");
            }
            sslrootcert = validarCertificado(sslrootcert);
        }

        String urlConfigurada = url;
        if (sslrootcert != null) {
            urlConfigurada = base + "?sslmode=" + codificarParametro(sslmode)
                    + "&sslrootcert=" + codificarParametro(sslrootcert);
        }
        return new DatosConexionPostgres(urlConfigurada, host, puerto, matcher.group("database"), sslmode, sslrootcert);
    }

    private static ConexionPostgres conexionPostgres(String url) {
        return new ConexionPostgres(parsearConexionPostgres(url).url());
    }

    private static void validarPuerto(String puerto) {
        if (puerto == null) return;
        try {
            int valor = Integer.parseInt(puerto);
            if (valor < 1 || valor > 65535) throw urlInvalida();
        } catch (NumberFormatException e) {
            throw urlInvalida();
        }
    }

    private static String decodificarParametro(String valor) {
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw urlInvalida();
        }
    }

    private static String codificarParametro(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String validarCertificado(String valor) {
        try {
            Path ruta = Path.of(valor);
            if (!ruta.isAbsolute() || !Files.isRegularFile(ruta) || !Files.isReadable(ruta)) {
                throw new IllegalArgumentException("sslrootcert debe ser la ruta absoluta y legible de un certificado raíz existente.");
            }
            return ruta.normalize().toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("sslrootcert debe ser una ruta absoluta y legible.");
        }
    }

    private static boolean esHostLocal(String host) {
        if (host.equalsIgnoreCase("localhost") || host.equals("::1")) return true;
        if (!host.matches("127(?:\\.[0-9]{1,3}){3}")) return false;
        for (String segmento : host.split("\\.")) {
            if (Integer.parseInt(segmento) > 255) return false;
        }
        return true;
    }

    private static IllegalArgumentException urlInvalida() {
        return new IllegalArgumentException("LATCH_DB_URL debe ser jdbc:postgresql://host[:puerto]/base con solo parámetros sslmode y sslrootcert, sin credenciales.");
    }

    private record ConexionPostgres(String url) {}

    public record DatosConexionPostgres(
            String url,
            String host,
            int puerto,
            String baseDatos,
            String sslMode,
            String sslRootCert
    ) { }

    private static String requerida(Map<String, String> entorno, String clave) {
        String valor = entorno.get(clave);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta configurar " + clave + ".");
        return valor;
    }
}
