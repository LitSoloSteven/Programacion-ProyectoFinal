package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.RutasPagos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CobranzaController {
    private final RutasPagos rutas;

    @PostMapping({"/api/v1/pagos", "/api/v1/pagos/"})
    public ResponseEntity<String> registrarPago(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.registrarPago(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/pagos", "/api/v1/pagos/"})
    public ResponseEntity<String> listarPagos(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listarPagos(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/cargos", "/api/v1/cargos/"})
    public ResponseEntity<String> listarCargos(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listarCargos(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/pagos/{id}", "/api/v1/pagos/{id}/"})
    public ResponseEntity<String> obtenerPago(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtenerPago(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/recibos/{numero}", "/api/v1/recibos/{numero}/"})
    public ResponseEntity<String> obtenerRecibo(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtenerRecibo(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/pagos/{id}/anulacion", "/api/v1/pagos/{id}/anulacion/"})
    public ResponseEntity<String> anularPago(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.anularPago(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/contratos/{id}/estado-cuenta", "/api/v1/contratos/{id}/estado-cuenta/"})
    public ResponseEntity<String> estadoCuenta(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.estadoCuenta(PeticionSpring.desde(peticion)));
    }
}
