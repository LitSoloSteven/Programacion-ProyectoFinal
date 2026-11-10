package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import ni.edu.uam.proyectofinalprogramacionappescritorio.entity.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.util.Properties;
import java.nio.file.Path;

public final class HibernateUtil {
    private static SessionFactory factory;
    private HibernateUtil() {}

    public static void main(String[] args) {
        try {
            HibernateTransactions tx = new HibernateTransactions(getSessionFactory());
            tx.ejecutar(session -> session.createQuery("select 1", Integer.class).getSingleResult());
            System.out.println("Conexión con PostgreSQL verificada correctamente.");
        } finally {
            shutdown();
        }
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (factory == null || factory.isClosed()) {
            factory = construir(DatabaseConfig.desdeArchivo(Path.of(".env"), System.getenv()));
        }
        return factory;
    }

    public static SessionFactory construir(Properties properties, Class<?>... entidadesAdicionales) {
        Configuration cfg = new Configuration().addProperties(properties)
                .addAnnotatedClass(Usuario.class).addAnnotatedClass(Auditoria.class)
                .addAnnotatedClass(Configuracion.class);
        for (Class<?> entidad : entidadesAdicionales) cfg.addAnnotatedClass(entidad);
        try {
            return cfg.buildSessionFactory();
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo iniciar la persistencia. Revisa la conexión y el esquema local.", e);
        }
    }

    public static synchronized void shutdown() {
        if (factory != null) {
            factory.close();
            factory = null;
        }
    }
}
