package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {
    @Test
    void resuelveRutaNormalConElSegmentoEsperado() {
        Router router = routerDeRecurso();
        Router.RutaNormalizada ruta = router.normalizar("/api/v1/recurso/abc-123");

        assertTrue(router.esPublica("GET", ruta));
        assertEquals(Map.of("id", "abc-123"), router.resolver("GET", ruta).parametros());
    }

    @Test
    void decodificaCaracteresDeRutaUnaSolaVezYConservaMas() {
        Router router = routerDeRecurso();
        Router.RutaNormalizada ruta = router.normalizar("/api/v1/recurso/ni%C3%B1o%20+%20uno");

        assertEquals(Map.of("id", "niño + uno"), router.resolver("GET", ruta).parametros());
    }

    @Test
    void rechazaCodificacionAnidadaEnLaRuta() {
        Router router = routerDeRecurso();

        ExcepcionApi error = assertThrows(ExcepcionApi.class,
                () -> router.normalizar("/api/v1/recurso/%252F"));

        assertEquals(400, error.getEstado());
        assertEquals("SOLICITUD_INVALIDA", error.getCodigo());
    }

    @Test
    void rechazaEscapePorcentualIncompleto() {
        Router router = routerDeRecurso();

        ExcepcionApi error = assertThrows(ExcepcionApi.class,
                () -> router.normalizar("/api/v1/recurso/%2G"));

        assertEquals(400, error.getEstado());
    }

    private static Router routerDeRecurso() {
        Router router = new Router();
        router.registrar("GET", "/api/v1/recurso/{id}", true,
                contexto -> RespuestaJson.ok(null));
        return router;
    }
}
