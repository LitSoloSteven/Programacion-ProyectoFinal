package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.gson.reflect.TypeToken;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.AnulacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoListaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ReciboResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private <T> PaginaResponse<T> listar(String ruta, int pagina, String texto, Class<T> tipo) {
        var tipoPagina = TypeToken.getParameterized(PaginaResponse.class, tipo).getType();
        String filtro = texto == null || texto.isBlank() ? ""
                : "&texto=" + URLEncoder.encode(texto, StandardCharsets.UTF_8);
        return api.get().get(ruta + "?pagina=" + pagina + filtro, tipoPagina);
    }
}
