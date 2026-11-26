package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.DocumentoReporteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.GenerarReporteRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.ReciboBusquedaItemResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Cliente HTTP de reportes JasperReports renderizados en el servidor (PNG/PDF/XLSX) y recibos. */
public final class ClienteReportes {
    private static final Duration TIMEOUT_REPORTES = Duration.ofSeconds(60);

    private final ClienteApi api;

    public ClienteReportes(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public DocumentoReporteResponse generar(GenerarReporteRequest request) {
        return api.post("/api/v1/reportes/generar", request, DocumentoReporteResponse.class, TIMEOUT_REPORTES);
    }

    public byte[] paginaPng(String documentoId, int indicePagina, float escala) {
        Objects.requireNonNull(documentoId);
        return api.getBytes("/api/v1/reportes/documentos/" + codificar(documentoId)
                + "/paginas/" + indicePagina + "?escala=" + escala);
    }

    public byte[] exportarPdf(String documentoId) {
        Objects.requireNonNull(documentoId);
        return api.getBytes("/api/v1/reportes/documentos/" + codificar(documentoId) + "/pdf");
    }

    public byte[] exportarXlsx(String documentoId) {
        Objects.requireNonNull(documentoId);
        return api.getBytes("/api/v1/reportes/documentos/" + codificar(documentoId) + "/xlsx");
    }

    public void eliminar(String documentoId) {
        if (documentoId == null || documentoId.isBlank()) return;
        api.delete("/api/v1/reportes/documentos/" + codificar(documentoId));
    }

    public List<ReciboBusquedaItemResponse> buscarRecibos(Long numero, String texto) {
        List<String> params = new ArrayList<>();
        if (numero != null) params.add("numero=" + numero);
        if (texto != null && !texto.isBlank()) params.add("texto=" + codificar(texto.strip()));
        String ruta = "/api/v1/recibos/buscar" + (params.isEmpty() ? "" : "?" + String.join("&", params));
        ReciboBusquedaItemResponse[] arr = api.get(ruta, ReciboBusquedaItemResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
