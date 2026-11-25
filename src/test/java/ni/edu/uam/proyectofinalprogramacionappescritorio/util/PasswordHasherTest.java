package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test
    void salAleatoriaYVerificacionDeArgon2idSinMutarPassword() {
        PasswordHasher hasher = new PasswordHasher();
        char[] password = "PasswordSintetica2026".toCharArray();
        char[] copiaOriginal = password.clone();
        String primero = hasher.crear(password);
        String segundo = hasher.crear(password);

        assertArrayEquals(copiaOriginal, password);
        assertTrue(primero.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), primero);
        assertNotEquals(primero, segundo);
        assertTrue(hasher.verificar(password, primero));
        assertFalse(hasher.verificar("incorrecta".toCharArray(), primero));
        assertFalse(hasher.verificar(password, "hash-invalido"));
        assertFalse(hasher.verificar(password, "pbkdf2-sha256$2147483647$AA==$AA=="));
        assertFalse(hasher.verificar(password, "$argon2id$v=19$m=1024,t=2,p=1$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        assertFalse(hasher.verificar(password, "$argon2id$v=19$m=999999,t=2,p=1$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        assertFalse(hasher.verificar(password, "$argon2id$v=19$m=19456,t=1,p=1$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        assertFalse(hasher.verificar(password, "$argon2id$v=19$m=19456,t=2,p=0$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        assertFalse(hasher.requiereActualizacion(primero));
        assertThrows(IllegalArgumentException.class, () -> hasher.crear("corta".toCharArray()));
    }

    @Test
    void verificaHashPbkdf2LegadoYMarcaActualizacionPendiente() throws Exception {
        PasswordHasher hasher = new PasswordHasher();
        char[] password = "ClaveLegada-2026".toCharArray();
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256);
        byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword();
        String legado = "pbkdf2-sha256$600000$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);

        assertTrue(hasher.verificar(password, legado));
        assertFalse(hasher.verificar("OtraClave-2026".toCharArray(), legado));
        assertTrue(hasher.requiereActualizacion(legado));
        assertFalse(hasher.requiereActualizacion(hasher.crear(password)));
    }
}
