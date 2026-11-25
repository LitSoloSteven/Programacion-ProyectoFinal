package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.DatabaseConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.DocumentoAdjunto;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Almacén controlado de archivos adjuntos en disco (RN-25).
 * Valida tamaño máximo, extensión, firma mágica, dimensiones de imagen y huella SHA-256.
 */
public final class AlmacenAdjuntos {
    public static final long MAX_TAMANO_BYTES = DocumentoAdjunto.MAX_TAMANO_BYTES;
    public static final int MAX_DIMENSION_IMAGEN = 8192;
    public static final long MAX_PIXELES_IMAGEN = 33_554_432L;

    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIRMA_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final Path directorioBase;

    public AlmacenAdjuntos(Path directorioBase) {
        this.directorioBase = Objects.requireNonNull(directorioBase, "Falta el directorio de adjuntos.")
                .toAbsolutePath().normalize();
    }

    public static AlmacenAdjuntos porDefecto() {
        Map<String, String> variables = DatabaseConfig.leerVariables(Path.of(".env"), System.getenv());
        String configurado = variables.get("LATCH_ADJUNTOS_DIR");
        if (configurado != null && !configurado.isBlank()) {
            return new AlmacenAdjuntos(Path.of(configurado.strip()));
        }
        String localAppData = variables.get("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return new AlmacenAdjuntos(Path.of(localAppData.strip(), "Latch", "adjuntos"));
        }
        return new AlmacenAdjuntos(Path.of(System.getProperty("user.home"), ".latch", "adjuntos"));
    }

    public Path getDirectorioBase() {
        return directorioBase;
    }

    public ArchivoGuardado guardar(Path origen, Set<String> mimesPermitidos) {
        if (origen == null || !Files.isRegularFile(origen) || !Files.isReadable(origen)) {
            throw new IllegalArgumentException("ERR-019: Selecciona un archivo existente y legible.");
        }
        Path nombre = origen.getFileName();
        String nombreOriginal = nombre == null ? "" : nombre.toString();
        byte[] contenido = leerAcotado(origen);
        return guardar(nombreOriginal, contenido, mimesPermitidos);
    }

