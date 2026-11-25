package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.time.Instant;

/** Estado de antigüedad del último respaldo válido frente a la meta de 24 horas (RNF-038). */
public record EstadoAlertaRespaldoDTO(
        Instant ultimoRespaldoUtc,
        long horasDesdeUltimo,
        boolean alertaVencida,
        String mensaje
) { }
