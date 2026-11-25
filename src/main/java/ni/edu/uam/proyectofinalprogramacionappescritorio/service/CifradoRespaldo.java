package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Cifrado autenticado por flujo AES-256-GCM con derivación PBKDF2-HMAC-SHA256 (600,000 iteraciones,
 * sal de 16 bytes, IV base de 12 bytes y etiqueta de 128 bits por bloque).
 */
public final class CifradoRespaldo {
    public static final int ITERACIONES_PBKDF2 = 600_000;
    public static final int TAMANO_SAL = 16;
    public static final int TAMANO_IV = 12;
    public static final int TAMANO_TAG_BITS = 128;
    public static final int TAMANO_TAG_BYTES = TAMANO_TAG_BITS / 8;
    public static final int TAMANO_CLAVE_BITS = 256;
    public static final int TAMANO_BLOQUE_DEFECTO = 4 * 1024 * 1024;
    public static final int MIN_LONGITUD_CONTRASENA = 12;
    public static final int MAX_LONGITUD_CONTRASENA = 128;

    private static final byte[] MAGIC = "LTCHBK01".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION_FORMATO = 1;
    private static final int TAMANO_ENCABEZADO = 56;
    private static final int MIN_TAMANO_BLOQUE = 4096;
    private static final int MAX_TAMANO_BLOQUE = 16 * 1024 * 1024;
    private static final int MAX_ITERACIONES_PBKDF2 = 5_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CifradoRespaldo() { }

    public static void validarContrasenaRespaldo(char[] contrasena) {
        if (contrasena == null || contrasena.length < MIN_LONGITUD_CONTRASENA
                || contrasena.length > MAX_LONGITUD_CONTRASENA) {
            throw new IllegalArgumentException(
                    "La contraseña de respaldo debe tener entre 12 y 128 caracteres.");
        }
        boolean soloEspacios = true;
        for (char c : contrasena) {
            if (!Character.isWhitespace(c)) {
                soloEspacios = false;
                break;
            }
        }
        if (soloEspacios) {
            throw new IllegalArgumentException(
                    "La contraseña de respaldo no puede estar compuesta solo por espacios.");
        }
    }

    public static void cifrarArchivo(Path origenPlano, Path destinoCifrado, char[] contrasena) {
        cifrarArchivo(origenPlano, destinoCifrado, contrasena, TAMANO_BLOQUE_DEFECTO);
    }

