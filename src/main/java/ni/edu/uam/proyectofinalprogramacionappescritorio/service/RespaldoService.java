package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.EstadoAlertaRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ManifiestoRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRestauracionDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoVerificacionRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Configuracion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ConsecutivoRecibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.DocumentoAdjunto;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ParametroSistema;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.PuertaMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Servicio coordinador de respaldo cifrado (AES-256-GCM) y restauración aislada (M12 / RN-29 / RNF-036..038).
 */
public final class RespaldoService {
    public static final int FORMATO_VERSION = 1;
    public static final String ESQUEMA_VERSION_ACTUAL = "V16";
    public static final String PARAM_LOCK_TIMEOUT_MS = "respaldo_lock_timeout_ms";
    public static final int LOCK_TIMEOUT_DEFECTO_MS = 3000;
    public static final String CONFIG_ULTIMO_RESPALDO_UTC = "respaldo.ultimo_utc";
    public static final long UMBRAL_ALERTA_HORAS = 24L;
    public static final String EXTENSION_RESPALDO = ".lbackup";

    private static final long MIN_ESPACIO_LIBRE_BYTES = 1024L * 1024L;
    private static final DateTimeFormatter FORMATO_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(RelojService.ZONA_OPERATIVA);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final HibernateTransactions transactions;
    private final Supplier<AlmacenAdjuntos> almacenProveedor;
    private final Supplier<MotorBaseRespaldo> motorProveedor;
    private final Clock relojLocal;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public RespaldoService(HibernateTransactions transactions) {
        this(transactions, AlmacenAdjuntos::porDefecto, MotorPostgresRespaldo::desdeConfiguracion, Clock.systemUTC());
    }

    public RespaldoService(HibernateTransactions transactions, AlmacenAdjuntos almacen,
                           MotorBaseRespaldo motor, Clock relojLocal) {
        this(
                transactions,
                () -> Objects.requireNonNull(almacen, "Falta el almacén de adjuntos."),
                () -> Objects.requireNonNull(motor, "Falta el motor de base de datos."),
                relojLocal);
    }

    private RespaldoService(HibernateTransactions transactions, Supplier<AlmacenAdjuntos> almacenProveedor,
                            Supplier<MotorBaseRespaldo> motorProveedor, Clock relojLocal) {
        this.transactions = Objects.requireNonNull(transactions, "Faltan las transacciones.");
        this.almacenProveedor = Objects.requireNonNull(almacenProveedor);
        this.motorProveedor = Objects.requireNonNull(motorProveedor);
        this.relojLocal = Objects.requireNonNull(relojLocal, "Falta el reloj local.");
    }

