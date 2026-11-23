package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto;

/**
 * Alta de una tasa informativa. La tasa viaja como texto para no perder
 * precisión ni depender de float/double.
 */
public record TasaRequest(String fecha, String tasa, String fuente) { }