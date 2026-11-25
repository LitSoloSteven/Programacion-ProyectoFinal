package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto;

public record ReferenciaTasaResponse(String estado, String fuente, String nioPorUsd,
                                     String fechaEfectiva, String consultadaEn,
                                     Long antiguedadSegundos, String aviso) {}