    public ResultadoRespaldoDTO crearRespaldo(SesionUsuario identidad, Path directorioDestino,
                                              char[] contrasenaRespaldo) {
        Objects.requireNonNull(directorioDestino, "Selecciona una carpeta de destino para el respaldo.");
        CifradoRespaldo.validarContrasenaRespaldo(contrasenaRespaldo);
        char[] claveCopia = contrasenaRespaldo.clone();

        long inicioNs = System.nanoTime();
        Path dirDestinoNorm = directorioDestino.toAbsolutePath().normalize();
        prepararYValidarDestino(dirDestinoNorm);

        AlmacenAdjuntos almacen = almacenProveedor.get();
        MotorBaseRespaldo motor = motorProveedor.get();

        Path trabajoTemporal = null;
        Path archivoTemporalCifrado = null;
        try {
            trabajoTemporal = Files.createTempDirectory("latch-respaldo-");
            Path dirTrabajo = trabajoTemporal;
            Path dumpTemporal = dirTrabajo.resolve("latch.dump");
            Path zipTemporal = dirTrabajo.resolve("paquete.zip");
            archivoTemporalCifrado = Files.createTempFile(dirDestinoNorm, ".latch-respaldo-", ".tmp");
            Path tempCifrado = archivoTemporalCifrado;

            ResultadoInternoCreado resultado = transactions.ejecutar(session -> {
                Usuario actor = permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
                RelojService.TiempoOperativo tiempoPrevio = reloj.leer(session);
                int lockTimeoutMs = resolverLockTimeoutMs(session, tiempoPrevio.instante());
                adquirirPuertaExclusiva(session, lockTimeoutMs);

                RelojService.TiempoOperativo corte = reloj.leer(session);
                motor.exportarBase(dumpTemporal);
                try {
                    if (!Files.isRegularFile(dumpTemporal) || Files.size(dumpTemporal) <= 0L) {
                        throw RespaldoException.integridad("El volcado de base de datos resultó vacío.");
                    }

                    String shaDump = CifradoRespaldo.calcularSha256(dumpTemporal);
                    long tamanoDump = Files.size(dumpTemporal);
                    Map<String, Long> conteos = obtenerConteosDesdeSesion(session);
                    MotorBaseRespaldo.EstadoRecibosBase estadoRecibos = obtenerRecibosDesdeSesion(session);

                    List<DocumentoAdjunto> adjuntosBd = session.createQuery(
                            "from DocumentoAdjunto d order by d.creadoEn asc, d.id asc",
                            DocumentoAdjunto.class).getResultList();
                    List<ManifiestoRespaldoDTO.EntradaAdjunto> entradasAdjuntos = new ArrayList<>();
                    List<Path> archivosAdjuntos = new ArrayList<>();
                    for (DocumentoAdjunto adj : adjuntosBd) {
                        Path archivoAdjunto = almacen.getDirectorioBase().resolve(adj.getRutaInterna()).normalize();
                        if (!Files.isRegularFile(archivoAdjunto) || !Files.isReadable(archivoAdjunto)) {
                            if (adj.isActivo()) {
                                throw RespaldoException.integridad(
                                        "Falta el archivo adjunto activo " + adj.getRutaInterna() + ".");
                            }
                            continue;
                        }
                        String shaActual = CifradoRespaldo.calcularSha256(archivoAdjunto);
                        if (!shaActual.equalsIgnoreCase(adj.getSha256())) {
                            throw RespaldoException.integridad(
                                    "La huella SHA-256 del adjunto " + adj.getRutaInterna() + " no coincide con la BD.");
                        }
                        entradasAdjuntos.add(new ManifiestoRespaldoDTO.EntradaAdjunto(
                                "adjuntos/" + adj.getRutaInterna(),
                                adj.getRutaInterna(),
                                Files.size(archivoAdjunto),
                                shaActual,
                                adj.isActivo()));
                        archivosAdjuntos.add(archivoAdjunto);
                    }

                    ManifiestoRespaldoDTO manifiesto = new ManifiestoRespaldoDTO(
                            FORMATO_VERSION,
                            "Latch",
                            ESQUEMA_VERSION_ACTUAL,
                            obtenerRevisionCodigo(),
                            motor.identificadorMotor(),
                            corte.instante().toString(),
                            RelojService.presentar(corte.instante()),
                            actor.getNombre(),
                            estadoRecibos.ultimoNumeroRecibo(),
                            estadoRecibos.siguienteNumeroRecibo(),
                            conteos,
                            new ManifiestoRespaldoDTO.EntradaArchivo("database/latch.dump", tamanoDump, shaDump),
                            List.copyOf(entradasAdjuntos));

                    escribirZipPaquete(zipTemporal, dumpTemporal, manifiesto, entradasAdjuntos, archivosAdjuntos);
                    CifradoRespaldo.cifrarArchivo(zipTemporal, tempCifrado, claveCopia);
                    eliminarSilenciosamente(zipTemporal);
                    eliminarSilenciosamente(dumpTemporal);

                    // Verificación completa antes de publicar el archivo final (RN-29).
                    ResultadoVerificacionRespaldoDTO verificacion = verificarPaquete(tempCifrado, claveCopia);

                    Path archivoFinal = resolverRutaFinal(dirDestinoNorm, corte.instante());
                    moverAtomicamente(tempCifrado, archivoFinal);
                    String shaPaquete = verificacion.sha256Paquete();
                    long tamanoPaquete = Files.size(archivoFinal);

                    registrarUltimoRespaldo(session, corte.instante());
                    auditoria.registrar(session, actor, UUID.randomUUID(), "RESPALDO_CREADO",
                            "archivo:" + archivoFinal.getFileName() + ";adjuntos:" + entradasAdjuntos.size());

                    return new ResultadoInternoCreado(
                            archivoFinal, tamanoPaquete, shaPaquete, corte.instante(),
                            entradasAdjuntos.size(), estadoRecibos.ultimoNumeroRecibo());
                } catch (IOException e) {
                    throw RespaldoException.operativo("ERR-055: Error de E/S al empaquetar el respaldo.", e);
                }
            });

            long duracionMs = Duration.ofNanos(System.nanoTime() - inicioNs).toMillis();
            return new ResultadoRespaldoDTO(
                    resultado.archivo(),
                    resultado.tamanoBytes(),
                    resultado.sha256Paquete(),
                    resultado.corteUtc(),
                    resultado.cantidadAdjuntos(),
                    resultado.ultimoNumeroRecibo(),
                    duracionMs);
        } catch (RespaldoException | RestauracionException | ConflictoDatosException
                 | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw RespaldoException.operativo("ERR-055: No se pudo generar el respaldo cifrado.", e);
        } finally {
            Arrays.fill(claveCopia, '\0');
            eliminarSilenciosamente(archivoTemporalCifrado);
            eliminarDirectorioRecursivo(trabajoTemporal);
        }
    }

