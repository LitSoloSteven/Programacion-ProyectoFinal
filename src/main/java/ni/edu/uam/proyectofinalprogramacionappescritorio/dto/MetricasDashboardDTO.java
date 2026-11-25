package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record MetricasDashboardDTO(
        int propiedades,
        int unidades,
        int unidadesActivas,
        int unidadesOcupadas,
        BigDecimal porcentajeOcupacion,
        String instanteGeneracion,
        String periodo,
        int inquilinos,
        int contratosVigentes,
        int contratosPorVencer,
        int contratosVencidos,
        List<TotalMonedaDTO> cargosPendientes,
        List<TotalMonedaDTO> principalPendiente,
        List<TotalMonedaDTO> moraExigible,
        List<TotalMonedaDTO> pagosRegistrados,
        ResumenTasaDashboardDTO tasaDisponible) {

    public MetricasDashboardDTO {
        porcentajeOcupacion = (porcentajeOcupacion == null ? BigDecimal.ZERO : porcentajeOcupacion)
                .setScale(2, RoundingMode.HALF_EVEN);
        cargosPendientes = cargosPendientes == null ? List.of() : List.copyOf(cargosPendientes);
        principalPendiente = principalPendiente == null ? List.of() : List.copyOf(principalPendiente);
        moraExigible = moraExigible == null ? List.of() : List.copyOf(moraExigible);
        pagosRegistrados = pagosRegistrados == null ? List.of() : List.copyOf(pagosRegistrados);
        tasaDisponible = tasaDisponible == null ? ResumenTasaDashboardDTO.sinTasa() : tasaDisponible;
    }

    public MetricasDashboardDTO(
            int propiedades,
            int unidades,
            int inquilinos,
            int contratosVigentes,
            List<TotalMonedaDTO> cargosPendientes,
            List<TotalMonedaDTO> pagosRegistrados) {
        this(propiedades, unidades, unidades, contratosVigentes, BigDecimal.ZERO,
                null, null, inquilinos, contratosVigentes, 0, 0,
                cargosPendientes, cargosPendientes, List.of(), pagosRegistrados,
                ResumenTasaDashboardDTO.sinTasa());
    }

    public long cantidadCargosPendientes() {
        return cargosPendientes.stream().mapToLong(TotalMonedaDTO::cantidad).sum();
    }

    public long cantidadPagosRegistrados() {
        return pagosRegistrados.stream().mapToLong(TotalMonedaDTO::cantidad).sum();
    }

    public long cantidadMoraExigible() {
        return moraExigible.stream().mapToLong(TotalMonedaDTO::cantidad).sum();
    }
}
