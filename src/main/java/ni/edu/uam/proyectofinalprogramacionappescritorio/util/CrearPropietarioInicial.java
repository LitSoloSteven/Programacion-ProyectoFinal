package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.entity.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.entity.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AuditoriaService;
import java.util.Arrays;
import java.util.UUID;

/** Herramienta local de instalación; ejecutar una sola vez con la aplicación detenida. */
public final class CrearPropietarioInicial {
    private CrearPropietarioInicial() {}

    public static void main(String[] args) {
        String nombre = System.getenv("LATCH_INITIAL_USER");
        String secreto = System.getenv("LATCH_INITIAL_PASSWORD");
        if (nombre == null || secreto == null) {
            throw new IllegalArgumentException("Configura LATCH_INITIAL_USER y LATCH_INITIAL_PASSWORD para la instalación.");
        }
        char[] password = secreto.toCharArray();
        try {
            String hash = new PasswordHasher().crear(password);
            HibernateTransactions tx = new HibernateTransactions(HibernateUtil.getSessionFactory());
            tx.ejecutar(session -> {
                // Serializa dos instaladores concurrentes; este comando sólo se ejecuta en PostgreSQL.
                session.createNativeQuery("select cast(pg_advisory_xact_lock(76102026) as text)", String.class).getSingleResult();
                Long cantidad = session.createQuery("select count(u) from Usuario u", Long.class).getSingleResult();
                if (cantidad != 0) throw new IllegalArgumentException("Ya existen usuarios. Usa UsuarioService para crear otras cuentas.");
                Usuario usuario = new Usuario(nombre, hash, Rol.PROPIETARIO);
                session.persist(usuario);
                new AuditoriaService().registrar(session, usuario, UUID.randomUUID(), "INSTALACION", "usuario:" + usuario.getId());
                return null;
            });
            System.out.println("Propietario inicial creado.");
        } finally {
            Arrays.fill(password, '\0');
            HibernateUtil.shutdown();
        }
    }
}
