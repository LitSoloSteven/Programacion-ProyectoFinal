package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Cliente HTTP puro de catálogos (sin dependencias a modelo.* ni Hibernate). */
@RequiredArgsConstructor
public final class ClienteCatalogos {
    private static final int TAMANO = 50;
    private final ClienteApi api;

    public List<PropiedadResponse> propiedades(boolean soloActivas) {
        return listar("propiedades", soloActivas ? "&activa=true" : "", PropiedadResponse.class);
    }

    public PropiedadResponse propiedad(UUID id) {
        return api.get(ruta("propiedades") + "/" + id, PropiedadResponse.class);
    }

    public List<UnidadResponse> unidades() {
        return listar("unidades", "", UnidadResponse.class);
    }

    public UnidadResponse unidad(UUID id) {
        return api.get(ruta("unidades") + "/" + id, UnidadResponse.class);
    }

    public List<InquilinoResponse> inquilinos(boolean soloActivos) {
        return listar("inquilinos", soloActivos ? "&activo=true" : "", InquilinoResponse.class);
    }

    public InquilinoResponse inquilino(UUID id) {
        return api.get(ruta("inquilinos") + "/" + id, InquilinoResponse.class);
    }

    public HistorialInquilinoResponse historialInquilino(UUID id) {
        return api.get(ruta("inquilinos") + "/" + id + "/historial", HistorialInquilinoResponse.class);
    }

    public List<InquilinoResponse> coincidenciasIdentificacion(String identificacion, UUID excluirId) {
        if (identificacion == null || identificacion.isBlank()) {
            return List.of();
        }
        StringBuilder url = new StringBuilder(ruta("inquilinos"))
                .append("/coincidencias?identificacion=")
                .append(URLEncoder.encode(identificacion.strip(), StandardCharsets.UTF_8));
        if (excluirId != null) {
            url.append("&excluirId=").append(excluirId);
        }
        InquilinoResponse[] arr = api.get(url.toString(), InquilinoResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public List<ContratoResponse> contratos(boolean soloVigentes) {
        return listar("contratos", soloVigentes ? "&estado=Vigente" : "", ContratoResponse.class);
    }

    public ContratoResponse contrato(UUID id) {
        return api.get(ruta("contratos") + "/" + id, ContratoResponse.class);
    }

    public PropiedadResponse guardarPropiedad(UUID id, PropiedadRequest solicitud) {
        return guardar("propiedades", id, solicitud, PropiedadResponse.class);
    }

    public UnidadResponse guardarUnidad(UUID id, UnidadRequest solicitud) {
        return guardar("unidades", id, solicitud, UnidadResponse.class);
    }

    public InquilinoResponse guardarInquilino(UUID id, InquilinoRequest solicitud) {
        return guardar("inquilinos", id, solicitud, InquilinoResponse.class);
    }

    public ContratoResponse guardarContrato(UUID id, ContratoRequest solicitud) {
        return guardar("contratos", id, solicitud, ContratoResponse.class);
    }

    public void eliminarPropiedad(UUID id) {
        api.delete(ruta("propiedades") + "/" + id);
    }

    public void eliminarUnidad(UUID id) {
        api.delete(ruta("unidades") + "/" + id);
    }

    public void eliminarInquilino(UUID id) {
        api.delete(ruta("inquilinos") + "/" + id);
    }

    public void eliminarContrato(UUID id) {
        api.delete(ruta("contratos") + "/" + id);
    }

    public void darDeBajaInquilino(UUID id) {
        api.patch(ruta("inquilinos") + "/" + id, new InquilinoEstadoRequest("false"), Void.class);
    }

    public void terminarContrato(UUID id) {
        api.patch(ruta("contratos") + "/" + id, new ContratoEstadoRequest("Terminado"), Void.class);
    }

    public ContratoResponse activarContrato(UUID id, long version) {
        return api.post(ruta("contratos") + "/" + id + "/activar",
                new ContratoVersionRequest(version), ContratoResponse.class);
    }

    public ContratoResponse cancelarBorrador(UUID id, long version, String motivo) {
        return api.post(ruta("contratos") + "/" + id + "/cancelar",
                new ContratoCancelarRequest(version, motivo), ContratoResponse.class);
    }

    public ContratoResponse renovarContrato(UUID anteriorId, ContratoRenovarRequest solicitud) {
        return api.post(ruta("contratos") + "/" + anteriorId + "/renovar",
                solicitud, ContratoResponse.class);
    }

    public ContratoResponse terminarContratoConCierre(UUID id, ContratoTerminarRequest solicitud) {
        return api.post(ruta("contratos") + "/" + id + "/terminar",
                solicitud, ContratoResponse.class);
    }

    public GarantiaResumenResponse obtenerGarantia(UUID contratoId) {
        return api.get(ruta("contratos") + "/" + contratoId + "/garantia", GarantiaResumenResponse.class);
    }

    public List<GarantiaListadoItemResponse> listarGarantias() {
        GarantiaListadoItemResponse[] arr = api.get("/api/v1/garantias", GarantiaListadoItemResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public GarantiaResumenResponse liquidarGarantia(UUID contratoId, UUID liquidacionId,
                                                    GarantiaLiquidacionRequest solicitud) {
        return api.postConCabecera(
                ruta("contratos") + "/" + contratoId + "/garantia/movimientos",
                solicitud, GarantiaResumenResponse.class,
                "Idempotency-Key", liquidacionId.toString());
    }

    private <T> T guardar(String recurso, UUID id, Object cuerpo, Class<T> tipo) {
        String base = ruta(recurso);
        return id == null ? api.post(base, cuerpo, tipo) : api.put(base + "/" + id, cuerpo, tipo);
    }

    private <R> List<R> listar(String recurso, String filtros, Class<R> tipo) {
        var tipoPagina = TypeToken.getParameterized(PaginaResponse.class, tipo).getType();
        List<R> filas = new ArrayList<>();
        for (int pagina = 1; ; pagina++) {
            PaginaResponse<R> respuesta = api.get(
                    ruta(recurso) + "?pagina=" + pagina + "&tamano=" + TAMANO + filtros, tipoPagina);
            filas.addAll(respuesta.elementos());
            if (respuesta.elementos().isEmpty() || (long) pagina * TAMANO >= respuesta.total()) {
                return filas;
            }
        }
    }

    private static String ruta(String recurso) {
        return "/api/v1/" + recurso;
    }
}