    public ArchivoGuardado guardar(String nombreOriginal, byte[] contenido, Set<String> mimesPermitidos) {
        Objects.requireNonNull(mimesPermitidos, "Faltan los tipos MIME permitidos.");
        String nombreLimpio = validarNombre(nombreOriginal);
        if (contenido == null || contenido.length < 1 || contenido.length > MAX_TAMANO_BYTES) {
            throw new IllegalArgumentException("ERR-019: El archivo adjunto debe pesar entre 1 byte y 5 MiB.");
        }

        String extension = extensionDe(nombreLimpio);
        TipoDetectado detectado = detectarPorFirma(contenido);
        if (detectado == null || !detectado.coincideExtension(extension) || !mimesPermitidos.contains(detectado.mime())) {
            throw new IllegalArgumentException(
                    "ERR-019: Formato o firma de archivo no admitida. Usa PDF, PNG o JPEG según el módulo.");
        }

        if (DocumentoAdjunto.MIMES_IMAGEN.contains(detectado.mime())) {
            validarDimensionesImagen(contenido, detectado.mime());
        }

        String sha256 = calcularSha256(contenido);
        String rutaInterna = UUID.randomUUID() + "." + detectado.extensionInterna();
        Path destino = resolverSeguro(rutaInterna);

        Path temporal = null;
        try {
            Files.createDirectories(directorioBase);
            temporal = Files.createTempFile(directorioBase, "adjunto-", ".tmp");
            Files.write(temporal, contenido);
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return new ArchivoGuardado(nombreLimpio, rutaInterna, detectado.mime(), contenido.length, sha256);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo adjunto en el almacenamiento local.", e);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignored) {
                    // Limpieza mejor esfuerzo del archivo temporal.
                }
            }
        }
    }

    public byte[] leerYVerificar(String rutaInterna, String sha256Esperado) {
        if (rutaInterna == null || !rutaInterna.matches("^[0-9a-f-]{36}\\.(pdf|png|jpg)$")) {
            throw new IllegalStateException("La referencia interna del archivo adjunto es inválida.");
        }
        if (sha256Esperado == null || !sha256Esperado.matches("^[0-9a-f]{64}$")) {
            throw new IllegalStateException("La huella del archivo adjunto es inválida.");
        }
        Path archivo = resolverSeguro(rutaInterna);
        if (!Files.isRegularFile(archivo) || !Files.isReadable(archivo)) {
            throw new IllegalStateException("El archivo adjunto no se encuentra disponible en el almacenamiento.");
        }
        try {
            long tamano = Files.size(archivo);
            if (tamano < 1 || tamano > MAX_TAMANO_BYTES) {
                throw new IllegalStateException("El archivo adjunto excede el tamaño máximo permitido.");
            }
            byte[] bytes = Files.readAllBytes(archivo);
            String sha256Actual = calcularSha256(bytes);
            if (!MessageDigest.isEqual(
                    sha256Esperado.getBytes(StandardCharsets.US_ASCII),
                    sha256Actual.getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalStateException("El archivo adjunto falló la verificación de integridad SHA-256.");
            }
            return bytes;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo adjunto.", e);
        }
    }

    public void eliminarSilenciosamente(String rutaInterna) {
        if (rutaInterna == null || !rutaInterna.matches("^[0-9a-f-]{36}\\.(pdf|png|jpg)$")) {
            return;
        }
        try {
            Files.deleteIfExists(resolverSeguro(rutaInterna));
        } catch (IOException | RuntimeException ignored) {
            // Limpieza ante reversión de transacción.
        }
    }

    private Path resolverSeguro(String rutaInterna) {
        Path resuelta = directorioBase.resolve(rutaInterna).normalize();
        if (!resuelta.startsWith(directorioBase) || !resuelta.getParent().equals(directorioBase)) {
            throw new IllegalArgumentException("ERR-019: Ruta de almacenamiento fuera del repositorio controlado.");
        }
        return resuelta;
    }

    private static byte[] leerAcotado(Path origen) {
        try {
            long tamano = Files.size(origen);
            if (tamano < 1 || tamano > MAX_TAMANO_BYTES) {
                throw new IllegalArgumentException("ERR-019: El archivo adjunto debe pesar entre 1 byte y 5 MiB.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("ERR-019: No se pudo verificar el tamaño del archivo.", e);
        }
        try (InputStream entrada = Files.newInputStream(origen);
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long acumulado = 0;
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) {
                acumulado += leidos;
                if (acumulado > MAX_TAMANO_BYTES) {
                    throw new IllegalArgumentException("ERR-019: El archivo adjunto supera el máximo de 5 MiB.");
                }
                salida.write(buffer, 0, leidos);
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("ERR-019: No se pudo leer el archivo seleccionado.", e);
        }
    }

    private static String validarNombre(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new IllegalArgumentException("ERR-019: El nombre del archivo es obligatorio.");
        }
        String limpio = nombreOriginal.strip();
        if (limpio.length() > 180
                || limpio.contains("..")
                || limpio.contains("/")
                || limpio.contains("\\")
                || limpio.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("ERR-019: El nombre del archivo no es válido.");
        }
        return limpio;
    }

    private static String extensionDe(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto <= 0 || punto == nombre.length() - 1) {
            throw new IllegalArgumentException("ERR-019: El archivo debe tener extensión .pdf, .png, .jpg o .jpeg.");
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private static TipoDetectado detectarPorFirma(byte[] contenido) {
        if (empiezaCon(contenido, FIRMA_PDF)) {
            return new TipoDetectado(DocumentoAdjunto.MIME_PDF, "pdf", Set.of("pdf"));
        }
        if (empiezaCon(contenido, FIRMA_PNG)) {
            return new TipoDetectado(DocumentoAdjunto.MIME_PNG, "png", Set.of("png"));
        }
        if (contenido.length >= 3
                && (contenido[0] & 0xFF) == 0xFF
                && (contenido[1] & 0xFF) == 0xD8
                && (contenido[2] & 0xFF) == 0xFF) {
            return new TipoDetectado(DocumentoAdjunto.MIME_JPEG, "jpg", Set.of("jpg", "jpeg"));
        }
        return null;
    }

    private static boolean empiezaCon(byte[] contenido, byte[] prefijo) {
        if (contenido.length < prefijo.length) return false;
        for (int i = 0; i < prefijo.length; i++) {
            if (contenido[i] != prefijo[i]) return false;
        }
        return true;
    }

    private static void validarDimensionesImagen(byte[] contenido, String mime) {
        Iterator<ImageReader> lectores = ImageIO.getImageReadersByMIMEType(mime);
        if (!lectores.hasNext()) {
            throw new IllegalArgumentException("ERR-019: No hay lector disponible para la imagen.");
        }
        ImageReader lector = lectores.next();
        try (ImageInputStream flujo = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenido))) {
            lector.setInput(flujo, true, true);
            int ancho = lector.getWidth(0);
            int alto = lector.getHeight(0);
            long pixeles = (long) ancho * alto;
            if (ancho <= 0 || alto <= 0 || ancho > MAX_DIMENSION_IMAGEN || alto > MAX_DIMENSION_IMAGEN
                    || pixeles > MAX_PIXELES_IMAGEN) {
                throw new IllegalArgumentException(
                        "ERR-019: La imagen supera las dimensiones máximas permitidas (8192x8192).");
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException iae) throw iae;
            throw new IllegalArgumentException("ERR-019: La imagen seleccionada está dañada o no es válida.", e);
        } finally {
            lector.dispose();
        }
    }

    private static String calcularSha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256.", e);
        }
    }

    private record TipoDetectado(String mime, String extensionInterna, Set<String> extensiones) {
        boolean coincideExtension(String extension) {
            return extensiones.contains(extension);
        }
    }

    public record ArchivoGuardado(
            String nombreOriginal,
            String rutaInterna,
            String mime,
            long tamanoBytes,
            String sha256
    ) { }
}
