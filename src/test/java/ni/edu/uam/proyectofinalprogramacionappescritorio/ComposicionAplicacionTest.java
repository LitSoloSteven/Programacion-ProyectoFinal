package ni.edu.uam.proyectofinalprogramacionappescritorio;

import org.springframework.web.bind.annotation.RequestMethod;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorSpringPrueba;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.UUID;

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
        if (factory != null && !factory.isClosed()) factory.close();
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
        try (var servidor = new ServidorSpringPrueba(factory)) {
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/pagos"));
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/propiedades"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/auth/login"));
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/contratos/{id}/garantia"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/contratos/{id}/terminar"));
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/cargos/cartera"));
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/cargos/generacion-mes/vista-previa"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/cargos/generacion-mes"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/cargos/revisar-mora"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/cargos/{id}/anulacion"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/pagos/aplicaciones/{id}/reversion"));
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/mantenimiento/solicitudes"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/mantenimiento/solicitudes"));
            assertTrue(servidor.tieneRuta(RequestMethod.PUT, "/api/v1/mantenimiento/solicitudes/{id}"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/mantenimiento/solicitudes/{id}/estado"));
            assertTrue(servidor.tieneRuta(RequestMethod.GET, "/api/v1/mantenimiento/gastos"));
            assertTrue(servidor.tieneRuta(RequestMethod.POST, "/api/v1/mantenimiento/gastos"));
            assertEquals(55, servidor.operacionesApi());
        }
    }
}
