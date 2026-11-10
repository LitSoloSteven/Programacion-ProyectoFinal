package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validador utilitario de datos de entrada en interfaz para prevenir inconsistencias
 * antes de enviar solicitudes a las capas de negocio y persistencia.
 */
public class ValidadorEntrada {

    private static final Pattern CORREO_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^[+0-9\\s-]{7,30}$");
    private static final Pattern CEDULA_PATTERN = Pattern.compile("^[0-9]{3}-?[0-9]{6}-?[0-9]{4}[A-Za-z]$");

    public static boolean esTextoValido(String texto, int min, int max) {
        if (texto == null) return false;
        String trimmed = texto.trim();
        return trimmed.length() >= min && trimmed.length() <= max;
    }

    public static BigDecimal parsearDecimalPositivo(String texto, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' es obligatorio.");
        }
        try {
            BigDecimal valor = new BigDecimal(texto.trim());
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El campo '" + nombreCampo + "' debe ser un valor positivo mayor a 0.");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' contiene un formato numérico inválido.");
        }
    }

    public static int parsearEnteroEnRango(String texto, int min, int max, String nombreCampo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' es obligatorio.");
        }
        try {
            int valor = Integer.parseInt(texto.trim());
            if (valor < min || valor > max) {
                throw new IllegalArgumentException("El campo '" + nombreCampo + "' debe estar comprendido entre " + min + " y " + max + ".");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + nombreCampo + "' debe ser un número entero.");
        }
    }

    public static void validarRangoFechas(LocalDate inicio, LocalDate fin, int mesesMinimos) {
        if (inicio == null) {
            throw new IllegalArgumentException("Debe seleccionar la fecha inicial.");
        }
        if (fin == null) {
            throw new IllegalArgumentException("Debe seleccionar la fecha final.");
        }
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la fecha inicial.");
        }
        if (mesesMinimos > 0 && fin.isBefore(inicio.plusMonths(mesesMinimos))) {
            throw new IllegalArgumentException("El periodo debe cubrir un mínimo de " + mesesMinimos + " mes(es).");
        }
    }

    public static boolean esCorreoValido(String correo) {
        if (correo == null || correo.trim().isEmpty()) return true;
        return CORREO_PATTERN.matcher(correo.trim()).matches();
    }

    public static boolean esTelefonoValido(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) return true;
        return TELEFONO_PATTERN.matcher(telefono.trim()).matches();
    }
}
