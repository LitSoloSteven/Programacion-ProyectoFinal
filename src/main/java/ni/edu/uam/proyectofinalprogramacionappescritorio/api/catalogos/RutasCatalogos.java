package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadEstadoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.PeticionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
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

    // ------------------------------------------------------------------
    // Propiedades
    // ------------------------------------------------------------------

    public RespuestaJson listarPropiedades(PeticionApi contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        Boolean activa = booleano(contexto.consulta("activa"), "activa");
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarPropiedades(pagina, tamano, texto, activa)));
    }

    public RespuestaJson obtenerPropiedad(PeticionApi contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerPropiedad(id)));
    }

    public RespuestaJson crearPropiedad(PeticionApi contexto) throws Exception {
        PropiedadRequest solicitud = leer(contexto, PropiedadRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearPropiedad(solicitud)));
    }

    public RespuestaJson actualizarPropiedad(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        PropiedadRequest solicitud = leer(contexto, PropiedadRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarPropiedad(id, version, solicitud)));
    }

    public RespuestaJson cambiarEstadoPropiedad(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        PropiedadEstadoRequest solicitud = leer(contexto, PropiedadEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoPropiedad(id,
                        booleanoRequerido(solicitud.activa(), "activa"))));
    }

    // ------------------------------------------------------------------
    // Unidades
    // ------------------------------------------------------------------

    public RespuestaJson listarUnidades(PeticionApi contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        UUID propiedadId = uuidOpcional(contexto.consulta("propiedadId"), "propiedadId");
        String estado = contexto.consulta("estado");
        String tipo = contexto.consulta("tipo");
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarUnidades(pagina, tamano, propiedadId, estado, tipo, texto)));
    }

    public RespuestaJson obtenerUnidad(PeticionApi contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerUnidad(id)));
    }

    public RespuestaJson crearUnidad(PeticionApi contexto) throws Exception {
        UnidadRequest solicitud = leer(contexto, UnidadRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearUnidad(solicitud)));
    }

    public RespuestaJson actualizarUnidad(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        UnidadRequest solicitud = leer(contexto, UnidadRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarUnidad(id, version, solicitud)));
    }

    public RespuestaJson cambiarEstadoUnidad(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        UnidadEstadoRequest solicitud = leer(contexto, UnidadEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoUnidad(id, solicitud.estado())));
    }

    // ------------------------------------------------------------------
    // Inquilinos
    // ------------------------------------------------------------------

    public RespuestaJson listarInquilinos(PeticionApi contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        Boolean activo = booleano(contexto.consulta("activo"), "activo");
        String texto = contexto.consulta("texto");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarInquilinos(pagina, tamano, activo, texto)));
    }

    public RespuestaJson obtenerInquilino(PeticionApi contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerInquilino(id)));
    }

    public RespuestaJson crearInquilino(PeticionApi contexto) throws Exception {
        InquilinoRequest solicitud = leer(contexto, InquilinoRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearInquilino(solicitud)));
    }

    public RespuestaJson actualizarInquilino(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        InquilinoRequest solicitud = leer(contexto, InquilinoRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarInquilino(id, version, solicitud)));
    }

    public RespuestaJson cambiarEstadoInquilino(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        InquilinoEstadoRequest solicitud = leer(contexto, InquilinoEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoInquilino(id,
                        booleanoRequerido(solicitud.activo(), "activo"))));
    }

    // ------------------------------------------------------------------
    // Contratos
    // ------------------------------------------------------------------

    public RespuestaJson listarContratos(PeticionApi contexto) {
        int pagina = pagina(contexto.consulta("pagina"));
        int tamano = tamano(contexto.consulta("tamano"));
        String estado = contexto.consulta("estado");
        UUID unidadId = uuidOpcional(contexto.consulta("unidadId"), "unidadId");
        UUID inquilinoId = uuidOpcional(contexto.consulta("inquilinoId"), "inquilinoId");
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.listarContratos(pagina, tamano, estado, unidadId, inquilinoId)));
    }

    public RespuestaJson obtenerContrato(PeticionApi contexto) {
        UUID id = identificador(contexto.parametro("id"));
        return RespuestaJson.ok(enSesion(contexto, () -> servicio.obtenerContrato(id)));
    }

    public RespuestaJson crearContrato(PeticionApi contexto) throws Exception {
        ContratoRequest solicitud = leer(contexto, ContratoRequest.class);
        return RespuestaJson.creado(enSesion(contexto, () -> servicio.crearContrato(solicitud)));
    }

    public RespuestaJson actualizarContrato(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        ContratoRequest solicitud = leer(contexto, ContratoRequest.class);
        long version = version(solicitud.version());
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.actualizarContrato(id, version, solicitud)));
    }

    public RespuestaJson cambiarEstadoContrato(PeticionApi contexto) throws Exception {
        UUID id = identificador(contexto.parametro("id"));
        ContratoEstadoRequest solicitud = leer(contexto, ContratoEstadoRequest.class);
        return RespuestaJson.ok(enSesion(contexto,
                () -> servicio.cambiarEstadoContrato(id, solicitud.estado())));
    }

    // ------------------------------------------------------------------
    // Utilidades de frontera
    // ------------------------------------------------------------------

    private static <T> T leer(PeticionApi contexto, Class<T> tipo) throws Exception {
        return Json.leer(contexto.leerCuerpo(), tipo);
    }

    private <T> T enSesion(PeticionApi contexto, Supplier<T> operacion) {
        return SesionUsuario.ejecutarComo(contexto.sesion().comoUsuario(),
                () -> servicio.enTransaccion(operacion));
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

    private static boolean booleanoRequerido(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + nombre + " es obligatorio.");
        }
        if ("true".equalsIgnoreCase(valor.strip())) return true;
        if ("false".equalsIgnoreCase(valor.strip())) return false;
        throw ExcepcionApi.entidadNoProcesable(
                "El campo " + nombre + " debe ser true o false.");
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
