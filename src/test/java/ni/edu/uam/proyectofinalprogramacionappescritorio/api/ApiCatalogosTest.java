package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.RutasCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.ServicioCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiCatalogosTest {
    private static final char[] PASSWORD = "ClaveSintetica-2026".toCharArray();
    private static final Gson GSON = new Gson();
    private static final String CORREO_PROPIETARIO = "propietario@latch.test";

    private static SessionFactory factory;
    private static ServidorApi servidor;
    private static HttpClient cliente;
    private static String base;
    private static String nombrePropietario;
    private static String nombreAsistente;

    @BeforeAll
    static void iniciar() throws Exception {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:latch_api_catalogos_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        p.setProperty("hibernate.show_sql", "false");

        factory = HibernateUtil.construir(p);
        HibernateTransactions tx = new HibernateTransactions(factory);
        String hash = new PasswordHasher().crear(PASSWORD);
        nombrePropietario = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        nombreAsistente = "asis-" + UUID.randomUUID().toString().substring(0, 8);
        tx.ejecutar(session -> {
            session.persist(new Usuario(nombrePropietario, hash, Rol.PROPIETARIO));
            session.persist(new Usuario(nombreAsistente, hash, Rol.ASISTENTE));
            return null;
        });
        SesionUsuario.cerrarSesion();

        Router router = new Router();
        new RutasCatalogos(new ServicioCatalogos(tx)).registrar(router);
        servidor = new ServidorApi(ApiConfig.paraPruebas(0),
                new AutenticacionService(tx), new TokenService(), router);
        servidor.iniciar();
        base = servidor.urlBase();
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void cerrar() {
        if (servidor != null) servidor.detener();
        if (factory != null) factory.close();
        SesionUsuario.cerrarSesion();
    }

    @BeforeEach
    void limpiar() {
        new HibernateTransactions(factory).ejecutar(session -> {
            session.createNativeMutationQuery("delete from garantia").executeUpdate();
            session.createNativeMutationQuery("delete from contrato").executeUpdate();
            session.createNativeMutationQuery("delete from unidad").executeUpdate();
            session.createNativeMutationQuery("delete from inquilino").executeUpdate();
            session.createNativeMutationQuery("delete from propiedad").executeUpdate();
            session.createNativeMutationQuery("delete from propietario").executeUpdate();
            return null;
        });
    }

    @Test
    void requiereTokenBearer() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/propiedades", null);

        assertEquals(401, respuesta.statusCode());
        assertEquals("NO_AUTENTICADO", error(respuesta).error().codigo());
    }

    @Test
    void flujoCompletoDeCatalogos() throws Exception {
        String token = login(nombrePropietario);

        PropiedadResponse propiedad = crearPropiedad(token,
                new PropiedadRequest("Edificio Central", "Calle 1", true,
                        "Ana Propietaria", CORREO_PROPIETARIO, "+505 8888-1111", null));
        assertEquals("Edificio Central", propiedad.nombre());
        assertEquals("Ana Propietaria", propiedad.propietarioNombre());
        assertEquals(0, propiedad.version());
        assertTrue(propiedad.activa());

        PaginaResponse<PropiedadResponse> lista = pagina("/api/v1/propiedades", token,
                new TypeToken<PaginaResponse<PropiedadResponse>>() { }.getType());
        assertEquals(1, lista.pagina());
        assertEquals(50, lista.tamano());
        assertEquals(1, lista.total());
        assertEquals(1, lista.elementos().size());

        PropiedadResponse actualizada = actualizarPropiedad(token, propiedad.id(),
                new PropiedadRequest("Edificio Norte", "Calle 2", true, null, null, null, 0L));
        assertEquals("Edificio Norte", actualizada.nombre());
        assertEquals(1, actualizada.version());
        assertEquals(CORREO_PROPIETARIO, actualizada.propietarioCorreo());

        HttpResponse<String> obsoleta = putJson("/api/v1/propiedades/" + propiedad.id(),
                GSON.toJson(new PropiedadRequest("Otra", "Calle 3", true,
                        null, null, null, 0L)), token);
        assertEquals(409, obsoleta.statusCode());
        assertEquals("CONFLICTO", error(obsoleta).error().codigo());

        UnidadResponse unidad = crearUnidad(token,
                new UnidadRequest(propiedad.id(), "A-01", "Frente al parque",
                        "Apartamento", "Disponible", "850.00", "USD", null));
        assertEquals("A-01", unidad.codigo());
        assertEquals("Edificio Norte", unidad.propiedadNombre());
        assertEquals("850.00", unidad.canonReferencia());

        InquilinoResponse inquilino = crearInquilino(token,
                new InquilinoRequest("Luis Inquilino", "001-010190-0001A",
                        "+505 7777-2222", "luis@latch.test", "Marta 8888-0000", true, null));
        assertTrue(inquilino.activo());

        ContratoResponse contrato = crearContrato(token,
                new ContratoRequest(unidad.id(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().plusYears(1).toString(),
                        "850.00", "USD", 5, "850.00", "Vigente", null));
        assertEquals("850.00", contrato.canon());
        assertEquals("850.00", contrato.depositoPactado());
        assertEquals("850.00", contrato.garantiaSaldo());
        assertEquals("Vigente", contrato.estado());
        assertEquals("Luis Inquilino", contrato.inquilinoNombre());

        PaginaResponse<ContratoResponse> vigentes = pagina(
                "/api/v1/contratos?estado=Vigente&inquilinoId=" + inquilino.id(), token,
                new TypeToken<PaginaResponse<ContratoResponse>>() { }.getType());
        assertEquals(1, vigentes.total());

        assertEquals("En mantenimiento",
                parchearUnidad(token, unidad.id(), "En mantenimiento").estado());
        assertFalse(parchearInquilino(token, inquilino.id(), false).activo());
        assertEquals("Terminado",
                parchearContrato(token, contrato.id(), "Terminado").estado());
        assertFalse(parchearPropiedad(token, propiedad.id(), false).activa());

        PaginaResponse<PropiedadResponse> inactivas = pagina(
                "/api/v1/propiedades?activa=false&texto=norte", token,
                new TypeToken<PaginaResponse<PropiedadResponse>>() { }.getType());
        assertEquals(1, inactivas.total());
    }

    @Test
    void paginaYFiltraInquilinos() throws Exception {
        String token = login(nombrePropietario);
        int indice = 0;
        for (String nombre : new String[]{"Ana", "Bruno", "Carla"}) {
            crearInquilino(token, new InquilinoRequest(nombre, "ID-" + indice++,
                    null, null, null, true, null));
        }

        PaginaResponse<InquilinoResponse> primera = pagina(
                "/api/v1/inquilinos?tamano=2", token,
                new TypeToken<PaginaResponse<InquilinoResponse>>() { }.getType());
        assertEquals(3, primera.total());
        assertEquals(2, primera.elementos().size());

        PaginaResponse<InquilinoResponse> segunda = pagina(
                "/api/v1/inquilinos?tamano=2&pagina=2", token,
                new TypeToken<PaginaResponse<InquilinoResponse>>() { }.getType());
        assertEquals(1, segunda.elementos().size());

        PaginaResponse<InquilinoResponse> texto = pagina(
                "/api/v1/inquilinos?texto=brun", token,
                new TypeToken<PaginaResponse<InquilinoResponse>>() { }.getType());
        assertEquals(1, texto.total());
        assertEquals("Bruno", texto.elementos().get(0).nombre());
    }

    @Test
    void validaCuerposYParametros() throws Exception {
        String token = login(nombrePropietario);

        assertEquals(422, postJson("/api/v1/propiedades",
                GSON.toJson(new PropiedadRequest(null, "Calle 1", true,
                        "Ana", CORREO_PROPIETARIO, null, null)), token).statusCode());
        assertEquals(400, get("/api/v1/propiedades?pagina=0", token).statusCode());
        assertEquals(400, get("/api/v1/propiedades?tamano=51", token).statusCode());
        assertEquals(400, get("/api/v1/propiedades?activa=quizas", token).statusCode());
        assertEquals(400, get("/api/v1/unidades?propiedadId=no-es-uuid", token).statusCode());
        assertEquals(404, get("/api/v1/propiedades/" + UUID.randomUUID(), token).statusCode());
        assertEquals(400, get("/api/v1/propiedades/no-es-uuid", token).statusCode());

        PropiedadResponse propiedad = crearPropiedad(token,
                new PropiedadRequest("Edificio", "Calle 1", true,
                        "Ana", CORREO_PROPIETARIO, null, null));
        assertEquals(400, putJson("/api/v1/propiedades/" + propiedad.id(),
                GSON.toJson(new PropiedadRequest("Otro", "Calle 2", true,
                        null, null, null, null)), token).statusCode());

        assertEquals(422, postJson("/api/v1/unidades",
                GSON.toJson(new UnidadRequest(propiedad.id(), "A-01", null,
                        "Bodega", "Disponible", "100.00", "USD", null)), token).statusCode());
        assertEquals(422, postJson("/api/v1/unidades",
                GSON.toJson(new UnidadRequest(propiedad.id(), "A-01", null,
                        "Casa", "Disponible", "100.00", "EUR", null)), token).statusCode());
        assertEquals(422, postJson("/api/v1/inquilinos",
                GSON.toJson(new InquilinoRequest("Luis", null, null,
                        "correo-invalido", null, true, null)), token).statusCode());

        InquilinoResponse inquilino = crearInquilino(token,
                new InquilinoRequest("Luis", "ID-1", null, null, null, true, null));
        UnidadResponse unidad = crearUnidad(token,
                new UnidadRequest(propiedad.id(), "A-02", null,
                        "Casa", "Disponible", "100.00", "USD", null));

        assertEquals(422, postJson("/api/v1/contratos",
                GSON.toJson(new ContratoRequest(unidad.id(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(),
                        "100.00", "USD", 5, "100.00", "Vigente", null)), token).statusCode());
        assertEquals(422, postJson("/api/v1/contratos",
                GSON.toJson(new ContratoRequest(unidad.id(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().plusMonths(1).toString(),
                        "100.123", "USD", 5, "100.00", "Vigente", null)), token).statusCode());
        assertEquals(422, postJson("/api/v1/contratos",
                GSON.toJson(new ContratoRequest(UUID.randomUUID().toString(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().plusMonths(1).toString(),
                        "100.00", "USD", 5, "100.00", "Vigente", null)), token).statusCode());
    }

    @Test
    void identificacionDuplicadaDevuelveConflicto() throws Exception {
        String token = login(nombrePropietario);
        crearInquilino(token, new InquilinoRequest("Luis", "001-010190-0001A",
                null, null, null, true, null));

        HttpResponse<String> respuesta = postJson("/api/v1/inquilinos",
                GSON.toJson(new InquilinoRequest("Otro", "001-010190-0001A",
                        null, null, null, true, null)), token);

        assertEquals(409, respuesta.statusCode());
        assertEquals("CONFLICTO", error(respuesta).error().codigo());
    }

    @Test
    void elAsistentePuedeOperarCatalogos() throws Exception {
        String token = login(nombreAsistente);

        HttpResponse<String> respuesta = postJson("/api/v1/inquilinos",
                GSON.toJson(new InquilinoRequest("Inquilino del asistente", null,
                        null, null, null, true, null)), token);

        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertNotNull(GSON.fromJson(respuesta.body(), InquilinoResponse.class).id());
    }

    private static PropiedadResponse crearPropiedad(String token, PropiedadRequest solicitud)
            throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/propiedades",
                GSON.toJson(solicitud), token);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), PropiedadResponse.class);
    }

    private static PropiedadResponse actualizarPropiedad(String token, String id,
                                                         PropiedadRequest solicitud)
            throws Exception {
        HttpResponse<String> respuesta = putJson("/api/v1/propiedades/" + id,
                GSON.toJson(solicitud), token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), PropiedadResponse.class);
    }

    private static PropiedadResponse parchearPropiedad(String token, String id, boolean activa)
            throws Exception {
        HttpResponse<String> respuesta = patchJson("/api/v1/propiedades/" + id,
                "{\"activa\":" + activa + "}", token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), PropiedadResponse.class);
    }

    private static UnidadResponse crearUnidad(String token, UnidadRequest solicitud)
            throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/unidades",
                GSON.toJson(solicitud), token);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), UnidadResponse.class);
    }

    private static UnidadResponse parchearUnidad(String token, String id, String estado)
            throws Exception {
        HttpResponse<String> respuesta = patchJson("/api/v1/unidades/" + id,
                "{\"estado\":\"" + estado + "\"}", token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), UnidadResponse.class);
    }

    private static InquilinoResponse crearInquilino(String token, InquilinoRequest solicitud)
            throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/inquilinos",
                GSON.toJson(solicitud), token);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), InquilinoResponse.class);
    }

    private static InquilinoResponse parchearInquilino(String token, String id, boolean activo)
            throws Exception {
        HttpResponse<String> respuesta = patchJson("/api/v1/inquilinos/" + id,
                "{\"activo\":" + activo + "}", token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), InquilinoResponse.class);
    }

    private static ContratoResponse crearContrato(String token, ContratoRequest solicitud)
            throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/contratos",
                GSON.toJson(solicitud), token);
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), ContratoResponse.class);
    }

    private static ContratoResponse parchearContrato(String token, String id, String estado)
            throws Exception {
        HttpResponse<String> respuesta = patchJson("/api/v1/contratos/" + id,
                "{\"estado\":\"" + estado + "\"}", token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), ContratoResponse.class);
    }

    private static <T> PaginaResponse<T> pagina(String ruta, String token, Type tipo)
            throws Exception {
        HttpResponse<String> respuesta = get(ruta, token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        return GSON.fromJson(respuesta.body(), tipo);
    }

    private static String login(String nombre) throws Exception {
        HttpResponse<String> respuesta = postJson("/api/v1/auth/login",
                GSON.toJson(new LoginRequest(nombre, new String(PASSWORD))), null);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        LoginResponse sesion = GSON.fromJson(respuesta.body(), LoginResponse.class);
        assertNotNull(sesion.token());
        return sesion.token();
    }

    private static HttpResponse<String> get(String ruta, String token) throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (token != null) constructor.header("Authorization", "Bearer " + token);
        return cliente.send(constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postJson(String ruta, String cuerpo, String token)
            throws Exception {
        return enviar(ruta, cuerpo, token, "POST");
    }

    private static HttpResponse<String> putJson(String ruta, String cuerpo, String token)
            throws Exception {
        return enviar(ruta, cuerpo, token, "PUT");
    }

    private static HttpResponse<String> patchJson(String ruta, String cuerpo, String token)
            throws Exception {
        return enviar(ruta, cuerpo, token, "PATCH");
    }

    private static HttpResponse<String> enviar(String ruta, String cuerpo, String token,
                                               String metodo) throws Exception {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(
                        cuerpo, StandardCharsets.UTF_8));
        if (token != null) constructor.header("Authorization", "Bearer " + token);
        return cliente.send(constructor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static ErrorApi.Cuerpo error(HttpResponse<String> respuesta) {
        return GSON.fromJson(respuesta.body(), ErrorApi.Cuerpo.class);
    }
}