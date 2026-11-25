package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.bouncycastle.crypto.PasswordConverter;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    private static final int ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13;
    private static final int ARGON2_MEMORIA_KIB = 19_456;
    private static final int ARGON2_MEMORIA_MAX_KIB = 65_536;
    private static final int ARGON2_ITERACIONES = 2;
    private static final int ARGON2_ITERACIONES_MAX = 10;
    private static final int ARGON2_PARALELISMO = 1;
    private static final int ARGON2_PARALELISMO_MAX = 4;
    private static final int SAL_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int PBKDF2_ITERACIONES = 600_000;
    private static final int PBKDF2_ITERACIONES_MAX = 1_200_000;
    private final SecureRandom random = new SecureRandom();

    public String crear(char[] password) {
        if (password == null || password.length < 12 || password.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");
        }
        byte[] salt = new byte[SAL_BYTES];
        random.nextBytes(salt);
        byte[] hash = derivarArgon2(password, salt, ARGON2_VERSION,
                ARGON2_MEMORIA_KIB, ARGON2_ITERACIONES, ARGON2_PARALELISMO);
        try {
            Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
            return "$argon2id$v=" + ARGON2_VERSION
                    + "$m=" + ARGON2_MEMORIA_KIB + ",t=" + ARGON2_ITERACIONES + ",p=" + ARGON2_PARALELISMO
                    + "$" + encoder.encodeToString(salt)
                    + "$" + encoder.encodeToString(hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    public boolean verificar(char[] password, String guardado) {
        if (password == null || password.length > 128 || guardado == null || guardado.length() > 255) {
            return false;
        }
        if (guardado.startsWith("$argon2id$")) {
            return verificarArgon2(password, guardado);
        }
        if (guardado.startsWith("pbkdf2-sha256$")) {
            return verificarPbkdf2(password, guardado);
        }
        return false;
    }

    public boolean requiereActualizacion(String guardado) {
        if (guardado == null || guardado.length() > 255) return false;
        if (guardado.startsWith("pbkdf2-sha256$")) return true;
        if (guardado.startsWith("$argon2id$")) {
            ParametrosArgon2 params = parsearArgon2(guardado);
            if (params == null) return false;
            return params.version() != ARGON2_VERSION
                    || params.memoriaKiB() < ARGON2_MEMORIA_KIB
                    || params.iteraciones() < ARGON2_ITERACIONES
                    || params.paralelismo() < ARGON2_PARALELISMO;
        }
        return false;
    }

    private boolean verificarArgon2(char[] password, String guardado) {
        ParametrosArgon2 params = parsearArgon2(guardado);
        if (params == null) return false;
        if (params.version() != ARGON2_VERSION
                || params.memoriaKiB() < ARGON2_MEMORIA_KIB || params.memoriaKiB() > ARGON2_MEMORIA_MAX_KIB
                || params.iteraciones() < ARGON2_ITERACIONES || params.iteraciones() > ARGON2_ITERACIONES_MAX
                || params.paralelismo() < ARGON2_PARALELISMO || params.paralelismo() > ARGON2_PARALELISMO_MAX) {
            return false;
        }
        byte[] actual = derivarArgon2(password, params.salt(), params.version(),
                params.memoriaKiB(), params.iteraciones(), params.paralelismo());
        try {
            return MessageDigest.isEqual(params.esperado(), actual);
        } finally {
            Arrays.fill(actual, (byte) 0);
            Arrays.fill(params.esperado(), (byte) 0);
        }
    }

    private ParametrosArgon2 parsearArgon2(String guardado) {
        try {
            String[] partes = guardado.split("\\$", -1);
            if (partes.length != 6 || !partes[0].isEmpty() || !"argon2id".equals(partes[1])) {
                return null;
            }
            if (!partes[2].startsWith("v=")) return null;
            int version = Integer.parseInt(partes[2].substring(2));
            String[] opts = partes[3].split(",", -1);
            if (opts.length != 3
                    || !opts[0].startsWith("m=")
                    || !opts[1].startsWith("t=")
                    || !opts[2].startsWith("p=")) {
                return null;
            }
            int memoriaKiB = Integer.parseInt(opts[0].substring(2));
            int iteraciones = Integer.parseInt(opts[1].substring(2));
            int paralelismo = Integer.parseInt(opts[2].substring(2));
            byte[] salt = Base64.getDecoder().decode(partes[4]);
            byte[] esperado = Base64.getDecoder().decode(partes[5]);
            if (salt.length != SAL_BYTES || esperado.length != HASH_BYTES) {
                Arrays.fill(esperado, (byte) 0);
                return null;
            }
            return new ParametrosArgon2(version, memoriaKiB, iteraciones, paralelismo, salt, esperado);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean verificarPbkdf2(char[] password, String guardado) {
        try {
            String[] partes = guardado.split("\\$", -1);
            if (partes.length != 4 || !"pbkdf2-sha256".equals(partes[0])) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < PBKDF2_ITERACIONES || iteraciones > PBKDF2_ITERACIONES_MAX) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (salt.length != SAL_BYTES || esperado.length != HASH_BYTES) return false;
            byte[] actual = derivarPbkdf2(password, salt, iteraciones);
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

    private byte[] derivarArgon2(char[] password, byte[] salt, int version,
                                 int memoriaKiB, int iteraciones, int paralelismo) {
        byte[] pwdBytes = PasswordConverter.UTF8.convert(password);
        byte[] salida = new byte[HASH_BYTES];
        try {
            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(version)
                    .withSalt(salt)
                    .withMemoryAsKB(memoriaKiB)
                    .withIterations(iteraciones)
                    .withParallelism(paralelismo)
                    .build();
            Argon2BytesGenerator generador = new Argon2BytesGenerator();
            generador.init(params);
            generador.generateBytes(pwdBytes, salida);
            return salida;
        } finally {
            Arrays.fill(pwdBytes, (byte) 0);
        }
    }

    private byte[] derivarPbkdf2(char[] password, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No está disponible el algoritmo de contraseñas.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private record ParametrosArgon2(
            int version, int memoriaKiB, int iteraciones, int paralelismo,
            byte[] salt, byte[] esperado) {}
}
