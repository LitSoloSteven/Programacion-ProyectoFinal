package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.AuditoriaRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.DocumentoReporteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.GenerarReporteRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.SaludResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba real de aislamiento en 2 JVM separadas:
 * JVM 1 ({@code latch-api} + H2 en archivo sin JavaFX) y JVM 2 (escritorio con su contrato HTTP
 * sin Spring, Hibernate, PostgreSQL, JasperReports ni BouncyCastle).
 */
class AislamientoDosJvmIT {

    private static final String USUARIO = "dosjvm-admin";
    private static final String PASSWORD = "ClaveDosJvm-2026";

    @AfterEach
    void limpiarSesion() {
        SesionCliente.cerrarSesion();
    }

    @Test
    void clienteNoTieneDependenciasServidorYOperaContraJvm1Persistente(@TempDir Path dir) throws Exception {
        // 1. Verificar en JVM 2 (cliente) ausencia estricta de librerías de servidor
        for (String claseServidor : List.of(
                "org.springframework.boot.SpringApplication",
                "org.hibernate.SessionFactory",
                "org.postgresql.Driver",
                "net.sf.jasperreports.engine.JasperPrint",
                "org.bouncycastle.jce.provider.BouncyCastleProvider")) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(claseServidor),
                    "El classpath de latch-cliente no debe incluir: " + claseServidor);
        }

        List<String> cpServidor = construirClasspathServidor(dir.resolve("war-libs"));
        assertTrue(cpServidor.stream().noneMatch(e -> e.contains("javafx-")),
                "El classpath de JVM 1 no debe incluir JavaFX: " + cpServidor);
        String classpathStr = String.join(File.pathSeparator, cpServidor);

        Path dbFile = dir.resolve("latch_dosjvm");
        Path adjuntosDir = dir.resolve("adjuntos");

        long numeroRecibo;
        String propiedadId;
        String contratoId;

        // 2. Arrancar JVM 1 y ejecutar flujo completo desde JVM 2
        ServidorProceso jvm1 = iniciarServidor(classpathStr, dbFile, adjuntosDir);
        try {
            ClienteLatch.iniciar("http://127.0.0.1:" + jvm1.puerto());
            SaludResponse salud = ClienteLatch.api().getSalud();
            assertEquals("OK", salud.estado());

            LoginResponse login = ClienteLatch.auth().login(USUARIO, PASSWORD);
            assertEquals(ConstantesLatch.ROL_PROPIETARIO, login.rol());

            PropiedadResponse prop = ClienteLatch.catalogos().guardarPropiedad(null, new PropiedadRequest(
                    "Residencia DosJVM", "Av. Central", true, "Ana", "ana@latch.test", "8888", null));
            propiedadId = prop.id();

            UnidadResponse unidad = ClienteLatch.catalogos().guardarUnidad(null, new UnidadRequest(
                    propiedadId, "A-101", "Departamento", "Apartamento", "Disponible", "500.00", "USD", null));

            InquilinoResponse inq = ClienteLatch.catalogos().guardarInquilino(null, new InquilinoRequest(
                    "Carlos Pérez", "001-010190-0001A", "8888-0000", "carlos@latch.test", "María", true, null));

            LocalDate hoy = LocalDate.now();
            ContratoResponse contrato = ClienteLatch.catalogos().guardarContrato(null, new ContratoRequest(
                    unidad.id(), inq.id(),
                    hoy.minusMonths(1).toString(), hoy.plusMonths(11).toString(),
                    "500.00", "USD", 5, "500.00", "Vigente", null));
            contratoId = contrato.id();

            CargoDetalleResponse cargo = ClienteLatch.pagos().crearCargo(new CargoRequest(
                    contratoId, "Renta", hoy.toString().substring(0, 7),
                    hoy.minusDays(5).toString(), hoy.plusDays(5).toString(),
                    "500.00", "Renta mensual", "Pendiente", null));
            assertEquals("500.00", cargo.importe());

            PagoRegistroResponse pago = ClienteLatch.pagos().registrar(
                    new PagoRequest(contratoId, hoy.toString(), "USD", "500.00",
                            "Transferencia", "REF-2JVM", List.of(), "Pago desde JVM 2", true),
                    UUID.randomUUID());
            assertTrue(pago.numeroRecibo() >= 1);
            numeroRecibo = pago.numeroRecibo();

            var recibosPorNumero = ClienteLatch.reportes().buscarRecibos(numeroRecibo, null);
            assertEquals(1, recibosPorNumero.size());
            assertEquals(numeroRecibo, recibosPorNumero.getFirst().numeroRecibo());
            assertFalse(recibosPorNumero.getFirst().anulado());
            var recibosPorRef = ClienteLatch.reportes().buscarRecibos(null, "REF-2JVM");
            assertFalse(recibosPorRef.isEmpty());

            DocumentoReporteResponse doc = ClienteLatch.reportes().generar(new GenerarReporteRequest(
                    "RECIBO", null, null, null, null, null, null, null, null,
                    pago.pagoId(), null, null, null, null, null, null, null, null, null));
            byte[] png = ClienteLatch.reportes().paginaPng(doc.documentoId(), 0, 1.0f);
            byte[] pdf = ClienteLatch.reportes().exportarPdf(doc.documentoId());
            assertEquals((byte) 0x89, png[0]);
            assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
            ClienteLatch.reportes().eliminar(doc.documentoId());

            List<AuditoriaRegistroResponse> audit = ClienteLatch.admin()
                    .consultarAuditoria(USUARIO, null, null, null, 20);
            assertFalse(audit.isEmpty(), "La auditoría debe registrar las operaciones HTTP de JVM 2");

            // Reinicio independiente del cliente mientras JVM 1 permanece activa
            ClienteLatch.cerrar();
            ClienteLatch.iniciar("http://127.0.0.1:" + jvm1.puerto());
            LoginResponse relogin = ClienteLatch.auth().login(USUARIO, PASSWORD);
            assertNotNull(relogin.token());
            assertEquals("Residencia DosJVM",
                    ClienteLatch.catalogos().propiedad(UUID.fromString(propiedadId)).nombre());
            assertEquals(1, ClienteLatch.reportes().buscarRecibos(numeroRecibo, null).size());
        } finally {
            jvm1.detener();
        }

        // 3. Con JVM 1 detenida, el cliente falla limpiamente con SIN_CONEXION
        ErrorClienteException sinConexion = assertThrows(ErrorClienteException.class,
                () -> ClienteLatch.api().getSalud());
        assertTrue(sinConexion.esSinConexion());
        assertEquals(0, sinConexion.getEstado());
        assertEquals("SIN_CONEXION", sinConexion.getCodigo());

        // 4. Reiniciar JVM 1 sobre la misma base H2 y verificar persistencia desde JVM 2
        ServidorProceso jvm1Reiniciado = iniciarServidor(classpathStr, dbFile, adjuntosDir);
        try {
            ClienteLatch.iniciar("http://127.0.0.1:" + jvm1Reiniciado.puerto());
            ClienteLatch.auth().login(USUARIO, PASSWORD);

            PropiedadResponse propPersistida = ClienteLatch.catalogos().propiedad(UUID.fromString(propiedadId));
            assertEquals("Residencia DosJVM", propPersistida.nombre());

            var recibo = ClienteLatch.pagos().recibo(numeroRecibo);
            assertEquals(numeroRecibo, recibo.numero());
            assertEquals(contratoId, recibo.contratoId());
        } finally {
            jvm1Reiniciado.detener();
        }
    }

    private static ServidorProceso iniciarServidor(String classpath, Path dbFile, Path adjuntosDir) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorAisladoPruebaMain",
                dbFile.toAbsolutePath().toString(),
                adjuntosDir.toAbsolutePath().toString(),
                USUARIO,
                PASSWORD);
        pb.redirectErrorStream(true);
        Process proceso = pb.start();
        BufferedReader stdout = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder log = new StringBuilder();
        long limite = System.currentTimeMillis() + 60_000L;
        int puerto = -1;
        while (System.currentTimeMillis() < limite) {
            String linea = stdout.readLine();
            if (linea == null) break;
            log.append(linea).append('\n');
            if (linea.startsWith("LATCH_READY_PORT=")) {
                puerto = Integer.parseInt(linea.substring("LATCH_READY_PORT=".length()).strip());
                break;
            }
        }
        if (puerto <= 0) {
            proceso.destroyForcibly();
            throw new IllegalStateException("JVM 1 no arrancó a tiempo. Log:\n" + log);
        }
        Thread drainer = new Thread(() -> {
            try {
                while (stdout.readLine() != null) {
                    // Drenar stdout restante
                }
            } catch (IOException ignored) {
            }
        }, "jvm1-stdout-drainer");
        drainer.setDaemon(true);
        drainer.start();
        return new ServidorProceso(proceso, puerto);
    }

    private static List<String> construirClasspathServidor(Path libsTempDir) throws IOException {
        Path raizApi = Files.isDirectory(Path.of("..", "latch-api"))
                ? Path.of("..", "latch-api")
                : Path.of("latch-api");
        Path classes = raizApi.resolve("target").resolve("classes");
        Path testClasses = raizApi.resolve("target").resolve("test-classes");
        assertTrue(Files.isDirectory(classes), "Debe existir target/classes de latch-api");
        assertTrue(Files.isDirectory(testClasses), "Debe existir target/test-classes de latch-api");

        List<String> cp = new ArrayList<>();
        cp.add(classes.toAbsolutePath().toString());
        cp.add(testClasses.toAbsolutePath().toString());

        Path explodedLibs = raizApi.resolve("target").resolve("ROOT").resolve("WEB-INF").resolve("lib");
        if (Files.isDirectory(explodedLibs)) {
            try (Stream<Path> s = Files.list(explodedLibs)) {
                s.filter(p -> p.toString().endsWith(".jar")).forEach(p -> cp.add(p.toAbsolutePath().toString()));
            }
        } else {
            Path war = raizApi.resolve("target").resolve("ROOT.war");
            assertTrue(Files.isRegularFile(war), "Debe existir target/ROOT.war para obtener libs del servidor");
            Files.createDirectories(libsTempDir);
            try (ZipFile zip = new ZipFile(war.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("WEB-INF/lib/") && entry.getName().endsWith(".jar")) {
                        Path dest = libsTempDir.resolve(Path.of(entry.getName()).getFileName().toString());
                        try (InputStream in = zip.getInputStream(entry)) {
                            Files.copy(in, dest);
                        }
                        cp.add(dest.toAbsolutePath().toString());
                    }
                }
            }
        }

        // Agregar únicamente dependencias provided/test requeridas en JVM 1 (Tomcat embebido y H2)
        String repoOverride = System.getProperty("maven.repo.local");
        Path m2 = repoOverride != null && !repoOverride.isBlank()
                ? Path.of(repoOverride)
                : Path.of(System.getProperty("user.home"), ".m2", "repository");
        for (String rel : List.of(
                "org/springframework/boot/spring-boot-tomcat/4.1.2/spring-boot-tomcat-4.1.2.jar",
                "org/springframework/boot/spring-boot-starter-tomcat/4.1.2/spring-boot-starter-tomcat-4.1.2.jar",
                "org/springframework/boot/spring-boot-starter-tomcat-runtime/4.1.2/spring-boot-starter-tomcat-runtime-4.1.2.jar",
                "org/apache/tomcat/embed/tomcat-embed-core/11.0.27/tomcat-embed-core-11.0.27.jar",
                "org/apache/tomcat/embed/tomcat-embed-el/11.0.27/tomcat-embed-el-11.0.27.jar",
                "org/apache/tomcat/embed/tomcat-embed-websocket/11.0.27/tomcat-embed-websocket-11.0.27.jar",
                "com/h2database/h2/2.3.232/h2-2.3.232.jar")) {
            Path jar = m2.resolve(rel);
            assertTrue(Files.isRegularFile(jar), "Falta JAR local para JVM 1: " + jar);
            cp.add(jar.toAbsolutePath().toString());
        }
        return cp;
    }

    private record ServidorProceso(Process proceso, int puerto) {
        void detener() throws InterruptedException {
            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(proceso.getOutputStream(), StandardCharsets.UTF_8), true)) {
                out.println("STOP");
            } catch (RuntimeException ignored) {
            }
            if (!proceso.waitFor(20, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                proceso.waitFor(5, TimeUnit.SECONDS);
            }
            assertEquals(0, proceso.exitValue(), "JVM 1 debe terminar con código 0");
        }
    }
}
