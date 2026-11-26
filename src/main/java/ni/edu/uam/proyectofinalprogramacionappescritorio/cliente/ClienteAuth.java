package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.CambiarPasswordRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.CambiarPasswordResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;

import java.util.Map;
import java.util.Objects;

public final class ClienteAuth {
    private final ClienteApi api;

    public ClienteAuth(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public LoginResponse login(String usuario, String password) {
        return login(usuario, password, null);
    }

    public LoginResponse login(String usuario, String password, String nuevaPassword) {
        LoginResponse respuesta = api.post("/api/v1/auth/login",
                new LoginRequest(usuario, password, nuevaPassword), LoginResponse.class);
        api.establecerToken(respuesta.token());
        if (respuesta.usuarioId() != null && respuesta.nombre() != null && respuesta.rol() != null) {
            SesionCliente.iniciar(respuesta.usuarioId(), respuesta.nombre(), respuesta.rol());
        }
        return respuesta;
    }

    public CambiarPasswordResponse cambiarPassword(char[] passwordActual, char[] nuevaPassword) {
        String actual = passwordActual == null ? "" : new String(passwordActual);
        String nueva = nuevaPassword == null ? "" : new String(nuevaPassword);
        CambiarPasswordResponse respuesta = api.post("/api/v1/auth/cambiar-password",
                new CambiarPasswordRequest(actual, nueva), CambiarPasswordResponse.class);
        if (respuesta != null && respuesta.token() != null) {
            api.establecerToken(respuesta.token());
        }
        return respuesta;
    }

    /**
     * Revoca un token ya capturado. No consulta ni modifica el token guardado,
     * por lo que es seguro llamarlo desde una tarea en segundo plano.
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        api.postSinContenidoConToken("/api/v1/auth/logout", Map.of(), token);
    }

    /** Extrae el token local de forma atómica y lo revoca en el servidor. */
    public void logout() {
        String token = api.tomarToken();
        SesionCliente.cerrarSesion();
        logout(token);
    }
}
