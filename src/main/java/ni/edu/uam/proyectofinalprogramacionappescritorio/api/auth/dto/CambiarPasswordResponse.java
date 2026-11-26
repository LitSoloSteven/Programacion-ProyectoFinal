package ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto;

public record CambiarPasswordResponse(String token, String expiraEn, long version) { }
