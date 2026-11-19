package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.server.PathContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.SesionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public final class FiltroApi extends OncePerRequestFilter {
    private static final String IDENTIDAD = FiltroApi.class.getName() + ".identidad";
    private final ObjectProvider<RequestMappingHandlerMapping> rutasMvc;
    private final AutenticacionService autenticacion;
    private final TokenService tokens;

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        try {
            validarHost(peticion);
            String rutaCruda = peticion.getRequestURI();
            var partes = SolicitudHttp.normalizarRuta(rutaCruda);
            String metodo = peticion.getMethod();
            if (!"POST".equals(metodo) || !partes.equals(List.of("api", "v1", "auth", "login"))) {
                peticion.setAttribute(IDENTIDAD, autenticar(peticion.getHeader("Authorization")));
            }
            if (partes.size() < 3 || !partes.subList(0, 2).equals(List.of("api", "v1"))) {
                throw ExcepcionApi.noEncontrado();
            }
            // Un punto y coma literal es parte del segmento, no un parámetro de matriz.
            String rutaMvc = rutaCruda.replace(";", "%3B");
            if ("HEAD".equals(metodo) || "OPTIONS".equals(metodo)) {
                // Spring admite HEAD y OPTIONS implícitos. El contrato exige rutas explícitas.
                var ruta = PathContainer.parsePath(rutaMvc);
                boolean existe = rutasMvc.getObject().getHandlerMethods().keySet().stream()
                        .flatMap(mapeo -> mapeo.getPathPatternsCondition().getPatterns().stream())
                        .anyMatch(patron -> patron.matches(ruta));
                if (existe) throw RespuestasApi.metodoNoPermitido();
                throw ExcepcionApi.noEncontrado();
            }
            peticion.setAttribute(PeticionSpring.CONSULTA,
                    SolicitudHttp.analizarConsulta(peticion.getQueryString()));
            HttpServletRequest entrada = rutaMvc.equals(rutaCruda) ? peticion
                    : new HttpServletRequestWrapper(peticion) {
                        @Override public String getRequestURI() {
                            return rutaMvc;
                        }
                    };
            cadena.doFilter(entrada, respuesta);
        } catch (Exception e) {
            if (respuesta.isCommitted()) throw e;
            respuesta.resetBuffer();
            RespuestasApi.escribir(respuesta, RespuestasApi.traducir(e));
        }
    }

    private static void validarHost(HttpServletRequest peticion) {
        var hosts = Collections.list(peticion.getHeaders("Host"));
        String esperado = ApiConfig.HOST_LOCAL + ":" + peticion.getLocalPort();
        if (hosts.size() != 1 || !esperado.equalsIgnoreCase(hosts.getFirst().strip())) {
            throw ExcepcionApi.solicitudInvalida("La cabecera Host no es válida.");
        }
    }

    private Identidad autenticar(String cabecera) {
        String valor = cabecera == null ? "" : cabecera.strip();
        if (!valor.regionMatches(true, 0, "Bearer ", 0, 7)) throw ExcepcionApi.noAutorizado();
        String token = valor.substring(7).strip();
        SesionApi sesion = tokens.validar(token).orElseThrow(ExcepcionApi::noAutorizado);
        if (!autenticacion.sesionVigente(sesion)) {
            tokens.revocarUsuario(sesion.usuarioId());
            throw ExcepcionApi.noAutorizado();
        }
        return new Identidad(token, sesion);
    }

    static Identidad identidad(HttpServletRequest peticion) {
        if (peticion.getAttribute(IDENTIDAD) instanceof Identidad identidad) return identidad;
        throw ExcepcionApi.noAutorizado();
    }

    static <T> T leerJson(HttpServletRequest peticion, Class<T> tipo) throws IOException {
        return Json.leer(leerCuerpo(peticion), tipo);
    }

    static String leerCuerpo(HttpServletRequest peticion) throws IOException {
        var recepcion = (ProtocoloApi.Recepcion) peticion.getAttribute(ProtocoloApi.RECEPCION);
        String cuerpo;
        try {
            var entrada = new FilterInputStream(peticion.getInputStream()) {
                @Override
                public int read(byte[] destino, int inicio, int longitud) throws IOException {
                    recepcion.antesDeLeer();
                    return super.read(destino, inicio, longitud);
                }
            };
            cuerpo = LectorCuerpo.leerJson(peticion.getContentType(),
                    peticion.getHeader("Content-Length"), entrada);
        } catch (SocketTimeoutException e) {
            recepcion.expirar();
            throw e;
        } catch (IOException e) {
            throw ExcepcionApi.solicitudInvalida("No se pudo leer el cuerpo de la solicitud.");
        }
        recepcion.completar();
        return cuerpo;
    }

    record Identidad(String token, SesionApi sesion) {
        @Override
        public String toString() {
            return "Identidad HTTP autenticada";
        }
    }
}
