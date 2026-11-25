package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoAdjuntoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.EstadoAlertaRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRestauracionDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoVerificacionRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Configuracion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ConsecutivoRecibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.GastoPropiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ParametroSistema;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.PuertaMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespaldoServiceTest {
    private static final char[] CLAVE_RESPALDO = "RespaldoSeguroLatch2026!".toCharArray();
    private static final byte[] PDF_MINIMO = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path tempDir;

    private String urlOrigen;
    private String urlAislada;
    private SessionFactory factoryOrigen;
    private HibernateTransactions txOrigen;
    private AlmacenAdjuntos almacenOrigen;
    private AtomicInteger sesionesActivasSimuladas;
    private MotorH2Respaldo motor;
    private RespaldoService service;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private DocumentoAdjuntoDTO adjuntoContrato;
    private DocumentoAdjuntoDTO adjuntoFoto;
    private Path dirRespaldos;
    private Path dirAdjuntosDestino;
    private Path dirCopiaPrevia;

    @BeforeEach
    void iniciar() throws Exception {
        String sufijo = UUID.randomUUID().toString().replace("-", "");
        urlOrigen = "jdbc:h2:mem:resp_orig_" + sufijo + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        urlAislada = "jdbc:h2:mem:resp_dest_" + sufijo + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

        factoryOrigen = HibernateUtil.construir(propiedadesH2(urlOrigen, "create-drop"));
        txOrigen = new HibernateTransactions(factoryOrigen);
        almacenOrigen = new AlmacenAdjuntos(tempDir.resolve("adjuntos-origen"));
        dirRespaldos = tempDir.resolve("respaldos");
        dirAdjuntosDestino = tempDir.resolve("adjuntos-destino");
        dirCopiaPrevia = tempDir.resolve("copias-previas");
        Files.createDirectories(dirRespaldos);

        sesionesActivasSimuladas = new AtomicInteger(0);
        motor = new MotorH2Respaldo(
                new MotorBaseRespaldo.ConexionJdbc(urlOrigen, "sa", new char[0]),
                sesionesActivasSimuladas::get);
        service = new RespaldoService(txOrigen, almacenOrigen, motor, Clock.systemUTC());

        sembrarDatosCompletos();
    }

    @AfterEach
    void cerrar() {
        if (factoryOrigen != null) {
            factoryOrigen.close();
        }
    }

    @Test
    void respaldoCompletoYRestauracionAisladaConCeroHashesDiscordantesYSinSecretos() throws Exception {
        EstadoAlertaRespaldoDTO alertaInicial = service.consultarEstadoAlerta(propietario);
        assertTrue(alertaInicial.alertaVencida());

        ResultadoRespaldoDTO respaldo = service.crearRespaldo(propietario, dirRespaldos, CLAVE_RESPALDO);
        assertTrue(Files.isRegularFile(respaldo.archivo()));
        assertTrue(respaldo.archivo().getFileName().toString().endsWith(RespaldoService.EXTENSION_RESPALDO));
        assertEquals(2, respaldo.cantidadAdjuntos());
        assertEquals(1L, respaldo.ultimoNumeroRecibo());

        EstadoAlertaRespaldoDTO alertaTrasRespaldo = service.consultarEstadoAlerta(propietario);
        assertFalse(alertaTrasRespaldo.alertaVencida());

        ResultadoVerificacionRespaldoDTO verificado = service.verificarPaquete(respaldo.archivo(), CLAVE_RESPALDO);
        assertEquals("V16", verificado.manifiesto().esquemaVersion());
        assertEquals(2, verificado.adjuntosVerificados());
        assertEquals(1L, verificado.manifiesto().ultimoNumeroRecibo());
        assertEquals(2L, verificado.manifiesto().siguienteNumeroRecibo());

        // El manifiesto descifrado no contiene contraseñas ni credenciales JDBC.
        Path zipPlano = tempDir.resolve("inspeccion.zip");
        CifradoRespaldo.descifrarYVerificarArchivo(respaldo.archivo(), zipPlano, CLAVE_RESPALDO);
        String manifestJson = leerEntradaZipTexto(zipPlano, "manifest.json");
        assertFalse(manifestJson.contains("RespaldoSeguroLatch2026"));
        assertFalse(manifestJson.contains("jdbc:"));
        Files.deleteIfExists(zipPlano);

        ResultadoRestauracionDTO restauracion = service.restaurarEnEntornoAislado(
                new RespaldoService.SolicitudRestauracion(
                        propietario,
                        respaldo.archivo(),
                        CLAVE_RESPALDO,
                        new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                        dirAdjuntosDestino,
                        dirCopiaPrevia,
                        true,
                        false,
                        false));

        assertEquals(0, restauracion.hashesDiscordantes());
        assertEquals(2, restauracion.adjuntosVerificados());
        assertTrue(Files.isRegularFile(restauracion.registroExterno()));

        AlmacenAdjuntos almacenRestaurado = new AlmacenAdjuntos(dirAdjuntosDestino);
        assertArrayEquals(PDF_MINIMO,
                almacenRestaurado.leerYVerificar(
                        verificado.manifiesto().adjuntos().get(0).rutaInterna(),
                        verificado.manifiesto().adjuntos().get(0).sha256()));
        assertArrayEquals(generarPngMinimo(),
                almacenRestaurado.leerYVerificar(
                        verificado.manifiesto().adjuntos().get(1).rutaInterna(),
                        verificado.manifiesto().adjuntos().get(1).sha256()));

        SessionFactory factoryAislada = HibernateUtil.construir(propiedadesH2(urlAislada, "validate"));
        try {
            HibernateTransactions txAislada = new HibernateTransactions(factoryAislada);
            Long recibos = txAislada.ejecutar(s -> s.createQuery("select count(r) from Recibo r", Long.class).getSingleResult());
            Long contratos = txAislada.ejecutar(s -> s.createQuery("select count(c) from Contrato c", Long.class).getSingleResult());
            Long auditoriaRespaldo = txOrigen.ejecutar(s -> s.createQuery(
                    "select count(a) from Auditoria a where a.accion = 'RESPALDO_CREADO'", Long.class).getSingleResult());
            assertEquals(1L, recibos);
            assertEquals(1L, contratos);
            assertEquals(1L, auditoriaRespaldo);
        } finally {
            factoryAislada.close();
        }
    }

    @Test
    void asistenteNoPuedeCrearRespaldoRestaurarNiConsultarAlerta() {
        assertThrows(SecurityException.class, () -> service.crearRespaldo(asistente, dirRespaldos, CLAVE_RESPALDO));
        assertThrows(SecurityException.class, () -> service.consultarEstadoAlerta(asistente));
        assertThrows(SecurityException.class, () -> service.restaurarEnEntornoAislado(
                new RespaldoService.SolicitudRestauracion(
                        asistente,
                        tempDir.resolve("inexistente.lbackup"),
                        CLAVE_RESPALDO,
                        new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                        dirAdjuntosDestino,
                        dirCopiaPrevia,
                        true,
                        false,
                        false)));
    }

    @Test
    void variantesCp095ClaveIncorrectaPaqueteAlteradoYHuellaDiscordanteConservanDestino() throws Exception {
        ResultadoRespaldoDTO respaldo = service.crearRespaldo(propietario, dirRespaldos, CLAVE_RESPALDO);

        // Sembrar destino con estado centinela para verificar no sustitución.
        SessionFactory factoryDest = prepararDestinoCentinela("dest-centinela");
        try {
            Path archivoCentinela = dirAdjuntosDestino.resolve("centinela.txt");
            Files.createDirectories(dirAdjuntosDestino);
            Files.writeString(archivoCentinela, "estado-previo-intacto", StandardCharsets.UTF_8);

            // Variante 1: Contraseña incorrecta -> ERR-059
            RestauracionException errClave = assertThrows(RestauracionException.class,
                    () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            propietario, respaldo.archivo(), "ContrasenaIncorrecta999".toCharArray(),
                            new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                            dirAdjuntosDestino, dirCopiaPrevia, true, false, false)));
            assertEquals(RestauracionException.CODIGO_AUTENTICIDAD, errClave.getCodigo());
            assertEquals("estado-previo-intacto", Files.readString(archivoCentinela));

            // Variante 2: Archivo cifrado alterado -> ERR-059
            Path paqueteAlterado = tempDir.resolve("alterado.lbackup");
            byte[] bytes = Files.readAllBytes(respaldo.archivo());
            bytes[bytes.length - 10] ^= 0x55;
            Files.write(paqueteAlterado, bytes);
            RestauracionException errTag = assertThrows(RestauracionException.class,
                    () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            propietario, paqueteAlterado, CLAVE_RESPALDO,
                            new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                            dirAdjuntosDestino, dirCopiaPrevia, true, false, false)));
            assertEquals(RestauracionException.CODIGO_AUTENTICIDAD, errTag.getCodigo());
            assertEquals("estado-previo-intacto", Files.readString(archivoCentinela));

            // Variante 3: Huella SHA-256 de adjunto discordante dentro de paquete re-cifrado -> ERR-055
            Path paqueteHuellaMala = crearPaqueteConAdjuntoAlterado(respaldo.archivo());
            RestauracionException errHash = assertThrows(RestauracionException.class,
                    () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            propietario, paqueteHuellaMala, CLAVE_RESPALDO,
                            new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                            dirAdjuntosDestino, dirCopiaPrevia, true, false, false)));
            assertEquals(RestauracionException.CODIGO_INTEGRIDAD, errHash.getCodigo());
            assertEquals("estado-previo-intacto", Files.readString(archivoCentinela));
            long conteoDest = new HibernateTransactions(factoryDest).ejecutar(
                    s -> s.createQuery("select count(r) from Recibo r", Long.class).getSingleResult());
            assertEquals(0L, conteoDest);
        } finally {
            factoryDest.close();
        }
    }

    @Test
    void rechazaRestauracionConRecibosPosterioresEnDestinoSinRenumerar() throws Exception {
        ResultadoRespaldoDTO respaldo = service.crearRespaldo(propietario, dirRespaldos, CLAVE_RESPALDO);
        SessionFactory factoryDest = prepararDestinoConReciboNumero(5L);
        try {
            RestauracionException err = assertThrows(RestauracionException.class,
                    () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            propietario, respaldo.archivo(), CLAVE_RESPALDO,
                            new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                            dirAdjuntosDestino, dirCopiaPrevia, true, false, false)));
            assertEquals(RestauracionException.CODIGO_CONCILIACION, err.getCodigo());
            MotorBaseRespaldo.EstadoRecibosBase estado = motor.inspeccionarRecibos(
                    new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]));
            assertEquals(5L, estado.ultimoNumeroRecibo());
            assertEquals(6L, estado.siguienteNumeroRecibo());
        } finally {
            factoryDest.close();
        }
    }

    @Test
    void rechazaSinModoMantenimientoOSesionesActivas() throws Exception {
        ResultadoRespaldoDTO respaldo = service.crearRespaldo(propietario, dirRespaldos, CLAVE_RESPALDO);
        assertThrows(RestauracionException.class,
                () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                        propietario, respaldo.archivo(), CLAVE_RESPALDO,
                        new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                        dirAdjuntosDestino, dirCopiaPrevia, false, false, false)));

        sesionesActivasSimuladas.set(2);
        RestauracionException errSesiones = assertThrows(RestauracionException.class,
                () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                        propietario, respaldo.archivo(), CLAVE_RESPALDO,
                        new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                        dirAdjuntosDestino, dirCopiaPrevia, true, false, false)));
        assertEquals(RestauracionException.CODIGO_CONCILIACION, errSesiones.getCodigo());
    }

    @Test
    void falloEnRestauracionDeBaseConservaEstadoPrevioYAdjuntos() throws Exception {
        ResultadoRespaldoDTO respaldo = service.crearRespaldo(propietario, dirRespaldos, CLAVE_RESPALDO);
        SessionFactory factoryDest = prepararDestinoCentinela("centinela-previo");
        try {
            Files.createDirectories(dirAdjuntosDestino);
            Path archivoPrevio = dirAdjuntosDestino.resolve("previo.txt");
            Files.writeString(archivoPrevio, "adjunto-previo-intacto", StandardCharsets.UTF_8);

            motor.setSimularFalloRestauracion(true);
            RestauracionException err = assertThrows(RestauracionException.class,
                    () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            propietario, respaldo.archivo(), CLAVE_RESPALDO,
                            new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]),
                            dirAdjuntosDestino, dirCopiaPrevia, true, false, false)));
            assertEquals(RestauracionException.CODIGO_INTEGRIDAD, err.getCodigo());
            assertEquals("adjunto-previo-intacto", Files.readString(archivoPrevio));
        } finally {
            motor.setSimularFalloRestauracion(false);
            factoryDest.close();
        }
    }

    @Test
    void puertaExclusivaRespetaLockTimeoutCuandoHayEscrituraActiva() throws Exception {
        new ConfiguracionService(txOrigen).registrarParametro(
                propietario, RespaldoService.PARAM_LOCK_TIMEOUT_MS, "300", null, "Timeout prueba");

        CountDownLatch bloqueoTomado = new CountDownLatch(1);
        CountDownLatch liberarBloqueo = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> tareaBloqueo = pool.submit(() -> txOrigen.ejecutar(session -> {
                session.find(PuertaMantenimiento.class, PuertaMantenimiento.ID, LockModeType.PESSIMISTIC_WRITE);
                bloqueoTomado.countDown();
                try {
                    liberarBloqueo.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            assertTrue(bloqueoTomado.await(3, TimeUnit.SECONDS));

            assertThrows(ConflictoDatosException.class,
                    () -> service.crearRespaldo(propietario, dirRespaldos, CLAVE_RESPALDO));
            try (var archivos = Files.list(dirRespaldos)) {
                assertEquals(0L, archivos.count(), "No debe publicarse ningún respaldo si no obtuvo la puerta.");
            }
            liberarBloqueo.countDown();
            tareaBloqueo.get(5, TimeUnit.SECONDS);
        } finally {
            liberarBloqueo.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void rechazaRutasQuePondrianEnRiesgoElPaqueteLaCopiaPreviaOElAlmacenOperativo() throws IOException {
        Path paquete = Files.writeString(dirRespaldos.resolve("paquete.lbackup"), "fixture");
        var destino = new MotorBaseRespaldo.ConexionJdbc(urlAislada, "sa", new char[0]);
        char[] clave = "ClaveSintetica-2026".toCharArray();

        for (Path adjuntos : List.of(dirRespaldos, almacenOrigen.getDirectorioBase())) {
            var error = assertThrows(RestauracionException.class,
                    () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            propietario, paquete, clave, destino, adjuntos, dirCopiaPrevia, true, false, false)));
            assertEquals(RestauracionException.CODIGO_INTEGRIDAD, error.getCodigo());
        }
        var error = assertThrows(RestauracionException.class,
                () -> service.restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                        propietario, paquete, clave, destino, dirAdjuntosDestino,
                        dirAdjuntosDestino.resolve("previa"), true, false, false)));
        assertEquals(RestauracionException.CODIGO_INTEGRIDAD, error.getCodigo());
        assertTrue(Files.isRegularFile(paquete));
        assertFalse(Files.exists(dirAdjuntosDestino));
    }

    @Test
    void alerta24HorasYPoliticaDeRetencionSieteDiariosCuatroSemanales() throws Exception {
        txOrigen.ejecutar(s -> {
            Instant ahoraBd = new RelojService().leer(s).instante();
            s.persist(new Configuracion(
                    RespaldoService.CONFIG_ULTIMO_RESPALDO_UTC,
                    ahoraBd.minus(Duration.ofHours(25).plusMinutes(1)).toString()));
            return null;
        });
        EstadoAlertaRespaldoDTO alerta = service.consultarEstadoAlerta(propietario);
        assertTrue(alerta.alertaVencida());
        assertTrue(alerta.horasDesdeUltimo() >= 25);

        Path dirRet = tempDir.resolve("retencion");
        Files.createDirectories(dirRet);
        List<Path> archivos = new ArrayList<>();
        // 12 días distintos a lo largo de varias semanas
        LocalDate base = LocalDate.of(2026, 11, 25);
        for (int i = 0; i < 12; i++) {
            LocalDate dia = base.minusDays(i * 3L);
            String nombre = String.format("latch-respaldo-%04d%02d%02d-120000.lbackup",
                    dia.getYear(), dia.getMonthValue(), dia.getDayOfMonth());
            Path f = dirRet.resolve(nombre);
            Files.writeString(f, "backup-" + i);
            archivos.add(f);
        }
        Path archivoAjeno = dirRet.resolve("notas-manual.txt");
        Files.writeString(archivoAjeno, "conservar");

        PoliticaRetencionRespaldos.PlanRetencion plan = new PoliticaRetencionRespaldos().evaluar(dirRet);
        assertTrue(plan.conservar().contains(archivos.getFirst()));
        assertTrue(plan.conservar().size() >= 7);
        assertFalse(plan.depurables().isEmpty());
        assertFalse(plan.depurables().contains(archivoAjeno));
        assertFalse(plan.conservar().contains(archivoAjeno));
    }

    private void sembrarDatosCompletos() throws Exception {
        UUID[] ids = new UUID[3];
        txOrigen.ejecutar(session -> {
            Usuario uProp = new Usuario("prop-resp", "hash", Rol.PROPIETARIO);
            Usuario uAsis = new Usuario("asis-resp", "hash", Rol.ASISTENTE);
            session.persist(uProp);
            session.persist(uAsis);
            session.flush();
            propietario = SesionUsuario.paraSesionAutenticada(
                    uProp.getId(), uProp.getNombre(), uProp.getRol(), uProp.getVersion());
            asistente = SesionUsuario.paraSesionAutenticada(
                    uAsis.getId(), uAsis.getNombre(), uAsis.getRol(), uAsis.getVersion());

            session.persist(new PuertaMantenimiento("Puerta de respaldo y adjuntos"));
            session.persist(new ConsecutivoRecibo((short) 1, 2L));

            Propietario propPersona = new Propietario(uProp, "Propietario Prueba", "prop@latch.test", "88880000");
            session.persist(propPersona);

            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Residencial Norte");
            propiedad.setDireccion("Km 4 Carretera Norte");
            propiedad.setPropietario(propPersona);
            session.persist(propiedad);

            Unidad unidad = new Unidad();
            unidad.setPropiedad(propiedad);
            unidad.setCodigo("N-101");
            unidad.setTipo("Apartamento");
            unidad.setEstado("Ocupada");
            unidad.setCanonReferencia(new BigDecimal("350.00"));
            unidad.setMoneda("USD");
            session.persist(unidad);

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Carlos Méndez");
            inquilino.setIdentificacion("001-101090-0001K");
            session.persist(inquilino);
            session.flush();

            Contrato contrato = new Contrato();
            contrato.setUnidad(unidad);
            contrato.setInquilino(inquilino);
            contrato.setInicio(LocalDate.of(2026, 1, 1));
            contrato.setFin(LocalDate.of(2026, 12, 31));
            contrato.setCanon(new BigDecimal("350.00"));
            contrato.setMoneda("USD");
            contrato.setDiaPago(5);
            contrato.setDepositoPactado(new BigDecimal("350.00"));
            contrato.setEstado("Vigente");
            contrato.inicializarGarantiaSeguimiento();
            session.persist(contrato);
            session.flush();

            Cargo cargo = new Cargo();
            cargo.setContratoId(contrato.getId());
            cargo.setTipo("Renta");
            cargo.setPeriodo(LocalDate.of(2026, 11, 1));
            cargo.setFechaEmision(LocalDate.of(2026, 11, 1));
            cargo.setVenceEl(LocalDate.of(2026, 11, 5));
            cargo.setImporte(new BigDecimal("350.00"));
            cargo.setConcepto("Renta noviembre");
            cargo.setEstado("Pagado");
            cargo.setCreadoPor(uProp.getId());
            session.persist(cargo);

            Instant ahora = Instant.now();
            TipoCambio tasa = TipoCambio.autorizada(
                    LocalDate.of(2026, 11, 5), new BigDecimal("36.62430000"),
                    "BCN", ahora, uProp, "Autorizada");
            session.persist(tasa);

            Pago pago = new Pago();
            pago.setContratoId(contrato.getId());
            pago.setFechaPago(LocalDate.of(2026, 11, 5));
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("350.00"));
            pago.setMetodo("Transferencia");
            pago.setReferencia("TRX-001");
            pago.setEstado("Registrado");
            pago.aplicarConversion("USD", new BigDecimal("350.00"), tasa);
            session.persist(pago);
            session.flush();

            session.persist(new Recibo(1L, pago, "Contrato N-101", "Emisor Norte"));
            SolicitudMantenimiento mant = new SolicitudMantenimiento(
                    unidad.getId(), "Revisar llave", "Media", "Técnico",
                    LocalDate.of(2026, 11, 10), ahora, uProp);
            session.persist(mant);
            session.persist(new GastoPropiedad(
                    propiedad.getId(), mant.getId(), LocalDate.of(2026, 11, 11),
                    new BigDecimal("25.00"), "USD", "Repuesto", ahora, uProp));
            session.persist(new ParametroSistema(
                    "dias.aviso", "30", ahora, "Inicial", ahora, uProp));
            session.persist(new Auditoria(uProp, ahora, UUID.randomUUID(), "SIEMBRA_PRUEBA", "test"));

            ids[0] = propiedad.getId();
            ids[1] = unidad.getId();
            ids[2] = contrato.getId();
            return null;
        });

        AdjuntoService adjService = new AdjuntoService(txOrigen, almacenOrigen);
        adjuntoContrato = adjService.adjuntarDocumentoContrato(propietario, ids[2], "contrato.pdf", PDF_MINIMO);
        adjuntoFoto = adjService.adjuntarFotoUnidad(propietario, ids[1], "unidad.png", generarPngMinimo());
        assertNotNull(adjuntoContrato);
        assertNotNull(adjuntoFoto);
    }

    private SessionFactory prepararDestinoCentinela(String nombreUsuario) {
        SessionFactory factoryDest = HibernateUtil.construir(propiedadesH2(urlAislada, "create-drop"));
        new HibernateTransactions(factoryDest).ejecutar(s -> {
            s.persist(new Usuario(nombreUsuario, "hash", Rol.PROPIETARIO));
            s.persist(new ConsecutivoRecibo((short) 1, 1L));
            return null;
        });
        return factoryDest;
    }

    private SessionFactory prepararDestinoConReciboNumero(long numero) {
        SessionFactory factoryDest = HibernateUtil.construir(propiedadesH2(urlAislada, "create-drop"));
        new HibernateTransactions(factoryDest).ejecutar(s -> {
            Usuario u = new Usuario("dest-recibo", "hash", Rol.PROPIETARIO);
            s.persist(u);
            Propiedad p = new Propiedad();
            p.setNombre("P");
            p.setDireccion("D");
            s.persist(p);
            Unidad un = new Unidad();
            un.setPropiedad(p);
            un.setCodigo("D-1");
            un.setCanonReferencia(new BigDecimal("100.00"));
            un.setMoneda("USD");
            s.persist(un);
            Inquilino inq = new Inquilino();
            inq.setNombre("I");
            s.persist(inq);
            s.flush();
            Contrato ct = new Contrato();
            ct.setUnidad(un);
            ct.setInquilino(inq);
            ct.setInicio(LocalDate.of(2026, 1, 1));
            ct.setFin(LocalDate.of(2026, 12, 31));
            ct.setCanon(new BigDecimal("100.00"));
            ct.setMoneda("USD");
            ct.setDiaPago(5);
            ct.setDepositoPactado(BigDecimal.ZERO);
            ct.setEstado("Vigente");
            ct.inicializarGarantiaSeguimiento();
            s.persist(ct);
            s.flush();
            TipoCambio tc = TipoCambio.autorizada(LocalDate.of(2026, 11, 5),
                    new BigDecimal("36.60000000"), "BCN", Instant.now(), u, "ok");
            s.persist(tc);
            Pago pg = new Pago();
            pg.setContratoId(ct.getId());
            pg.setFechaPago(LocalDate.of(2026, 11, 5));
            pg.setMonedaOriginal("USD");
            pg.setMontoOriginal(new BigDecimal("100.00"));
            pg.setMetodo("Efectivo");
            pg.setEstado("Registrado");
            pg.aplicarConversion("USD", new BigDecimal("100.00"), tc);
            s.persist(pg);
            s.flush();
            s.persist(new Recibo(numero, pg, "C", "E"));
            s.persist(new ConsecutivoRecibo((short) 1, numero + 1));
            return null;
        });
        return factoryDest;
    }

    private Path crearPaqueteConAdjuntoAlterado(Path paqueteOriginal) throws Exception {
        Path zipOrig = tempDir.resolve("orig.zip");
        Path zipMod = tempDir.resolve("mod.zip");
        Path paqueteMod = tempDir.resolve("huella-mala.lbackup");
        CifradoRespaldo.descifrarYVerificarArchivo(paqueteOriginal, zipOrig, CLAVE_RESPALDO);

        boolean modificado = false;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipOrig));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipMod))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(new ZipEntry(entry.getName()));
                byte[] contenido = zis.readAllBytes();
                if (!modificado && entry.getName().startsWith("adjuntos/")) {
                    contenido[contenido.length - 1] ^= 0x1F;
                    modificado = true;
                }
                zos.write(contenido);
                zos.closeEntry();
                zis.closeEntry();
            }
        }
        assertTrue(modificado);
        CifradoRespaldo.cifrarArchivo(zipMod, paqueteMod, CLAVE_RESPALDO);
        Files.deleteIfExists(zipOrig);
        Files.deleteIfExists(zipMod);
        return paqueteMod;
    }

    private static String leerEntradaZipTexto(Path zip, String nombreEntrada) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (nombreEntrada.equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }
        throw new AssertionError("Falta la entrada ZIP: " + nombreEntrada);
    }

    private static Properties propiedadesH2(String url, String hbm2ddl) {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url", url);
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", hbm2ddl);
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");
        return p;
    }

    private static byte[] generarPngMinimo() throws Exception {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
