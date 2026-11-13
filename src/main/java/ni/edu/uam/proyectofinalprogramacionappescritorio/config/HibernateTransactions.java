package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import java.util.Objects;
import java.util.function.Function;

public final class HibernateTransactions {
    private final SessionFactory factory;
    public HibernateTransactions(SessionFactory factory) { this.factory = Objects.requireNonNull(factory); }

    public <T> T ejecutar(Function<Session, T> operacion) {
        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T resultado = operacion.apply(session);
                tx.commit();
                return resultado;
            } catch (RuntimeException | Error e) {
                try {
                    if (tx.isActive()) tx.rollback();
                } catch (RuntimeException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (OptimisticLockException | StaleStateException e) {
            throw new IllegalStateException("Otro usuario modificó el registro. Actualiza los datos antes de reintentar.", e);
        } catch (ConstraintViolationException e) {
            String mensaje = switch (e.getKind()) {
                case UNIQUE -> "Ya existe un registro con esos datos.";
                default -> "La operación viola una restricción de la base de datos.";
            };
            throw new IllegalStateException(mensaje, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo completar la operación. Revisa la conexión e inténtalo nuevamente.", e);
        }
    }
}
