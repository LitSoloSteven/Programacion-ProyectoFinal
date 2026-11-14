package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ContextoPeticion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.AutorizarTasaRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ListaTasasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * API 2: consulta, registro y autorización de tasas NIO por USD.
 * Las tasas informativas las registra cualquier operador; solo el propietario
 * puede autorizarlas, y la autorización queda auditada.
 */
public final class RutasTasas {
    private final TipoCambioService servicio;

    public RutasTasas(TipoCambioService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    public void registrar(Router router) {
        router.registrar("GET", "/api/v1/tasas", false, this::listar);
        router.registrar("GET", "/api/v1/tasas/vigente", false, this::vigente);
        router.registrar("GET", "/api/v1/tasas/{id}", false, this::obtener);
        router.registrar("POST", "/api/v1/tasas", false, this::registrarTasa);
        router.registrar("POST", "/api/v1/tasas/{id}/autorizar", false, this::autorizar);
    }

    private RespuestaJson listar(ContextoPeticion contexto) {
        LocalDate fecha = fechaOpcional(contexto.consulta("fecha"));
        Boolean autorizada = booleano(contexto.consulta("autorizada"));
        int limite = limite(contexto.consulta("limite"));

        ListaTasasResponse respuesta = enSesion(contexto, () -> {
            List<TasaResponse> tasas = servicio.listar(fecha, autorizada, limite)
                    .stream().map(RutasTasas::respuesta).toList();
            return new ListaTasasResponse(tasas);
        });
        return RespuestaJson.ok(respuesta);
    }

    private RespuestaJson vigente(ContextoPeticion contexto) {
        String valor = contexto.consulta("fecha");
        LocalDate fecha = valor == null || valor.isBlank() ? LocalDate.now() : fecha(valor);
        Optional<TipoCambio> tasa = enSesion(contexto, () -> servicio.vigente(fecha));
        return RespuestaJson.ok(respuesta(tasa.orElseThrow(() -> new ExcepcionApi(404,
                "TASA_NO_DISPONIBLE",
                "No hay una tasa autorizada para la fecha indicada."))));
    }

    private RespuestaJson obtener(ContextoPeticion contexto) {
        UUID id = identificador(contexto.parametro("id"));
        Optional<TipoCambio> tasa = enSesion(contexto, () -> servicio.obtener(id));
        return RespuestaJson.ok(respuesta(tasa.orElseThrow(ExcepcionApi::noEncontrado)));
    }

    private RespuestaJson registrarTasa(ContextoPeticion contexto) throws Exception {
        TasaRequest solicitud = Json.leer(LectorCuerpo.leerJson(contexto.intercambio()),
                TasaRequest.class);
        LocalDate fecha = fechaRequerida(solicitud.fecha());
        BigDecimal tasa = decimal(solicitud.tasa());
        if (solicitud.fuente() == null || solicitud.fuente().isBlank()) {
            throw ExcepcionApi.entidadNoProcesable("La fuente de la tasa es obligatoria.");
        }
        try {
            TipoCambio registro = enSesion(contexto,
                    () -> servicio.registrar(fecha, tasa, solicitud.fuente()));
            return RespuestaJson.creado(respuesta(registro));
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
    }

    private RespuestaJson autorizar(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        AutorizarTasaRequest solicitud = Json.leer(
                LectorCuerpo.leerJson(contexto.intercambio()), AutorizarTasaRequest.class);
        if (solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El motivo de autorización es obligatorio.");
        }
        try {
            Optional<TipoCambio> autorizada = enSesion(contexto,
                    () -> servicio.autorizar(id, solicitud.motivo()));
            return RespuestaJson.ok(respuesta(autorizada.orElseThrow(ExcepcionApi::noEncontrado)));
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
    }

    private static <T> T enSesion(ContextoPeticion contexto, Supplier<T> operacion) {
        return SesionUsuario.ejecutarComo(contexto.sesion().comoUsuario(), operacion);
    }

    private static TasaResponse respuesta(TipoCambio tasa) {
        String motivo = tasa.getMotivo();
        return new TasaResponse(
                tasa.getId().toString(),
                tasa.getFecha().toString(),
                tasa.getNioPorUsd().toPlainString(),
                tasa.getFuente(),
                tasa.getRegistradoEn().toString(),
                tasa.isAutorizada(),
                tasa.getAutorizadoPor() == null ? null : tasa.getAutorizadoPor().toString(),
                tasa.getAutorizadoPorNombre(),
                motivo == null || motivo.isBlank() ? null : motivo);
    }

    private static LocalDate fecha(String valor) {
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "La fecha debe tener el formato AAAA-MM-DD.");
        }
    }

    private static LocalDate fechaOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : fecha(valor);
    }

    private static LocalDate fechaRequerida(String valor) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.solicitudInvalida("La fecha de la tasa es obligatoria.");
        }
        return fecha(valor);
    }

    private static BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.entidadNoProcesable("La tasa es obligatoria.");
        }
        try {
            return new BigDecimal(valor.strip());
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("La tasa debe ser un número decimal.");
        }
    }

    private static Boolean booleano(String valor) {
        if (valor == null || valor.isBlank()) return null;
        if ("true".equalsIgnoreCase(valor.strip())) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(valor.strip())) return Boolean.FALSE;
        throw ExcepcionApi.solicitudInvalida(
                "El parámetro autorizada debe ser true o false.");
    }

    private static int limite(String valor) {
        if (valor == null || valor.isBlank()) return 50;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < 1 || numero > TipoCambioService.LIMITE_MAXIMO) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El límite debe ser un número entre 1 y " + TipoCambioService.LIMITE_MAXIMO + ".");
        }
    }

    private static UUID identificador(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida("El identificador no es válido.");
        }
    }
}