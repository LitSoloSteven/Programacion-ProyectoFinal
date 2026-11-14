package ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ContextoPeticion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ControlIntentosLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.SesionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.util.Arrays;
import java.util.Objects;

/**
 * API 1: autenticación y sesión. El login emite un token opaco de 15 minutos.
 * La sesión global de JavaFX no se modifica desde el servidor HTTP.
 */
public final class RutasAuth {
    private final AutenticacionService autenticacion;
    private final TokenService tokens;
    private final ControlIntentosLogin intentos;

    public RutasAuth(AutenticacionService autenticacion, TokenService tokens) {
        this(autenticacion, tokens, new ControlIntentosLogin());
    }

    public RutasAuth(AutenticacionService autenticacion, TokenService tokens,
                     ControlIntentosLogin intentos) {
        this.autenticacion = Objects.requireNonNull(autenticacion);
        this.tokens = Objects.requireNonNull(tokens);
        this.intentos = Objects.requireNonNull(intentos);
    }

    public void registrar(Router router) {
        router.registrar("POST", "/api/v1/auth/login", true, this::login);
        router.registrar("POST", "/api/v1/auth/logout", false, this::logout);
    }

    private RespuestaJson login(ContextoPeticion contexto) throws Exception {
        LoginRequest solicitud = Json.leer(LectorCuerpo.leerJson(contexto.intercambio()),
                LoginRequest.class);

        if (solicitud.usuario() == null || solicitud.usuario().isBlank()
                || solicitud.password() == null || solicitud.password().isEmpty()) {
            throw ExcepcionApi.solicitudInvalida("Usuario y contraseña son obligatorios.");
        }

        if (intentos.estaBloqueado(solicitud.usuario())) {
            throw ExcepcionApi.demasiadosIntentos();
        }

        char[] password = solicitud.password().toCharArray();
        try {
            SesionUsuario sesion = autenticacion.autenticar(
                    solicitud.usuario(), password, false);
            intentos.registrarExito(solicitud.usuario());
            TokenService.TokenEmitido emitido = tokens.emitir(
                    new SesionApi(sesion.getUsuarioId(), sesion.getNombre(), sesion.getRol()));
            return RespuestaJson.ok(new LoginResponse(
                    emitido.token(),
                    emitido.expiraEn().toString(),
                    sesion.getRol(),
                    sesion.getUsuarioId(),
                    sesion.getNombre()));
        } catch (IllegalArgumentException e) {
            intentos.registrarFallo(solicitud.usuario());
            throw ExcepcionApi.credencialesInvalidas();
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private RespuestaJson logout(ContextoPeticion contexto) {
        tokens.revocar(contexto.token());
        return RespuestaJson.sinContenido();
    }
}