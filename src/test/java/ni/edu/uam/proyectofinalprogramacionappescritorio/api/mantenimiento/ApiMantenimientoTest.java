package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento;

import com.google.gson.Gson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.CambioEstadoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ListaSolicitudesMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ResumenGastosMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoUpdateRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorSpringPrueba;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiMantenimientoTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();

    private static SessionFactory factory;
    private static ServidorSpringPrueba servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombreAsistente;
    private static UUID propiedad1Id;
    private static UUID propiedad2Id;
    private static UUID unidad1Id;

    @BeforeAll
    static void iniciar() {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_m8_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
        HibernateTransactions tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombreAsistente = "asis-api-" + UUID.randomUUID().toString().substring(0, 6);
        tx.ejecutar(session -> {
            session.persist(new Usuario(nombreAsistente, hash, Rol.ASISTENTE));

            Propiedad p1 = new Propiedad();
            p1.setNombre("Torre A");
            p1.setDireccion("Calle 1");
            session.persist(p1);

            Propiedad p2 = new Propiedad();
            p2.setNombre("Torre B");
            p2.setDireccion("Calle 2");
            session.persist(p2);
            session.flush();

            Unidad u1 = new Unidad();
            u1.setPropiedad(p1);
            u1.setCodigo("A-101");
            u1.setCanonReferencia(new BigDecimal("250.00"));
            u1.setMoneda("USD");
            session.persist(u1);
            session.flush();

            propiedad1Id = p1.getId();
            propiedad2Id = p2.getId();
            unidad1Id = u1.getId();
            return null;
        });
        SesionUsuario.cerrarSesion();

        servidor = new ServidorSpringPrueba(factory);
        base = servidor.urlBase();
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) cliente.close();
        if (servidor != null) servidor.close();
        if (factory != null && !factory.isClosed()) factory.close();
        SesionUsuario.cerrarSesion();
    }

    @Test
    void cicloCompletoHttpDeSolicitudesGastosYCodigosDeError() throws Exception {
        // Sin token -> 401
        HttpResponse<String> sinToken = enviar("GET", "/api/v1/mantenimiento/solicitudes", null, null);
        assertEquals(401, sinToken.statusCode());

        String token = login(nombreAsistente);

        // Crear solicitud -> 201
        HttpResponse<String> resCrear = enviar("POST", "/api/v1/mantenimiento/solicitudes",
                new SolicitudMantenimientoRequest(unidad1Id.toString(), "Puerta dañada", "Alta", "Juan", "2026-11-20"),
                token);
        assertEquals(201, resCrear.statusCode());
        SolicitudMantenimientoResponse creada = GSON.fromJson(resCrear.body(), SolicitudMantenimientoResponse.class);
        assertNotNull(creada.id());
        assertEquals("Abierta", creada.estado());

        // Editar detalles -> 200
        HttpResponse<String> resEditar = enviar("PUT", "/api/v1/mantenimiento/solicitudes/" + creada.id(),
                new SolicitudMantenimientoUpdateRequest(creada.version(), "Puerta y cerradura", "Media", "Carlos"),
                token);
        assertEquals(200, resEditar.statusCode());
        SolicitudMantenimientoResponse editada = GSON.fromJson(resEditar.body(), SolicitudMantenimientoResponse.class);
        assertEquals("Carlos", editada.responsable());

        // Versión vieja -> 409
        HttpResponse<String> resConflicto = enviar("PUT", "/api/v1/mantenimiento/solicitudes/" + creada.id(),
                new SolicitudMantenimientoUpdateRequest(creada.version(), "Desactualizada", "Baja", "X"),
                token);
        assertEquals(409, resConflicto.statusCode());

        // Transición inválida Abierta -> Resuelta -> 422 ERR-066
        HttpResponse<String> resErr66 = enviar("POST", "/api/v1/mantenimiento/solicitudes/" + creada.id() + "/estado",
                new CambioEstadoMantenimientoRequest(editada.version(), "Resuelta", "2026-11-21", "Listo"),
                token);
        assertEquals(422, resErr66.statusCode());
        ErrorApi.Cuerpo err66 = GSON.fromJson(resErr66.body(), ErrorApi.Cuerpo.class);
        assertEquals("ERR-066", err66.error().codigo());
        assertFalse(resErr66.body().contains("Exception"));

        // Propiedad incompatible en gasto -> 422 ERR-065
        HttpResponse<String> resErr65 = enviar("POST", "/api/v1/mantenimiento/gastos",
                new GastoMantenimientoRequest(propiedad2Id.toString(), creada.id(), "2026-11-21", "25.00", "USD", "Cerradura"),
                token);
        assertEquals(422, resErr65.statusCode());
        ErrorApi.Cuerpo err65 = GSON.fromJson(resErr65.body(), ErrorApi.Cuerpo.class);
        assertEquals("ERR-065", err65.error().codigo());

        // Registrar gasto válido en propiedad1 -> 201
        HttpResponse<String> resGasto = enviar("POST", "/api/v1/mantenimiento/gastos",
                new GastoMantenimientoRequest(propiedad1Id.toString(), creada.id(), "2026-11-21", "25.00", "USD", "Cerradura"),
                token);
        assertEquals(201, resGasto.statusCode());
        GastoMantenimientoResponse gasto = GSON.fromJson(resGasto.body(), GastoMantenimientoResponse.class);
        assertEquals("25.00", gasto.importe());

        // Pasar a En proceso y Resolver -> 200
        HttpResponse<String> resProc = enviar("POST", "/api/v1/mantenimiento/solicitudes/" + creada.id() + "/estado",
                new CambioEstadoMantenimientoRequest(editada.version(), "En proceso", null, "Carlos"),
                token);
        assertEquals(200, resProc.statusCode());
        SolicitudMantenimientoResponse proc = GSON.fromJson(resProc.body(), SolicitudMantenimientoResponse.class);

        HttpResponse<String> resRes = enviar("POST", "/api/v1/mantenimiento/solicitudes/" + creada.id() + "/estado",
                new CambioEstadoMantenimientoRequest(proc.version(), "Resuelta", "2026-11-22", "Cerradura cambiada"),
                token);
        assertEquals(200, resRes.statusCode());
        SolicitudMantenimientoResponse resuelta = GSON.fromJson(resRes.body(), SolicitudMantenimientoResponse.class);
        assertEquals("Resuelta", resuelta.estado());
        assertEquals("25.00", resuelta.totalGastoUsd());

        // Consultar listados y resumen de gastos
        HttpResponse<String> resLista = enviar("GET", "/api/v1/mantenimiento/solicitudes?propiedadId=" + propiedad1Id, null, token);
        assertEquals(200, resLista.statusCode());
        assertEquals(1, GSON.fromJson(resLista.body(), ListaSolicitudesMantenimientoResponse.class).solicitudes().size());

        HttpResponse<String> resResumen = enviar("GET",
                "/api/v1/mantenimiento/gastos?propiedadId=" + propiedad1Id + "&desde=2026-11-01&hasta=2026-11-30",
                null, token);
        assertEquals(200, resResumen.statusCode());
        ResumenGastosMantenimientoResponse resumen = GSON.fromJson(resResumen.body(), ResumenGastosMantenimientoResponse.class);
        assertEquals("25.00", resumen.totalUsd());
        assertEquals("0.00", resumen.totalNio());
    }

    private static String login(String usuario) throws Exception {
        HttpResponse<String> res = enviar("POST", "/api/v1/auth/login",
                new LoginRequest(usuario, new String(PASSWORD)), null);
        assertEquals(200, res.statusCode());
        return GSON.fromJson(res.body(), LoginResponse.class).token();
    }

    private static HttpResponse<String> enviar(String metodo, String ruta, Object cuerpo, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(5));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (cuerpo != null) {
            builder.header("Content-Type", "application/json");
            String json = GSON.toJson(cuerpo);
            builder.method(metodo, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        } else {
            builder.method(metodo, HttpRequest.BodyPublishers.noBody());
        }
        return cliente.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
