package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Cliente HTTP administrativo (usuarios, configuración, emisor, auditoría y respaldos). */
public final class ClienteAdmin {
    private static final Duration TIMEOUT_RESPALDOS = Duration.ofSeconds(120);

    private final ClienteApi api;

    public ClienteAdmin(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public List<UsuarioResponse> listarUsuarios() {
        UsuarioResponse[] arr = api.get("/api/v1/usuarios", UsuarioResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public UsuarioResponse crearUsuario(String nombre, char[] password, String rol) {
        String clave = password == null ? "" : new String(password);
        return api.post("/api/v1/usuarios", new CrearUsuarioRequest(nombre, clave, rol), UsuarioResponse.class);
    }

    public UsuarioResponse cambiarRol(long id, String rol) {
        return api.post("/api/v1/usuarios/" + id + "/rol", new CambiarRolRequest(rol), UsuarioResponse.class);
    }

    public UsuarioResponse activarUsuario(long id) {
        return api.patch("/api/v1/usuarios/" + id + "/estado",
                new CambiarEstadoUsuarioRequest(true), UsuarioResponse.class);
    }

    public UsuarioResponse desactivarUsuario(long id) {
        return api.patch("/api/v1/usuarios/" + id + "/estado",
                new CambiarEstadoUsuarioRequest(false), UsuarioResponse.class);
    }

    public UsuarioResponse restablecerPassword(long id, char[] nuevaPassword) {
        String clave = nuevaPassword == null ? "" : new String(nuevaPassword);
        return api.post("/api/v1/usuarios/" + id + "/restablecer-password",
                new RestablecerPasswordRequest(clave), UsuarioResponse.class);
    }

    public void desbloquearCuenta(long id) {
        api.post("/api/v1/usuarios/" + id + "/desbloquear", MapVacio.VACIO, Void.class);
    }

    public int leerMinutosInactividad() {
        InactividadResponse res = api.get("/api/v1/configuracion/inactividad", InactividadResponse.class);
        return res == null ? 15 : res.minutos();
    }

    public PerfilEmisorResponse obtenerEmisor() {
        return api.get("/api/v1/admin/emisor", PerfilEmisorResponse.class);
    }

    public ParametroHistorialResponse guardarEmisor(PerfilEmisorRequest request) {
        return api.put("/api/v1/admin/emisor", request, ParametroHistorialResponse.class);
    }

    public List<ParametroHistorialResponse> listarParametros(String claveOpcional) {
        String ruta = claveOpcional == null || claveOpcional.isBlank()
                ? "/api/v1/admin/parametros"
                : "/api/v1/admin/parametros?clave=" + codificar(claveOpcional.strip());
        ParametroHistorialResponse[] arr = api.get(ruta, ParametroHistorialResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public ParametroHistorialResponse registrarParametro(String clave, String valor,
                                                         Instant vigenteDesde, String motivo) {
        RegistrarParametroRequest req = new RegistrarParametroRequest(
                clave, valor, vigenteDesde == null ? null : vigenteDesde.toString(), motivo);
        return api.post("/api/v1/admin/parametros", req, ParametroHistorialResponse.class);
    }

    public List<AuditoriaRegistroResponse> consultarAuditoria(String usuario, String accion,
                                                              String desde, String hasta, Integer limite) {
        List<String> params = new ArrayList<>();
        if (usuario != null && !usuario.isBlank()) params.add("usuario=" + codificar(usuario.strip()));
        if (accion != null && !accion.isBlank()) params.add("accion=" + codificar(accion.strip()));
        if (desde != null && !desde.isBlank()) params.add("desde=" + codificar(desde.strip()));
        if (hasta != null && !hasta.isBlank()) params.add("hasta=" + codificar(hasta.strip()));
        if (limite != null && limite > 0) params.add("limite=" + limite);
        String ruta = "/api/v1/admin/auditoria" + (params.isEmpty() ? "" : "?" + String.join("&", params));
        AuditoriaRegistroResponse[] arr = api.get(ruta, AuditoriaRegistroResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public EstadoRespaldoResponse consultarEstadoAlertaRespaldo() {
        return api.get("/api/v1/admin/respaldos/estado", EstadoRespaldoResponse.class);
    }

    public ResultadoRespaldoResponse crearRespaldo(String directorioDestino, char[] password) {
        String clave = password == null ? "" : new String(password);
        return api.post("/api/v1/admin/respaldos",
                new CrearRespaldoRequest(directorioDestino, clave),
                ResultadoRespaldoResponse.class, TIMEOUT_RESPALDOS);
    }

    public ResultadoVerificacionRespaldoResponse verificarRespaldo(String archivoRespaldo, char[] password) {
        String clave = password == null ? "" : new String(password);
        return api.post("/api/v1/admin/respaldos/verificar",
                new VerificarRespaldoRequest(archivoRespaldo, clave),
                ResultadoVerificacionRespaldoResponse.class, TIMEOUT_RESPALDOS);
    }

    public ResultadoRestauracionResponse restaurarRespaldo(RestaurarRespaldoRequest request) {
        return api.post("/api/v1/admin/respaldos/restaurar", request,
                ResultadoRestauracionResponse.class, TIMEOUT_RESPALDOS);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private record MapVacio() {
        static final MapVacio VACIO = new MapVacio();
    }
}
