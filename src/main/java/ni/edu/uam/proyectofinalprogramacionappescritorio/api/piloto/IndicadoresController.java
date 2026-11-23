package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.RutasIndicadores;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IndicadoresController {
    private final RutasIndicadores rutas;

    @GetMapping({"/api/v1/dashboard/metricas", "/api/v1/dashboard/metricas/"})
    public ResponseEntity<String> metricas(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.metricas(PeticionSpring.desde(peticion)));
    }
}
