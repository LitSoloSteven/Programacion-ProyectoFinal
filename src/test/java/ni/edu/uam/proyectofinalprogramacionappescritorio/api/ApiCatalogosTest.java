package ni.edu.uam.proyectofinalprogramacionappescritorio.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoCancelarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRenovarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoTerminarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoVersionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaLiquidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaResumenResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.ServidorSpringPrueba;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
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
import java.util.List;
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
    private static ServidorSpringPrueba servidor;
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

    @BeforeEach
    void limpiar() {
        new HibernateTransactions(factory).ejecutar(session -> {
            session.createNativeMutationQuery("delete from aplicacion_pago").executeUpdate();
            session.createNativeMutationQuery("delete from movimiento_garantia").executeUpdate();
            session.createNativeMutationQuery("delete from cargo").executeUpdate();
            session.createNativeMutationQuery("delete from garantia").executeUpdate();
            session.createNativeMutationQuery("update contrato set predecesor_id = null").executeUpdate();
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
    void crearPropiedadUsaUnaTransaccionDeNegocio() throws Exception {
        String token = login(nombrePropietario);
        factory.getStatistics().setStatisticsEnabled(true);
        factory.getStatistics().clear();

        HttpResponse<String> respuesta = postJson("/api/v1/propiedades",
                GSON.toJson(new PropiedadRequest("Edificio Transaccional", "Calle 1", true,
                        "Ana Propietaria", CORREO_PROPIETARIO, "+505 8888-1111", null)), token);

        assertEquals(201, respuesta.statusCode(), respuesta.body());
        assertEquals(2, factory.getStatistics().getTransactionCount(),
                "una transacción valida la sesión y una cubre toda la operación del catálogo");
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
        var unidades = ApiCatalogosTest.<UnidadResponse>pagina(
                "/api/v1/unidades?pagina=1&tamano=1&propiedadId=" + propiedad.id() + "&texto=A-01",
                token, new TypeToken<PaginaResponse<UnidadResponse>>() { }.getType());
        assertEquals(1, unidades.tamano());
        assertEquals(1, unidades.total());
        assertEquals(unidad.id(), unidades.elementos().getFirst().id());
        verificarActualizacion("/api/v1/unidades/" + unidad.id(), token, unidad.version(),
                new UnidadRequest(propiedad.id(), "A-01", "Descripción actualizada",
                        "Apartamento", "Disponible", "850.00", "USD", unidad.version()));

        InquilinoResponse inquilino = crearInquilino(token,
                new InquilinoRequest("Luis Inquilino", "001-010190-0001A",
                        "+505 7777-2222", "luis@latch.test", "Marta 8888-0000", true, null));
        assertTrue(inquilino.activo());
        verificarActualizacion("/api/v1/inquilinos/" + inquilino.id(), token, inquilino.version(),
                new InquilinoRequest("Luis Inquilino", "001-010190-0001A", "+505 7777-2233",
                        "luis@latch.test", "Marta 8888-0000", true, inquilino.version()));

        ContratoResponse contrato = crearContrato(token,
                new ContratoRequest(unidad.id(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().plusYears(1).toString(),
                        "850.00", "USD", 5, "850.00", "Vigente", null));
        assertEquals("850.00", contrato.canon());
        assertEquals("850.00", contrato.depositoPactado());
        assertEquals("850.00", contrato.garantiaSaldo());
        assertEquals("Vigente", contrato.estado());
        assertEquals("Luis Inquilino", contrato.inquilinoNombre());
        verificarActualizacion("/api/v1/contratos/" + contrato.id(), token, contrato.version(),
                new ContratoRequest(unidad.id(), inquilino.id(), LocalDate.now().toString(),
                        LocalDate.now().plusYears(1).plusDays(1).toString(),
                        "850.00", "USD", 5, "850.00", "Vigente", contrato.version()));

        PaginaResponse<ContratoResponse> vigentes = pagina(
                "/api/v1/contratos?estado=Vigente&inquilinoId=" + inquilino.id(), token,
                new TypeToken<PaginaResponse<ContratoResponse>>() { }.getType());
        assertEquals(1, vigentes.total());

        assertEquals(422, patchJson("/api/v1/propiedades/" + propiedad.id(), "{}", token)
                .statusCode());
        assertEquals(422, patchJson("/api/v1/propiedades/" + propiedad.id(),
                "{\"activa\":\"quizas\"}", token).statusCode());
        assertEquals(422, patchJson("/api/v1/inquilinos/" + inquilino.id(), "{}", token)
                .statusCode());
        assertEquals(422, patchJson("/api/v1/inquilinos/" + inquilino.id(),
                "{\"activo\":\"quizas\"}", token).statusCode());
        assertEquals(422, patchJson("/api/v1/unidades/" + unidad.id(), "{}", token)
                .statusCode());
        assertEquals(422, patchJson("/api/v1/unidades/" + unidad.id(),
                "{\"estado\":\"Desconocido\"}", token).statusCode());
        assertEquals(422, patchJson("/api/v1/contratos/" + contrato.id(), "{}", token)
                .statusCode());
        assertEquals(422, patchJson("/api/v1/contratos/" + contrato.id(),
                "{\"estado\":\"Desconocido\"}", token).statusCode());

        assertTrue(GSON.fromJson(get("/api/v1/propiedades/" + propiedad.id(), token).body(),
                PropiedadResponse.class).activa());
        assertTrue(GSON.fromJson(get("/api/v1/inquilinos/" + inquilino.id(), token).body(),
                InquilinoResponse.class).activo());
        assertEquals("Disponible", GSON.fromJson(
                get("/api/v1/unidades/" + unidad.id(), token).body(),
                UnidadResponse.class).estado());
        assertEquals("Vigente", GSON.fromJson(
                get("/api/v1/contratos/" + contrato.id(), token).body(),
                ContratoResponse.class).estado());

        HttpResponse<String> unidadEnContrato = patchJson("/api/v1/unidades/" + unidad.id(),
                "{\"estado\":\"En mantenimiento\"}", token);
        assertEquals(409, unidadEnContrato.statusCode(), unidadEnContrato.body());
        assertEquals("CONFLICTO", error(unidadEnContrato).error().codigo());

        HttpResponse<String> inquilinoEnContrato = patchJson("/api/v1/inquilinos/" + inquilino.id(),
                "{\"activo\":false}", token);
        assertEquals(409, inquilinoEnContrato.statusCode(), inquilinoEnContrato.body());
        assertEquals("CONFLICTO", error(inquilinoEnContrato).error().codigo());

        assertEquals("Terminado",
                parchearContrato(token, contrato.id(), "Terminado").estado());

        assertEquals("En mantenimiento",
                parchearUnidad(token, unidad.id(), "En mantenimiento").estado());
        assertFalse(parchearInquilino(token, inquilino.id(), false).activo());

        HttpResponse<String> propiedadConUnidadActiva = patchJson("/api/v1/propiedades/" + propiedad.id(),
                "{\"activa\":false}", token);
        assertEquals(409, propiedadConUnidadActiva.statusCode(), propiedadConUnidadActiva.body());
        assertEquals("CONFLICTO", error(propiedadConUnidadActiva).error().codigo());

        assertEquals("Inactiva",
                parchearUnidad(token, unidad.id(), "Inactiva").estado());
        assertFalse(parchearPropiedad(token, propiedad.id(), false).activa());

        PaginaResponse<PropiedadResponse> inactivas = pagina(
                "/api/v1/propiedades?activa=false&texto=norte", token,
                new TypeToken<PaginaResponse<PropiedadResponse>>() { }.getType());
        assertEquals(1, inactivas.total());
    }

    @Test
    void rechazaExponenteExtremoSinCrearUnidad() throws Exception {
        String token = login(nombrePropietario);
        PropiedadResponse propiedad = crearPropiedad(token,
                new PropiedadRequest("Edificio", "Calle 1", true,
                        "Ana Propietaria", CORREO_PROPIETARIO, null, null));
        long unidadesAntes = new HibernateTransactions(factory).ejecutar(session ->
                session.createQuery("select count(u) from Unidad u", Long.class)
                        .getSingleResult());

        HttpResponse<String> respuesta = postJson("/api/v1/unidades",
                GSON.toJson(new UnidadRequest(propiedad.id(), "A-02", null,
                        "Apartamento", "Disponible", "1E+999999999", "USD", null)), token);
        long unidadesDespues = new HibernateTransactions(factory).ejecutar(session ->
                session.createQuery("select count(u) from Unidad u", Long.class)
                        .getSingleResult());

        assertEquals(422, respuesta.statusCode(), respuesta.body());
        assertEquals(unidadesAntes, unidadesDespues);
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
    void busquedaDeCatalogoTrataComodinesComoTextoLiteral() throws Exception {
        String token = login(nombrePropietario);
        crearInquilino(token, new InquilinoRequest("Ana % porcentaje", "LIKE-PCT-1",
                null, null, null, true, null));
        crearInquilino(token, new InquilinoRequest("Ana X porcentaje", "LIKE-PCT-2",
                null, null, null, true, null));
        crearInquilino(token, new InquilinoRequest("Linea_baja", "LIKE-UND-1",
                null, null, null, true, null));
        crearInquilino(token, new InquilinoRequest("LineaXbaja", "LIKE-UND-2",
                null, null, null, true, null));
        crearInquilino(token, new InquilinoRequest("Signo!literal", "LIKE-ESC-1",
                null, null, null, true, null));
        crearInquilino(token, new InquilinoRequest("Signo literal", "LIKE-ESC-2",
                null, null, null, true, null));

        for (String consulta : new String[]{"%25", "%5F", "%21"}) {
            PaginaResponse<InquilinoResponse> resultado = pagina(
                    "/api/v1/inquilinos?texto=" + consulta, token,
                    new TypeToken<PaginaResponse<InquilinoResponse>>() { }.getType());
            assertEquals(1, resultado.total(), consulta);
        }
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

    @Test
    void asistenteEditaCondicionesSinAlterarCodigoHistorico() throws Exception {
        String propietario = login(nombrePropietario);
        PropiedadResponse propiedad = crearPropiedad(propietario,
                new PropiedadRequest("Propiedad histórica", "Calle Principal", true,
                        "Ana Propietaria", CORREO_PROPIETARIO, null, null));
        UnidadResponse unidad = crearUnidad(propietario,
                new UnidadRequest(propiedad.id(), "HIST-01", "Inicial",
                        "Apartamento", "Disponible", "800.00", "USD", null));
        new HibernateTransactions(factory).ejecutar(session -> {
            session.createMutationQuery("update Unidad set codigo = :codigo where id = :id")
                    .setParameter("codigo", " HIST-01 ")
                    .setParameter("id", UUID.fromString(unidad.id()))
                    .executeUpdate();
            return null;
        });

        HttpResponse<String> respuesta = putJson("/api/v1/unidades/" + unidad.id(),
                GSON.toJson(new UnidadRequest(propiedad.id(), "HIST-01", "  Actualizada  ",
                        "Apartamento", "Disponible", "925.50", "nio", unidad.version())),
                login(nombreAsistente));

        assertEquals(200, respuesta.statusCode(), respuesta.body());
        UnidadResponse actualizada = GSON.fromJson(respuesta.body(), UnidadResponse.class);
        assertEquals(" HIST-01 ", actualizada.codigo());
        assertEquals("Actualizada", actualizada.descripcion());
        assertEquals("NIO", actualizada.moneda());
    }

    @Test
    void asistenteRespetaPermisosDeTabla7EnM02YM03() throws Exception {
        String tokenPropietario = login(nombrePropietario);
        PropiedadResponse propiedad = crearPropiedad(tokenPropietario,
                new PropiedadRequest("Propiedad T7", "Calle Principal", true,
                        "Ana Propietaria", CORREO_PROPIETARIO, null, null));
        UnidadResponse unidad = crearUnidad(tokenPropietario,
                new UnidadRequest(propiedad.id(), "T7-01", "Inicial",
                        "Apartamento", "Disponible", "800.00", "USD", null));

        String tokenAsistente = login(nombreAsistente);

        assertEquals(200, get("/api/v1/propiedades", tokenAsistente).statusCode());
        assertEquals(200, get("/api/v1/unidades", tokenAsistente).statusCode());

        assertEquals(403, postJson("/api/v1/propiedades",
                GSON.toJson(new PropiedadRequest("Otra", "Calle 2", true,
                        "Ana", CORREO_PROPIETARIO, null, null)), tokenAsistente).statusCode());
        assertEquals(403, putJson("/api/v1/propiedades/" + propiedad.id(),
                GSON.toJson(new PropiedadRequest("Editada", "Calle 2", true,
                        null, null, null, propiedad.version())), tokenAsistente).statusCode());
        assertEquals(403, patchJson("/api/v1/propiedades/" + propiedad.id(),
                "{\"activa\":false}", tokenAsistente).statusCode());

        assertEquals(403, postJson("/api/v1/unidades",
                GSON.toJson(new UnidadRequest(propiedad.id(), "T7-02", null,
                        "Casa", "Disponible", "500.00", "USD", null)), tokenAsistente).statusCode());
        assertEquals(403, patchJson("/api/v1/unidades/" + unidad.id(),
                "{\"estado\":\"En mantenimiento\"}", tokenAsistente).statusCode());
        assertEquals(403, patchJson("/api/v1/unidades/" + unidad.id(),
                "{\"estado\":\"Inactiva\"}", tokenAsistente).statusCode());

        assertEquals(403, putJson("/api/v1/unidades/" + unidad.id(),
                GSON.toJson(new UnidadRequest(propiedad.id(), "T7-01", "Cambio estado",
                        "Apartamento", "En mantenimiento", "800.00", "USD", unidad.version())),
                tokenAsistente).statusCode());
        assertEquals(403, putJson("/api/v1/unidades/" + unidad.id(),
                GSON.toJson(new UnidadRequest(propiedad.id(), "T7-99", "Cambio codigo",
                        "Apartamento", "Disponible", "800.00", "USD", unidad.version())),
                tokenAsistente).statusCode());
        assertEquals(403, putJson("/api/v1/unidades/" + unidad.id(),
                GSON.toJson(new UnidadRequest(propiedad.id(), "T7-01", "Cambio tipo",
                        "Casa", "Disponible", "800.00", "USD", unidad.version())),
                tokenAsistente).statusCode());

        HttpResponse<String> edicionPermitidaUnidad = putJson("/api/v1/unidades/" + unidad.id(),
                GSON.toJson(new UnidadRequest(propiedad.id(), "T7-01", "Condiciones actualizadas",
                        "Apartamento", "Disponible", "925.50", "NIO", unidad.version())),
                tokenAsistente);
        assertEquals(200, edicionPermitidaUnidad.statusCode(), edicionPermitidaUnidad.body());
        UnidadResponse unidadActualizada = GSON.fromJson(edicionPermitidaUnidad.body(), UnidadResponse.class);
        assertEquals("Condiciones actualizadas", unidadActualizada.descripcion());
        assertEquals("925.50", unidadActualizada.canonReferencia());
        assertEquals("NIO", unidadActualizada.moneda());
        assertEquals("Disponible", unidadActualizada.estado());

        InquilinoResponse inquilino = crearInquilino(tokenAsistente,
                new InquilinoRequest("Carlos Asistente", "001-020290-0001B",
                        "+505 8888-9999", "carlos@latch.test", "Maria 7777-0000", true, null));
        HttpResponse<String> edicionInquilino = putJson("/api/v1/inquilinos/" + inquilino.id(),
                GSON.toJson(new InquilinoRequest("Carlos Asistente", "001-020290-0001B",
                        "+505 8888-0000", "carlos.nuevo@latch.test", "Maria 7777-0000",
                        true, inquilino.version())),
                tokenAsistente);
        assertEquals(200, edicionInquilino.statusCode(), edicionInquilino.body());
        InquilinoResponse inquilinoActualizado = GSON.fromJson(edicionInquilino.body(), InquilinoResponse.class);
        assertEquals("carlos.nuevo@latch.test", inquilinoActualizado.correo());

        assertEquals(403, patchJson("/api/v1/inquilinos/" + inquilino.id(),
                "{\"activo\":false}", tokenAsistente).statusCode());
        assertEquals(403, putJson("/api/v1/inquilinos/" + inquilino.id(),
                GSON.toJson(new InquilinoRequest("Carlos Asistente", "001-020290-0001B",
                        "+505 8888-0000", "carlos.nuevo@latch.test", "Maria 7777-0000",
                        false, inquilinoActualizado.version())),
                tokenAsistente).statusCode());

        new HibernateTransactions(factory).ejecutar(session -> {
            long audUnidad = session.createQuery(
                            "select count(a) from Auditoria a where a.accion = 'ACTUALIZAR_UNIDAD' and a.referencia = :ref",
                            Long.class)
                    .setParameter("ref", "unidad:" + unidad.id())
                    .getSingleResult();
            long audInquilino = session.createQuery(
                            "select count(a) from Auditoria a where a.accion = 'ACTUALIZAR_INQUILINO' and a.referencia = :ref",
                            Long.class)
                    .setParameter("ref", "inquilino:" + inquilino.id())
                    .getSingleResult();
            assertEquals(1L, audUnidad);
            assertEquals(1L, audInquilino);
            return null;
        });
    }

    @Test
    void cicloDeVidaContratoYLiquidacionGarantiaPorHttp() throws Exception {
        String tokenProp = login(nombrePropietario);
        String tokenAsist = login(nombreAsistente);

        PropiedadResponse propiedad = crearPropiedad(tokenProp,
                new PropiedadRequest("Residencial P2", "Managua", true,
                        "Ana", "ana@latch.test", null, null));
        UnidadResponse unidad = crearUnidad(tokenProp,
                new UnidadRequest(propiedad.id(), "P2-01", "Departamento P2",
                        "Apartamento", "Disponible", "500.00", "USD", null));
        InquilinoResponse inquilino = crearInquilino(tokenProp,
                new InquilinoRequest("Elena P2", "001-020290-0001B", "+505 8888-1111",
                        "elena@latch.test", null, true, null));

        // Duración < 1 mes calendario devuelve 422
        assertEquals(422, postJson("/api/v1/contratos",
                GSON.toJson(new ContratoRequest(unidad.id(), inquilino.id(),
                        "2027-01-01", "2027-01-31", "500.00", "USD", 5, "500.00", "Borrador", null)),
                tokenProp).statusCode());

        // Borrador cancelado con motivo
        ContratoResponse borradorCancelable = crearContrato(tokenAsist,
                new ContratoRequest(unidad.id(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().plusMonths(6).toString(),
                        "500.00", "USD", 5, "500.00", "Borrador", null));
        HttpResponse<String> canceladoResp = postJson(
                "/api/v1/contratos/" + borradorCancelable.id() + "/cancelar",
                GSON.toJson(new ContratoCancelarRequest(borradorCancelable.version(), "Desistimiento del cliente")),
                tokenAsist);
        assertEquals(200, canceladoResp.statusCode(), canceladoResp.body());
        ContratoResponse cancelado = GSON.fromJson(canceladoResp.body(), ContratoResponse.class);
        assertEquals("Cancelado", cancelado.estado());
        assertEquals("Desistimiento del cliente", cancelado.motivoCierre());

        // Borrador activado por Asistente
        ContratoResponse borrador = crearContrato(tokenAsist,
                new ContratoRequest(unidad.id(), inquilino.id(),
                        LocalDate.now().toString(), LocalDate.now().plusMonths(12).toString(),
                        "500.00", "USD", 5, "500.00", "Borrador", null));
        HttpResponse<String> activadoResp = postJson(
                "/api/v1/contratos/" + borrador.id() + "/activar",
                GSON.toJson(new ContratoVersionRequest(borrador.version())), tokenAsist);
        assertEquals(200, activadoResp.statusCode(), activadoResp.body());
        ContratoResponse activo = GSON.fromJson(activadoResp.body(), ContratoResponse.class);
        assertEquals("Vigente", activo.estado());
        assertEquals("Retenido", activo.garantiaEstado());

        // Renovar crea sucesor Borrador enlazado (201)
        HttpResponse<String> renovadoResp = postJson(
                "/api/v1/contratos/" + activo.id() + "/renovar",
                GSON.toJson(new ContratoRenovarRequest(
                        LocalDate.now().toString(), LocalDate.now().plusMonths(12).toString(),
                        "550.00", "USD", 5, "550.00")),
                tokenAsist);
        assertEquals(201, renovadoResp.statusCode(), renovadoResp.body());
        ContratoResponse sucesor = GSON.fromJson(renovadoResp.body(), ContratoResponse.class);
        assertEquals("Borrador", sucesor.estado());
        assertEquals(activo.id(), sucesor.predecesorId());

        // Asistente puede consultar garantía (200) pero no liquidarla (403)
        assertEquals(200, get("/api/v1/contratos/" + activo.id() + "/garantia", tokenAsist).statusCode());
        String liqId = UUID.randomUUID().toString();
        var liqReq = new GarantiaLiquidacionRequest(liqId, List.of(
                new GarantiaLiquidacionRequest.MovimientoItemRequest(
                        "Devolucion", null, "500.00", LocalDate.now().toString(),
                        "Transferencia", "Devolución total")));
        assertEquals(403, postJson("/api/v1/contratos/" + activo.id() + "/garantia/movimientos",
                GSON.toJson(liqReq), tokenAsist).statusCode());

        // Terminar sin liquidar garantía pendiente -> 422 ERR-062
        HttpResponse<String> cierreBloqueado = postJson(
                "/api/v1/contratos/" + activo.id() + "/terminar",
                GSON.toJson(new ContratoTerminarRequest(
                        activo.version(), LocalDate.now().toString(), "Fin anticipado", null, null)),
                tokenProp);
        assertEquals(422, cierreBloqueado.statusCode(), cierreBloqueado.body());
        assertTrue(cierreBloqueado.body().contains("ERR-062"), cierreBloqueado.body());

        // Propietario liquida garantía (201) + idempotencia (201) + conflicto (409)
        HttpResponse<String> liqOk = postJson(
                "/api/v1/contratos/" + activo.id() + "/garantia/movimientos",
                GSON.toJson(liqReq), tokenProp);
        assertEquals(201, liqOk.statusCode(), liqOk.body());
        GarantiaResumenResponse resumen = GSON.fromJson(liqOk.body(), GarantiaResumenResponse.class);
        assertEquals("Devuelto", resumen.estado());
        assertEquals("0.00", resumen.saldoActual());
        assertTrue(resumen.conciliado());

        assertEquals(201, postJson("/api/v1/contratos/" + activo.id() + "/garantia/movimientos",
                GSON.toJson(liqReq), tokenProp).statusCode());

        var liqDistinta = new GarantiaLiquidacionRequest(liqId, List.of(
                new GarantiaLiquidacionRequest.MovimientoItemRequest(
                        "Deduccion", null, "100.00", LocalDate.now().toString(),
                        null, "Cambio incompatible")));
        assertEquals(409, postJson("/api/v1/contratos/" + activo.id() + "/garantia/movimientos",
                GSON.toJson(liqDistinta), tokenProp).statusCode());

        // Cierre atómico con activación del sucesor
        ContratoResponse relectura = GSON.fromJson(
                get("/api/v1/contratos/" + activo.id(), tokenProp).body(), ContratoResponse.class);
        HttpResponse<String> cierreOk = postJson(
                "/api/v1/contratos/" + activo.id() + "/terminar",
                GSON.toJson(new ContratoTerminarRequest(
                        relectura.version(), LocalDate.now().toString(),
                        "Renovación formalizada", sucesor.id(), null)),
                tokenProp);
        assertEquals(200, cierreOk.statusCode(), cierreOk.body());
        ContratoResponse terminado = GSON.fromJson(cierreOk.body(), ContratoResponse.class);
        assertEquals("Terminado", terminado.estado());
        assertEquals("Renovación formalizada", terminado.motivoCierre());
        assertEquals("Devuelto", terminado.garantiaEstado());

        ContratoResponse sucesorActivo = GSON.fromJson(
                get("/api/v1/contratos/" + sucesor.id(), tokenProp).body(), ContratoResponse.class);
        assertEquals("Vigente", sucesorActivo.estado());
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

    private static void verificarActualizacion(String ruta, String token, long version, Object solicitud)
            throws Exception {
        String cuerpo = GSON.toJson(solicitud);
        var actualizada = putJson(ruta, cuerpo, token);
        assertEquals(200, actualizada.statusCode(), actualizada.body());
        var persistida = get(ruta, token);
        assertEquals(200, persistida.statusCode(), persistida.body());
        assertEquals(version + 1, com.google.gson.JsonParser.parseString(persistida.body())
                .getAsJsonObject().get("version").getAsLong());
        var obsoleta = putJson(ruta, cuerpo, token);
        assertEquals(409, obsoleta.statusCode(), obsoleta.body());
        assertEquals("CONFLICTO", error(obsoleta).error().codigo());
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
