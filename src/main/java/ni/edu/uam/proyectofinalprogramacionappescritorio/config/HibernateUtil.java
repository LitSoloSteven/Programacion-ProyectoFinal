package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

// Importa todas las entidades
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.util.Properties;

public final class HibernateUtil {
    private static SessionFactory factory;
    private HibernateUtil() {}

    public static synchronized SessionFactory getSessionFactory() {
        if (factory == null || factory.isClosed()) {
            factory = construir(DatabaseConfig.desdeEntorno(System.getenv()));
        }
        return factory;
    }

    public static SessionFactory construir(Properties properties, Class<?>... entidadesAdicionales) {
        Configuration cfg = new Configuration().addProperties(properties)
                // Entidades de Seguridad y Configuración (Steven)
                .addAnnotatedClass(Usuario.class)
                .addAnnotatedClass(Auditoria.class)
                .addAnnotatedClass(Configuracion.class)
                // TUS ENTIDADES (Inventario, Alquiler y Finanzas)
                .addAnnotatedClass(Propietario.class)
                .addAnnotatedClass(Propiedad.class)
                .addAnnotatedClass(Unidad.class)
                .addAnnotatedClass(Inquilino.class)
                .addAnnotatedClass(Contrato.class)
                .addAnnotatedClass(Cargo.class)
                .addAnnotatedClass(Pago.class)
                .addAnnotatedClass(Garantia.class);

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