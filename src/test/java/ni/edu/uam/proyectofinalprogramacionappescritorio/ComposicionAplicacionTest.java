package ni.edu.uam.proyectofinalprogramacionappescritorio;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ComposicionAplicacionTest {
    private SessionFactory factory;
    private ComposicionAplicacion composicion;

    @BeforeEach
    void iniciar() {
        Properties propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:composicion_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.connection.password", "");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        propiedades.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil
                .construir(propiedades);
        composicion = new ComposicionAplicacion(factory);
    }

    @AfterEach
    void cerrar() {
        if (factory != null) factory.close();
    }

    @Test
    void laApiRecibeLasMismasInstanciasDeServiciosQueLaInterfaz() {
        assertSame(composicion.pagos(), composicion.pagosApi().pagoService());
        assertSame(composicion.cargos(), composicion.pagosApi().cargoService());
        assertSame(composicion.propiedades(), composicion.catalogosApi().propiedadService());
        assertSame(composicion.unidades(), composicion.catalogosApi().unidadService());
        assertSame(composicion.inquilinos(), composicion.catalogosApi().inquilinoService());
        assertSame(composicion.contratos(), composicion.catalogosApi().contratoService());
    }

    @Test
    void construyeElServidorConLasRutasDeAmbasEntradas() {
        var servidor = composicion.crearServidorApi(ApiConfig.paraPruebas(0));
        try {
            assertNotNull(servidor.router().resolver("GET", "/api/v1/pagos"));
            assertNotNull(servidor.router().resolver("GET", "/api/v1/propiedades"));
            assertNotNull(servidor.router().resolver("POST", "/api/v1/auth/login"));
        } finally {
            servidor.close();
        }
    }
}
