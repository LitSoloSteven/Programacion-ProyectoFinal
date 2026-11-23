package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitudHttpTest {
    @Test
    void resuelveRutaNormalConElSegmentoEsperado() {
        assertEquals(List.of("api", "v1", "recurso", "abc-123"),
                SolicitudHttp.normalizarRuta("/api/v1/recurso/abc-123"));
    }

    @Test
    void decodificaCaracteresDeRutaUnaSolaVezYConservaMas() {
        assertEquals(List.of("api", "v1", "recurso", "niño + uno"),
                SolicitudHttp.normalizarRuta("/api/v1/recurso/ni%C3%B1o%20+%20uno"));
    }

    @Test
    void rechazaCodificacionAnidadaEnLaRuta() {
        ExcepcionApi error = assertThrows(ExcepcionApi.class,
                () -> SolicitudHttp.normalizarRuta("/api/v1/recurso/%252F"));

        assertEquals(400, error.getEstado());
        assertEquals("SOLICITUD_INVALIDA", error.getCodigo());
    }

    @Test
    void rechazaEscapePorcentualIncompleto() {
        ExcepcionApi error = assertThrows(ExcepcionApi.class,
                () -> SolicitudHttp.normalizarRuta("/api/v1/recurso/%2G"));

        assertEquals(400, error.getEstado());
    }

}
