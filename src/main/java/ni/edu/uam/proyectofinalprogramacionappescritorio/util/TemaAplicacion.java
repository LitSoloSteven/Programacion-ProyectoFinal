package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Aplica de forma centralizada el tema, la modalidad sin efectos y las preferencias locales. */
public final class TemaAplicacion {
    public enum Tema { CLARO, OSCURO }

    public static final String CLAVE_ULTIMO_TEMA = "lastTheme";
    public static final String CLAVE_ULTIMO_SIN_EFECTOS = "lastSinEfectos";
    public static final String MENSAJE_FALLO_GUARDADO =
            "No se pudo guardar tu preferencia visual en este equipo. El cambio se mantendrá durante esta sesión.";

    private static final String CSS = "/ni/edu/uam/proyectofinalprogramacionappescritorio/estilos.css";
    private static final Map<Scene, Boolean> ESCENAS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<DialogPane, Boolean> DIALOGOS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicBoolean AVISO_GUARDADO_EMITIDO = new AtomicBoolean(false);

    private static volatile AlmacenPreferencias almacen = AlmacenPreferencias.predeterminado();
    private static volatile String idOpacoActivo;
    private static volatile Tema tema = leerTemaSeguro(CLAVE_ULTIMO_TEMA);
    private static volatile boolean sinEfectos = leerSinEfectosSeguro(CLAVE_ULTIMO_SIN_EFECTOS);
    private static volatile Consumer<String> observadorAviso;
    private static volatile String ultimoAviso;

    private TemaAplicacion() { }

    /** Debe llamarse antes de crear la primera Scene. */
    public static void inicializar() {
        cargarFuente("fonts/FiraSans-Regular.ttf");
        cargarFuente("fonts/FiraCode-Regular.ttf");
    }

    public static Tema getTema() {
        return tema;
    }

    public static boolean isMovimientoReducido() {
        return sinEfectos;
    }

    public static boolean isSinEfectos() {
        return sinEfectos;
    }

    public static Optional<String> ultimoAvisoGuardado() {
        return Optional.ofNullable(ultimoAviso);
    }

    public static void registrarObservadorAviso(Consumer<String> observador) {
        observadorAviso = observador;
    }

    public static void configurarAlmacen(AlmacenPreferencias nuevoAlmacen) {
        almacen = nuevoAlmacen == null ? AlmacenPreferencias.predeterminado() : nuevoAlmacen;
        idOpacoActivo = null;
        AVISO_GUARDADO_EMITIDO.set(false);
        ultimoAviso = null;
        cargarPreferenciaPreAcceso();
    }

    public static void restaurarAlmacenPorDefecto() {
        configurarAlmacen(AlmacenPreferencias.predeterminado());
    }

    /** Carga la última elección local antes de autenticar (fallback seguro a Claro). */
    public static void cargarPreferenciaPreAcceso() {
        idOpacoActivo = null;
        AVISO_GUARDADO_EMITIDO.set(false);
        ultimoAviso = null;
        tema = leerTemaSeguro(CLAVE_ULTIMO_TEMA);
        sinEfectos = leerSinEfectosSeguro(CLAVE_ULTIMO_SIN_EFECTOS);
        refrescarEscenasYDialogos();
    }

    /** Carga las preferencias personales asociadas al identificador opaco del usuario autenticado. */
    public static void cargarPreferenciasUsuario(Long usuarioId) {
        String idOpaco = AlmacenPreferencias.idOpacoUsuario(usuarioId);
        idOpacoActivo = idOpaco;
        AVISO_GUARDADO_EMITIDO.set(false);
        ultimoAviso = null;
        tema = leerTemaSeguro(claveUsuarioTema(idOpaco));
        sinEfectos = leerSinEfectosSeguro(claveUsuarioSinEfectos(idOpaco));
        guardarSilenciosoPreAcceso(tema, sinEfectos);
        refrescarEscenasYDialogos();
    }

    /** Restablece el contexto de usuario al cerrar sesión y conserva la última preferencia local en acceso. */
    public static void limpiarUsuarioActivo() {
        cargarPreferenciaPreAcceso();
    }

    private static void cargarFuente(String recurso) {
        try (InputStream fuente = TemaAplicacion.class.getResourceAsStream(recurso)) {
            if (fuente != null) Font.loadFont(fuente, 14);
        } catch (Exception ignored) {
            // La interfaz usa Segoe UI y Consolas si las fuentes no están empaquetadas.
        }
    }

    public static void aplicar(Scene scene) {
        if (scene == null) return;
        ejecutarFx(() -> aplicarEscena(scene));
    }

    private static void aplicarEscena(Scene scene) {
        var hoja = TemaAplicacion.class.getResource(CSS);
        if (hoja != null && !scene.getStylesheets().contains(hoja.toExternalForm())) {
            scene.getStylesheets().add(hoja.toExternalForm());
        }
        ESCENAS.put(scene, Boolean.TRUE);
        aplicarClaseEscena(scene);
        if (scene.getRoot() instanceof StackPane stack) {
            Notificaciones.registrarAnfitrion(stack);
        }
    }

    public static void aplicar(DialogPane pane) {
        if (pane == null) return;
        ejecutarFx(() -> aplicarDialogo(pane));
    }

