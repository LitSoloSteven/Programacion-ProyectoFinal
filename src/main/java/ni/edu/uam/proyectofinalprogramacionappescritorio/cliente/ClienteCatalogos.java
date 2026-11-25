package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import lombok.RequiredArgsConstructor;

import com.google.gson.reflect.TypeToken;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Adapta los catálogos HTTP a las filas y formularios existentes de JavaFX. */
@RequiredArgsConstructor
public final class ClienteCatalogos {
    private static final int TAMANO = 50;
    private final ClienteApi api;

    public List<Propiedad> propiedades(boolean soloActivas) {
        return listar("propiedades", soloActivas ? "&activa=true" : "",
                PropiedadResponse.class, ClienteCatalogos::propiedad);
    }

    public List<Unidad> unidades() {
        return listar("unidades", "", UnidadResponse.class, ClienteCatalogos::unidad);
    }

    public List<Inquilino> inquilinos(boolean soloActivos) {
        return listar("inquilinos", soloActivos ? "&activo=true" : "",
                InquilinoResponse.class, ClienteCatalogos::inquilino);
    }

    public List<Contrato> contratos(boolean soloVigentes) {
        return listar("contratos", soloVigentes ? "&estado=Vigente" : "",
                ContratoResponse.class, ClienteCatalogos::contrato);
    }

    public void guardarPropiedad(UUID id, PropiedadRequest solicitud) {
        guardar("propiedades", id, solicitud);
    }

    public void guardarUnidad(UUID id, UnidadRequest solicitud) {
        guardar("unidades", id, solicitud);
    }

    public void guardarInquilino(UUID id, InquilinoRequest solicitud) {
        guardar("inquilinos", id, solicitud);
    }

    public void guardarContrato(UUID id, ContratoRequest solicitud) {
        guardar("contratos", id, solicitud);
    }

    public void darDeBajaInquilino(UUID id) {
        api.patch(ruta("inquilinos") + "/" + id, new InquilinoEstadoRequest("false"), Void.class);
    }

    public void terminarContrato(UUID id) {
        api.patch(ruta("contratos") + "/" + id, new ContratoEstadoRequest("Terminado"), Void.class);
    }

    public Contrato activarContrato(UUID id, long version) {
        return contrato(api.post(ruta("contratos") + "/" + id + "/activar",
                new ContratoVersionRequest(version), ContratoResponse.class));
    }

    public Contrato cancelarBorrador(UUID id, long version, String motivo) {
        return contrato(api.post(ruta("contratos") + "/" + id + "/cancelar",
                new ContratoCancelarRequest(version, motivo), ContratoResponse.class));
    }

    public Contrato renovarContrato(UUID anteriorId, ContratoRenovarRequest solicitud) {
        return contrato(api.post(ruta("contratos") + "/" + anteriorId + "/renovar",
                solicitud, ContratoResponse.class));
    }

    public Contrato terminarContratoConCierre(UUID id, ContratoTerminarRequest solicitud) {
        return contrato(api.post(ruta("contratos") + "/" + id + "/terminar",
                solicitud, ContratoResponse.class));
    }

    public GarantiaResumenResponse obtenerGarantia(UUID contratoId) {
        return api.get(ruta("contratos") + "/" + contratoId + "/garantia",
                GarantiaResumenResponse.class);
    }

    public GarantiaResumenResponse liquidarGarantia(UUID contratoId, UUID liquidacionId,
                                                    GarantiaLiquidacionRequest solicitud) {
        return api.postConCabecera(
                ruta("contratos") + "/" + contratoId + "/garantia/movimientos",
                solicitud, GarantiaResumenResponse.class,
                "Idempotency-Key", liquidacionId.toString());
    }

    private void guardar(String recurso, UUID id, Object cuerpo) {
        String ruta = ruta(recurso);
        if (id == null) {
            api.post(ruta, cuerpo, Void.class);
        } else {
            api.put(ruta + "/" + id, cuerpo, Void.class);
        }
    }

    private <R, T> List<T> listar(String recurso, String filtros, Class<R> tipo,
                                   Function<R, T> convertir) {
        var tipoPagina = TypeToken.getParameterized(PaginaResponse.class, tipo).getType();
        List<T> filas = new ArrayList<>();
        for (int pagina = 1; ; pagina++) {
            PaginaResponse<R> respuesta = api.get(
                    ruta(recurso) + "?pagina=" + pagina + "&tamano=" + TAMANO + filtros, tipoPagina);
            respuesta.elementos().stream().map(convertir).forEach(filas::add);
            if (respuesta.elementos().isEmpty() || (long) pagina * TAMANO >= respuesta.total()) {
                return filas;
            }
        }
    }

    private static String ruta(String recurso) {
        return "/api/v1/" + recurso;
    }

    private static Propiedad propiedad(PropiedadResponse datos) {
        Propiedad fila = new Propiedad(UUID.fromString(datos.id()), datos.nombre(), datos.direccion(),
                datos.activa(), instante(datos.creadoEn()), datos.version());
        if (datos.propietarioNombre() != null) {
            fila.setPropietario(new Propietario(null, datos.propietarioNombre(),
                    datos.propietarioCorreo(), datos.propietarioTelefono()));
        }
        return fila;
    }

    private static Unidad unidad(UnidadResponse datos) {
        Unidad fila = new Unidad(UUID.fromString(datos.id()), UUID.fromString(datos.propiedadId()),
                datos.propiedadNombre(), datos.codigo(), datos.tipo(), datos.estado(),
                decimal(datos.canonReferencia()), datos.moneda(), datos.version());
        fila.setDescripcion(datos.descripcion());
        return fila;
    }

    private static Inquilino inquilino(InquilinoResponse datos) {
        return new Inquilino(UUID.fromString(datos.id()), datos.nombre(), datos.identificacion(),
                datos.telefono(), datos.correo(), datos.contactoEmergencia(), datos.activo(),
                instante(datos.creadoEn()), datos.version());
    }

    private static Contrato contrato(ContratoResponse datos) {
        Contrato fila = new Contrato(UUID.fromString(datos.id()), UUID.fromString(datos.unidadId()), datos.unidadInfo(),
                UUID.fromString(datos.inquilinoId()), datos.inquilinoNombre(), LocalDate.parse(datos.inicio()),
                LocalDate.parse(datos.fin()), decimal(datos.canon()), datos.moneda(), datos.diaPago(),
                decimal(datos.depositoPactado()), datos.estado(), datos.version());
        if (datos.predecesorId() != null && !datos.predecesorId().isBlank()) {
            fila.setPredecesorId(UUID.fromString(datos.predecesorId()));
        }
        if (datos.terminadoEl() != null && !datos.terminadoEl().isBlank()) {
            fila.setTerminadoEl(LocalDate.parse(datos.terminadoEl()));
        }
        fila.setMotivoCierre(datos.motivoCierre());
        return fila;
    }

    private static Timestamp instante(String valor) {
        return valor == null ? null : Timestamp.from(Instant.parse(valor));
    }

    private static BigDecimal decimal(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }
}
