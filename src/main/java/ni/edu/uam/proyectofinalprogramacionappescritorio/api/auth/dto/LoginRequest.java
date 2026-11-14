package ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto;

/**
 * Credenciales de entrada. La contraseña se convierte a char[] y se borra en
 * el manejador; nunca se registra en logs.
 */
public record LoginRequest(String usuario, String password) { }