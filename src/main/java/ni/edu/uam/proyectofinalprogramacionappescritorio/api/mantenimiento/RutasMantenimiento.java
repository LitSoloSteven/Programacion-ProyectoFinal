package ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.PeticionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.CambioEstadoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ListaSolicitudesMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ResumenGastosMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoUpdateRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.GastoPropiedadDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenGastosMantenimientoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.SolicitudMantenimientoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.MantenimientoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadIncompatibleException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TransicionInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Rutas HTTP para solicitudes de mantenimiento y gastos de propiedad (M08). */
public final class RutasMantenimiento {
    private final MantenimientoService servicio;

    public RutasMantenimiento(MantenimientoService servicio) {
        this.servicio = Objects.requireNonNull(servicio, "Falta el servicio de mantenimiento.");
    }

    public RespuestaJson listarSolicitudes(PeticionApi contexto) {
        UUID propiedadId = uuidOpcional(contexto.consulta("propiedadId"));
        UUID unidadId = uuidOpcional(contexto.consulta("unidadId"));
        String prioridad = textoOpcional(contexto.consulta("prioridad"));
        String estado = textoOpcional(contexto.consulta("estado"));
        List<SolicitudMantenimientoResponse> lista = ejecutar(contexto, () ->
                servicio.listarSolicitudes(SesionUsuario.getActual(), propiedadId, unidadId, prioridad, estado)
                        .stream().map(RutasMantenimiento::aResponse).toList());
        return RespuestaJson.ok(new ListaSolicitudesMantenimientoResponse(lista));
    }

    public RespuestaJson crearSolicitud(PeticionApi contexto) throws Exception {
        SolicitudMantenimientoRequest req = Json.leer(contexto.leerCuerpo(), SolicitudMantenimientoRequest.class);
        UUID unidadId = uuidRequerido(req.unidadId(), "unidadId");
        LocalDate abiertaEl = fechaOpcional(req.abiertaEl(), "abiertaEl");
        SolicitudMantenimientoDTO dto = ejecutar(contexto, () ->
                servicio.abrir(SesionUsuario.getActual(), unidadId, req.descripcion(),
                        req.prioridad(), req.responsable(), abiertaEl));
        return RespuestaJson.creado(aResponse(dto));
    }

    public RespuestaJson actualizarSolicitud(PeticionApi contexto) throws Exception {
        UUID id = uuidRequerido(contexto.parametro("id"), "id");
        SolicitudMantenimientoUpdateRequest req = Json.leer(
                contexto.leerCuerpo(), SolicitudMantenimientoUpdateRequest.class);
        if (req.version() == null || req.version() < 0) {
            throw ExcepcionApi.entidadNoProcesable("La versión de la solicitud es obligatoria.");
        }
        SolicitudMantenimientoDTO dto = ejecutar(contexto, () ->
                servicio.actualizarDetalles(SesionUsuario.getActual(), id, req.version(),
                        req.descripcion(), req.prioridad(), req.responsable()));
        return RespuestaJson.ok(aResponse(dto));
    }

    public RespuestaJson cambiarEstado(PeticionApi contexto) throws Exception {
        UUID id = uuidRequerido(contexto.parametro("id"), "id");
        CambioEstadoMantenimientoRequest req = Json.leer(
                contexto.leerCuerpo(), CambioEstadoMantenimientoRequest.class);
        if (req.version() == null || req.version() < 0) {
            throw ExcepcionApi.entidadNoProcesable("La versión de la solicitud es obligatoria.");
        }
        LocalDate fechaCierre = fechaOpcional(req.fechaCierre(), "fechaCierre");
        SolicitudMantenimientoDTO dto = ejecutar(contexto, () ->
                servicio.cambiarEstado(SesionUsuario.getActual(), id, req.version(),
                        req.estado(), fechaCierre, req.motivo()));
        return RespuestaJson.ok(aResponse(dto));
    }

