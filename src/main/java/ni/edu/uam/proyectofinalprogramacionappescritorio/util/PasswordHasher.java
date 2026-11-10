package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    private static final int ITERACIONES = 600_000;
    private final SecureRandom random = new SecureRandom();

    public String crear(char[] password) {
        if (password == null || password.length < 12 || password.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");
        }
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] hash = derivar(password, salt, ITERACIONES);
        try {
            return "pbkdf2-sha256$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    public boolean verificar(char[] password, String guardado) {
        if (password == null || password.length > 128 || guardado == null || guardado.length() > 255) return false;
        try {
            String[] partes = guardado.split("\\$");
            if (partes.length != 4 || !partes[0].equals("pbkdf2-sha256")) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < ITERACIONES || iteraciones > 1_200_000) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (salt.length != 16 || esperado.length != 32) return false;
            byte[] actual = derivar(password, salt, iteraciones);
            try {
                return MessageDigest.isEqual(esperado, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
                Arrays.fill(esperado, (byte) 0);
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(char[] password, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No está disponible el algoritmo de contraseñas.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
