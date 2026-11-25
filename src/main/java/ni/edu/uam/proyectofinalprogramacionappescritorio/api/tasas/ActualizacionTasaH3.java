package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ReferenciaTasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ControlFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ReferenciaTipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AuditoriaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConfiguracionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.Permiso;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PermisoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import org.hibernate.Session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class ActualizacionTasaH3 implements AutoCloseable {
    private static final Duration INTERVALO_MINIMO = Duration.ofHours(24);
    private static final String LIMITE_MENSUAL = "LIMITE_MENSUAL";
    private static final System.Logger LOG = System.getLogger(ActualizacionTasaH3.class.getName());

    private final HibernateTransactions transacciones;
    private final ExchangeRateApiCliente proveedor;
    private final String llave;
    private final String huellaConfiguracion;
    private final Function<Session, Instant> reloj;
    private final AuditoriaService auditoria = new AuditoriaService();
    private final PermisoService permisos = new PermisoService();
    private final AtomicBoolean cerrando = new AtomicBoolean(false);
    private final AtomicReference<CountDownLatch> enCurso = new AtomicReference<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> disparos;
    private boolean pausado;

    public static ActualizacionTasaH3 crear(HibernateTransactions transacciones, String llave) {
        var reloj = new RelojService();
        return new ActualizacionTasaH3(transacciones,
                llave == null || llave.isBlank() ? null : new ExchangeRateApiCliente(),
                llave, sesion -> reloj.leer(sesion).instante());
    }

    ActualizacionTasaH3(HibernateTransactions transacciones, ExchangeRateApiCliente proveedor,
                        String llave, Function<Session, Instant> reloj) {
        this.transacciones = transacciones;
        this.proveedor = proveedor;
        this.llave = llave;
        this.huellaConfiguracion = llave == null || llave.isBlank() ? null : huella(llave);
        this.reloj = reloj;
    }

    public synchronized void iniciar() {
        if (proveedor == null || scheduler != null || cerrando.get()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(tarea -> {
            var hilo = new Thread(tarea, "latch-h3");
            hilo.setDaemon(true);
            return hilo;
        });
        if (!pausado) programarDisparos();
    }

    private void programarDisparos() {
        disparos = scheduler.scheduleWithFixedDelay(this::ejecutarProgramada, 0, 5, TimeUnit.SECONDS);
    }

    public synchronized void pausarParaCierre() {
        pausado = true;
        if (disparos != null) {
            disparos.cancel(true);
            disparos = null;
        }
    }

    public synchronized void reanudarTrasCierreAbortado() {
        if (!pausado || cerrando.get()) return;
        pausado = false;
        if (scheduler != null) programarDisparos();
    }

    public boolean hayActividad() {
        return enCurso.get() != null;
    }

    public synchronized void solicitarDetencion() {
        cerrando.set(true);
        pausarParaCierre();
        if (scheduler != null) scheduler.shutdownNow();
        if (proveedor != null) proveedor.close();
    }

    public boolean esperarTerminacion(Duration limite) throws InterruptedException {
        long inicio = System.nanoTime();
        CountDownLatch latch = enCurso.get();
        boolean libre = latch == null || latch.await(Math.max(0L, limite.toMillis()), TimeUnit.MILLISECONDS);
        ScheduledExecutorService actual;
        synchronized (this) {
            actual = scheduler;
        }
        if (actual == null || !actual.isShutdown()) return libre;
        long transcurridoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        long restanteMs = Math.max(0L, limite.toMillis() - transcurridoMs);
        return libre && actual.awaitTermination(restanteMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        solicitarDetencion();
        try {
            esperarTerminacion(Duration.ofSeconds(3));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void ejecutarProgramada() {
        try {
            ejecutarSiPendiente();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "H3 no pudo completar una operación local.");
        }
    }

    boolean ejecutarSiPendiente() {
        CountDownLatch latch = new CountDownLatch(1);
        synchronized (this) {
            if (proveedor == null || pausado || cerrando.get() || Thread.currentThread().isInterrupted()) return false;
            if (!enCurso.compareAndSet(null, latch)) return false;
        }
        try {
            UUID concesion = transacciones.ejecutar(this::reservar);
            if (concesion == null) return false;
            try {
                var referencia = proveedor.consultar(llave);
                transacciones.ejecutar(sesion -> {
                    publicar(sesion, concesion, referencia);
                    return null;
                });
            } catch (ExchangeRateApiCliente.Fallo error) {
                transacciones.ejecutar(sesion -> {
                    registrarFallo(sesion, concesion, error);
                    return null;
                });
            } catch (InterruptedException e) {
                try {
                    transacciones.ejecutar(sesion -> {
                        var control = bloquear(sesion);
                        if (concesion.equals(control.getConcesionId())) control.liberar();
                        return null;
                    });
                } finally {
                    Thread.currentThread().interrupt();
                }
            }
            return true;
        } finally {
            enCurso.compareAndSet(latch, null);
            latch.countDown();
        }
    }

    private UUID reservar(Session sesion) {
        var control = bloquear(sesion);
        Instant ahora = reloj.apply(sesion);
        if (control.conConcesionVigente(ahora)) return null;

        LocalDate mes = RelojService.fechaOperativa(ahora).withDayOfMonth(1);
        control.abrirMes(mes);
        boolean suspendida = control.getHuellaConfiguracion() != null;
        if (suspendida && huellaConfiguracion.equals(control.getHuellaConfiguracion())) return null;
        if (control.getIntentosMes() >= 100) {
            if (!LIMITE_MENSUAL.equals(control.getAviso())) {
                control.terminar(maximo(control.getProximoIntento(), siguienteMes(ahora)),
                        false, LIMITE_MENSUAL, null);
            }
            return null;
        }
        if (!suspendida && ahora.isBefore(control.getProximoIntento())) return null;

        UUID concesion = UUID.randomUUID();
        control.reservar(concesion, ahora.plusSeconds(90));
        return concesion;
    }

    private void publicar(Session sesion, UUID concesion, ExchangeRateApiCliente.Referencia referencia) {
        var control = bloquear(sesion);
        Instant ahora = reloj.apply(sesion);
        if (!concesion.equals(control.getConcesionId()) || !control.conConcesionVigente(ahora)) return;
        sesion.persist(new ReferenciaTipoCambio(referencia.tasa(), referencia.actualizada(),
                referencia.proxima(), referencia.consultada(), referencia.huella()));
        control.terminar(maximo(ahora.plus(intervalo(sesion, ahora)), referencia.proxima()), false, null, null);
    }

    private void registrarFallo(Session sesion, UUID concesion, ExchangeRateApiCliente.Fallo error) {
        var control = bloquear(sesion);
        Instant ahora = reloj.apply(sesion);
        if (!concesion.equals(control.getConcesionId()) || !control.conConcesionVigente(ahora)) return;
        boolean reintento = error.tipo() == ExchangeRateApiCliente.TipoFallo.TRANSITORIO && !control.isReintento();
        Instant proximoOrdinario = ahora.plus(intervalo(sesion, ahora));
        Instant siguiente = switch (error.tipo()) {
            case TRANSITORIO -> reintento ? ahora.plusSeconds(60) : proximoOrdinario;
            case CUOTA_TEMPORAL -> hastaCuotaTemporal(ahora, error.retryAfter());
            case CUOTA_AGOTADA -> siguienteMes(ahora);
            default -> proximoOrdinario;
        };
        String huella = error.tipo() == ExchangeRateApiCliente.TipoFallo.CONFIGURACION
                ? huellaConfiguracion : null;
        control.terminar(siguiente, reintento, error.tipo().name(), huella);
        boolean cuarentena = error.tipo() == ExchangeRateApiCliente.TipoFallo.FUERA_DE_BANDA;
        auditoria.registrarSistema(sesion, concesion, cuarentena ? "REFERENCIA_EN_CUARENTENA" : "INCIDENTE_H3",
                cuarentena ? error.detalleCuarentena() : "origen:H3;causa:" + error.tipo().name());
    }

    public ReferenciaTasaResponse consultar(SesionUsuario identidad) {
        return transacciones.ejecutar(sesion -> {
            permisos.exigir(sesion, identidad, Permiso.CONSULTAR_REPORTES);
            Instant ahora = reloj.apply(sesion);
            var control = sesion.find(ControlFx.class, ControlFx.ID);
            String aviso = control == null ? null : control.getAviso();
            var referencia = sesion.createQuery("""
                    from ReferenciaTipoCambio r order by r.consultadoEn desc, r.id desc
                    """, ReferenciaTipoCambio.class).setMaxResults(1).uniqueResult();
            if (referencia == null) {
                boolean consultando = control != null && control.conConcesionVigente(ahora);
                return new ReferenciaTasaResponse(consultando ? "CONSULTANDO" : "NO_DISPONIBLE",
                        null, null, null, null, null, aviso);
            }
            LocalDate efectiva = RelojService.fechaOperativa(referencia.getActualizadoEn());
            boolean historica = !efectiva.equals(RelojService.fechaOperativa(ahora))
                    || !ahora.isBefore(referencia.getProximoEn());
            long antiguedad = Math.max(0, Duration.between(referencia.getConsultadoEn(), ahora).toSeconds());
            return new ReferenciaTasaResponse(historica ? "HISTORICA" : "INFORMATIVA",
                    referencia.getProveedor(), referencia.getValor().toPlainString(), efectiva.toString(),
                    RelojService.presentar(referencia.getConsultadoEn()), antiguedad, aviso);
        });
    }

    private static ControlFx bloquear(Session sesion) {
        var control = sesion.find(ControlFx.class, ControlFx.ID, LockModeType.PESSIMISTIC_WRITE);
        if (control == null) throw new IllegalStateException("Falta inicializar control_fx.");
        return control;
    }

    private static Duration intervalo(Session sesion, Instant ahora) {
        var parametro = ConfiguracionService.buscarParametroVigente(sesion, "fx_intervalo_horas", ahora);
        if (parametro.isEmpty()) return INTERVALO_MINIMO;
        try {
            int horas = Integer.parseInt(parametro.get().getValor().strip());
            if (horas >= 24 && horas <= 168) return Duration.ofHours(horas);
        } catch (NumberFormatException ignorado) {
            // Un parámetro histórico mal formado no impide conservar una referencia válida.
        }
        return INTERVALO_MINIMO;
    }

    private static Instant hastaCuotaTemporal(Instant ahora, String retryAfter) {
        if (retryAfter != null) {
            try {
                long segundos = Long.parseLong(retryAfter.strip());
                if (segundos >= 0) return ahora.plusSeconds(Math.max(1, segundos));
            } catch (NumberFormatException | DateTimeException | ArithmeticException ignorado) {
                try {
                    Instant fecha = ZonedDateTime.parse(retryAfter.strip(), DateTimeFormatter.RFC_1123_DATE_TIME)
                            .toInstant();
                    return maximo(fecha, ahora.plusSeconds(1));
                } catch (DateTimeException invalida) {
                    return ahora.plus(INTERVALO_MINIMO);
                }
            }
        }
        return ahora.plus(INTERVALO_MINIMO);
    }

    private static Instant siguienteMes(Instant ahora) {
        return RelojService.fechaOperativa(ahora).withDayOfMonth(1).plusMonths(1)
                .atStartOfDay(RelojService.ZONA_OPERATIVA).toInstant();
    }

    private static Instant maximo(Instant primero, Instant segundo) {
        return primero.isAfter(segundo) ? primero : segundo;
    }

    private static String huella(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }
}
