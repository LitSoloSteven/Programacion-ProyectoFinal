package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.DatabaseConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementación de respaldo y restauración para PostgreSQL mediante pg_dump/pg_restore (TRD / RN-29).
 */
public final class MotorPostgresRespaldo implements MotorBaseRespaldo {
    private static final Set<String> BASES_PROTEGIDAS = Set.of("latch_db", "latch_demo");
    private static final long TIMEOUT_PROCESO_MINUTOS = 30L;
    private static final int MAX_SALIDA_BYTES = 64 * 1024;

    private final ConexionJdbc origen;
    private final String pgBinDir;

    public MotorPostgresRespaldo(ConexionJdbc origen) {
        this(origen, System.getenv("LATCH_PG_BIN_DIR"));
    }

    public MotorPostgresRespaldo(ConexionJdbc origen, String pgBinDir) {
        this.origen = MotorBaseRespaldo.validarNoNula(origen);
        this.pgBinDir = pgBinDir == null || pgBinDir.isBlank() ? null : pgBinDir.strip();
    }

    public static MotorPostgresRespaldo desdeConfiguracion() {
        Map<String, String> variables = DatabaseConfig.leerVariables(Path.of(".env"), System.getenv());
        String url = variables.get("LATCH_DB_URL");
        String user = variables.get("LATCH_DB_USER");
        String password = variables.get("LATCH_DB_PASSWORD");
        if (url == null || url.isBlank() || user == null || user.isBlank() || password == null) {
            throw RespaldoException.operativo(
                    "ERR-055: Faltan las variables de conexión PostgreSQL para operar el respaldo.", null);
        }
        return new MotorPostgresRespaldo(
                new ConexionJdbc(url, user, password.toCharArray()),
                variables.get("LATCH_PG_BIN_DIR"));
    }

