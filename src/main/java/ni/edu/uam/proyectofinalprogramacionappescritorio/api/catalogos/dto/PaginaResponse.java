package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto;

import java.util.List;

/** Envoltura uniforme para listas paginadas de la API de catálogos. */
public record PaginaResponse<T>(int pagina, int tamano, long total, List<T> elementos) {
    public PaginaResponse {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }
}