package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HibernateTransactionsTest {
    private SessionFactory factory;
    private HibernateTransactions transacciones;

    @BeforeEach
    void iniciar() {
        Properties propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:transacciones_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.connection.password", "");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = HibernateUtil.construir(propiedades);
        factory.getStatistics().setStatisticsEnabled(true);
        factory.getStatistics().clear();
        transacciones = new HibernateTransactions(factory);
    }

    @AfterEach
    void cerrar() {
        if (factory != null) factory.close();
    }

    @Test
    void llamadasAnidadasCompartenSesionYUnaSolaTransaccion() {
        transacciones.ejecutar(session -> {
            transacciones.ejecutar(anidada -> {
                assertSame(session, anidada);
                return null;
            });
            return null;
        });

        assertEquals(1, factory.getStatistics().getTransactionCount());
    }

    @Test
    void errorAnidadoRevierteAunqueLaLlamadaExteriorLoCapture() {
        assertThrows(IllegalArgumentException.class, () -> transacciones.ejecutar(session -> {
            try {
                transacciones.ejecutar(anidada -> {
                    anidada.persist(TipoCambio.informativa(LocalDate.of(2026, 11, 14),
                            new BigDecimal("36.7"), "BCN", Instant.now()));
                    anidada.flush();
                    throw new IllegalArgumentException("fallo intencional");
                });
            } catch (IllegalArgumentException ignorada) {
                // El contexto exterior debe mantener la transacción marcada para rollback.
            }
            return null;
        }));

        long tasas = transacciones.ejecutar(session -> session.createQuery(
                "select count(t.id) from TipoCambio t", Long.class).getSingleResult());
        assertEquals(0, tasas);
    }
}
