package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import java.util.function.Function;

/** Un propietario transaccional por operacion; nunca comparte Session entre hilos. */
public final class HibernateTransaccion {
    private HibernateTransaccion() { }
    public static <T> T ejecutar(SessionFactory factory, Function<Session, T> trabajo) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T resultado = trabajo.apply(session);
                tx.commit();
                return resultado;
            } catch (RuntimeException | Error fallo) {
                try { if (tx.isActive()) tx.rollback(); }
                catch (RuntimeException rollback) { fallo.addSuppressed(rollback); }
                throw fallo;
            }
        }
    }
}
