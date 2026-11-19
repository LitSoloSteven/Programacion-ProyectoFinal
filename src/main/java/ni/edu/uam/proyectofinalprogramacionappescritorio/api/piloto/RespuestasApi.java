package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import jakarta.servlet.http.HttpServletResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ErrorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojDesfasadoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class RespuestasApi {
    static final String JSON_UTF8 = "application/json; charset=UTF-8";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> manejar(Exception error) {
        ExcepcionApi api = traducir(error);
        return respuesta(api.getEstado()).body(cuerpo(api));
    }

    static ResponseEntity<String> json(int estado, Object cuerpo) {
        return respuesta(estado).body(Json.escribir(cuerpo));
    }

    static ResponseEntity<String> json(RespuestaJson respuesta) {
        return json(respuesta.estado(), respuesta.cuerpo());
    }

    private static ResponseEntity.BodyBuilder respuesta(int estado) {
        var respuesta = ResponseEntity.status(estado).header("Content-Type", JSON_UTF8);
        if (estado == 401) respuesta.header("WWW-Authenticate", "Bearer");
        return respuesta;
    }

    static void escribir(HttpServletResponse respuesta, ExcepcionApi error) throws IOException {
        respuesta.setStatus(error.getEstado());
        respuesta.setContentType(JSON_UTF8);
        if (error.getEstado() == 401) respuesta.setHeader("WWW-Authenticate", "Bearer");
        respuesta.getWriter().write(cuerpo(error));
    }

    static String cuerpo(ExcepcionApi error) {
        return Json.escribir(new ErrorApi.Cuerpo(error.getCodigo(), error.getMessage()));
    }

    static ExcepcionApi traducir(Exception error) {
        return switch (error) {
            case ExcepcionApi e -> e;
            case IllegalArgumentException e ->
                    ExcepcionApi.solicitudInvalida("La solicitud contiene datos inválidos.");
            case SecurityException e -> ExcepcionApi.sinPermiso();
            case ConflictoDatosException e -> ExcepcionApi.conflicto(e.getMessage());
            case RelojDesfasadoException e -> ExcepcionApi.relojDesfasado();
            case ErrorResponse e -> porEstado(e.getStatusCode().value());
            default -> ExcepcionApi.interno();
        };
    }

    static ExcepcionApi porEstado(int estado) {
        return switch (estado) {
            case 401 -> ExcepcionApi.noAutorizado();
            case 403 -> ExcepcionApi.sinPermiso();
            case 404 -> ExcepcionApi.noEncontrado();
            case 405 -> metodoNoPermitido();
            case 413 -> ExcepcionApi.cuerpoDemasiadoGrande();
            case 415 -> ExcepcionApi.contenidoNoSoportado();
            case 500 -> ExcepcionApi.interno();
            default -> new ExcepcionApi(estado, "SOLICITUD_INVALIDA", "La solicitud HTTP no es válida.");
        };
    }

    static ExcepcionApi metodoNoPermitido() {
        return new ExcepcionApi(405, "METODO_NO_PERMITIDO", "Método HTTP no permitido.");
    }
}
