package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.adjuntos.dto.DocumentoAdjuntoResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Cliente HTTP para gestión de archivos adjuntos y logotipo del emisor (≤ 5 MiB). */
public final class ClienteAdjuntos {
    public static final String ENTIDAD_UNIDAD_FOTO = "UNIDAD_FOTO";
    public static final String ENTIDAD_INQUILINO_DOC = "INQUILINO_DOC";
    public static final String ENTIDAD_CONTRATO_PDF = "CONTRATO_PDF";
    public static final String ENTIDAD_PROPIEDAD_DOC = "PROPIEDAD_DOC";
    public static final String ENTIDAD_EMISOR_LOGO = "EMISOR_LOGO";

    private final ClienteApi api;

    public ClienteAdjuntos(ClienteApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public Optional<DocumentoAdjuntoResponse> obtenerFotoUnidad(UUID unidadId) {
        Objects.requireNonNull(unidadId);
        return listar(ENTIDAD_UNIDAD_FOTO, unidadId).stream().findFirst();
    }

    public List<DocumentoAdjuntoResponse> listarPorInquilino(UUID inquilinoId) {
        Objects.requireNonNull(inquilinoId);
        return listar(ENTIDAD_INQUILINO_DOC, inquilinoId);
    }

    public List<DocumentoAdjuntoResponse> listarPorContrato(UUID contratoId) {
        Objects.requireNonNull(contratoId);
        return listar(ENTIDAD_CONTRATO_PDF, contratoId);
    }

    public List<DocumentoAdjuntoResponse> listarPorPropiedad(UUID propiedadId) {
        Objects.requireNonNull(propiedadId);
        return listar(ENTIDAD_PROPIEDAD_DOC, propiedadId);
    }

    public Optional<DocumentoAdjuntoResponse> obtenerLogotipoVigente() {
        return listar(ENTIDAD_EMISOR_LOGO, null).stream().findFirst();
    }

    public List<DocumentoAdjuntoResponse> listar(String entidad, UUID entidadId) {
        StringBuilder ruta = new StringBuilder("/api/v1/adjuntos?entidad=")
                .append(URLEncoder.encode(entidad, StandardCharsets.UTF_8));
        if (entidadId != null) {
            ruta.append("&entidadId=").append(entidadId);
        }
        DocumentoAdjuntoResponse[] arr = api.get(ruta.toString(), DocumentoAdjuntoResponse[].class);
        return arr == null ? List.of() : Arrays.asList(arr);
    }

    public DocumentoAdjuntoResponse adjuntarFotoUnidad(UUID unidadId, Path archivo) throws IOException {
        Objects.requireNonNull(unidadId);
        return subirArchivo(ENTIDAD_UNIDAD_FOTO, unidadId, archivo);
    }

    public DocumentoAdjuntoResponse adjuntarDocumentoInquilino(UUID inquilinoId, Path archivo) throws IOException {
        Objects.requireNonNull(inquilinoId);
        return subirArchivo(ENTIDAD_INQUILINO_DOC, inquilinoId, archivo);
    }

    public DocumentoAdjuntoResponse adjuntarDocumentoContrato(UUID contratoId, Path archivo) throws IOException {
        Objects.requireNonNull(contratoId);
        return subirArchivo(ENTIDAD_CONTRATO_PDF, contratoId, archivo);
    }

    public DocumentoAdjuntoResponse adjuntarDocumentoPropiedad(UUID propiedadId, Path archivo) throws IOException {
        Objects.requireNonNull(propiedadId);
        return subirArchivo(ENTIDAD_PROPIEDAD_DOC, propiedadId, archivo);
    }

    public DocumentoAdjuntoResponse adjuntarLogotipoEmisor(Path archivo) throws IOException {
        return subirArchivo(ENTIDAD_EMISOR_LOGO, null, archivo);
    }

    public DocumentoAdjuntoResponse subirArchivo(String entidad, UUID entidadId, Path archivo) throws IOException {
        Objects.requireNonNull(archivo, "Selecciona un archivo válido.");
        byte[] bytes = Files.readAllBytes(archivo);
        String nombre = archivo.getFileName() == null ? "archivo.bin" : archivo.getFileName().toString();
        return subirBytes(entidad, entidadId, nombre, bytes);
    }

    public DocumentoAdjuntoResponse subirBytes(String entidad, UUID entidadId, String nombreArchivo, byte[] bytes) {
        Map<String, String> cabeceras = new LinkedHashMap<>();
        cabeceras.put("X-Entidad", Objects.requireNonNull(entidad));
        if (entidadId != null) {
            cabeceras.put("X-Entidad-Id", entidadId.toString());
        }
        cabeceras.put("X-Nombre-Archivo", URLEncoder.encode(
                nombreArchivo == null ? "archivo.bin" : nombreArchivo, StandardCharsets.UTF_8));
        return api.postBytes("/api/v1/adjuntos", bytes, cabeceras, DocumentoAdjuntoResponse.class);
    }

    public byte[] leerContenido(UUID adjuntoId) {
        Objects.requireNonNull(adjuntoId);
        return api.getBytes("/api/v1/adjuntos/" + adjuntoId + "/contenido");
    }

    public void desactivar(UUID adjuntoId) {
        Objects.requireNonNull(adjuntoId);
        api.delete("/api/v1/adjuntos/" + adjuntoId);
    }
}