    private static void aplicarDialogo(DialogPane pane) {
        var hoja = TemaAplicacion.class.getResource(CSS);
        if (hoja != null && !pane.getStylesheets().contains(hoja.toExternalForm())) {
            pane.getStylesheets().add(hoja.toExternalForm());
        }
        DIALOGOS.put(pane, Boolean.TRUE);
        aplicarClase(pane);
    }

    private static void aplicarClaseEscena(Scene scene) {
        if (scene == null) return;
        Node focoPrevio = scene.getFocusOwner();
        aplicarClase(scene.getRoot());
        if (focoPrevio != null && focoPrevio.getScene() == scene && !focoPrevio.isFocused()) {
            focoPrevio.requestFocus();
        }
    }

    private static void aplicarClase(Node node) {
        if (node == null) return;
        var clases = node.getStyleClass();
        clases.removeAll("theme-light", "theme-dark", "reduced-motion", "no-effects");
        clases.add(tema == Tema.OSCURO ? "theme-dark" : "theme-light");
        if (sinEfectos) {
            clases.add("reduced-motion");
            clases.add("no-effects");
        }
        for (Node decoracion : node.lookupAll(".aurora-layer")) {
            decoracion.setVisible(!sinEfectos);
            decoracion.setManaged(!sinEfectos);
            if (sinEfectos) {
                decoracion.setEffect(null);
            }
        }
    }

    public static void establecerTema(Tema nuevoTema) {
        if (nuevoTema == null) return;
        tema = nuevoTema;
        persistirPreferencia(CLAVE_ULTIMO_TEMA,
                idOpacoActivo == null ? null : claveUsuarioTema(idOpacoActivo),
                nuevoTema.name());
        refrescarEscenasYDialogos();
    }

    public static void establecerSinEfectos(boolean desactivarEfectos) {
        establecerMovimientoReducido(desactivarEfectos);
    }

    public static void establecerMovimientoReducido(boolean reducido) {
        sinEfectos = reducido;
        persistirPreferencia(CLAVE_ULTIMO_SIN_EFECTOS,
                idOpacoActivo == null ? null : claveUsuarioSinEfectos(idOpacoActivo),
                Boolean.toString(reducido));
        refrescarEscenasYDialogos();
    }

    private static void refrescarEscenasYDialogos() {
        ejecutarFx(() -> {
            if (sinEfectos) {
                TransicionesUI.finalizarActivas();
            }
            synchronized (ESCENAS) {
                ESCENAS.keySet().forEach(TemaAplicacion::aplicarClaseEscena);
            }
            synchronized (DIALOGOS) {
                DIALOGOS.keySet().forEach(TemaAplicacion::aplicarClase);
            }
        });
    }

    private static void persistirPreferencia(String clavePreAcceso, String claveUsuario, String valor) {
        try {
            almacen.guardar(clavePreAcceso, valor);
            if (claveUsuario != null) {
                almacen.guardar(claveUsuario, valor);
            }
        } catch (Exception error) {
            notificarFalloGuardadoUnaVez();
        }
    }

    private static void guardarSilenciosoPreAcceso(Tema temaActual, boolean sinEfectosActual) {
        try {
            almacen.guardar(CLAVE_ULTIMO_TEMA, temaActual.name());
            almacen.guardar(CLAVE_ULTIMO_SIN_EFECTOS, Boolean.toString(sinEfectosActual));
        } catch (Exception ignored) {
            // No bloquea la autenticación si el almacén local está en solo lectura.
        }
    }

    private static void notificarFalloGuardadoUnaVez() {
        ultimoAviso = MENSAJE_FALLO_GUARDADO;
        if (!AVISO_GUARDADO_EMITIDO.compareAndSet(false, true)) {
            return;
        }
        Consumer<String> obs = observadorAviso;
        ejecutarFx(() -> {
            if (obs != null) {
                obs.accept(MENSAJE_FALLO_GUARDADO);
            } else {
                Notificaciones.mostrarAdvertencia(MENSAJE_FALLO_GUARDADO);
            }
        });
    }

    private static Tema leerTemaSeguro(String clave) {
        try {
            Optional<String> valor = almacen.leer(clave);
            if (valor.isEmpty() || valor.get().isBlank()) {
                return Tema.CLARO;
            }
            return Tema.valueOf(valor.get().strip());
        } catch (RuntimeException error) {
            return Tema.CLARO;
        }
    }

    private static boolean leerSinEfectosSeguro(String clave) {
        try {
            return almacen.leer(clave)
                    .map(String::strip)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static String claveUsuarioTema(String idOpaco) {
        return "users/" + idOpaco + "/theme";
    }

    private static String claveUsuarioSinEfectos(String idOpaco) {
        return "users/" + idOpaco + "/sinEfectos";
    }

    private static void ejecutarFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
        } else if (fxIniciado()) {
            Platform.runLater(accion);
        } else {
            // En pruebas sin toolkit JavaFX iniciado, el estado en memoria ya quedó actualizado.
        }
    }

    private static boolean fxIniciado() {
        try {
            Platform.runLater(() -> { });
            return true;
        } catch (IllegalStateException noIniciado) {
            return false;
        }
    }
}
