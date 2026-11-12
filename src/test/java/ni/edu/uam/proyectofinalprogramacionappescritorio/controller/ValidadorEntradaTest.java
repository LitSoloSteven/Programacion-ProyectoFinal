package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ValidadorEntradaTest {

    @Test
    void testEsTextoValido() {
        assertTrue(ValidadorEntrada.esTextoValido("Edificio Central", 3, 50));
        assertFalse(ValidadorEntrada.esTextoValido("  ", 1, 50));
        assertFalse(ValidadorEntrada.esTextoValido(null, 1, 50));
        assertFalse(ValidadorEntrada.esTextoValido("AB", 3, 50));
    }

    @Test
    void testParsearDecimalPositivo() {
        BigDecimal res = ValidadorEntrada.parsearDecimalPositivo("150.75", "Canon");
        assertEquals(new BigDecimal("150.75"), res);

        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.parsearDecimalPositivo("-10.00", "Canon");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.parsearDecimalPositivo("0.00", "Canon");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.parsearDecimalPositivo("no_es_numero", "Canon");
        });
    }

    @Test
    void testParsearEnteroEnRango() {
        int dia = ValidadorEntrada.parsearEnteroEnRango("15", 1, 31, "Día de pago");
        assertEquals(15, dia);

        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.parsearEnteroEnRango("0", 1, 31, "Día de pago");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.parsearEnteroEnRango("32", 1, 31, "Día de pago");
        });
    }

    @Test
    void testValidarRangoFechas() {
        LocalDate ini = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 6, 1);
        assertDoesNotThrow(() -> ValidadorEntrada.validarRangoFechas(ini, fin, 1));

        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.validarRangoFechas(fin, ini, 1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ValidadorEntrada.validarRangoFechas(ini, ini.plusDays(10), 1);
        });
    }

    @Test
    void testValidarCorreoYTelefono() {
        assertTrue(ValidadorEntrada.esCorreoValido("usuario@empresa.com"));
        assertFalse(ValidadorEntrada.esCorreoValido("invalido@"));
        assertTrue(ValidadorEntrada.esTelefonoValido("+505 8899-0011"));
        assertFalse(ValidadorEntrada.esTelefonoValido("12"));
    }
}
