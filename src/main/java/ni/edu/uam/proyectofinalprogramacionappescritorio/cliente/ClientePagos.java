package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Cliente tipado para el registro idempotente de pagos desde JavaFX. */
public final class ClientePagos {
    private final Supplier<ClienteApi> api;

    public ClientePagos() {
        this(ClienteLatch::api);
    }

    public ClientePagos(ClienteApi api) {
        this(() -> Objects.requireNonNull(api, "Falta el cliente HTTP."));
    }

    private ClientePagos(Supplier<ClienteApi> api) {
        this.api = Objects.requireNonNull(api, "Falta el proveedor del cliente HTTP.");
    }

    public PagoRegistroResponse registrar(PagoRequest solicitud, UUID clave) {
        Objects.requireNonNull(solicitud, "Faltan los datos del pago.");
        Objects.requireNonNull(clave, "Falta la clave idempotente.");
        return api.get().postConCabecera("/api/v1/pagos", solicitud,
                PagoRegistroResponse.class, "Idempotency-Key", clave.toString());
    }
}
