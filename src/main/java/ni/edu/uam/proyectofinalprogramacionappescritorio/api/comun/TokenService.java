package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emisión y validación de tokens Bearer en memoria. Solo se guarda la huella
 * SHA-256 del token; nunca el valor original ni datos de la contraseña.
 */
public final class TokenService {
    public static final Duration VIGENCIA_PREDETERMINADA = Duration.ofMinutes(15);
    private static final int BYTES_TOKEN = 32;
    private static final int LONGITUD_MAXIMA_TOKEN = 512;

    private final SecureRandom aleatorio = new SecureRandom();
    private final ConcurrentHashMap<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final Duration vigencia;

    public TokenService() {
        this(VIGENCIA_PREDETERMINADA);
    }

    public TokenService(Duration vigencia) {
        if (vigencia == null || vigencia.isZero() || vigencia.isNegative()) {
            throw new IllegalArgumentException("La vigencia del token debe ser positiva.");
        }
        this.vigencia = vigencia;
    }

    public TokenEmitido emitir(SesionApi sesion) {
        Objects.requireNonNull(sesion, "Falta la sesión del token.");
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiraEn = Instant.now().plus(vigencia);
        entradas.put(huella(token), new Entrada(sesion, expiraEn));
        limpiarExpirados();
        return new TokenEmitido(token, expiraEn, sesion);
    }

    public Optional<SesionApi> validar(String token) {
        if (token == null || token.isBlank() || token.length() > LONGITUD_MAXIMA_TOKEN) {
            return Optional.empty();
        }
        String clave = huella(token);
        Entrada entrada = entradas.get(clave);
        if (entrada == null) return Optional.empty();
        if (!entrada.expiraEn().isAfter(Instant.now())) {
            entradas.remove(clave, entrada);
            return Optional.empty();
        }
        return Optional.of(entrada.sesion());
    }

    public void revocar(String token) {
        if (token == null || token.isBlank() || token.length() > LONGITUD_MAXIMA_TOKEN) return;
        entradas.remove(huella(token));
    }

    public void limpiarExpirados() {
        Instant ahora = Instant.now();
        entradas.entrySet().removeIf(entrada -> !entrada.getValue().expiraEn().isAfter(ahora));
    }

    int cantidadActiva() {
        limpiarExpirados();
        return entradas.size();
    }

    private static String huella(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256.", e);
        }
    }

    public record TokenEmitido(String token, Instant expiraEn, SesionApi sesion) { }

    private record Entrada(SesionApi sesion, Instant expiraEn) { }
}