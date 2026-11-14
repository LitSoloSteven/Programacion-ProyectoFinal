package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ContextoPeticion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.AnulacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * API 4: cobranza y pagos. La escritura de pagos exige Idempotency-Key; la
 * anulación es exclusiva del propietario y el estado de cuenta se calcula a
 * una fecha de corte.
 */
public final class RutasPagos {
    private final ServicioPagos servicio;

    public RutasPagos(ServicioPagos servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    public void registrar(Router router) {
        router.registrar("POST", "/api/v1/pagos", false, this::registrarPago);
        router.registrar("GET", "/api/v1/pagos/{id}", false, this::obtenerPago);
        router.registrar("GET", "/api/v1/recibos/{numero}", false, this::obtenerRecibo);
        router.registrar("POST", "/api/v1/pagos/{id}/anulacion", false, this::anularPago);
        router.registrar("GET", "/api/v1/contratos/{id}/estado-cuenta", false,
                this::estadoCuenta);
    }

    private RespuestaJson registrarPago(ContextoPeticion contexto) throws Exception {
        String clave = contexto.intercambio().getRequestHeaders().getFirst("Idempotency-Key");
        if (clave == null || clave.isBlank()) {
            throw ExcepcionApi.solicitudInvalida(
                    "La cabecera Idempotency-Key es obligatoria.");
        }
        PagoRequest solicitud = leer(contexto, PagoRequest.class);
        try {
            return RespuestaJson.creado(enSesion(contexto,
                    () -> servicio.registrar(clave, solicitud)));
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
    }

    private RespuestaJson obtenerPago(ContextoPeticion contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerPago(id)));
    }

    private RespuestaJson obtenerRecibo(ContextoPeticion contexto) {
        long numero = numeroRecibo(contexto.parametro("numero"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerRecibo(numero)));
    }

    private RespuestaJson anularPago(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        AnulacionRequest solicitud = leer(contexto, AnulacionRequest.class);
        try {
            return RespuestaJson.ok(enSesion(contexto,
                    () -> servicio.anular(id, solicitud.motivo())));
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
    }

    private RespuestaJson estadoCuenta(ContextoPeticion contexto) {
        UUID contratoId = identificador(contexto.parametro("id"));
        LocalDate corte = fechaOpcional(contexto.consulta("corte"));
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.estadoCuenta(contratoId, corte)));
    }

    private static <T> T leer(ContextoPeticion contexto, Class<T> tipo) throws Exception {
        return Json.leer(LectorCuerpo.leerJson(contexto.intercambio()), tipo);
    }

    private static <T> T enSesion(ContextoPeticion contexto, Supplier<T> operacion) {
        return SesionUsuario.ejecutarComo(contexto.sesion().comoUsuario(), operacion);
    }

    private static UUID identificador(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida("El identificador no es válido.");
        }
    }

    private static long numeroRecibo(String valor) {
        try {
            long numero = Long.parseLong(valor);
            if (numero <= 0) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El número de recibo debe ser un entero positivo.");
        }
    }

    private static LocalDate fechaOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El parámetro corte debe tener el formato AAAA-MM-DD.");
        }
    }
}