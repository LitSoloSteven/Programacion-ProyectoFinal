package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.AutorizarTasaRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ListaTasasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.ReferenciaTasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClienteTasas {
    private final ClienteApi api;

    public ClienteTasas(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public List<TasaResponse> listar(LocalDate fecha, Boolean autorizada, int limite) {
        return listar(fecha, autorizada, limite, false);
    }

    public List<TasaResponse> listar(LocalDate fecha, Boolean autorizada, int limite, boolean operativa) {
        StringBuilder ruta = new StringBuilder("/api/v1/tasas?limite=").append(limite);
        if (operativa) ruta.append("&vista=operativa");
        if (fecha != null) ruta.append("&fecha=").append(fecha);
        if (autorizada != null) ruta.append("&autorizada=").append(autorizada);
        ListaTasasResponse respuesta = api.get(ruta.toString(), ListaTasasResponse.class);
        return respuesta == null ? List.of() : respuesta.tasas();
    }

    public TasaResponse registrar(LocalDate fecha, BigDecimal tasa, String fuente) {
        TasaRequest solicitud = new TasaRequest(fecha.toString(), tasa.toPlainString(), fuente);
        return api.post("/api/v1/tasas", solicitud, TasaResponse.class);
    }

    public TasaResponse editar(String id, LocalDate fecha, BigDecimal tasa, String fuente) {
        TasaRequest solicitud = new TasaRequest(fecha.toString(), tasa.toPlainString(), fuente);
        return api.post("/api/v1/tasas/" + id + "/revisiones", solicitud, TasaResponse.class);
    }

    public TasaResponse autorizar(String id, String motivo) {
        return autorizar(id, motivo, false);
    }

    public TasaResponse autorizar(String id, String motivo, boolean confirmarFueraDeBanda) {
        return api.post("/api/v1/tasas/" + id + "/autorizar",
                new AutorizarTasaRequest(motivo, confirmarFueraDeBanda), TasaResponse.class);
    }

    public Optional<TasaResponse> vigente(LocalDate fecha) {
        try {
            return Optional.ofNullable(api.get(
                    "/api/v1/tasas/vigente?fecha=" + fecha, TasaResponse.class));
        } catch (ErrorClienteException e) {
            if (e.esNoEncontrado()) return Optional.empty();
            throw e;
        }
    }

    public ReferenciaTasaResponse referencia() {
        return api.get("/api/v1/tasas/referencia", ReferenciaTasaResponse.class);
    }
}
