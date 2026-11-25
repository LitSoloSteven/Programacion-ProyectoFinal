package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.CambioEstadoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.GastoMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ListaSolicitudesMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.ResumenGastosMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto.SolicitudMantenimientoUpdateRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Cliente HTTP de mantenimiento y gastos de propiedad (M08). */
public final class ClienteMantenimiento {
    private final ClienteApi api;

    public ClienteMantenimiento(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public List<SolicitudMantenimientoResponse> listarSolicitudes(UUID propiedadId, UUID unidadId,
                                                                String prioridad, String estado) {
        List<String> params = new ArrayList<>();
        if (propiedadId != null) params.add("propiedadId=" + propiedadId);
        if (unidadId != null) params.add("unidadId=" + unidadId);
        if (prioridad != null && !prioridad.isBlank()) params.add("prioridad=" + codificar(prioridad));
        if (estado != null && !estado.isBlank()) params.add("estado=" + codificar(estado));
        String ruta = "/api/v1/mantenimiento/solicitudes" + (params.isEmpty() ? "" : "?" + String.join("&", params));
        ListaSolicitudesMantenimientoResponse res = api.get(ruta, ListaSolicitudesMantenimientoResponse.class);
        return res == null || res.solicitudes() == null ? List.of() : res.solicitudes();
    }

    public SolicitudMantenimientoResponse crearSolicitud(SolicitudMantenimientoRequest request) {
        return api.post("/api/v1/mantenimiento/solicitudes", request, SolicitudMantenimientoResponse.class);
    }

    public SolicitudMantenimientoResponse actualizarSolicitud(UUID id, SolicitudMantenimientoUpdateRequest request) {
        return api.put("/api/v1/mantenimiento/solicitudes/" + id, request, SolicitudMantenimientoResponse.class);
    }

    public SolicitudMantenimientoResponse cambiarEstado(UUID id, CambioEstadoMantenimientoRequest request) {
        return api.post("/api/v1/mantenimiento/solicitudes/" + id + "/estado",
                request, SolicitudMantenimientoResponse.class);
    }

    public ResumenGastosMantenimientoResponse consultarGastos(UUID propiedadId, LocalDate desde,
                                                              LocalDate hasta, String moneda) {
        List<String> params = new ArrayList<>();
        if (propiedadId != null) params.add("propiedadId=" + propiedadId);
        if (desde != null) params.add("desde=" + desde);
        if (hasta != null) params.add("hasta=" + hasta);
        if (moneda != null && !moneda.isBlank()) params.add("moneda=" + codificar(moneda));
        String ruta = "/api/v1/mantenimiento/gastos" + (params.isEmpty() ? "" : "?" + String.join("&", params));
        ResumenGastosMantenimientoResponse res = api.get(ruta, ResumenGastosMantenimientoResponse.class);
        return res == null ? new ResumenGastosMantenimientoResponse(List.of(), "0.00", "0.00") : res;
    }

    public GastoMantenimientoResponse registrarGasto(GastoMantenimientoRequest request) {
        return api.post("/api/v1/mantenimiento/gastos", request, GastoMantenimientoResponse.class);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor.strip(), StandardCharsets.UTF_8);
    }
}
