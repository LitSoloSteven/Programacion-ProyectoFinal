package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.PeticionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.SesionApi;

import java.io.IOException;
import java.util.Map;

record PeticionSpring(HttpServletRequest intercambio, Map<String, String> parametros,
                      Map<String, String> consulta, SesionApi sesion) implements PeticionApi {
    static final String CONSULTA = PeticionSpring.class.getName() + ".consulta";

    @SuppressWarnings("unchecked")
    static PeticionSpring desde(HttpServletRequest intercambio) {
        var parametros = (Map<String, String>) intercambio.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        var consulta = (Map<String, String>) intercambio.getAttribute(CONSULTA);
        return new PeticionSpring(intercambio, parametros == null ? Map.of() : parametros, consulta,
                FiltroApi.identidad(intercambio).sesion());
    }

    @Override
    public String parametro(String nombre) {
        return parametros.get(nombre);
    }

    @Override
    public String consulta(String nombre) {
        return consulta.get(nombre);
    }

    @Override
    public String cabecera(String nombre) {
        return intercambio.getHeader(nombre);
    }

    @Override
    public String leerCuerpo() throws IOException {
        return FiltroApi.leerCuerpo(intercambio);
    }
}