    public ResultadoVerificacionRespaldoDTO verificarPaquete(Path archivoRespaldo, char[] contrasenaRespaldo) {
        Objects.requireNonNull(archivoRespaldo, "Selecciona el archivo de respaldo a verificar.");
        if (!Files.isRegularFile(archivoRespaldo) || !Files.isReadable(archivoRespaldo)) {
            throw RestauracionException.integridadInvalida("El archivo de respaldo no existe o no es legible.");
        }
        if (contrasenaRespaldo == null || contrasenaRespaldo.length == 0) {
            throw RestauracionException.autenticidadInvalida(null);
        }

        long inicioNs = System.nanoTime();
        char[] claveCopia = contrasenaRespaldo.clone();
        Path trabajo = null;
        try {
            String shaPaquete = CifradoRespaldo.calcularSha256(archivoRespaldo);
            trabajo = Files.createTempDirectory("latch-verify-");
            Path zipPlano = trabajo.resolve("paquete.zip");
            Path extraido = trabajo.resolve("extraido");
            Files.createDirectories(extraido);

            CifradoRespaldo.descifrarYVerificarArchivo(archivoRespaldo, zipPlano, claveCopia);
            extraerZipSeguro(zipPlano, extraido);
            eliminarSilenciosamente(zipPlano);

            ManifiestoRespaldoDTO manifiesto = leerYValidarManifiesto(extraido);
            validarContenidoContraManifiesto(extraido, manifiesto);

            long duracionMs = Duration.ofNanos(System.nanoTime() - inicioNs).toMillis();
            return new ResultadoVerificacionRespaldoDTO(
                    manifiesto, shaPaquete, manifiesto.adjuntos().size(), duracionMs);
        } catch (RestauracionException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw RestauracionException.integridadInvalida("No fue posible verificar el paquete de respaldo.");
        } finally {
            Arrays.fill(claveCopia, '\0');
            eliminarDirectorioRecursivo(trabajo);
        }
    }

