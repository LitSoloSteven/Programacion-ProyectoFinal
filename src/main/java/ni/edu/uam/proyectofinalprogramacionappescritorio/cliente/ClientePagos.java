package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.gson.reflect.TypeToken;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.AnulacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CarteraResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.GeneracionMesRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PrevisualizacionLoteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ReciboResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ResultadoLoteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ResultadoRevisionH2Response;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.RevisionMoraRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Cliente tipado para el registro idempotente de pagos desde JavaFX. */
public final class ClientePagos {
    private record Cobro(long usuarioId, PagoRequest solicitud) { }

    private final Supplier<ClienteApi> api;
    private final ConcurrentMap<Cobro, UUID> cobrosPendientes = new ConcurrentHashMap<>();

    public ClientePagos() {
        this(ClienteLatch::api);
    }

    public ClientePagos(ClienteApi api) {
        this(() -> Objects.requireNonNull(api, "Falta el cliente HTTP."));
    }

    private ClientePagos(Supplier<ClienteApi> api) {
        this.api = Objects.requireNonNull(api, "Falta el proveedor del cliente HTTP.");
    }

    public PagoRegistroResponse registrarReintentable(long usuarioId, PagoRequest solicitud) {
        var cobro = new Cobro(usuarioId, solicitud);
        UUID clave = cobrosPendientes.computeIfAbsent(cobro, pendiente -> UUID.randomUUID());
        PagoRegistroResponse respuesta = registrar(solicitud, clave);
        cobrosPendientes.remove(cobro, clave);
        return respuesta;
    }

    public PagoRegistroResponse registrar(PagoRequest solicitud, UUID clave) {
        Objects.requireNonNull(solicitud, "Faltan los datos del pago.");
        Objects.requireNonNull(clave, "Falta la clave idempotente.");
        return api.get().postConCabecera("/api/v1/pagos", solicitud,
                PagoRegistroResponse.class, "Idempotency-Key", clave.toString());
    }

    public PaginaResponse<PagoListaResponse> listarPagos(int pagina, String texto) {
        return listar("/api/v1/pagos", pagina, texto, PagoListaResponse.class);
    }

    public PaginaResponse<CargoListaResponse> listarCargos(int pagina, String texto) {
        return listar("/api/v1/cargos", pagina, texto, CargoListaResponse.class);
    }

    public PagoDetalleResponse detalle(String id) {
        return api.get().get("/api/v1/pagos/" + id, PagoDetalleResponse.class);
    }

    public ReciboResponse recibo(long numero) {
        return api.get().get("/api/v1/recibos/" + numero, ReciboResponse.class);
    }

    public void anular(String id, String motivo) {
        api.get().post("/api/v1/pagos/" + id + "/anulacion", new AnulacionRequest(motivo), Void.class);
    }

    public EstadoCuentaResponse estadoCuenta(String contratoId, LocalDate corte) {
        if (contratoId == null || contratoId.isBlank()) {
            throw new IllegalArgumentException("Falta el identificador del contrato.");
        }
        String ruta = "/api/v1/contratos/" + URLEncoder.encode(contratoId.strip(), StandardCharsets.UTF_8)
                + "/estado-cuenta";
        if (corte != null) {
            ruta += "?corte=" + corte;
        }
        return api.get().get(ruta, EstadoCuentaResponse.class);
    }

    public CarteraResponse consultarCartera(LocalDate corte, String contratoId,
                                            YearMonth periodo, String moneda, String texto) {
        StringBuilder ruta = new StringBuilder("/api/v1/cargos/cartera");
        char separador = '?';
        if (corte != null) {
            ruta.append(separador).append("corte=").append(corte);
            separador = '&';
        }
        if (contratoId != null && !contratoId.isBlank()) {
            ruta.append(separador).append("contratoId=")
                    .append(URLEncoder.encode(contratoId.strip(), StandardCharsets.UTF_8));
            separador = '&';
        }
        if (periodo != null) {
            ruta.append(separador).append("periodo=").append(periodo);
            separador = '&';
        }
        if (moneda != null && !moneda.isBlank()) {
            ruta.append(separador).append("moneda=")
                    .append(URLEncoder.encode(moneda.strip(), StandardCharsets.UTF_8));
            separador = '&';
        }
        if (texto != null && !texto.isBlank()) {
            ruta.append(separador).append("texto=")
                    .append(URLEncoder.encode(texto.strip(), StandardCharsets.UTF_8));
        }
        return api.get().get(ruta.toString(), CarteraResponse.class);
    }

    public PrevisualizacionLoteResponse previsualizarGeneracionMes(YearMonth periodo) {
        String query = periodo == null ? "" : "?periodo=" + periodo;
        return api.get().get("/api/v1/cargos/generacion-mes/vista-previa" + query,
                PrevisualizacionLoteResponse.class);
    }

    public ResultadoLoteResponse generarMes(YearMonth periodo) {
        Objects.requireNonNull(periodo, "Falta el período.");
        return api.get().post("/api/v1/cargos/generacion-mes",
                new GeneracionMesRequest(periodo.toString()), ResultadoLoteResponse.class);
    }

    public ResultadoRevisionH2Response revisarMora(LocalDate fecha) {
        return api.get().post("/api/v1/cargos/revisar-mora",
                new RevisionMoraRequest(fecha == null ? null : fecha.toString()),
                ResultadoRevisionH2Response.class);
    }

    public CargoDetalleResponse obtenerCargo(UUID id) {
        Objects.requireNonNull(id, "Falta el identificador del cargo.");
        return api.get().get("/api/v1/cargos/" + id, CargoDetalleResponse.class);
    }

    public CargoDetalleResponse crearCargo(CargoRequest solicitud) {
        Objects.requireNonNull(solicitud, "Faltan los datos del cargo.");
        return api.get().post("/api/v1/cargos", solicitud, CargoDetalleResponse.class);
    }

    public CargoDetalleResponse actualizarCargo(UUID id, CargoRequest solicitud) {
        Objects.requireNonNull(id, "Falta el identificador del cargo.");
        Objects.requireNonNull(solicitud, "Faltan los datos del cargo.");
        return api.get().put("/api/v1/cargos/" + id, solicitud, CargoDetalleResponse.class);
    }

    public void anularCargo(String cargoId, String motivo) {
        api.get().post("/api/v1/cargos/" + URLEncoder.encode(cargoId.strip(), StandardCharsets.UTF_8)
                + "/anulacion", new AnulacionRequest(motivo), Void.class);
    }

    public void revertirAplicacionCredito(String aplicacionId, String motivo) {
        api.get().post("/api/v1/pagos/aplicaciones/"
                        + URLEncoder.encode(aplicacionId.strip(), StandardCharsets.UTF_8)
                        + "/reversion",
                new AnulacionRequest(motivo), Void.class);
    }

    private <T> PaginaResponse<T> listar(String ruta, int pagina, String texto, Class<T> tipo) {
        var tipoPagina = TypeToken.getParameterized(PaginaResponse.class, tipo).getType();
        String filtro = texto == null || texto.isBlank() ? ""
                : "&texto=" + URLEncoder.encode(texto, StandardCharsets.UTF_8);
        return api.get().get(ruta + "?pagina=" + pagina + filtro, tipoPagina);
    }
}
