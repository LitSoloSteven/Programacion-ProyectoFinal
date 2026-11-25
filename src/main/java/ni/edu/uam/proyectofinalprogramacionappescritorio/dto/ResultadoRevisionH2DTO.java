package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ResultadoRevisionH2DTO(
        LocalDate fechaEvaluacion,
        int contratosEvaluados,
        int marcadosPorVencer,
        int marcadosVencido,
        int morasGeneradas,
        int incidentes,
        List<UUID> cargosMoraGenerados) {
}
