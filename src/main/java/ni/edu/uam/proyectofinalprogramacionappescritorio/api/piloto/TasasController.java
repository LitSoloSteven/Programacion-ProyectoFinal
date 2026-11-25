package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TasasController {
    private final RutasTasas rutas;

    @GetMapping({"/api/v1/tasas", "/api/v1/tasas/"})
    public ResponseEntity<String> listar(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listar(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/tasas/vigente", "/api/v1/tasas/vigente/"})
    public ResponseEntity<String> vigente(HttpServletRequest peticion) throws Exception {
        var contexto = PeticionSpring.desde(peticion);
        // MVC separa parámetros de matriz. El router original los conserva dentro del identificador.
        return RespuestasApi.json(contexto.parametro("id") == null
                ? rutas.vigente(contexto) : rutas.obtener(contexto));
    }

    @GetMapping({"/api/v1/tasas/referencia", "/api/v1/tasas/referencia/"})
    public ResponseEntity<String> referencia(HttpServletRequest peticion) {
        var contexto = PeticionSpring.desde(peticion);
        return RespuestasApi.json(contexto.parametro("id") == null
                ? rutas.referencia(contexto) : rutas.obtener(contexto));
    }

    @GetMapping({"/api/v1/tasas/{id}", "/api/v1/tasas/{id}/"})
    public ResponseEntity<String> obtener(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtener(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/tasas", "/api/v1/tasas/"})
    public ResponseEntity<String> registrarTasa(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.registrarTasa(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/tasas/{id}/autorizar", "/api/v1/tasas/{id}/autorizar/"})
    public ResponseEntity<String> autorizar(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.autorizar(PeticionSpring.desde(peticion)));
    }
}
