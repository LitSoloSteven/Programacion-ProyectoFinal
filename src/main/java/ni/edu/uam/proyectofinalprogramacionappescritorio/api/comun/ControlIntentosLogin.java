package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Bloqueo temporal en memoria tras varios fallos de autenticación. No revela
 * si el usuario existe: el mensaje de respuesta es siempre el mismo.
 */
public final class ControlIntentosLogin {
    private static final int MAX_FALLOS_PREDETERMINADO = 5;
    private static final Duration VENTANA_PREDETERMINADA = Duration.ofMinutes(15);
    private static final Duration BLOQUEO_PREDETERMINADO = Duration.ofMinutes(15);
    private static final int MAXIMO_ENTRADAS = 10_000;

    private final int maxFallos;
    private final Duration ventana;
    private final Duration bloqueo;
    private final java.util.Map<String, Estado> estados = new java.util.HashMap<>();

    public ControlIntentosLogin() {
        this(MAX_FALLOS_PREDETERMINADO, VENTANA_PREDETERMINADA, BLOQUEO_PREDETERMINADO);
    }

    public ControlIntentosLogin(int maxFallos, Duration ventana, Duration bloqueo) {
        if (maxFallos < 1) throw new IllegalArgumentException("El máximo de fallos debe ser positivo.");
        if (ventana == null || ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("La ventana de intentos debe ser positiva.");
        }
        if (bloqueo == null || bloqueo.isZero() || bloqueo.isNegative()) {
            throw new IllegalArgumentException("El bloqueo debe ser positivo.");
        }
        this.maxFallos = maxFallos;
        this.ventana = ventana;
        this.bloqueo = bloqueo;
    }

    public synchronized boolean estaBloqueado(String usuario) {
        Estado estado = estadoVigente(clave(usuario));
        return estado != null && estado.bloqueadoHasta != null
                && estado.bloqueadoHasta.isAfter(Instant.now());
    }

    public synchronized void registrarFallo(String usuario) {
        String clave = clave(usuario);
        Instant ahora = Instant.now();
        Estado estado = estadoVigente(clave);
        if (estado == null) estado = new Estado();
        if (estado.bloqueadoHasta != null && estado.bloqueadoHasta.isAfter(ahora)) {
            estados.put(clave, estado);
            return;
        }
        estado.fallos++;
        estado.ultimoFallo = ahora;
        if (estado.fallos >= maxFallos) {
            estado.bloqueadoHasta = ahora.plus(bloqueo);
            estado.fallos = 0;
        }
        estados.put(clave, estado);
        limpiarSiCreceDemasiado(ahora);
    }

    public synchronized void registrarExito(String usuario) {
        estados.remove(clave(usuario));
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
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
    }

    private static final class Estado {
        private int fallos;
        private Instant ultimoFallo;
        private Instant bloqueadoHasta;
    }
}