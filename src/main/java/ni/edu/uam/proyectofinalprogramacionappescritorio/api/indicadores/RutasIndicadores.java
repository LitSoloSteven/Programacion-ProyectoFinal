package ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.PeticionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TotalMonedaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * API 5: indicadores del panel. Reutiliza DashboardService y conserva los
 * totales separados por moneda, sin sumar USD con NIO.
 */
public final class RutasIndicadores {
    private final DashboardService dashboard;

    public RutasIndicadores(DashboardService dashboard) {
        this.dashboard = Objects.requireNonNull(dashboard);
    }

    public RespuestaJson metricas(PeticionApi contexto) {
        MetricasDashboardDTO datos = SesionUsuario.ejecutarComo(
                contexto.sesion().comoUsuario(), dashboard::obtenerMetricas);
        return RespuestaJson.ok(new MetricasResponse(
                datos.propiedades(),
                datos.unidades(),
                datos.inquilinos(),
                datos.contratosVigentes(),
                convertir(datos.cargosPendientes()),
                convertir(datos.pagosRegistrados())));
    }

    private static List<TotalMonedaResponse> convertir(List<TotalMonedaDTO> totales) {
        return totales.stream()
                .map(total -> new TotalMonedaResponse(
                        total.moneda(),
                        total.cantidad(),
                        decimal(total.total())))
                .toList();
    }

    private static String decimal(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor)
                .setScale(2, RoundingMode.HALF_EVEN)
                .toPlainString();
    }
}
