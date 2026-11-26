package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Identidad del usuario autenticado en el cliente de escritorio JavaFX.
 * No conserva contraseñas ni accede a Hibernate/BD.
 */
public final class SesionCliente {
    private static final AtomicLong GENERACION = new AtomicLong();
    private static volatile SesionCliente actual;

    private final long usuarioId;
    private final String nombre;
    private final String rol;
    private final boolean esPropietario;

    public SesionCliente(long usuarioId, String nombre, String rol) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("Identificador de usuario inválido.");
        }
        this.usuarioId = usuarioId;
        this.nombre = Objects.requireNonNull(nombre, "Falta el nombre de usuario.");
        this.rol = Objects.requireNonNull(rol, "Falta el rol del usuario.");
        this.esPropietario = ConstantesLatch.ROL_PROPIETARIO.equalsIgnoreCase(rol.strip());
    }

    public static void iniciar(long usuarioId, String nombre, String rol) {
        actual = new SesionCliente(usuarioId, nombre, rol);
        GENERACION.incrementAndGet();
    }

    public static void cerrarSesion() {
        actual = null;
        GENERACION.incrementAndGet();
    }

    public static long generacionActual() {
        return GENERACION.get();
    }

    public static SesionCliente getActual() {
        SesionCliente sesion = actual;
        if (sesion == null) {
            throw new IllegalStateException("No hay una sesión activa en el cliente.");
        }
        return sesion;
    }

    public static boolean haySesionActiva() {
        return actual != null;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public boolean esPropietario() {
        return esPropietario;
    }

    public boolean esAsistente() {
        return !esPropietario;
    }
}
