package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto;

import java.util.List;

public record ListaTasasResponse(List<TasaResponse> tasas) {
    public ListaTasasResponse {
        tasas = tasas == null ? List.of() : List.copyOf(tasas);
    }
}