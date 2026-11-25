package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.time.Instant;
import java.util.UUID;

/** Proyección de un registro histórico inmutable de ParametroSistema. */
public record ParametroHistorialDTO(
        UUID id,
        String clave,
        String valor,
        Instant vigenteDesde,
        String motivo,
        Instant creadoEn,
        String creadoPor) {
}
