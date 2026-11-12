package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.util.List;

public record MetricasDashboardDTO(
        int propiedades,
        int unidades,
        int inquilinos,
        int contratosVigentes,
        List<TotalMonedaDTO> cargosPendientes,
        List<TotalMonedaDTO> pagosRegistrados) {

    public MetricasDashboardDTO {
        cargosPendientes = List.copyOf(cargosPendientes);
        pagosRegistrados = List.copyOf(pagosRegistrados);
    }

    public long cantidadCargosPendientes() {
        return cargosPendientes.stream().mapToLong(TotalMonedaDTO::cantidad).sum();
    }

    public long cantidadPagosRegistrados() {
        return pagosRegistrados.stream().mapToLong(TotalMonedaDTO::cantidad).sum();
    }
}
