package ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ControlIntentosLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.SesionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.util.Arrays;
import java.util.Objects;

public final class ServicioLogin {
    private final AutenticacionService autenticacion;
    private final TokenService tokens;
    private final ControlIntentosLogin intentos;

    public ServicioLogin(AutenticacionService autenticacion, TokenService tokens,
                         ControlIntentosLogin intentos) {
        this.autenticacion = Objects.requireNonNull(autenticacion);
        this.tokens = Objects.requireNonNull(tokens);
        this.intentos = Objects.requireNonNull(intentos);
    }

    public LoginResponse autenticar(LoginRequest solicitud, String origen) {
        if (solicitud.usuario() == null || solicitud.usuario().isBlank()
                || solicitud.password() == null || solicitud.password().isEmpty()) {
            throw ExcepcionApi.solicitudInvalida("Usuario y contraseña son obligatorios.");
        }
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
        return new LoginResponse(emitido.token(), emitido.expiraEn().toString(),
                sesion.getRol(), sesion.getUsuarioId(), sesion.getNombre());
    }
}
