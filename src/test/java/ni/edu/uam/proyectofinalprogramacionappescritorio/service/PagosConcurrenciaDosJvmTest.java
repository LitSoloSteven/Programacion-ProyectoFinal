package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.PostgresTestSupport;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ConsecutivoRecibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.h2.tools.Server;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PagosConcurrenciaDosJvmTest {
    @TempDir
    Path tempDir;

    @Test
    void cp041Cp042Cp032DiezEmisoresEnDosJvmCienPagosYDiezRollbacksSinHuecos() throws Exception {
        try (BasePrueba base = abrirBase()) {
            String jdbcUrl = base.props().getProperty("hibernate.connection.url");
            HibernateTransactions tx = new HibernateTransactions(base.factory());
            Seed seed = sembrar(tx, new BigDecimal("500.00"));

            Path barrierFile = tempDir.resolve("barrier_centena.txt");
            try (Worker jvm1 = lanzarWorker("CENTENA", jdbcUrl, 1, seed.contratoId(),
                    seed.usuarioId(), seed.usuarioNombre(), barrierFile, 10,
                    "5", "10", "1");
                 Worker jvm2 = lanzarWorker("CENTENA", jdbcUrl, 2, seed.contratoId(),
                    seed.usuarioId(), seed.usuarioNombre(), barrierFile, 10,
                    "5", "10", "1")) {
                esperarSalida(jvm1, "JVM-1");
                esperarSalida(jvm2, "JVM-2");
            }

            List<Long> numeros = tx.ejecutar(session -> session.createQuery(
                            "select r.numero from Recibo r order by r.numero asc", Long.class)
                    .getResultList());
            assertEquals(100, numeros.size());
            for (int i = 0; i < 100; i++) {
                assertEquals(i + 1L, numeros.get(i));
            }
            Long siguienteNumero = tx.ejecutar(session ->
                    session.find(ConsecutivoRecibo.class, (short) 1).getSiguienteNumero());
            assertEquals(101L, siguienteNumero);
            assertEquals(100L, contar(tx, "select count(p) from Pago p"));

            BigDecimal sumaPagos = tx.ejecutar(session -> session.createQuery(
                            "select sum(p.montoContrato) from Pago p where p.estado = 'Registrado'", BigDecimal.class)
                    .getSingleResult());
            BigDecimal sumaAplicaciones = tx.ejecutar(session -> session.createQuery(
                            "select coalesce(sum(a.monto), 0) from AplicacionPago a where a.revertidaEn is null",
                            BigDecimal.class)
                    .getSingleResult());
            // 100 pagos de 10.00 = 1000.00; cargo de 500.00 cubierto; 500.00 quedan como crédito.
            assertEquals(new BigDecimal("1000.00"), sumaPagos);
            assertEquals(new BigDecimal("500.00"), sumaAplicaciones);

            // Variante CP-032 (D22): anular 10 pagos tras commit -> 90 Registrado, 10 Anulado, 100 recibos intactos, siguiente_numero = 101.
            PagoService pagoService = new PagoService(tx);
            SesionUsuario prop = SesionUsuario.paraSesionAutenticada(
                    seed.usuarioId(), seed.usuarioNombre(), Rol.PROPIETARIO);
            List<UUID> primerosDiez = tx.ejecutar(session -> session.createQuery(
                            "select p.id from Pago p order by p.id", UUID.class)
                    .setMaxResults(10)
                    .getResultList());
            SesionUsuario.ejecutarComo(prop, () -> {
                for (UUID id : primerosDiez) {
                    pagoService.anular(id, "Anulación variante D22");
                }
                return null;
            });

            assertEquals(100L, contar(tx, "select count(r) from Recibo r"));
            assertEquals(90L, contar(tx, "select count(p) from Pago p where p.estado = 'Registrado'"));
            assertEquals(10L, contar(tx, "select count(p) from Pago p where p.estado = 'Anulado'"));
            Long siguientePostAnulacion = tx.ejecutar(session ->
                    session.find(ConsecutivoRecibo.class, (short) 1).getSiguienteNumero());
            assertEquals(101L, siguientePostAnulacion);
        } finally {
            SesionUsuario.cerrarSesion();
        }
    }

    @Test
    void cp048DosPagosConcurrentesEnDosJvmAlMismoSaldoNoGeneranSaldoNegativo() throws Exception {
        try (BasePrueba base = abrirBase()) {
            String jdbcUrl = base.props().getProperty("hibernate.connection.url");
            HibernateTransactions tx = new HibernateTransactions(base.factory());
            Seed seed = sembrar(tx, new BigDecimal("100.00"));

            Path barrierFile = tempDir.resolve("barrier_saldo.txt");
            try (Worker jvm1 = lanzarWorker("MISMO_SALDO", jdbcUrl, 1, seed.contratoId(),
                    seed.usuarioId(), seed.usuarioNombre(), barrierFile, 2, "80.00");
                 Worker jvm2 = lanzarWorker("MISMO_SALDO", jdbcUrl, 2, seed.contratoId(),
                    seed.usuarioId(), seed.usuarioNombre(), barrierFile, 2, "80.00")) {
                esperarSalida(jvm1, "JVM-1");
                esperarSalida(jvm2, "JVM-2");
            }

            BigDecimal aplicadoTotal = tx.ejecutar(session -> session.createQuery(
                            "select coalesce(sum(a.monto), 0) from AplicacionPago a where a.cargoId = :cid and a.revertidaEn is null",
                            BigDecimal.class)
                    .setParameter("cid", seed.cargoId())
                    .getSingleResult());
            Cargo cargo = tx.ejecutar(session -> session.find(Cargo.class, seed.cargoId()));
            assertEquals(new BigDecimal("100.00"), aplicadoTotal);
            assertEquals(new BigDecimal("0.00"), cargo.saldoPendiente(aplicadoTotal));

            BigDecimal montoPagos = tx.ejecutar(session -> session.createQuery(
                            "select sum(p.montoContrato) from Pago p where p.estado = 'Registrado'", BigDecimal.class)
                    .getSingleResult());
            assertEquals(new BigDecimal("160.00"), montoPagos);
            assertEquals(new BigDecimal("60.00"), montoPagos.subtract(aplicadoTotal));
        }
    }

    private static Worker lanzarWorker(String modo, String jdbcUrl, int jvmIndex, UUID contratoId,
                                        long usuarioId, String usuarioNombre, Path barrierFile,
                                        int expectedArrivals, String... extraArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(PagosConcurrenciaWorker.class.getName());
        cmd.add(modo);
        cmd.add(jdbcUrl);
        cmd.add(String.valueOf(jvmIndex));
        cmd.add(contratoId.toString());
        cmd.add(String.valueOf(usuarioId));
        cmd.add(usuarioNombre);
        cmd.add(barrierFile.toAbsolutePath().toString());
        cmd.add(String.valueOf(expectedArrivals));
        cmd.addAll(List.of(extraArgs));
        Path salida = barrierFile.resolveSibling(barrierFile.getFileName() + "-" + jvmIndex + ".log");
        return new Worker(new ProcessBuilder(cmd).redirectErrorStream(true)
                .redirectOutput(salida.toFile()).start(), salida);
    }

    private static void esperarSalida(Worker worker, String nombre) throws Exception {
        Process proceso = worker.proceso();
        boolean termino = proceso.waitFor(60, TimeUnit.SECONDS);
        if (!termino) proceso.destroyForcibly();
        String salida = Files.readString(worker.salida(), StandardCharsets.UTF_8);
        assertTrue(termino, nombre + " excedió el tiempo: " + salida);
        assertEquals(0, proceso.exitValue(), nombre + " falló con salida:\n" + salida);
    }

    private static Properties propiedades(String jdbcUrl, String ddl) {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        props.setProperty("hibernate.connection.url", jdbcUrl);
        props.setProperty("hibernate.connection.username", "sa");
        props.setProperty("hibernate.connection.password", "");
        props.setProperty("hibernate.hbm2ddl.auto", ddl);
        props.setProperty("hibernate.jdbc.time_zone", "UTC");
        return props;
    }

    private static BasePrueba abrirBase() throws Exception {
        if (Boolean.getBoolean("latch.test.postgres")) {
            Properties props = PostgresTestSupport.propiedadesHibernateValidate();
            try {
                PostgresTestSupport.aplicarEsquemaInicial(props);
                PostgresTestSupport.aplicarMigraciones(props);
                return new BasePrueba(HibernateUtil.construir(props), props, null);
            } catch (RuntimeException error) {
                try {
                    PostgresTestSupport.limpiarEsquemaPrueba(props);
                } catch (RuntimeException limpieza) {
                    error.addSuppressed(limpieza);
                }
                throw error;
            }
        }
        Server tcp = Server.createTcpServer("-tcp", "-tcpPort", "0", "-ifNotExists").start();
        try {
            Properties props = propiedades("jdbc:h2:tcp://127.0.0.1:" + tcp.getPort()
                    + "/mem:latch_2jvm_" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000", "create-drop");
            return new BasePrueba(HibernateUtil.construir(props), props, tcp);
        } catch (RuntimeException error) {
            tcp.stop();
            throw error;
        }
    }

    private record BasePrueba(SessionFactory factory, Properties props, Server tcp)
            implements AutoCloseable {
        @Override
        public void close() {
            try {
                factory.close();
            } finally {
                if (tcp == null) PostgresTestSupport.limpiarEsquemaPrueba(props);
                else tcp.stop();
            }
        }
    }

    private record Worker(Process proceso, Path salida) implements AutoCloseable {
        @Override
        public void close() {
            if (proceso.isAlive()) proceso.destroyForcibly();
        }
    }

    private static Seed sembrar(HibernateTransactions tx, BigDecimal importeCargo) {
        return tx.ejecutar(session -> {
            if (session.find(ConsecutivoRecibo.class, (short) 1) == null) {
                session.persist(new ConsecutivoRecibo((short) 1, 1L));
            }
            Usuario prop = new Usuario("jvm-prop-" + UUID.randomUUID().toString().substring(0, 8),
                    "hash", Rol.PROPIETARIO);
            session.persist(prop);

            Propiedad p = new Propiedad();
            p.setNombre("Edificio Concurrencia");
            p.setDireccion("Managua");
            session.persist(p);

            Unidad u = new Unidad();
            u.setPropiedadId(p.getId());
            u.setCodigo("C-01");
            u.setTipo("Local");
            u.setEstado("Ocupada");
            u.setMoneda("USD");
            u.setCanonReferencia(importeCargo);
            session.persist(u);

            Inquilino i = new Inquilino();
            i.setNombre("Inquilino Concurrencia");
            session.persist(i);
            session.flush();

            Contrato c = new Contrato();
            c.setUnidadId(u.getId());
            c.setInquilinoId(i.getId());
            c.setInicio(LocalDate.of(2026, 1, 1));
            c.setFin(LocalDate.of(2027, 12, 31));
            c.setCanon(importeCargo);
            c.setMoneda("USD");
            c.setDiaPago(5);
            c.setDepositoPactado(BigDecimal.ZERO);
            c.setEstado("Vigente");
            c.inicializarGarantiaSeguimiento();
            session.persist(c);
            session.flush();

            Cargo cargo = new Cargo();
            cargo.setContratoId(c.getId());
            cargo.setTipo("Renta");
            cargo.setPeriodo(LocalDate.of(2026, 11, 1));
            cargo.setFechaEmision(LocalDate.of(2026, 11, 1));
            cargo.setVenceEl(LocalDate.of(2026, 11, 5));
            cargo.setImporte(importeCargo);
            cargo.setConcepto("Renta vencida");
            cargo.setEstado("Pendiente");
            session.persist(cargo);
            session.flush();
            return new Seed(c.getId(), cargo.getId(), prop.getId(), prop.getNombre());
        });
    }

    private static long contar(HibernateTransactions tx, String hql) {
        return tx.ejecutar(session -> session.createQuery(hql, Long.class).getSingleResult());
    }

    private record Seed(UUID contratoId, UUID cargoId, long usuarioId, String usuarioNombre) { }
}
