package ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validador utilitario de datos de entrada en interfaz para prevenir inconsistencias
 * antes de enviar solicitudes a las capas de negocio y persistencia.
 */
public class ValidadorEntrada {

    private static final Pattern CORREO_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^\\+?[0-9]+(?:[ -][0-9]+)*$");
    public static final int LONGITUD_MAXIMA_VALOR = 255;
    private static final int DIGITOS_TELEFONO_MINIMOS = 7;
    private static final int DIGITOS_TELEFONO_MAXIMOS = 15;
    private static final Pattern CEDULA_PATTERN = Pattern.compile("^[0-9]{3}-?[0-9]{6}-?[0-9]{4}[A-Za-z]$");

    public static boolean esTextoValido(String texto, int min, int max) {
        if (esVacio(texto)) return false;
        String trimmed = texto.trim();
        return trimmed.length() >= min && trimmed.length() <= max;
    }

    public static BigDecimal parsearDecimalPositivo(String texto, String nombreCampo) {
        if (esVacio(texto)) {
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
        if (esVacio(texto)) {
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
        if (mesesMinimos < 0) {
            throw new IllegalArgumentException("Los meses mínimos no pueden ser negativos.");
        }
        if (mesesMinimos == 0) return;
        LocalDate fechaMinima;
        try {
            fechaMinima = inicio.plusMonths(mesesMinimos);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    "El periodo mínimo supera el rango de fechas admitido.");
        }
        if (fin.isBefore(fechaMinima)) {
            throw new IllegalArgumentException("El periodo debe cubrir un mínimo de " + mesesMinimos + " mes(es).");
        }
    }

    public static boolean esCorreoValido(String correo) {
        if (esVacio(correo)) return true;
        return CORREO_PATTERN.matcher(correo.trim()).matches();
    }

    public static boolean esTelefonoValido(String telefono) {
        if (esVacio(telefono)) return true;
        String limpio = telefono.strip();
        if (limpio.length() > 30 || !TELEFONO_PATTERN.matcher(limpio).matches()) return false;
        long digitos = limpio.chars().filter(Character::isDigit).count();
        return digitos >= DIGITOS_TELEFONO_MINIMOS && digitos <= DIGITOS_TELEFONO_MAXIMOS;
    }

    /** Acepta el formato 000-000000-0000A con o sin guiones. */
    public static boolean esCedulaValida(String cedula) {
        if (esVacio(cedula)) return true;
        return CEDULA_PATTERN.matcher(cedula.trim()).matches();
    }

    public static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public static boolean excedeLongitudValor(String valor) {
        return valor != null
                && valor.codePointCount(0, valor.length()) > LONGITUD_MAXIMA_VALOR;
    }

    /** Acepta un decimal positivo con dos decimales significativos como máximo. */
    public static boolean esMontoValido(String monto) {
        if (esVacio(monto)) return false;
        try {
            BigDecimal valor = new BigDecimal(monto.trim());
            return valor.signum() > 0 && valor.stripTrailingZeros().scale() <= 2;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
