package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.concurrent.Task;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CobranzaService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinadorCierreH6Test {
    @Test
    void registroTareaUnicaImpideDobleEjecucionSimultaneaYRechazaDuranteCierre() {
        RegistroTareaUnica registro = new RegistroTareaUnica("H1-prueba");
        try (var primera = registro.iniciar("Lote 1")) {
            assertTrue(registro.hayActividad());
            assertThrows(IllegalStateException.class, () -> registro.iniciar("Lote 2"));
        }
        assertFalse(registro.hayActividad());

        registro.bloquearNuevas(true);
        try {
            assertThrows(IllegalStateException.class, () -> registro.iniciar("Lote 3"));
        } finally {
            registro.bloquearNuevas(false);
        }
    }

    @Test
    void cierraRecursosCuandoTodasLasTareasTerminanAntesDelLimite() {
        AtomicBoolean cancelacionSolicitada = new AtomicBoolean(false);
        AtomicBoolean activa = new AtomicBoolean(true);
        CountDownLatch liberada = new CountDownLatch(1);
        AtomicInteger cierresDeRecursos = new AtomicInteger(0);

        CoordinadorCierreH6.ParticipanteCierre participante = new CoordinadorCierreH6.ParticipanteCierre() {
            @Override public String nombre() { return "H1: Generación de rentas"; }
            @Override public boolean hayActividad() { return activa.get(); }
            @Override public void solicitarCancelacion() {
                cancelacionSolicitada.set(true);
                activa.set(false);
                liberada.countDown();
            }
            @Override public boolean esperarTerminacion(Duration limite) throws InterruptedException {
                return liberada.await(limite.toMillis(), TimeUnit.MILLISECONDS);
            }
        };

        CoordinadorCierreH6 h6 = new CoordinadorCierreH6(
                List.of(participante),
                () -> { },
                () -> { },
                cierresDeRecursos::incrementAndGet);

        var resultado = h6.ejecutarCierre(Duration.ofSeconds(2));
        assertTrue(resultado.completado());
        assertTrue(cancelacionSolicitada.get());
        assertTrue(resultado.tareasPendientes().isEmpty());
        assertEquals(1, cierresDeRecursos.get());
    }

    @Test
    void devuelveErr056YMantieneRecursosAbiertosCuandoTareaSuperaTiempoLimite() {
        AtomicInteger cierresDeRecursos = new AtomicInteger(0);
        AtomicBoolean bloqueoLiberadoTrasFallo = new AtomicBoolean(false);

        CoordinadorCierreH6.ParticipanteCierre bloqueada = new CoordinadorCierreH6.ParticipanteCierre() {
            @Override public String nombre() { return "H5: Reportes y exportación"; }
            @Override public boolean hayActividad() { return true; }
            @Override public void solicitarCancelacion() { }
            @Override public boolean esperarTerminacion(Duration limite) { return false; }
        };

        CoordinadorCierreH6 h6 = new CoordinadorCierreH6(
                List.of(bloqueada),
                () -> { },
                () -> bloqueoLiberadoTrasFallo.set(true),
                cierresDeRecursos::incrementAndGet);

        var resultado = h6.ejecutarCierre(Duration.ofMillis(50));
        assertFalse(resultado.completado());
        assertEquals(TareasActivasException.CODIGO, resultado.codigoError());
        assertEquals(TareasActivasException.MENSAJE_MSG_061, resultado.mensaje());
        assertEquals(List.of("H5: Reportes y exportación"), resultado.tareasPendientes());
        assertEquals(0, cierresDeRecursos.get(), "Nunca se deben cerrar conexiones si hay tareas activas");
        assertTrue(bloqueoLiberadoTrasFallo.get());
    }

    @Test
    void admiteTareasDeInterfazTrasUnCierreBloqueadoYPuedeVolverACerrar() throws InterruptedException {
        var ejecutor = new TareasSegundoPlano();
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicInteger cierres = new AtomicInteger();
        var h6 = CoordinadorCierreH6.paraAplicacion(ejecutor, null, cierres::incrementAndGet);
        Task<Void> activa = new Task<>() {
            @Override protected Void call() { return null; }
            @Override public void run() {
                iniciada.countDown();
                try {
                    liberar.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        Task<Void> posterior = new Task<>() {
            @Override protected Void call() { return null; }
            @Override public void run() { }
        };
        try {
            ejecutor.ejecutar(activa);
            assertTrue(iniciada.await(2, TimeUnit.SECONDS));
            var bloqueado = h6.ejecutarCierre(Duration.ZERO);
            assertFalse(bloqueado.completado());
            assertEquals(List.of("H4/Tareas de interfaz en curso"), bloqueado.tareasPendientes());
            assertEquals(0, cierres.get());
            ejecutor.ejecutar(posterior);
            liberar.countDown();
            assertTrue(h6.ejecutarCierre(Duration.ofSeconds(2)).completado());
            assertEquals(1, cierres.get());
            assertThrows(RejectedExecutionException.class, () -> ejecutor.ejecutar(posterior));
        } finally {
            liberar.countDown();
            ejecutor.esperarTareas(Duration.ofSeconds(2));
            ejecutor.cerrar();
            RegistroTareaUnica.H1.bloquearNuevas(false);
            RegistroTareaUnica.H2.bloquearNuevas(false);
            GestorH5Reportes.bloquearNuevas(false);
        }
    }

    @Test
    void h2RespetaCancelacionCooperativaEntreContratos() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        props.setProperty("hibernate.connection.url", "jdbc:h2:mem:h2cancel_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        props.setProperty("hibernate.connection.username", "sa");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");

        try (SessionFactory factory = HibernateUtil.construir(props)) {
            HibernateTransactions tx = new HibernateTransactions(factory);
            tx.ejecutar(session -> {
                Usuario u = new Usuario("h2-prop", new PasswordHasher().crear("ClaveSintetica-2026".toCharArray()), Rol.PROPIETARIO);
                session.persist(u);
                session.flush();
                Propietario prop = new Propietario(u, "Prop H2", "h2@example.test", null);
                session.persist(prop);
                Propiedad p = new Propiedad();
                p.setNombre("Edificio H2");
                p.setDireccion("Dir H2");
                p.setPropietario(prop);
                session.persist(p);
                Inquilino inq = new Inquilino();
                inq.setNombre("Inq H2");
                inq.setIdentificacion("H2-" + UUID.randomUUID().toString().replace("-", ""));
                session.persist(inq);
                for (int i = 0; i < 3; i++) {
                    Unidad uni = new Unidad();
                    uni.setPropiedadId(p.getId());
                    uni.setCodigo("U-" + i);
                    uni.setCanonReferencia(new BigDecimal("100.00"));
                    uni.setMoneda("USD");
                    session.persist(uni);
                    session.flush();
                    session.createNativeMutationQuery("""
                            insert into contrato (id, unidad_id, inquilino_id, inicio, fin, canon, moneda,
                                                  dia_pago, deposito_pactado, estado, version)
                            values (:id, :uid, :iid, :ini, :fin, 100.00, 'USD', 5, 0, 'Vigente', 0)
                            """)
                            .setParameter("id", UUID.randomUUID())
                            .setParameter("uid", uni.getId())
                            .setParameter("iid", inq.getId())
                            .setParameter("ini", LocalDate.of(2026, 1, 1))
                            .setParameter("fin", LocalDate.of(2026, 11, 30))
                            .executeUpdate();
                }
                return null;
            });

            CobranzaService cobranza = new CobranzaService(tx);
            var res = cobranza.revisarMoraYVencimientosSistema(LocalDate.of(2026, 11, 25), () -> true);
            assertEquals(0, res.marcadosPorVencer());
            assertEquals(0, res.marcadosVencido());
        }
    }
}
