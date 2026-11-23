package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.PeticionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
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

    public RespuestaJson listarPagos(PeticionApi contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarPagos(pagina, tamano, texto)));
    }

    public RespuestaJson listarCargos(PeticionApi contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarCargos(pagina, tamano, texto)));
    }

    public RespuestaJson registrarPago(PeticionApi contexto) throws Exception {
        String clave = contexto.cabecera("Idempotency-Key");
        if (clave == null || clave.isBlank()) {
            throw ExcepcionApi.solicitudInvalida(
                    "La cabecera Idempotency-Key es obligatoria.");
        }
        PagoRequest solicitud = leer(contexto, PagoRequest.class);
        try {
            return RespuestaJson.creado(SesionUsuario.ejecutarComo(
                    contexto.sesion().comoUsuario(), () -> servicio.registrar(clave, solicitud)));
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
    }

    public RespuestaJson obtenerPago(PeticionApi contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerPago(id)));
    }

    public RespuestaJson obtenerRecibo(PeticionApi contexto) {
        long numero = numeroRecibo(contexto.parametro("numero"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerRecibo(numero)));
    }

    public RespuestaJson anularPago(PeticionApi contexto) throws Exception {
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

    public RespuestaJson estadoCuenta(PeticionApi contexto) {
        UUID contratoId = identificador(contexto.parametro("id"));
        LocalDate corte = fechaOpcional(contexto.consulta("corte"));
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.estadoCuenta(contratoId, corte)));
    }

    private static <T> T leer(PeticionApi contexto, Class<T> tipo) throws Exception {
        return Json.leer(contexto.leerCuerpo(), tipo);
    }

    private <T> T enSesion(PeticionApi contexto, Supplier<T> operacion) {
        return SesionUsuario.ejecutarComo(contexto.sesion().comoUsuario(),
                () -> servicio.enTransaccion(operacion));
    }

    private static UUID identificador(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida("El identificador no es válido.");
        }
    }

    private static int pagina(String valor) {
        if (valor == null || valor.isBlank()) return 1;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < 1 || numero > ServicioPagos.PAGINA_MAXIMA) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("La página debe ser un entero entre 1 y "
                    + ServicioPagos.PAGINA_MAXIMA + ".");
        }
    }

    private static int tamano(String valor) {
        if (valor == null || valor.isBlank()) return ServicioPagos.TAMANO_PAGINA_PREDETERMINADO;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < 1 || numero > ServicioPagos.TAMANO_PAGINA_MAXIMO) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("El tamaño debe ser un entero entre 1 y "
                    + ServicioPagos.TAMANO_PAGINA_MAXIMO + ".");
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
