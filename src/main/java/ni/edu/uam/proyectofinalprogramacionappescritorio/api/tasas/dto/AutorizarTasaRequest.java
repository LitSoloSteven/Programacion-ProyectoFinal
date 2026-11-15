package ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto;

/**
 * Autorización de una tasa informativa. Fuera de la banda 1–100 NIO/USD el
 * propietario debe enviar {@code confirmarFueraDeBanda} en {@code true}.
 */
public record AutorizarTasaRequest(String motivo, Boolean confirmarFueraDeBanda) { }
