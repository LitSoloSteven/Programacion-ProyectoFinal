package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Bloqueo temporal en memoria tras varios fallos de autenticación. No revela
 * si el usuario existe: el mensaje de respuesta es siempre el mismo.
 */
public final class ControlIntentosLogin {
    private static final int MAX_FALLOS_PREDETERMINADO = 5;
    private static final int MAX_FALLOS_ORIGEN_PREDETERMINADO = 25;
    private static final Duration VENTANA_PREDETERMINADA = Duration.ofMinutes(15);
    private static final Duration BLOQUEO_PREDETERMINADO = Duration.ofMinutes(15);
    private static final int MAXIMO_ENTRADAS = 10_000;

    private final int maxFallos;
    private final int maxFallosOrigen;
    private final Duration ventana;
    private final Duration bloqueo;
    private final Map<String, Estado> estados = new LinkedHashMap<>(16, 0.75f, true);

    public ControlIntentosLogin() {
        this(MAX_FALLOS_PREDETERMINADO, MAX_FALLOS_ORIGEN_PREDETERMINADO,
                VENTANA_PREDETERMINADA, BLOQUEO_PREDETERMINADO);
    }

    public ControlIntentosLogin(int maxFallos, Duration ventana, Duration bloqueo) {
        this(maxFallos, maxFallos, ventana, bloqueo);
    }

    public ControlIntentosLogin(int maxFallos, int maxFallosOrigen,
                                Duration ventana, Duration bloqueo) {
        if (maxFallos < 1) throw new IllegalArgumentException("El máximo de fallos debe ser positivo.");
        if (maxFallosOrigen < 1) throw new IllegalArgumentException("El máximo de fallos por origen debe ser positivo.");
        if (ventana == null || ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("La ventana de intentos debe ser positiva.");
        }
        if (bloqueo == null || bloqueo.isZero() || bloqueo.isNegative()) {
            throw new IllegalArgumentException("El bloqueo debe ser positivo.");
        }
        this.maxFallos = maxFallos;
        this.maxFallosOrigen = maxFallosOrigen;
        this.ventana = ventana;
        this.bloqueo = bloqueo;
    }

    public synchronized boolean estaBloqueado(String usuario) {
        return estaBloqueado(usuario, null);
    }

    public synchronized boolean estaBloqueado(String usuario, String origen) {
        Instant ahora = Instant.now();
        boolean usuarioBloqueado = bloqueado(claveUsuario(usuario), ahora);
        boolean origenBloqueado = bloqueado(claveOrigen(origen), ahora);
        return usuarioBloqueado || origenBloqueado;
    }

    public synchronized void registrarFallo(String usuario) {
        registrarFallo(usuario, null);
    }

    public synchronized void registrarFallo(String usuario, String origen) {
        Instant ahora = Instant.now();
        incrementar(claveUsuario(usuario), maxFallos, ahora);
        incrementar(claveOrigen(origen), maxFallosOrigen, ahora);
        limpiarSiCreceDemasiado(ahora);
    }

    public synchronized void registrarExito(String usuario) {
        registrarExito(usuario, null);
    }

    public synchronized void registrarExito(String usuario, String origen) {
        estados.remove(claveUsuario(usuario));
        String claveOrigen = claveOrigen(origen);
        if (claveOrigen != null) estados.remove(claveOrigen);
    }

    private void incrementar(String clave, int limite, Instant ahora) {
        if (clave == null) return;
        Estado estado = estadoVigente(clave);
        if (estado == null) estado = new Estado();
        if (estado.bloqueadoHasta != null && estado.bloqueadoHasta.isAfter(ahora)) {
            estados.put(clave, estado);
            return;
        }
        estado.fallos++;
        estado.ultimoFallo = ahora;
        if (estado.fallos >= limite) {
            estado.bloqueadoHasta = ahora.plus(bloqueo);
            estado.fallos = 0;
        }
        estados.put(clave, estado);
    }

    private boolean bloqueado(String clave, Instant ahora) {
        if (clave == null) return false;
        Estado estado = estadoVigente(clave);
        return estado != null && estado.bloqueadoHasta != null
                && estado.bloqueadoHasta.isAfter(ahora);
    }

    private Estado estadoVigente(String clave) {
        Estado estado = estados.get(clave);
        if (estado == null) return null;
        Instant ahora = Instant.now();
        boolean bloqueoVencido = estado.bloqueadoHasta == null || !estado.bloqueadoHasta.isAfter(ahora);
        boolean ventanaVencida = estado.ultimoFallo != null
                && !estado.ultimoFallo.plus(ventana).isAfter(ahora);
        if (bloqueoVencido && ventanaVencida) {
            estados.remove(clave);
            return null;
        }
        return estado;
    }

    private void limpiarSiCreceDemasiado(Instant ahora) {
        if (estados.size() <= MAXIMO_ENTRADAS) return;
        estados.entrySet().removeIf(entrada -> {
            Estado estado = entrada.getValue();
            boolean bloqueoVencido = estado.bloqueadoHasta == null
                    || !estado.bloqueadoHasta.isAfter(ahora);
            boolean ventanaVencida = estado.ultimoFallo != null
                    && !estado.ultimoFallo.plus(ventana).isAfter(ahora);
            return bloqueoVencido && ventanaVencida;
        });
        Iterator<String> claves = estados.keySet().iterator();
        while (estados.size() > MAXIMO_ENTRADAS && claves.hasNext()) {
            claves.next();
            claves.remove();
        }
    }

    private static String claveUsuario(String usuario) {
        String normalizado = usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
        return "usuario:" + sha256(normalizado);
    }

    private static String claveOrigen(String origen) {
        if (origen == null || origen.isBlank()) return null;
        return "origen:" + sha256(origen.strip().toLowerCase(Locale.ROOT));
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256 para limitar intentos.", e);
        }
    }

    private static final class Estado {
        private int fallos;
        private Instant ultimoFallo;
        private Instant bloqueadoHasta;
    }
}
