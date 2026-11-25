package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.RutasMantenimiento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MantenimientoController {
    private final RutasMantenimiento rutas;

    @GetMapping({"/api/v1/mantenimiento/solicitudes", "/api/v1/mantenimiento/solicitudes/"})
    public ResponseEntity<String> listarSolicitudes(HttpServletRequest peticion) {
        return RespuestasApi.json(rutas.listarSolicitudes(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/mantenimiento/solicitudes", "/api/v1/mantenimiento/solicitudes/"})
    public ResponseEntity<String> crearSolicitud(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.crearSolicitud(PeticionSpring.desde(peticion)));
    }

    @PutMapping({"/api/v1/mantenimiento/solicitudes/{id}", "/api/v1/mantenimiento/solicitudes/{id}/"})
    public ResponseEntity<String> actualizarSolicitud(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.actualizarSolicitud(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/mantenimiento/solicitudes/{id}/estado", "/api/v1/mantenimiento/solicitudes/{id}/estado/"})
    public ResponseEntity<String> cambiarEstado(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.cambiarEstado(PeticionSpring.desde(peticion)));
    }

    @GetMapping({"/api/v1/mantenimiento/gastos", "/api/v1/mantenimiento/gastos/"})
    public ResponseEntity<String> consultarGastos(HttpServletRequest peticion) {
        return RespuestasApi.json(rutas.consultarGastos(PeticionSpring.desde(peticion)));
    }

    @PostMapping({"/api/v1/mantenimiento/gastos", "/api/v1/mantenimiento/gastos/"})
    public ResponseEntity<String> registrarGasto(HttpServletRequest peticion) throws Exception {
        return RespuestasApi.json(rutas.registrarGasto(PeticionSpring.desde(peticion)));
    }
}
