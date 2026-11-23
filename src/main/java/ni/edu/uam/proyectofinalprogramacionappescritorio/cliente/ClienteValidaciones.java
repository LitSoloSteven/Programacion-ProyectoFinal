package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionResponse;

import java.util.Objects;

/**
 * Cliente de la API de validaciones. Devuelve el resultado lógico del servidor
 * y deja que ClienteApi traduzca los errores HTTP en ErrorClienteException.
 */
public final class ClienteValidaciones {
    private final ClienteApi api;

    public ClienteValidaciones(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public ValidacionResponse correo(String correo) {
        return enviar(new ValidacionRequest("correo", correo, null, null, null));
    }

    public ValidacionResponse telefono(String telefono) {
        return enviar(new ValidacionRequest("telefono", telefono, null, null, null));
    }

    public ValidacionResponse cedula(String cedula) {
        return enviar(new ValidacionRequest("cedula", cedula, null, null, null));
    }

    public ValidacionResponse monto(String monto) {
        return enviar(new ValidacionRequest("monto", monto, null, null, null));
    }

    public ValidacionResponse rangoFechas(String inicio, String fin, Integer mesesMinimos) {
        return enviar(new ValidacionRequest("rangoFechas", null, inicio, fin, mesesMinimos));
    }

    private ValidacionResponse enviar(ValidacionRequest solicitud) {
        ValidacionResponse respuesta = api.post("/api/v1/validaciones",
                solicitud, ValidacionResponse.class);
        return respuesta == null
                ? new ValidacionResponse(false, "El servicio no devolvió un resultado.")
                : respuesta;
    }
}