package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelojServiceTest {
    private static SessionFactory factory;
    private static HibernateTransactions transactions;

    @BeforeAll
    static void iniciar() {
        Properties propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_reloj_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.connection.password", "");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
        propiedades.setProperty("hibernate.show_sql", "false");
        factory = HibernateUtil.construir(propiedades);
        transactions = new HibernateTransactions(factory);
    }

    @AfterAll
    static void cerrar() {
        if (factory != null && !factory.isClosed()) factory.close();
    }

    @Test
    void obtieneHoraOperativaDeLaBase() {
        RelojService.TiempoOperativo tiempo = transactions.ejecutar(
                session -> new RelojService().leer(session));

        assertTrue(Duration.between(tiempo.instante(), Instant.now()).abs()
                .compareTo(Duration.ofSeconds(2)) <= 0);
        assertEquals(RelojService.fechaOperativa(tiempo.instante()), tiempo.fecha());
    }

    @Test
    void fechaOperativaYPresentacionUsanManagua() {
        Instant instante = Instant.parse("2026-11-15T05:30:00Z");

        assertEquals(LocalDate.of(2026, 11, 14), RelojService.fechaOperativa(instante));
        assertEquals("2026-11-14T23:30-06:00", RelojService.presentar(instante));
    }

    @Test
    void permiteHastaCincoMinutosYRechazaUnDesfaseMayorEnAmbasDirecciones() {
        Instant bd = Instant.parse("2026-11-14T12:00:00Z");

        RelojService.exigirDesfasePermitido(bd, bd.plus(Duration.ofMinutes(5)));
        RelojService.exigirDesfasePermitido(bd, bd.minus(Duration.ofMinutes(5)));
        assertThrows(RelojDesfasadoException.class, () ->
                RelojService.exigirDesfasePermitido(bd, bd.plus(Duration.ofMinutes(5)
                        .plusMillis(1))));
        assertThrows(RelojDesfasadoException.class, () ->
                RelojService.exigirDesfasePermitido(bd, bd.minus(Duration.ofMinutes(5)
                        .plusMillis(1))));
    }

    @Test
    void comprobacionDeSesionBloqueaSiElRelojLocalSeDesvia() {
        RelojService desfasado = new RelojService(Clock.offset(
                Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofMinutes(6)));

        assertThrows(RelojDesfasadoException.class, () -> transactions.ejecutar(
                session -> desfasado.comprobar(session)));
    }
}