    public static boolean esBaseProtegida(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            String base = DatabaseConfig.parsearConexionPostgres(url).baseDatos();
            return BASES_PROTEGIDAS.contains(base.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String identificadorMotor() {
        return "postgresql";
    }

    @Override
    public void exportarBase(Path archivoDump) {
        exportarBaseDestino(origen, archivoDump);
    }

    @Override
    public void exportarBaseDestino(ConexionJdbc destino, Path archivoDump) {
        MotorBaseRespaldo.validarNoNula(destino);
        Objects.requireNonNull(archivoDump, "Falta la ruta del archivo dump.");
        DatabaseConfig.DatosConexionPostgres datos = DatabaseConfig.parsearConexionPostgres(destino.url());
        Path ejecutable = resolverEjecutable("pg_dump");
        List<String> comando = List.of(
                ejecutable.toString(),
                "--format=custom",
                "--no-owner",
                "--no-privileges",
                "--no-password",
                "--host=" + datos.host(),
                "--port=" + datos.puerto(),
                "--username=" + destino.usuario(),
                "--file=" + archivoDump.toAbsolutePath().normalize(),
                datos.baseDatos()
        );
        ejecutarProcesoPostgres(comando, destino, datos, "pg_dump");
    }

    @Override
    public void restaurarEnBaseAislada(Path archivoDump, ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        Objects.requireNonNull(archivoDump, "Falta el archivo dump para restaurar.");
        DatabaseConfig.DatosConexionPostgres datos = DatabaseConfig.parsearConexionPostgres(destino.url());
        Path ejecutable = resolverEjecutable("pg_restore");
        List<String> comando = List.of(
                ejecutable.toString(),
                "--clean",
                "--if-exists",
                "--no-owner",
                "--no-privileges",
                "--no-password",
                "--single-transaction",
                "--host=" + datos.host(),
                "--port=" + datos.puerto(),
                "--username=" + destino.usuario(),
                "--dbname=" + datos.baseDatos(),
                archivoDump.toAbsolutePath().normalize().toString()
        );
        ejecutarProcesoPostgres(comando, destino, datos, "pg_restore");
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
                    "ERR-055: No se pudo inspeccionar el consecutivo de recibos en la base destino.", e);
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
                    "ERR-055: No se pudieron verificar los conteos de tablas en PostgreSQL.", e);
        }
    }

    @Override
    public int contarSesionesOperativasActivas(ConexionJdbc destino) {
        MotorBaseRespaldo.validarNoNula(destino);
        try (Connection conn = abrirConexion(destino);
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT COUNT(*) FROM pg_stat_activity
                     WHERE datname = current_database()
                       AND pid <> pg_backend_pid()
                       AND backend_type = 'client backend'
                     """);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudieron comprobar las sesiones activas en PostgreSQL.", e);
        }
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
                        "Faltan tablas esperadas tras la restauración: " + String.join(", ", faltantes) + ".");
            }
        } catch (SQLException e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudo validar el esquema restaurado en PostgreSQL.", e);
        }
    }

    private Connection abrirConexion(ConexionJdbc jdbc) throws SQLException {
        DatabaseConfig.DatosConexionPostgres datos = DatabaseConfig.parsearConexionPostgres(jdbc.url());
        return DriverManager.getConnection(datos.url(), jdbc.usuario(), jdbc.claveTexto());
    }

    private static Set<String> listarTablasExistentes(Connection conn) throws SQLException {
        Set<String> tablas = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'
                """);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tablas.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        return tablas;
    }

    private void ejecutarProcesoPostgres(List<String> comando, ConexionJdbc jdbc,
                                         DatabaseConfig.DatosConexionPostgres datos, String herramienta) {
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("PGPASSWORD", jdbc.claveTexto());
        if (datos.sslMode() != null) {
            env.put("PGSSLMODE", datos.sslMode());
        }
        if (datos.sslRootCert() != null) {
            env.put("PGSSLROOTCERT", datos.sslRootCert());
        }

        Process proceso;
        try {
            proceso = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo ejecutar " + herramienta + ".", e);
        }
        var lectura = new FutureTask<>(() -> {
            try (InputStream entrada = proceso.getInputStream()) {
                return leerSalidaAcotada(entrada);
            }
        });
        long plazoNanos = TimeUnit.MINUTES.toNanos(TIMEOUT_PROCESO_MINUTOS);
        long vencimiento = System.nanoTime() + plazoNanos;
        Thread.startVirtualThread(lectura);
        try {
            if (!proceso.waitFor(plazoNanos, TimeUnit.NANOSECONDS)) throw new TimeoutException();
            String salida = lectura.get(Math.max(0L, vencimiento - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (proceso.exitValue() != 0) {
                String detalle = salida.isBlank() ? "" : " Detalle: " + resumir(salida);
                throw new IllegalStateException(
                        herramienta + " finalizó con código " + proceso.exitValue() + "." + detalle);
            }
        } catch (ExecutionException e) {
            throw new IllegalStateException("No se pudo leer la salida de " + herramienta + ".", e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("La herramienta " + herramienta + " excedió el tiempo máximo permitido.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la ejecución de " + herramienta + ".", e);
        } finally {
            if (proceso.isAlive()) proceso.destroyForcibly();
            lectura.cancel(true);
        }
    }

    private Path resolverEjecutable(String nombreBase) {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        String nombreArchivo = windows ? nombreBase + ".exe" : nombreBase;

        if (pgBinDir != null) {
            Path candidato = Path.of(pgBinDir, nombreArchivo);
            if (Files.isRegularFile(candidato) && Files.isExecutable(candidato)) {
                return candidato;
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            String separador = System.getProperty("path.separator", ";");
            for (String dir : pathEnv.split(java.util.regex.Pattern.quote(separador))) {
                if (dir.isBlank()) continue;
                Path candidato = Path.of(dir.strip(), nombreArchivo);
                if (Files.isRegularFile(candidato) && Files.isExecutable(candidato)) {
                    return candidato;
                }
            }
        }

        if (windows) {
            Path raizPg = Path.of("C:\\Program Files\\PostgreSQL");
            if (Files.isDirectory(raizPg)) {
                try (var dirs = Files.list(raizPg)) {
                    List<Path> versiones = dirs.filter(Files::isDirectory)
                            .sorted(Comparator.comparingInt(MotorPostgresRespaldo::extraerVersion).reversed())
                            .toList();
                    for (Path versionDir : versiones) {
                        Path candidato = versionDir.resolve("bin").resolve(nombreArchivo);
                        if (Files.isRegularFile(candidato) && Files.isExecutable(candidato)) {
                            return candidato;
                        }
                    }
                } catch (IOException ignored) {
                    // Continúa buscando en rutas alternas.
                }
            }
        } else {
            Path usrBin = Path.of("/usr/bin", nombreArchivo);
            if (Files.isRegularFile(usrBin) && Files.isExecutable(usrBin)) {
                return usrBin;
            }
        }

        throw new IllegalStateException(
                "No se encontró el ejecutable " + nombreArchivo + ". Configura LATCH_PG_BIN_DIR o el PATH.");
    }

    private static int extraerVersion(Path dir) {
        try {
            return Integer.parseInt(dir.getFileName().toString().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String leerSalidaAcotada(InputStream entrada) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int leidos;
        while ((leidos = entrada.read(buf)) != -1) {
            int disponible = MAX_SALIDA_BYTES - baos.size();
            if (disponible > 0) {
                baos.write(buf, 0, Math.min(leidos, disponible));
            }
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static String resumir(String salida) {
        String limpia = salida.replaceAll("[\\r\\n]+", " | ").strip();
        return limpia.length() <= 300 ? limpia : limpia.substring(0, 300) + "...";
    }
}
