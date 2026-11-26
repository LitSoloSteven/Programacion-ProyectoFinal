package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record RegistrarParametroRequest(
        String clave,
        String valor,
        String vigenteDesde,
        String motivo) { }