    static void cifrarArchivo(Path origenPlano, Path destinoCifrado, char[] contrasena, int tamanoBloque) {
        Objects.requireNonNull(origenPlano, "Falta el archivo de origen del respaldo.");
        Objects.requireNonNull(destinoCifrado, "Falta el archivo de destino cifrado.");
        validarContrasenaRespaldo(contrasena);
        if (tamanoBloque < MIN_TAMANO_BLOQUE || tamanoBloque > MAX_TAMANO_BLOQUE) {
            throw new IllegalArgumentException("Tamaño de bloque inválido para cifrado.");
        }

        byte[] sal = new byte[TAMANO_SAL];
        byte[] ivBase = new byte[TAMANO_IV];
        RANDOM.nextBytes(sal);
        RANDOM.nextBytes(ivBase);

        byte[] encabezado = construirEncabezado(ITERACIONES_PBKDF2, tamanoBloque, sal, ivBase);
        byte[] claveRaw = null;
        try {
            claveRaw = derivarClave(contrasena, sal, ITERACIONES_PBKDF2);
            SecretKeySpec clave = new SecretKeySpec(claveRaw, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            if (destinoCifrado.getParent() != null) {
                Files.createDirectories(destinoCifrado.getParent());
            }

            try (InputStream entrada = new BufferedInputStream(Files.newInputStream(origenPlano));
                 OutputStream salida = new BufferedOutputStream(Files.newOutputStream(destinoCifrado))) {
                salida.write(encabezado);

                byte[] buffer = new byte[tamanoBloque];
                int offset = 0;
                long indiceBloque = 0L;
                while (true) {
                    int leidos = leerHasta(entrada, buffer, offset, tamanoBloque - offset);
                    int total = offset + leidos;
                    int siguiente = -1;
                    boolean esUltimo;
                    if (total < tamanoBloque) {
                        esUltimo = true;
                    } else {
                        siguiente = entrada.read();
                        esUltimo = (siguiente == -1);
                    }

                    byte[] ivBloque = derivarIvBloque(ivBase, indiceBloque);
                    byte[] aad = construirAad(encabezado, indiceBloque, esUltimo);
                    cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, ivBloque));
                    cipher.updateAAD(aad);
                    byte[] cifrado = cipher.doFinal(buffer, 0, total);

                    salida.write(esUltimo ? 1 : 0);
                    escribirInt(salida, cifrado.length);
                    salida.write(cifrado);

                    if (esUltimo) {
                        break;
                    }
                    buffer[0] = (byte) siguiente;
                    offset = 1;
                    indiceBloque++;
                }
                Arrays.fill(buffer, (byte) 0);
            }
        } catch (IOException | GeneralSecurityException e) {
            eliminarSilenciosamente(destinoCifrado);
            throw RespaldoException.operativo("ERR-055: No se pudo cifrar el paquete de respaldo.", e);
        } finally {
            if (claveRaw != null) {
                Arrays.fill(claveRaw, (byte) 0);
            }
        }
    }

    public static void descifrarYVerificarArchivo(Path origenCifrado, Path destinoPlano, char[] contrasena) {
        Objects.requireNonNull(origenCifrado, "Falta el archivo cifrado de respaldo.");
        Objects.requireNonNull(destinoPlano, "Falta el archivo de destino plano.");
        if (contrasena == null || contrasena.length == 0) {
            throw RestauracionException.autenticidadInvalida(null);
        }

        byte[] claveRaw = null;
        try {
            if (!Files.isRegularFile(origenCifrado) || !Files.isReadable(origenCifrado)) {
                throw RestauracionException.autenticidadInvalida(null);
            }
            if (destinoPlano.getParent() != null) {
                Files.createDirectories(destinoPlano.getParent());
            }

            try (InputStream entrada = new BufferedInputStream(Files.newInputStream(origenCifrado));
                 OutputStream salida = new BufferedOutputStream(Files.newOutputStream(destinoPlano))) {
                byte[] encabezado = entrada.readNBytes(TAMANO_ENCABEZADO);
                EncabezadoLeido meta = parsearEncabezado(encabezado);

                claveRaw = derivarClave(contrasena, meta.sal(), meta.iteraciones());
                SecretKeySpec clave = new SecretKeySpec(claveRaw, "AES");
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                long indiceBloque = 0L;
                while (true) {
                    int bandera = entrada.read();
                    if (bandera != 0 && bandera != 1) {
                        throw new AEADBadTagException("Bloque de respaldo ausente o corrupto.");
                    }
                    boolean esUltimo = (bandera == 1);
                    int longitudCifrado = leerInt(entrada);
                    if (longitudCifrado < TAMANO_TAG_BYTES
                            || longitudCifrado > meta.tamanoBloque() + TAMANO_TAG_BYTES) {
                        throw new AEADBadTagException("Longitud de bloque cifrado inválida.");
                    }
                    byte[] cifrado = entrada.readNBytes(longitudCifrado);
                    if (cifrado.length != longitudCifrado) {
                        throw new AEADBadTagException("Bloque cifrado truncado.");
                    }

                    byte[] ivBloque = derivarIvBloque(meta.ivBase(), indiceBloque);
                    byte[] aad = construirAad(encabezado, indiceBloque, esUltimo);
                    cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, ivBloque));
                    cipher.updateAAD(aad);
                    byte[] plano = cipher.doFinal(cifrado);
                    salida.write(plano);
                    Arrays.fill(plano, (byte) 0);

                    if (esUltimo) {
                        if (entrada.read() != -1) {
                            throw new AEADBadTagException("Datos inesperados tras el bloque final.");
                        }
                        break;
                    }
                    indiceBloque++;
                }
            }
        } catch (RestauracionException e) {
            eliminarSilenciosamente(destinoPlano);
            throw e;
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            eliminarSilenciosamente(destinoPlano);
            throw RestauracionException.autenticidadInvalida(e);
        } finally {
            if (claveRaw != null) {
                Arrays.fill(claveRaw, (byte) 0);
            }
        }
    }

    public static String calcularSha256(Path archivo) {
        Objects.requireNonNull(archivo, "Falta el archivo para calcular SHA-256.");
        try (InputStream entrada = new BufferedInputStream(Files.newInputStream(archivo))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                digest.update(buffer, 0, leidos);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw RespaldoException.operativo("ERR-055: No se pudo calcular la huella SHA-256 del archivo.", e);
        }
    }

    private static byte[] derivarClave(char[] contrasena, byte[] sal, int iteraciones)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(contrasena, sal, iteraciones, TAMANO_CLAVE_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] construirEncabezado(int iteraciones, int tamanoBloque, byte[] sal, byte[] ivBase) {
        ByteBuffer buf = ByteBuffer.allocate(TAMANO_ENCABEZADO);
        buf.put(MAGIC);
        buf.putInt(VERSION_FORMATO);
        buf.putInt(iteraciones);
        buf.putInt(tamanoBloque);
        buf.putInt(sal.length);
        buf.put(sal);
        buf.putInt(ivBase.length);
        buf.put(ivBase);
        return buf.array();
    }

    private static EncabezadoLeido parsearEncabezado(byte[] encabezado) throws AEADBadTagException {
        if (encabezado.length != TAMANO_ENCABEZADO) {
            throw new AEADBadTagException("Encabezado incompleto.");
        }
        ByteBuffer buf = ByteBuffer.wrap(encabezado);
        byte[] magic = new byte[MAGIC.length];
        buf.get(magic);
        if (!Arrays.equals(MAGIC, magic)) {
            throw new AEADBadTagException("Formato de respaldo no reconocido.");
        }
        int version = buf.getInt();
        int iteraciones = buf.getInt();
        int tamanoBloque = buf.getInt();
        int salLen = buf.getInt();
        if (version != VERSION_FORMATO
                || iteraciones < ITERACIONES_PBKDF2 || iteraciones > MAX_ITERACIONES_PBKDF2
                || tamanoBloque < MIN_TAMANO_BLOQUE || tamanoBloque > MAX_TAMANO_BLOQUE
                || salLen != TAMANO_SAL) {
            throw new AEADBadTagException("Parámetros de encabezado inválidos.");
        }
        byte[] sal = new byte[TAMANO_SAL];
        buf.get(sal);
        int ivLen = buf.getInt();
        if (ivLen != TAMANO_IV) {
            throw new AEADBadTagException("Longitud de IV inválida.");
        }
        byte[] ivBase = new byte[TAMANO_IV];
        buf.get(ivBase);
        return new EncabezadoLeido(iteraciones, tamanoBloque, sal, ivBase);
    }

    private static byte[] derivarIvBloque(byte[] ivBase, long indiceBloque) {
        byte[] iv = ivBase.clone();
        for (int i = 0; i < 8; i++) {
            iv[4 + i] ^= (byte) (indiceBloque >>> (56 - 8 * i));
        }
        return iv;
    }

    private static byte[] construirAad(byte[] encabezado, long indiceBloque, boolean esUltimo) {
        ByteBuffer buf = ByteBuffer.allocate(encabezado.length + Long.BYTES + 1);
        buf.put(encabezado);
        buf.putLong(indiceBloque);
        buf.put(esUltimo ? (byte) 1 : (byte) 0);
        return buf.array();
    }

    private static int leerHasta(InputStream entrada, byte[] buffer, int offset, int maximo) throws IOException {
        int acumulado = 0;
        while (acumulado < maximo) {
            int n = entrada.read(buffer, offset + acumulado, maximo - acumulado);
            if (n == -1) {
                break;
            }
            acumulado += n;
        }
        return acumulado;
    }

    private static void escribirInt(OutputStream salida, int valor) throws IOException {
        salida.write((valor >>> 24) & 0xFF);
        salida.write((valor >>> 16) & 0xFF);
        salida.write((valor >>> 8) & 0xFF);
        salida.write(valor & 0xFF);
    }

    private static int leerInt(InputStream entrada) throws IOException, AEADBadTagException {
        byte[] bytes = entrada.readNBytes(4);
        if (bytes.length != 4) {
            throw new AEADBadTagException("Longitud de bloque truncada.");
        }
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
    }

    private static void eliminarSilenciosamente(Path archivo) {
        if (archivo == null) return;
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // Limpieza mejor esfuerzo de archivos temporales o planos fallidos.
        }
    }

    private record EncabezadoLeido(int iteraciones, int tamanoBloque, byte[] sal, byte[] ivBase) { }
}
