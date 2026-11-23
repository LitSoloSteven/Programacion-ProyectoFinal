package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

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
        LoginResponse respuesta = api.post("/api/v1/auth/login",
                new LoginRequest(usuario, password), LoginResponse.class);
        api.establecerToken(respuesta.token());
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
        logout(api.tomarToken());
    }
}