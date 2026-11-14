package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteApiTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final LocalDate FECHA = LocalDate.now().minusDays(1);

    private static SessionFactory factory;
    private static ServidorApi servidor;
    private static String nombrePropietario;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:cliente_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");

        factory = HibernateUtil.construir(p);
        HibernateTransactions tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombrePropietario = "cli-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            session.persist(new Usuario(nombrePropietario, hash, Rol.PROPIETARIO));
            return null;
        });
        SesionUsuario.cerrarSesion();

        Router router = new Router();
        new RutasTasas(new TipoCambioService(tx)).registrar(router);
        servidor = new ServidorApi(ApiConfig.paraPruebas(0),
                new AutenticacionService(tx), new TokenService(), router);
        servidor.iniciar();
    }

    @AfterAll
    static void cerrar() {
        if (servidor != null) servidor.detener();
        if (factory != null) factory.close();
        SesionUsuario.cerrarSesion();
    }

    @BeforeEach
    void limpiarTasas() {
        new HibernateTransactions(factory).ejecutar(session -> {
            session.createNativeMutationQuery("delete from tipo_cambio").executeUpdate();
            return null;
        });
    }

    @Test
    void loginRegistroAutorizacionYLogout() {
        ClienteApi api = new ClienteApi(servidor.urlBase());
        ClienteAuth auth = new ClienteAuth(api);
        ClienteTasas tasas = new ClienteTasas(api);

        LoginResponse sesion = auth.login(nombrePropietario, new String(PASSWORD));
        assertFalse(sesion.token().isBlank());
        assertEquals(Rol.PROPIETARIO, sesion.rol());

        TasaResponse informativa = tasas.registrar(FECHA, new BigDecimal("36.7"), "BCN");
        assertEquals("36.70000000", informativa.nioPorUsd());
        assertFalse(informativa.autorizada());

        List<TasaResponse> lista = tasas.listar(FECHA, null, 50);
        assertEquals(1, lista.size());

        TasaResponse autorizada = tasas.autorizar(informativa.id(), "Verificada");
        assertNotEquals(informativa.id(), autorizada.id());
        assertTrue(autorizada.autorizada());
        assertEquals(autorizada.id(), tasas.vigente(FECHA).orElseThrow().id());

        auth.logout();
        ErrorClienteException error = assertThrows(ErrorClienteException.class,
                () -> tasas.listar(FECHA, null, 50));
        assertEquals(401, error.getEstado());
    }

    @Test
    void traduceCredencialesInvalidasYFaltaDeTasa() {
        ClienteApi api = new ClienteApi(servidor.urlBase());
        ClienteAuth auth = new ClienteAuth(api);

        ErrorClienteException error = assertThrows(ErrorClienteException.class,
                () -> auth.login(nombrePropietario, "ClaveIncorrecta-2026"));
        assertEquals(401, error.getEstado());
        assertEquals("CREDENCIALES_INVALIDAS", error.getCodigo());

        auth.login(nombrePropietario, new String(PASSWORD));
        assertTrue(new ClienteTasas(api).vigente(FECHA).isEmpty());
    }
}