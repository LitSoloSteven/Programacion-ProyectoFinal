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
import java.net.InetAddress;
import java.net.InetSocketAddress;

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

        String origen = origen(contexto);
        if (intentos.estaBloqueado(solicitud.usuario(), origen)) {
            throw ExcepcionApi.demasiadosIntentos();
        }

        char[] password = solicitud.password().toCharArray();
        SesionUsuario sesion;
        try {
            sesion = autenticacion.autenticar(solicitud.usuario(), password, false, origen);
        } catch (IllegalArgumentException e) {
            intentos.registrarFallo(solicitud.usuario(), origen);
            throw ExcepcionApi.credencialesInvalidas();
        } finally {
            Arrays.fill(password, '\0');
        }
        intentos.registrarExito(solicitud.usuario(), origen);
        TokenService.TokenEmitido emitido = tokens.emitir(
                new SesionApi(sesion.getUsuarioId(), sesion.getNombre(), sesion.getRol(), sesion.getVersion()));
        return RespuestaJson.ok(new LoginResponse(
                emitido.token(),
                emitido.expiraEn().toString(),
                sesion.getRol(),
                sesion.getUsuarioId(),
                sesion.getNombre()));
    }

    private static String origen(ContextoPeticion contexto) {
        InetSocketAddress remoto = contexto.intercambio().getRemoteAddress();
        InetAddress direccion = remoto == null ? null : remoto.getAddress();
        return direccion == null ? "desconocido" : direccion.getHostAddress();
    }

    private RespuestaJson logout(ContextoPeticion contexto) {
        tokens.revocar(contexto.token());
        return RespuestaJson.sinContenido();
    }
}
