package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record ParametroHistorialResponse(
        String id,
        String clave,
        String valor,
        String vigenteDesde,
        String motivo,
        String creadoEn,
        String creadoPor) { }
