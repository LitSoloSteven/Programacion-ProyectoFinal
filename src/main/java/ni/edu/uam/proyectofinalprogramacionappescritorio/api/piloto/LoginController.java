package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.ServicioLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.ServicioValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class LoginController {
    static final String LOGIN = "/api/v1/auth/login";
    static final String LOGOUT = "/api/v1/auth/logout";
    static final String VALIDACIONES = "/api/v1/validaciones";

    private final ServicioLogin servicioLogin;
    private final TokenService tokens;
    private final ServicioValidaciones validaciones;

    @PostMapping({LOGIN, LOGIN + "/"})
    public ResponseEntity<String> login(HttpServletRequest peticion) throws IOException {
        var solicitud = FiltroApi.leerJson(peticion, LoginRequest.class);
        return RespuestasApi.json(200, servicioLogin.autenticar(solicitud, peticion.getRemoteAddr()));
    }

    @PostMapping({LOGOUT, LOGOUT + "/"})
    public ResponseEntity<Void> logout(HttpServletRequest peticion) {
        tokens.revocar(FiltroApi.identidad(peticion).token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping({VALIDACIONES, VALIDACIONES + "/"})
    public ResponseEntity<String> validar(HttpServletRequest peticion) throws IOException {
        var solicitud = FiltroApi.leerJson(peticion, ValidacionRequest.class);
        var identidad = FiltroApi.identidad(peticion).sesion().comoUsuario();
        return RespuestasApi.json(200, validaciones.validar(solicitud, identidad));
    }
}