    public ResultadoRestauracionDTO restaurarEnEntornoAislado(SolicitudRestauracion solicitud) {
        Objects.requireNonNull(solicitud, "Falta la solicitud de restauración.");
        if (!solicitud.modoMantenimientoActivo()) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_CONCILIACION,
                    "ERR-058: La restauración exige activar el modo de mantenimiento sin sesiones operativas.",
                    null);
        }
        if (solicitud.identidad() != null) {
            transactions.ejecutar(session -> {
                permisos.exigir(session, solicitud.identidad(), Permiso.CAMBIAR_CONFIGURACION);
                return null;
            });
        }
        MotorBaseRespaldo.ConexionJdbc destinoJdbc = MotorBaseRespaldo.validarNoNula(solicitud.destinoJdbc());
        if (!solicitud.permitirDestinoOperativo() && MotorPostgresRespaldo.esBaseProtegida(destinoJdbc.url())) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: La restauración debe validarse primero en una base aislada, nunca directamente sobre latch_db o latch_demo.",
                    null);
        }

        Objects.requireNonNull(solicitud.archivoRespaldo(), "Selecciona el archivo de respaldo.");
        Objects.requireNonNull(solicitud.directorioAdjuntosDestino(), "Indica el directorio de adjuntos de destino.");
        if (!Files.isRegularFile(solicitud.archivoRespaldo()) || !Files.isReadable(solicitud.archivoRespaldo())) {
            throw RestauracionException.integridadInvalida("El archivo de respaldo no existe o no es legible.");
        }
        if (solicitud.contrasenaRespaldo() == null || solicitud.contrasenaRespaldo().length == 0) {
            throw RestauracionException.autenticidadInvalida(null);
        }

        long inicioNs = System.nanoTime();
        char[] claveCopia = solicitud.contrasenaRespaldo().clone();
        MotorBaseRespaldo motor = motorProveedor.get();

        Path dirAdjuntosDestino = solicitud.directorioAdjuntosDestino().toAbsolutePath().normalize();
        Path dirCopiaBaseRaiz = solicitud.directorioCopiaPrevia() != null
                ? solicitud.directorioCopiaPrevia().toAbsolutePath().normalize()
                : dirAdjuntosDestino.resolveSibling("latch-copia-previa-" + System.currentTimeMillis());

        Path trabajo = null;
        Path stagingAdjuntos = null;
        try {
            validarRutasRestauracion(solicitud.archivoRespaldo().toAbsolutePath().normalize(),
                    dirAdjuntosDestino, dirCopiaBaseRaiz, solicitud.permitirDestinoOperativo());
            trabajo = Files.createTempDirectory("latch-restore-");
            Path zipPlano = trabajo.resolve("paquete.zip");
            Path extraido = trabajo.resolve("extraido");
            Files.createDirectories(extraido);

            String shaPaquete = CifradoRespaldo.calcularSha256(solicitud.archivoRespaldo());
            CifradoRespaldo.descifrarYVerificarArchivo(solicitud.archivoRespaldo(), zipPlano, claveCopia);
            extraerZipSeguro(zipPlano, extraido);
            eliminarSilenciosamente(zipPlano);

            ManifiestoRespaldoDTO manifiesto = leerYValidarManifiesto(extraido);
            validarContenidoContraManifiesto(extraido, manifiesto);

            int sesionesActivas = motor.contarSesionesOperativasActivas(destinoJdbc);
            if (sesionesActivas > 0) {
                throw RestauracionException.operativo(
                        RestauracionException.CODIGO_CONCILIACION,
                        "ERR-058: Hay " + sesionesActivas
                                + " sesiones activas en la base destino. Cierra los clientes antes de restaurar.",
                        null);
            }

            MotorBaseRespaldo.EstadoRecibosBase recibosDestino = motor.inspeccionarRecibos(destinoJdbc);
            if ((recibosDestino.ultimoNumeroRecibo() > manifiesto.ultimoNumeroRecibo()
                    || recibosDestino.siguienteNumeroRecibo() > manifiesto.siguienteNumeroRecibo())
                    && !solicitud.permitirConciliacionManualConfirmada()) {
                throw RestauracionException.conciliacionRequerida(
                        manifiesto.ultimoNumeroRecibo(), recibosDestino.ultimoNumeroRecibo());
            }

            // Crear copia previa obligatoria antes de tocar destino.
            Path copiaDir = dirCopiaBaseRaiz.resolve("previa-" + FORMATO_ARCHIVO.format(relojLocal.instant()));
            Files.createDirectories(copiaDir);
            Path copiaPreviaBase = null;
            Map<String, Long> conteosPrevios = motor.contarTablas(destinoJdbc);
            boolean destinoTieneDatos = conteosPrevios.values().stream().anyMatch(v -> v > 0)
                    || recibosDestino.ultimoNumeroRecibo() > 0;
            if (destinoTieneDatos) {
                copiaPreviaBase = copiaDir.resolve("base-previa.dump");
                motor.exportarBaseDestino(destinoJdbc, copiaPreviaBase);
            }
            Path copiaPreviaAdjuntos = copiaDir.resolve("adjuntos-previos");
            Files.createDirectories(copiaPreviaAdjuntos);
            if (Files.isDirectory(dirAdjuntosDestino)) {
                copiarDirectorio(dirAdjuntosDestino, copiaPreviaAdjuntos);
            }

            // Preparar staging de adjuntos verificando SHA-256 antes de sustituir.
            Path padreAdjuntos = dirAdjuntosDestino.getParent() != null
                    ? dirAdjuntosDestino.getParent() : dirAdjuntosDestino;
            Files.createDirectories(padreAdjuntos);
            stagingAdjuntos = Files.createTempDirectory(padreAdjuntos, ".latch-adjuntos-staging-");
            int adjuntosVerificados = 0;
            int hashesDiscordantes = 0;
            for (ManifiestoRespaldoDTO.EntradaAdjunto entry : manifiesto.adjuntos()) {
                Path origenAdj = resolverEnBase(extraido, entry.ruta());
                Path destinoStaging = resolverEnBase(stagingAdjuntos, entry.rutaInterna());
                Files.copy(origenAdj, destinoStaging, StandardCopyOption.REPLACE_EXISTING);
                String shaCopiado = CifradoRespaldo.calcularSha256(destinoStaging);
                if (!shaCopiado.equalsIgnoreCase(entry.sha256())) {
                    hashesDiscordantes++;
                } else {
                    adjuntosVerificados++;
                }
            }
            if (hashesDiscordantes > 0) {
                throw RestauracionException.integridadInvalida(
                        "Se detectaron " + hashesDiscordantes + " archivos adjuntos con huella discordante.");
            }

            // Restaurar base de datos y validar esquema/conteos antes de tocar adjuntos destino.
            Path dumpExtraido = resolverEnBase(extraido, manifiesto.dump().ruta());
            try {
                motor.restaurarEnBaseAislada(dumpExtraido, destinoJdbc);
                motor.validarEsquemaRestaurado(destinoJdbc);
                validarConteosPostRestauracion(motor.contarTablas(destinoJdbc), manifiesto.conteosTablas());
            } catch (RuntimeException falloBd) {
                if (copiaPreviaBase != null && Files.isRegularFile(copiaPreviaBase)) {
                    try {
                        motor.restaurarEnBaseAislada(copiaPreviaBase, destinoJdbc);
                    } catch (RuntimeException rollbackError) {
                        falloBd.addSuppressed(rollbackError);
                    }
                }
                if (falloBd instanceof RestauracionException re) {
                    throw re;
                }
                throw RestauracionException.operativo(
                        RestauracionException.CODIGO_INTEGRIDAD,
                        "ERR-055: Falló la restauración de la base aislada; se conservó el estado previo.",
                        falloBd);
            }

            intercambiarDirectorioAdjuntos(stagingAdjuntos, dirAdjuntosDestino, copiaPreviaBase, motor, destinoJdbc);
            stagingAdjuntos = null;

            Path registroExterno = escribirRegistroExternoRestauracion(
                    copiaDir, shaPaquete, manifiesto, adjuntosVerificados,
                    copiaPreviaBase, copiaPreviaAdjuntos, dirAdjuntosDestino);

            long duracionMs = Duration.ofNanos(System.nanoTime() - inicioNs).toMillis();
            return new ResultadoRestauracionDTO(
                    manifiesto,
                    copiaPreviaBase,
                    copiaPreviaAdjuntos,
                    dirAdjuntosDestino,
                    registroExterno,
                    adjuntosVerificados,
                    hashesDiscordantes,
                    duracionMs);
        } catch (RestauracionException | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw RestauracionException.operativo(
                    RestauracionException.CODIGO_INTEGRIDAD,
                    "ERR-055: No se pudo completar la restauración en entorno aislado.", e);
        } finally {
            Arrays.fill(claveCopia, '\0');
            eliminarDirectorioRecursivo(stagingAdjuntos);
            eliminarDirectorioRecursivo(trabajo);
        }
    }

    private void validarRutasRestauracion(Path paquete, Path adjuntos, Path copiaPrevia,
                                          boolean permitirDestinoOperativo) throws IOException {
        Path adjuntosReal = resolverRutaRealPotencial(adjuntos);
        if (paquete.startsWith(adjuntos) || paquete.toRealPath().startsWith(adjuntosReal)
                || copiaPrevia.startsWith(adjuntos) || resolverRutaRealPotencial(copiaPrevia).startsWith(adjuntosReal)) {
            throw RestauracionException.integridadInvalida(
                    "El respaldo y la copia previa deben estar fuera del directorio de adjuntos que se sustituirá.");
        }
        if (!permitirDestinoOperativo) {
            Path operativos = resolverRutaRealPotencial(almacenProveedor.get().getDirectorioBase());
            if (adjuntosReal.startsWith(operativos) || operativos.startsWith(adjuntosReal)) {
                throw RestauracionException.integridadInvalida(
                        "El directorio de adjuntos de destino debe estar separado del almacén operativo.");
            }
        }
    }

    private static Path resolverRutaRealPotencial(Path ruta) throws IOException {
        Path existente = ruta;
        while (!Files.exists(existente, LinkOption.NOFOLLOW_LINKS)) {
            existente = existente.getParent();
            if (existente == null) throw new IOException("No se pudo resolver el directorio de restauración.");
        }
        return existente.toRealPath().resolve(existente.relativize(ruta));
    }

    public EstadoAlertaRespaldoDTO consultarEstadoAlerta(SesionUsuario identidad) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            Instant ahora = reloj.leer(session).instante();
            Optional<Instant> ultimo = buscarUltimoRespaldoUtc(session);
            if (ultimo.isEmpty()) {
                return new EstadoAlertaRespaldoDTO(
                        null, -1L, true,
                        "Alerta (RNF-038): No existe ningún respaldo registrado. Realiza un respaldo hoy.");
            }
            Instant instanteUltimo = ultimo.get();
            long horas = Math.max(0L, Duration.between(instanteUltimo, ahora).toHours());
            boolean vencida = Duration.between(instanteUltimo, ahora).compareTo(Duration.ofHours(UMBRAL_ALERTA_HORAS)) >= 0;
            String mensaje = vencida
                    ? "Alerta (RNF-038): El último respaldo tiene " + horas + " h de antigüedad (supera 24 h)."
                    : "Al día: Último respaldo hace " + horas + " h (" + RelojService.presentar(instanteUltimo) + ").";
            return new EstadoAlertaRespaldoDTO(instanteUltimo, horas, vencida, mensaje);
        });
    }

    private Optional<Instant> buscarUltimoRespaldoUtc(Session session) {
        Configuracion config = session.find(Configuracion.class, CONFIG_ULTIMO_RESPALDO_UTC);
        if (config != null && config.getValor() != null && !config.getValor().isBlank()) {
            try {
                return Optional.of(Instant.parse(config.getValor().strip()));
            } catch (DateTimeParseException ignored) {
                // Si hubiera un valor corrupto, intenta recuperar desde auditoría.
            }
        }
        return session.createQuery("""
                        select max(a.instante) from Auditoria a
                        where a.accion = 'RESPALDO_CREADO'
                        """, Instant.class)
                .uniqueResultOptional()
                .map(Optional::ofNullable)
                .flatMap(opt -> opt);
    }

    private static void registrarUltimoRespaldo(Session session, Instant corteUtc) {
        Configuracion config = session.find(Configuracion.class, CONFIG_ULTIMO_RESPALDO_UTC);
        if (config == null) {
            session.persist(new Configuracion(CONFIG_ULTIMO_RESPALDO_UTC, corteUtc.toString()));
        } else {
            config.cambiarValor(corteUtc.toString());
        }
    }

    private static int resolverLockTimeoutMs(Session session, Instant ahora) {
        Optional<ParametroSistema> param = ConfiguracionService.buscarParametroVigente(
                session, PARAM_LOCK_TIMEOUT_MS, ahora);
        if (param.isEmpty()) {
            return LOCK_TIMEOUT_DEFECTO_MS;
        }
        try {
            int valor = Integer.parseInt(param.get().getValor().strip());
            return Math.clamp(valor, 200, 60_000);
        } catch (NumberFormatException e) {
            return LOCK_TIMEOUT_DEFECTO_MS;
        }
    }

    private static void adquirirPuertaExclusiva(Session session, int lockTimeoutMs) {
        PuertaMantenimiento puerta = session.find(PuertaMantenimiento.class, PuertaMantenimiento.ID);
        if (puerta == null) {
            session.persist(new PuertaMantenimiento("Puerta de respaldo y adjuntos"));
            session.flush();
        }
        session.doWork(conn -> {
            String producto = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            try (Statement st = conn.createStatement()) {
                if (producto.contains("postgres")) {
                    st.execute("SET LOCAL lock_timeout = '" + lockTimeoutMs + "ms'");
                } else if (producto.contains("h2")) {
                    st.execute("SET LOCK_TIMEOUT " + lockTimeoutMs);
                }
            }
        });
        try {
            session.createNativeQuery(
                            "SELECT id FROM puerta_mantenimiento WHERE id = :id FOR UPDATE", UUID.class)
                    .setParameter("id", PuertaMantenimiento.ID)
                    .uniqueResult();
        } catch (RuntimeException e) {
            if (esTimeoutDeBloqueo(e)) {
                throw new ConflictoDatosException(
                        "Hay escrituras activas en curso; no se pudo obtener la puerta de respaldo en "
                                + lockTimeoutMs + " ms.");
            }
            throw e;
        }
    }

    private static boolean esTimeoutDeBloqueo(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof LockTimeoutException || actual instanceof PessimisticLockException) {
                return true;
            }
            if (actual instanceof SQLException sql) {
                String state = sql.getSQLState();
                if ("55P03".equals(state) || "HYT00".equals(state) || sql.getErrorCode() == 50200) {
                    return true;
                }
            }
            if (actual instanceof PersistenceException && actual.getMessage() != null
                    && actual.getMessage().toLowerCase(Locale.ROOT).contains("lock")) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Long> obtenerConteosDesdeSesion(Session session) {
        Map<String, Long> conteos = new LinkedHashMap<>();
        for (String tabla : MotorBaseRespaldo.TABLAS_VERIFICADAS) {
            Long n = session.createNativeQuery("SELECT COUNT(*) FROM " + tabla, Long.class)
                    .getSingleResult();
            conteos.put(tabla, n == null ? 0L : n);
        }
        return Map.copyOf(conteos);
    }

    private static MotorBaseRespaldo.EstadoRecibosBase obtenerRecibosDesdeSesion(Session session) {
        Long maxRecibo = session.createNativeQuery(
                "SELECT COALESCE(MAX(numero), 0) FROM recibo", Long.class).getSingleResult();
        ConsecutivoRecibo consecutivo = session.find(ConsecutivoRecibo.class, (short) 1, LockModeType.PESSIMISTIC_READ);
        long siguiente = consecutivo == null ? 1L : Math.max(1L, consecutivo.getSiguienteNumero());
        return new MotorBaseRespaldo.EstadoRecibosBase(maxRecibo == null ? 0L : maxRecibo, siguiente);
    }

    private static void prepararYValidarDestino(Path dirDestino) {
        try {
            Files.createDirectories(dirDestino);
            if (!Files.isDirectory(dirDestino) || !Files.isWritable(dirDestino)) {
                throw RespaldoException.operativo(
                        "ERR-055: La carpeta seleccionada para el respaldo no es escribible.", null);
            }
            long espacioLibre = Files.getFileStore(dirDestino).getUsableSpace();
            if (espacioLibre < MIN_ESPACIO_LIBRE_BYTES) {
                throw RespaldoException.operativo(
                        "ERR-055: Espacio insuficiente en el destino del respaldo.", null);
            }
        } catch (IOException e) {
            throw RespaldoException.operativo(
                    "ERR-055: No se pudo validar la carpeta de destino del respaldo.", e);
        }
    }

    private static void escribirZipPaquete(Path zipDestino, Path dump, ManifiestoRespaldoDTO manifiesto,
                                           List<ManifiestoRespaldoDTO.EntradaAdjunto> entradas,
                                           List<Path> archivosAdjuntos) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipDestino)))) {
            agregarArchivoZip(zos, "database/latch.dump", dump);
            for (int i = 0; i < entradas.size(); i++) {
                agregarArchivoZip(zos, entradas.get(i).ruta(), archivosAdjuntos.get(i));
            }
            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(GSON.toJson(manifiesto).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private static void agregarArchivoZip(ZipOutputStream zos, String rutaZip, Path origen) throws IOException {
        zos.putNextEntry(new ZipEntry(rutaZip));
        try (InputStream in = new BufferedInputStream(Files.newInputStream(origen))) {
            in.transferTo(zos);
        }
        zos.closeEntry();
    }

    private static void extraerZipSeguro(Path zipPlano, Path destinoDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPlano)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String nombre = entry.getName().replace('\\', '/');
                if (nombre.isBlank() || nombre.startsWith("/") || nombre.contains("..")) {
                    throw RestauracionException.integridadInvalida("Entrada ZIP inválida: " + nombre);
                }
                Path salida = resolverEnBase(destinoDir, nombre);
                if (entry.isDirectory()) {
                    Files.createDirectories(salida);
                } else {
                    if (salida.getParent() != null) {
                        Files.createDirectories(salida.getParent());
                    }
                    try (var out = new BufferedOutputStream(Files.newOutputStream(salida))) {
                        zis.transferTo(out);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static ManifiestoRespaldoDTO leerYValidarManifiesto(Path extraidoDir) {
        Path rutaManifiesto = extraidoDir.resolve("manifest.json");
        if (!Files.isRegularFile(rutaManifiesto)) {
            throw RestauracionException.integridadInvalida("Falta manifest.json dentro del paquete.");
        }
        try {
            String json = Files.readString(rutaManifiesto, StandardCharsets.UTF_8);
            ManifiestoRespaldoDTO m = GSON.fromJson(json, ManifiestoRespaldoDTO.class);
            if (m == null || m.formatoVersion() != FORMATO_VERSION
                    || !ESQUEMA_VERSION_ACTUAL.equals(m.esquemaVersion())
                    || m.dump() == null || m.adjuntos() == null || m.conteosTablas() == null) {
                throw RestauracionException.integridadInvalida(
                        "El manifiesto no es compatible con la versión de esquema " + ESQUEMA_VERSION_ACTUAL + ".");
            }
            return m;
        } catch (IOException | JsonParseException e) {
            throw RestauracionException.integridadInvalida("El manifiesto del respaldo está dañado.");
        }
    }

    private static void validarContenidoContraManifiesto(Path extraidoDir, ManifiestoRespaldoDTO manifiesto)
            throws IOException {
        Path dump = resolverEnBase(extraidoDir, manifiesto.dump().ruta());
        if (!Files.isRegularFile(dump) || Files.size(dump) != manifiesto.dump().tamanoBytes()) {
            throw RestauracionException.integridadInvalida("El volcado de base de datos falta o cambió de tamaño.");
        }
        String shaDumpActual = CifradoRespaldo.calcularSha256(dump);
        if (!shaDumpActual.equalsIgnoreCase(manifiesto.dump().sha256())) {
            throw RestauracionException.integridadInvalida("La huella SHA-256 del volcado de BD no coincide.");
        }

        for (ManifiestoRespaldoDTO.EntradaAdjunto adj : manifiesto.adjuntos()) {
            if (adj.rutaInterna() == null || !adj.rutaInterna().matches("^[0-9a-f-]{36}\\.(pdf|png|jpg)$")) {
                throw RestauracionException.integridadInvalida("Ruta interna de adjunto inválida en manifiesto.");
            }
            Path archivo = resolverEnBase(extraidoDir, adj.ruta());
            if (!Files.isRegularFile(archivo) || Files.size(archivo) != adj.tamanoBytes()) {
                throw RestauracionException.integridadInvalida("Falta o cambió de tamaño el adjunto " + adj.rutaInterna() + ".");
            }
            String shaActual = CifradoRespaldo.calcularSha256(archivo);
            if (!shaActual.equalsIgnoreCase(adj.sha256())) {
                throw RestauracionException.integridadInvalida(
                        "La huella SHA-256 del adjunto " + adj.rutaInterna() + " no coincide.");
            }
        }
    }

    private static void validarConteosPostRestauracion(Map<String, Long> reales, Map<String, Long> esperados) {
        for (Map.Entry<String, Long> entrada : esperados.entrySet()) {
            long real = reales.getOrDefault(entrada.getKey(), -1L);
            if (real != entrada.getValue()) {
                throw RestauracionException.integridadInvalida(
                        "Conteo discordante en tabla " + entrada.getKey()
                                + ": esperado " + entrada.getValue() + ", encontrado " + real + ".");
            }
        }
    }

    private static void intercambiarDirectorioAdjuntos(Path stagingAdjuntos, Path destinoAdjuntos,
                                                     Path copiaPreviaBase, MotorBaseRespaldo motor,
                                                     MotorBaseRespaldo.ConexionJdbc destinoJdbc) throws IOException {
        Path respaldoTemporalDestino = Files.exists(destinoAdjuntos)
                ? destinoAdjuntos.resolveSibling(".latch-adjuntos-anterior-" + UUID.randomUUID())
                : null;
        if (respaldoTemporalDestino != null) {
            moverAtomicamente(destinoAdjuntos, respaldoTemporalDestino);
        }
        try {
            moverAtomicamente(stagingAdjuntos, destinoAdjuntos);
            eliminarDirectorioRecursivo(respaldoTemporalDestino);
        } catch (IOException | RuntimeException e) {
            if (respaldoTemporalDestino != null && Files.exists(respaldoTemporalDestino) && !Files.exists(destinoAdjuntos)) {
                try {
                    moverAtomicamente(respaldoTemporalDestino, destinoAdjuntos);
                } catch (IOException restoreDirErr) {
                    e.addSuppressed(restoreDirErr);
                }
            }
            if (copiaPreviaBase != null && Files.isRegularFile(copiaPreviaBase)) {
                try {
                    motor.restaurarEnBaseAislada(copiaPreviaBase, destinoJdbc);
                } catch (RuntimeException restoreDbErr) {
                    e.addSuppressed(restoreDbErr);
                }
            }
            throw e;
        }
    }

    private Path escribirRegistroExternoRestauracion(Path copiaDir, String shaPaquete,
                                                     ManifiestoRespaldoDTO manifiesto, int adjuntosVerificados,
                                                     Path copiaPreviaBase, Path copiaPreviaAdjuntos,
                                                     Path destinoAdjuntos) throws IOException {
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("restauradoEnUtc", relojLocal.instant().toString());
        registro.put("sha256Paquete", shaPaquete);
        registro.put("corteRespaldoUtc", manifiesto.corteUtc());
        registro.put("esquemaVersion", manifiesto.esquemaVersion());
        registro.put("ultimoNumeroRecibo", manifiesto.ultimoNumeroRecibo());
        registro.put("adjuntosVerificados", adjuntosVerificados);
        registro.put("hashesDiscordantes", 0);
        registro.put("copiaPreviaBase", copiaPreviaBase == null ? null : copiaPreviaBase.toString());
        registro.put("copiaPreviaAdjuntos", copiaPreviaAdjuntos.toString());
        registro.put("directorioAdjuntosRestaurado", destinoAdjuntos.toString());

        Path archivo = copiaDir.resolve("restauracion-" + FORMATO_ARCHIVO.format(relojLocal.instant()) + ".json");
        Files.writeString(archivo, GSON.toJson(registro), StandardCharsets.UTF_8);
        return archivo;
    }

    private static Path resolverRutaFinal(Path dirDestino, Instant corteUtc) {
        String base = "latch-respaldo-" + FORMATO_ARCHIVO.format(corteUtc);
        Path candidato = dirDestino.resolve(base + EXTENSION_RESPALDO);
        if (!Files.exists(candidato)) {
            return candidato;
        }
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        return dirDestino.resolve(base + "-" + sufijo + EXTENSION_RESPALDO);
    }

    private static Path resolverEnBase(Path base, String relativo) {
        Path normalizadoBase = base.toAbsolutePath().normalize();
        Path resuelto = normalizadoBase.resolve(relativo).normalize();
        if (!resuelto.startsWith(normalizadoBase)) {
            throw RestauracionException.integridadInvalida("Ruta fuera del directorio autorizado: " + relativo);
        }
        return resuelto;
    }

    private static void moverAtomicamente(Path origen, Path destino) throws IOException {
        try {
            Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copiarDirectorio(Path origen, Path destino) throws IOException {
        try (var flujo = Files.walk(origen)) {
            for (Path ruta : flujo.toList()) {
                Path relativa = origen.relativize(ruta);
                Path objetivo = destino.resolve(relativa);
                if (Files.isDirectory(ruta)) {
                    Files.createDirectories(objetivo);
                } else {
                    if (objetivo.getParent() != null) {
                        Files.createDirectories(objetivo.getParent());
                    }
                    Files.copy(ruta, objetivo, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void eliminarSilenciosamente(Path archivo) {
        if (archivo == null) return;
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // Limpieza mejor esfuerzo.
        }
    }

    private static void eliminarDirectorioRecursivo(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (var flujo = Files.walk(dir)) {
            List<Path> rutas = flujo.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path r : rutas) {
                Files.deleteIfExists(r);
            }
        } catch (IOException ignored) {
            // Limpieza mejor esfuerzo de áreas temporales.
        }
    }

    private static String obtenerRevisionCodigo() {
        Package pkg = RespaldoService.class.getPackage();
        String impl = pkg == null ? null : pkg.getImplementationVersion();
        return impl == null || impl.isBlank() ? "dev-local-" + Instant.now().atOffset(ZoneOffset.UTC).toLocalDate() : impl;
    }

    public record SolicitudRestauracion(
            SesionUsuario identidad,
            Path archivoRespaldo,
            char[] contrasenaRespaldo,
            MotorBaseRespaldo.ConexionJdbc destinoJdbc,
            Path directorioAdjuntosDestino,
            Path directorioCopiaPrevia,
            boolean modoMantenimientoActivo,
            boolean permitirConciliacionManualConfirmada,
            boolean permitirDestinoOperativo
    ) { }

    private record ResultadoInternoCreado(
            Path archivo,
            long tamanoBytes,
            String sha256Paquete,
            Instant corteUtc,
            int cantidadAdjuntos,
            long ultimoNumeroRecibo
    ) { }
}
