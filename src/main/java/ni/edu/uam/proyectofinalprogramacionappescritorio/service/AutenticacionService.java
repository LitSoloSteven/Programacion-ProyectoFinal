package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import lombok.RequiredArgsConstructor;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.SesionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UsuarioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import java.util.UUID;

@RequiredArgsConstructor
public final class AutenticacionService {
    private final HibernateTransactions transactions;
    private final UsuarioDao usuarios = new UsuarioDao();
    private final PasswordHasher passwords = new PasswordHasher();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final String hashInexistente = passwords.crear(UUID.randomUUID().toString().toCharArray());

    public SesionUsuario autenticar(String nombre, char[] password) {
        return autenticar(nombre, password, true, "desktop");
    }

    /**
     * Autentica sin modificar la sesión global cuando se invoca desde la API.
     * La auditoría de inicio de sesión correcto se conserva en la transacción.
     */
    public SesionUsuario autenticar(String nombre, char[] password, boolean establecerSesionGlobal) {
        return autenticar(nombre, password, establecerSesionGlobal, "desktop");
    }

    public SesionUsuario autenticar(String nombre, char[] password,
                                    boolean establecerSesionGlobal, String origen) {
        return autenticar(nombre, password, establecerSesionGlobal, origen, null);
    }

    public SesionUsuario autenticar(String nombre, char[] password,
                                    boolean establecerSesionGlobal, String origen, char[] nuevaPassword) {
        String normalizado;
        try { normalizado = Usuario.normalizarNombre(nombre); }
        catch (IllegalArgumentException e) { normalizado = null; }
        String identificadorNormalizado = normalizado;
        SesionUsuario identidad = transactions.ejecutar(session -> {
            Usuario usuario = identificadorNormalizado == null ? null
                    : usuarios.buscarPorNombre(session, identificadorNormalizado).orElse(null);
            boolean valido = passwords.verificar(password, usuario == null ? hashInexistente : usuario.getPasswordHash());
            if (!valido || usuario == null || !usuario.isActivo()) {
                auditoria.registrarIntentoLoginFallido(session, UUID.randomUUID(), nombre, origen);
                return null;
            }
            if (usuario.isDebeCambiarPassword()) {
                if (nuevaPassword == null || nuevaPassword.length == 0) {
                    throw new CambioPasswordRequerido("Debes cambiar tu contraseña temporal para iniciar sesión.");
                }
                if (java.util.Arrays.equals(password, nuevaPassword)) {
                    throw new CambioPasswordRequerido("La nueva contraseña debe ser diferente a la temporal.");
                }
                try {
                    usuario.completarCambioPassword(passwords.crear(nuevaPassword));
                } catch (IllegalArgumentException e) {
                    throw new CambioPasswordRequerido(e.getMessage());
                }
                session.flush();
                auditoria.registrar(session, usuario, UUID.randomUUID(),
                        "CAMBIAR_PASSWORD_PROPIA", "usuario:" + usuario.getId());
            } else if (passwords.requiereActualizacion(usuario.getPasswordHash())) {
                usuario.actualizarPasswordHash(passwords.crear(password));
                session.flush();
                auditoria.registrar(session, usuario, UUID.randomUUID(),
                        "MIGRAR_HASH_PASSWORD", "usuario:" + usuario.getId());
            }
            auditoria.registrar(session, usuario, UUID.randomUUID(), "INICIO_SESION", "usuario:" + usuario.getId());
            SesionUsuario sesionUsuario = new SesionUsuario(
                    usuario.getId(), usuario.getNombre(), usuario.getRol(), usuario.getVersion());
            if (establecerSesionGlobal) SesionUsuario.establecer(sesionUsuario);
            return sesionUsuario;
        });
        if (identidad == null) throw credencialesInvalidas();
        return identidad;
    }

    public static final class CambioPasswordRequerido extends IllegalArgumentException {
        public CambioPasswordRequerido(String mensaje) {
            super(mensaje);
        }
    }

    public boolean sesionVigente(SesionApi identidad) {
        if (identidad == null) return false;
        return transactions.ejecutar(session -> {
            Usuario usuario = session.find(Usuario.class, identidad.usuarioId());
            return usuario != null && usuario.isActivo()
                    && usuario.getNombre().equals(identidad.nombre())
                    && usuario.getRol().equals(identidad.rol())
                    && usuario.getVersion() == identidad.version();
        });
    }

    private IllegalArgumentException credencialesInvalidas() {
        return new IllegalArgumentException("Usuario o contraseña incorrectos.");
    }
}
