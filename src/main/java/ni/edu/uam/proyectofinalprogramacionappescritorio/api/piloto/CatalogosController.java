package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.RutasCatalogos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CatalogosController {
    private final RutasCatalogos rutas;

    @GetMapping({"/api/v1/propiedades", "/api/v1/propiedades/"})
    public ResponseEntity<String> listarPropiedades(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listarPropiedades(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/propiedades/{id}", "/api/v1/propiedades/{id}/"})
    public ResponseEntity<String> obtenerPropiedad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtenerPropiedad(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/propiedades", "/api/v1/propiedades/"})
    public ResponseEntity<String> crearPropiedad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.crearPropiedad(PeticionSpring.desde(peticion)));
    }

    @PutMapping({"/api/v1/propiedades/{id}", "/api/v1/propiedades/{id}/"})
    public ResponseEntity<String> actualizarPropiedad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.actualizarPropiedad(PeticionSpring.desde(peticion)));
    }

    @PatchMapping({"/api/v1/propiedades/{id}", "/api/v1/propiedades/{id}/"})
    public ResponseEntity<String> cambiarEstadoPropiedad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.cambiarEstadoPropiedad(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/unidades", "/api/v1/unidades/"})
    public ResponseEntity<String> listarUnidades(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listarUnidades(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/unidades/{id}", "/api/v1/unidades/{id}/"})
    public ResponseEntity<String> obtenerUnidad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtenerUnidad(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/unidades", "/api/v1/unidades/"})
    public ResponseEntity<String> crearUnidad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.crearUnidad(PeticionSpring.desde(peticion)));
    }

    @PutMapping({"/api/v1/unidades/{id}", "/api/v1/unidades/{id}/"})
    public ResponseEntity<String> actualizarUnidad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.actualizarUnidad(PeticionSpring.desde(peticion)));
    }

    @PatchMapping({"/api/v1/unidades/{id}", "/api/v1/unidades/{id}/"})
    public ResponseEntity<String> cambiarEstadoUnidad(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.cambiarEstadoUnidad(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/inquilinos", "/api/v1/inquilinos/"})
    public ResponseEntity<String> listarInquilinos(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listarInquilinos(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/inquilinos/{id}", "/api/v1/inquilinos/{id}/"})
    public ResponseEntity<String> obtenerInquilino(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtenerInquilino(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/inquilinos", "/api/v1/inquilinos/"})
    public ResponseEntity<String> crearInquilino(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.crearInquilino(PeticionSpring.desde(peticion)));
    }

    @PutMapping({"/api/v1/inquilinos/{id}", "/api/v1/inquilinos/{id}/"})
    public ResponseEntity<String> actualizarInquilino(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.actualizarInquilino(PeticionSpring.desde(peticion)));
    }

    @PatchMapping({"/api/v1/inquilinos/{id}", "/api/v1/inquilinos/{id}/"})
    public ResponseEntity<String> cambiarEstadoInquilino(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.cambiarEstadoInquilino(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/contratos", "/api/v1/contratos/"})
    public ResponseEntity<String> listarContratos(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.listarContratos(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/contratos/{id}", "/api/v1/contratos/{id}/"})
    public ResponseEntity<String> obtenerContrato(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.obtenerContrato(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/contratos", "/api/v1/contratos/"})
    public ResponseEntity<String> crearContrato(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.crearContrato(PeticionSpring.desde(peticion)));
    }

    @PutMapping({"/api/v1/contratos/{id}", "/api/v1/contratos/{id}/"})
    public ResponseEntity<String> actualizarContrato(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.actualizarContrato(PeticionSpring.desde(peticion)));
    }

    @PatchMapping({"/api/v1/contratos/{id}", "/api/v1/contratos/{id}/"})
    public ResponseEntity<String> cambiarEstadoContrato(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.cambiarEstadoContrato(PeticionSpring.desde(peticion)));
    }
}
