package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.PostgresTestSupport;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Proceso hijo real (segunda/primera JVM) para las pruebas de concurrencia CP-041, CP-042 y CP-048.
 */
public final class PagosConcurrenciaWorker {
    private PagosConcurrenciaWorker() {}

    public static void main(String[] args) throws Exception {
        String modo = args[0];
        String jdbcUrl = args[1];
        int jvmIndex = Integer.parseInt(args[2]);
        UUID contratoId = UUID.fromString(args[3]);
        long usuarioId = Long.parseLong(args[4]);
        String usuarioNombre = args[5];
        Path barrierFile = Path.of(args[6]);
        int expectedArrivals = Integer.parseInt(args[7]);

        Properties props;
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            props = PostgresTestSupport.propiedadesHibernateValidate();
            if (!jdbcUrl.equals(props.getProperty("hibernate.connection.url"))) {
                throw new IllegalArgumentException("La JVM hija debe usar la misma base desechable.");
            }
        } else {
            props = new Properties();
            props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
            props.setProperty("hibernate.connection.url", jdbcUrl);
            props.setProperty("hibernate.connection.username", "sa");
            props.setProperty("hibernate.connection.password", "");
            props.setProperty("hibernate.hbm2ddl.auto", "validate");
            props.setProperty("hibernate.jdbc.time_zone", "UTC");
        }

        SessionFactory factory = HibernateUtil.construir(props);
        try {
            HibernateTransactions tx = new HibernateTransactions(factory);
            PagoService pagos = new PagoService(tx);
            SesionUsuario sesion = SesionUsuario.paraSesionAutenticada(
                    usuarioId, usuarioNombre, Rol.PROPIETARIO);

            if ("CENTENA".equals(modo)) {
                int hilos = Integer.parseInt(args[8]);
                int pagosPorHilo = Integer.parseInt(args[9]);
                int rollbacksPorHilo = Integer.parseInt(args[10]);
                ejecutarCentena(tx, pagos, sesion, contratoId, jvmIndex, hilos,
                        pagosPorHilo, rollbacksPorHilo, barrierFile, expectedArrivals);
            } else if ("MISMO_SALDO".equals(modo)) {
                BigDecimal monto = new BigDecimal(args[8]);
                ejecutarMismoSaldo(pagos, sesion, contratoId, monto, barrierFile, expectedArrivals);
            } else {
                throw new IllegalArgumentException("Modo desconocido: " + modo);
            }
        } finally {
            SesionUsuario.cerrarSesion();
            factory.close();
        }
    }

    private static void ejecutarCentena(HibernateTransactions tx, PagoService pagos,
                                        SesionUsuario sesion, UUID contratoId, int jvmIndex,
                                        int hilos, int pagosPorHilo, int rollbacksPorHilo,
                                        Path barrierFile, int expectedArrivals) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listoLocal = new CountDownLatch(hilos);
        try {
            List<Future<Void>> futuros = new ArrayList<>(hilos);
            for (int h = 0; h < hilos; h++) {
                final int hiloId = h;
                futuros.add(pool.submit(() -> {
                    listoLocal.countDown();
                    listoLocal.await();
                    marcarLlegadaYEsperar(barrierFile, expectedArrivals);

                    for (int i = 0; i < pagosPorHilo; i++) {
                        Pago pago = crearPago(contratoId, new BigDecimal("10.00"),
                                "J" + jvmIndex + "-H" + hiloId + "-P" + i);
                        SesionUsuario.ejecutarComo(sesion, () -> pagos.registrarPagoAutomatico(pago));
                    }
                    for (int r = 0; r < rollbacksPorHilo; r++) {
                        Pago pagoRb = crearPago(contratoId, new BigDecimal("10.00"),
                                "J" + jvmIndex + "-H" + hiloId + "-RB" + r);
                        try {
                            SesionUsuario.ejecutarComo(sesion, () -> tx.ejecutar(session -> {
                                pagos.registrarPagoEnSesion(session, pagoRb, List.of(),
                                        pagos.comprobarReloj(session), false);
                                throw new IllegalArgumentException("Rollback intencional CP-042");
                            }));
                            throw new AssertionError("El rollback intencional no abortó la transacción.");
                        } catch (IllegalArgumentException esperado) {
                            if (!"Rollback intencional CP-042".equals(esperado.getMessage())) {
                                throw esperado;
                            }
                        }
                    }
                    return null;
                }));
            }
            for (Future<Void> f : futuros) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static void ejecutarMismoSaldo(PagoService pagos, SesionUsuario sesion,
                                           UUID contratoId, BigDecimal monto,
                                           Path barrierFile, int expectedArrivals) throws Exception {
        marcarLlegadaYEsperar(barrierFile, expectedArrivals);
        Pago pago = crearPago(contratoId, monto, "SALDO-" + UUID.randomUUID().toString().substring(0, 8));
        SesionUsuario.ejecutarComo(sesion, () -> pagos.registrarPagoAutomatico(pago));
    }

    private static Pago crearPago(UUID contratoId, BigDecimal monto, String ref) {
        Pago pago = new Pago();
        pago.setContratoId(contratoId);
        pago.setFechaPago(LocalDate.of(2026, 11, 15));
        pago.setMonedaOriginal("USD");
        pago.setMontoOriginal(monto);
        pago.setMetodo("Efectivo");
        pago.setReferencia(ref);
        return pago;
    }

    private static void marcarLlegadaYEsperar(Path barrierFile, int expectedArrivals)
            throws IOException, InterruptedException {
        synchronized (PagosConcurrenciaWorker.class) {
            Files.writeString(barrierFile, "1\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (System.nanoTime() < limite) {
            List<String> lineas = Files.readAllLines(barrierFile, StandardCharsets.UTF_8);
            if (lineas.size() >= expectedArrivals) return;
            Thread.sleep(20);
        }
        throw new IllegalStateException("Tiempo agotado esperando barrera entre JVMs.");
    }
}
