package ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.PeticionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.RespuestaJson;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TasaResumenResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenTasaDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TotalMonedaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
        YearMonth periodo = parsearPeriodo(contexto.consulta("periodo"));
        MetricasDashboardDTO datos = SesionUsuario.ejecutarComo(
                contexto.sesion().comoUsuario(), () -> dashboard.obtenerMetricas(periodo));
        return RespuestaJson.ok(new MetricasResponse(
                datos.propiedades(),
                datos.unidades(),
                datos.unidadesActivas(),
                datos.unidadesOcupadas(),
                decimal(datos.porcentajeOcupacion()),
                datos.instanteGeneracion(),
                datos.periodo(),
                datos.inquilinos(),
                datos.contratosVigentes(),
                datos.contratosPorVencer(),
                datos.contratosVencidos(),
                convertir(datos.cargosPendientes()),
                convertir(datos.principalPendiente()),
                convertir(datos.moraExigible()),
                convertir(datos.pagosRegistrados()),
                convertirTasa(datos.tasaDisponible())));
    }

    private static YearMonth parsearPeriodo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(texto.strip());
        } catch (DateTimeParseException error) {
            throw ExcepcionApi.solicitudInvalida("El período debe usar el formato AAAA-MM.");
        }
    }

    private static TasaResumenResponse convertirTasa(ResumenTasaDashboardDTO tasa) {
        if (tasa == null || !tasa.disponible()) {
            return TasaResumenResponse.sinTasa();
        }
        return new TasaResumenResponse(
                true,
                tasa.estado(),
                tasa.valor() == null ? null : tasa.valor().setScale(8, RoundingMode.HALF_EVEN).toPlainString(),
                tasa.origen(),
                tasa.fechaEfectiva() == null ? null : tasa.fechaEfectiva().toString(),
                tasa.antiguedadSegundos(),
                tasa.autorizadaParaPagos());
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