    public RespuestaJson consultarGastos(PeticionApi contexto) {
        UUID propiedadId = uuidOpcional(contexto.consulta("propiedadId"));
        LocalDate desde = fechaOpcional(contexto.consulta("desde"), "desde");
        LocalDate hasta = fechaOpcional(contexto.consulta("hasta"), "hasta");
        String moneda = textoOpcional(contexto.consulta("moneda"));
        ResumenGastosMantenimientoDTO resumen = ejecutar(contexto, () ->
                servicio.consultarGastos(SesionUsuario.getActual(), propiedadId, desde, hasta, moneda));
        return RespuestaJson.ok(new ResumenGastosMantenimientoResponse(
                resumen.gastos().stream().map(RutasMantenimiento::aResponse).toList(),
                montoTexto(resumen.totalUsd()),
                montoTexto(resumen.totalNio())));
    }

    public RespuestaJson registrarGasto(PeticionApi contexto) throws Exception {
        GastoMantenimientoRequest req = Json.leer(contexto.leerCuerpo(), GastoMantenimientoRequest.class);
        UUID propiedadId = uuidRequerido(req.propiedadId(), "propiedadId");
        UUID solicitudId = uuidOpcional(req.solicitudId());
        LocalDate fechaGasto = fechaOpcional(req.fechaGasto(), "fechaGasto");
        BigDecimal importe = decimalRequerido(req.importe(), "importe");
        GastoPropiedadDTO dto = ejecutar(contexto, () ->
                servicio.registrarGasto(SesionUsuario.getActual(), propiedadId, solicitudId,
                        fechaGasto, importe, req.moneda(), req.concepto()));
        return RespuestaJson.creado(aResponse(dto));
    }

    private static <T> T ejecutar(PeticionApi contexto, Supplier<T> operacion) {
        try {
            return SesionUsuario.ejecutarComo(contexto.sesion().comoUsuario(), operacion);
        } catch (PropiedadIncompatibleException e) {
            throw new ExcepcionApi(422, PropiedadIncompatibleException.CODIGO, e.getMessage());
        } catch (TransicionInvalidaException e) {
            throw new ExcepcionApi(422, TransicionInvalidaException.CODIGO, e.getMessage());
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        } catch (SecurityException e) {
            throw ExcepcionApi.sinPermiso();
        }
    }

    private static SolicitudMantenimientoResponse aResponse(SolicitudMantenimientoDTO dto) {
        return new SolicitudMantenimientoResponse(
                dto.id().toString(),
                dto.unidadId().toString(),
                dto.unidadCodigo(),
                dto.propiedadId() == null ? null : dto.propiedadId().toString(),
                dto.propiedadNombre(),
                dto.descripcion(),
                dto.prioridad(),
                dto.estado(),
                dto.responsable(),
                dto.abiertaEl() == null ? null : dto.abiertaEl().toString(),
                dto.cerradaEl() == null ? null : dto.cerradaEl().toString(),
                dto.motivoCancelacion(),
                dto.detalleResolucion(),
                montoTexto(dto.totalGastoUsd()),
                montoTexto(dto.totalGastoNio()),
                RelojService.presentar(dto.creadoEn()),
                dto.creadoPor(),
                dto.version());
    }

    private static GastoMantenimientoResponse aResponse(GastoPropiedadDTO dto) {
        return new GastoMantenimientoResponse(
                dto.id().toString(),
                dto.propiedadId().toString(),
                dto.propiedadNombre(),
                dto.solicitudId() == null ? null : dto.solicitudId().toString(),
                dto.solicitudDescripcion(),
                dto.fechaGasto().toString(),
                montoTexto(dto.importe()),
                dto.moneda(),
                dto.concepto(),
                RelojService.presentar(dto.creadoEn()),
                dto.creadoPor());
    }

    private static String montoTexto(BigDecimal monto) {
        return (monto == null ? BigDecimal.ZERO : monto).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static UUID uuidRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.entidadNoProcesable("El campo '" + campo + "' es obligatorio.");
        }
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable("El identificador '" + campo + "' no es válido.");
        }
    }

    private static UUID uuidOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable("Uno de los identificadores enviados no es válido.");
        }
    }

    private static LocalDate fechaOpcional(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException e) {
            throw ExcepcionApi.entidadNoProcesable("La fecha '" + campo + "' debe usar formato AAAA-MM-DD.");
        }
    }

    private static BigDecimal decimalRequerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.entidadNoProcesable("El campo '" + campo + "' es obligatorio.");
        }
        try {
            return new BigDecimal(valor.strip());
        } catch (NumberFormatException e) {
            throw ExcepcionApi.entidadNoProcesable("El campo '" + campo + "' debe ser numérico.");
        }
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
