package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UsuarioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import java.util.UUID;

public final class AutenticacionService {
    private final HibernateTransactions transactions;
    private final UsuarioDao usuarios = new UsuarioDao();
    private final PasswordHasher passwords = new PasswordHasher();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final String hashInexistente = passwords.crear(UUID.randomUUID().toString().toCharArray());

    public AutenticacionService(HibernateTransactions transactions) { this.transactions = transactions; }

    public SesionUsuario autenticar(String nombre, char[] password) {
        return autenticar(nombre, password, true);
    }

    /**
     * Autentica sin modificar la sesión global cuando se invoca desde la API.
     * La auditoría de inicio de sesión correcto se conserva en la transacción.
     */
    public SesionUsuario autenticar(String nombre, char[] password, boolean establecerSesionGlobal) {
        String normalizado;
        try { normalizado = Usuario.normalizarNombre(nombre); }
        catch (IllegalArgumentException e) { throw credencialesInvalidas(); }
        return transactions.ejecutar(session -> {
            Usuario usuario = usuarios.buscarPorNombre(session, normalizado).orElse(null);
            boolean valido = passwords.verificar(password, usuario == null ? hashInexistente : usuario.getPasswordHash());
            if (!valido || usuario == null || !usuario.isActivo()) throw credencialesInvalidas();
            auditoria.registrar(session, usuario, UUID.randomUUID(), "INICIO_SESION", "usuario:" + usuario.getId());
            SesionUsuario identidad = new SesionUsuario(usuario.getId(), usuario.getNombre(), usuario.getRol());
            if (establecerSesionGlobal) SesionUsuario.establecer(identidad);
            return identidad;
        });
    }

    private IllegalArgumentException credencialesInvalidas() {
        return new IllegalArgumentException("Usuario o contraseña incorrectos.");
    }
}
