package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.time.Instant;
import java.util.UUID;

/** Proyección de solo lectura de un evento de auditoría. */
public record AuditoriaRegistroDTO(
        Long id,
        String actor,
        Instant instante,
        UUID correlacion,
        String accion,
        String referencia) {
}
