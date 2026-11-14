package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class SesionUsuario implements AutoCloseable {
    private final Long usuarioId;
    private final String nombre;
    private final String rol;
    private static volatile SesionUsuario actual;
    private static final ThreadLocal<SesionUsuario> contextoHilo = new ThreadLocal<>();
    // Marca los hilos que ya están atendiendo una petición aislada (API). En
    // ese caso no se permite caer en la sesión global de la interfaz: si la
    // identidad no es válida, la operación falla cerrada en vez de ejecutarse
    // como el usuario del escritorio.
    private static final ThreadLocal<Boolean> contextoAislado = new ThreadLocal<>();
    private final AtomicBoolean vigente = new AtomicBoolean(true);

    SesionUsuario(Long usuarioId, String nombre, String rol) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.rol = rol;
    }
    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public boolean isVigente() { return vigente.get(); }

    /**
     * Devuelve la identidad del hilo actual si existe; en caso contrario,
     * conserva la sesión global que usa la interfaz JavaFX. Dentro de una
     * operación aislada (API) nunca se usa la sesión global.
     */
    public static SesionUsuario getActual() {
        SesionUsuario contexto = contextoHilo.get();
        if (contexto != null && contexto.isVigente()) return contexto;
        if (Boolean.TRUE.equals(contextoAislado.get())) return null;
        return actual != null && actual.isVigente() ? actual : null;
    }

    /**
     * Ejecuta una operación con una identidad aislada para este hilo. El
     * servidor HTTP lo usa para que dos peticiones concurrentes no compartan
     * la sesión global de la interfaz.
     */
    public static <T> T ejecutarComo(SesionUsuario identidad, Supplier<T> operacion) {
        Objects.requireNonNull(identidad, "Falta la identidad de la sesión.");
        Objects.requireNonNull(operacion, "Falta la operación a ejecutar.");
        SesionUsuario anterior = contextoHilo.get();
        Boolean aisladoAnterior = contextoAislado.get();
        contextoHilo.set(identidad);
        contextoAislado.set(Boolean.TRUE);
        try {
            return operacion.get();
        } finally {
            if (anterior == null) contextoHilo.remove();
            else contextoHilo.set(anterior);
            if (aisladoAnterior == null) contextoAislado.remove();
            else contextoAislado.set(aisladoAnterior);
        }
    }

    public static void ejecutarComo(SesionUsuario identidad, Runnable operacion) {
        Objects.requireNonNull(operacion, "Falta la operación a ejecutar.");
        ejecutarComo(identidad, () -> {
            operacion.run();
            return null;
        });
    }

    /**
     * Construye una identidad ya autenticada por la API. No verifica
     * credenciales: solo debe usarse después de un login correcto.
     */
    public static SesionUsuario paraSesionAutenticada(Long usuarioId, String nombre, String rol) {
        if (usuarioId == null || nombre == null || nombre.isBlank() || !Rol.esValido(rol)) {
            throw new IllegalArgumentException("Identidad de sesión inválida.");
        }
        return new SesionUsuario(usuarioId, nombre, rol);
    }

    public static void establecer(SesionUsuario sesion) { cerrarSesion(); actual = sesion; }
    public static void cerrarSesion() { SesionUsuario anterior = actual; actual = null; if (anterior != null) anterior.close(); }
    public static boolean esPropietario() {
        SesionUsuario sesion = getActual();
        return sesion != null && Rol.PROPIETARIO.equals(sesion.getRol());
    }
    @Override public void close() { vigente.set(false); }
}
