package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto;

import java.util.List;

/** Resultado de la revisión H2 de vencimientos y mora. */
public record ResultadoRevisionH2Response(
        String fechaEvaluacion,
        int contratosEvaluados,
        int marcadosPorVencer,
        int marcadosVencido,
        int morasGeneradas,
        int incidentes,
        List<String> cargosMoraGenerados) {
}
