package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record ResultadoRestauracionResponse(
        String copiaPreviaBase,
        String copiaPreviaAdjuntos,
        String directorioAdjuntosRestaurado,
        String registroExterno,
        int adjuntosVerificados,
        int hashesDiscordantes,
        long duracionMs) { }
