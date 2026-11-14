package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ContextoPeticion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.LectorCuerpo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * API 3: catálogos de propiedades, unidades, inquilinos y contratos.
 * Lecturas paginadas y escrituras con control de versión; la baja es lógica
 * mediante PATCH.
 */
public final class RutasCatalogos {
    private final ServicioCatalogos servicio;

    public RutasCatalogos(ServicioCatalogos servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    public void registrar(Router router) {
        router.registrar("GET", "/api/v1/propiedades", false, this::listarPropiedades);
        router.registrar("GET", "/api/v1/propiedades/{id}", false, this::obtenerPropiedad);
        router.registrar("POST", "/api/v1/propiedades", false, this::crearPropiedad);
        router.registrar("PUT", "/api/v1/propiedades/{id}", false, this::actualizarPropiedad);
        router.registrar("PATCH", "/api/v1/propiedades/{id}", false, this::cambiarEstadoPropiedad);

        router.registrar("GET", "/api/v1/unidades", false, this::listarUnidades);
        router.registrar("GET", "/api/v1/unidades/{id}", false, this::obtenerUnidad);
        router.registrar("POST", "/api/v1/unidades", false, this::crearUnidad);
        router.registrar("PUT", "/api/v1/unidades/{id}", false, this::actualizarUnidad);
        router.registrar("PATCH", "/api/v1/unidades/{id}", false, this::cambiarEstadoUnidad);

        router.registrar("GET", "/api/v1/inquilinos", false, this::listarInquilinos);
        router.registrar("GET", "/api/v1/inquilinos/{id}", false, this::obtenerInquilino);
        router.registrar("POST", "/api/v1/inquilinos", false, this::crearInquilino);
        router.registrar("PUT", "/api/v1/inquilinos/{id}", false, this::actualizarInquilino);
        router.registrar("PATCH", "/api/v1/inquilinos/{id}", false, this::cambiarEstadoInquilino);

        router.registrar("GET", "/api/v1/contratos", false, this::listarContratos);
        router.registrar("GET", "/api/v1/contratos/{id}", false, this::obtenerContrato);
        router.registrar("POST", "/api/v1/contratos", false, this::crearContrato);
        router.registrar("PUT", "/api/v1/contratos/{id}", false, this::actualizarContrato);
        router.registrar("PATCH", "/api/v1/contratos/{id}", false, this::cambiarEstadoContrato);
    }

    // ------------------------------------------------------------------
    // Propiedades
    // ------------------------------------------------------------------

    private RespuestaJson listarPropiedades(ContextoPeticion contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        Boolean activa = booleano(contexto.consulta("activa"), "activa");
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarPropiedades(pagina, tamano, texto, activa)));
    }

    private RespuestaJson obtenerPropiedad(ContextoPeticion contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerPropiedad(id)));
    }

    private RespuestaJson crearPropiedad(ContextoPeticion contexto) throws Exception {
        PropiedadRequest solicitud = leer(contexto, PropiedadRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearPropiedad(solicitud)));
    }

    private RespuestaJson actualizarPropiedad(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        PropiedadRequest solicitud = leer(contexto, PropiedadRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarPropiedad(id, version, solicitud)));
    }

    private RespuestaJson cambiarEstadoPropiedad(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        PropiedadEstadoRequest solicitud = leer(contexto, PropiedadEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoPropiedad(id, solicitud.activa())));
    }

    // ------------------------------------------------------------------
    // Unidades
    // ------------------------------------------------------------------

    private RespuestaJson listarUnidades(ContextoPeticion contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        UUID propiedadId = uuidOpcional(contexto.consulta("propiedadId"), "propiedadId");
        String estado = contexto.consulta("estado");
        String tipo = contexto.consulta("tipo");
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarUnidades(pagina, tamano, propiedadId, estado, tipo, texto)));
    }

    private RespuestaJson obtenerUnidad(ContextoPeticion contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerUnidad(id)));
    }

    private RespuestaJson crearUnidad(ContextoPeticion contexto) throws Exception {
        UnidadRequest solicitud = leer(contexto, UnidadRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearUnidad(solicitud)));
    }

    private RespuestaJson actualizarUnidad(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        UnidadRequest solicitud = leer(contexto, UnidadRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarUnidad(id, version, solicitud)));
    }

    private RespuestaJson cambiarEstadoUnidad(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        UnidadEstadoRequest solicitud = leer(contexto, UnidadEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoUnidad(id, solicitud.estado())));
    }

    // ------------------------------------------------------------------
    // Inquilinos
    // ------------------------------------------------------------------

    private RespuestaJson listarInquilinos(ContextoPeticion contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        Boolean activo = booleano(contexto.consulta("activo"), "activo");
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarInquilinos(pagina, tamano, activo, texto)));
    }

    private RespuestaJson obtenerInquilino(ContextoPeticion contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerInquilino(id)));
    }

    private RespuestaJson crearInquilino(ContextoPeticion contexto) throws Exception {
        InquilinoRequest solicitud = leer(contexto, InquilinoRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearInquilino(solicitud)));
    }

    private RespuestaJson actualizarInquilino(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        InquilinoRequest solicitud = leer(contexto, InquilinoRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarInquilino(id, version, solicitud)));
    }

    private RespuestaJson cambiarEstadoInquilino(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        InquilinoEstadoRequest solicitud = leer(contexto, InquilinoEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoInquilino(id, solicitud.activo())));
    }

    // ------------------------------------------------------------------
    // Contratos
    // ------------------------------------------------------------------

    private RespuestaJson listarContratos(ContextoPeticion contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        String estado = contexto.consulta("estado");
        UUID unidadId = uuidOpcional(contexto.consulta("unidadId"), "unidadId");
        UUID inquilinoId = uuidOpcional(contexto.consulta("inquilinoId"), "inquilinoId");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarContratos(pagina, tamano, estado, unidadId, inquilinoId)));
    }

    private RespuestaJson obtenerContrato(ContextoPeticion contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerContrato(id)));
    }

    private RespuestaJson crearContrato(ContextoPeticion contexto) throws Exception {
        ContratoRequest solicitud = leer(contexto, ContratoRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearContrato(solicitud)));
    }

    private RespuestaJson actualizarContrato(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        ContratoRequest solicitud = leer(contexto, ContratoRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarContrato(id, version, solicitud)));
    }

    private RespuestaJson cambiarEstadoContrato(ContextoPeticion contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        ContratoEstadoRequest solicitud = leer(contexto, ContratoEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoContrato(id, solicitud.estado())));
    }

    // ------------------------------------------------------------------
    // Utilidades de frontera
    // ------------------------------------------------------------------

    private static <T> T leer(ContextoPeticion contexto, Class<T> tipo) throws Exception {
        return Json.leer(LectorCuerpo.leerJson(contexto.intercambio()), tipo);
    }

    private static <T> T enSesion(ContextoPeticion contexto, Supplier<T> operacion) {
        return SesionUsuario.ejecutarComo(contexto.sesion().comoUsuario(), operacion);
    }

    private static long version(Long valor) {
        if (valor == null || valor < 0) {
            throw ExcepcionApi.solicitudInvalida("La versión del registro es obligatoria.");
        }
        return valor;
    }

    private static int pagina(String valor) {
        if (valor == null || valor.isBlank()) return 1;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < 1 || numero > ServicioCatalogos.PAGINA_MAXIMA) throw new NumberFormatException();
            return numero;
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("La página debe ser un entero entre 1 y "
                    + ServicioCatalogos.PAGINA_MAXIMA + ".");
        }
    }

    private static int tamano(String valor) {
        if (valor == null || valor.isBlank()) return ServicioCatalogos.TAMANO_PREDETERMINADO;
        try {
            int numero = Integer.parseInt(valor.strip());
            if (numero < 1 || numero > ServicioCatalogos.TAMANO_MAXIMO) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("El tamaño debe ser un entero entre 1 y "
                    + ServicioCatalogos.TAMANO_MAXIMO + ".");
        }
    }

    private static Boolean booleano(String valor, String nombre) {
        if (valor == null || valor.isBlank()) return null;
        if ("true".equalsIgnoreCase(valor.strip())) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(valor.strip())) return Boolean.FALSE;
        throw ExcepcionApi.solicitudInvalida(
                "El parámetro " + nombre + " debe ser true o false.");
    }

    private static UUID uuidOpcional(String valor, String nombre) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El parámetro " + nombre + " no es un identificador válido.");
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