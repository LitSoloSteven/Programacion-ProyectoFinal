package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void salAleatoriaYVerificacionDePassword() {
        PasswordHasher hasher = new PasswordHasher();
        char[] password = "PasswordSintetica2026".toCharArray();
        String primero = hasher.crear(password);
        String segundo = hasher.crear(password);
        assertNotEquals(primero, segundo);
        assertTrue(hasher.verificar(password, primero));
        assertFalse(hasher.verificar("incorrecta".toCharArray(), primero));
        assertFalse(hasher.verificar(password, "hash-invalido"));
        assertFalse(hasher.verificar(password, "pbkdf2-sha256$2147483647$AA==$AA=="));
        assertThrows(IllegalArgumentException.class, () -> hasher.crear("corta".toCharArray()));
    }
}
