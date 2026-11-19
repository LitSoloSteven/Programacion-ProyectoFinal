package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import jakarta.persistence.OptimisticLockException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojDesfasadoException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import java.util.Objects;
import java.util.function.Function;

public final class HibernateTransactions {
    private final SessionFactory factory;
    private final ThreadLocal<ContextoTransaccion> actual = new ThreadLocal<>();

    public HibernateTransactions(SessionFactory factory) { this.factory = Objects.requireNonNull(factory); }

    public <T> T ejecutar(Function<Session, T> operacion) {
        Objects.requireNonNull(operacion, "Falta la operación transaccional.");
        ContextoTransaccion existente = actual.get();
        if (existente != null) return ejecutarAnidada(existente, operacion);

        try (Session session = factory.openSession()) {
            Transaction tx = session.beginTransaction();
            ContextoTransaccion contexto = new ContextoTransaccion(session);
            actual.set(contexto);
            try {
                T resultado = operacion.apply(session);
                if (contexto.rollbackOnly) propagar(contexto.error);
                tx.commit();
                return resultado;
            } catch (RuntimeException | Error e) {
                try {
                    if (tx.isActive()) tx.rollback();
                } catch (RuntimeException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                actual.remove();
            }
        } catch (RuntimeException e) {
            throw traducir(e);
        }
    }

    private static <T> T ejecutarAnidada(ContextoTransaccion contexto,
                                          Function<Session, T> operacion) {
        try {
            return operacion.apply(contexto.session);
        } catch (RuntimeException | Error e) {
            Throwable traducido = e instanceof RuntimeException runtime
                    ? traducir(runtime) : e;
            if (!contexto.rollbackOnly) {
                contexto.rollbackOnly = true;
                contexto.error = traducido;
            }
            if (traducido instanceof RuntimeException runtime) throw runtime;
            throw (Error) traducido;
        }
    }

    private static RuntimeException traducir(RuntimeException error) {
        if (error instanceof SecurityException || error instanceof IllegalArgumentException
                || error instanceof ConflictoDatosException
                || error instanceof RelojDesfasadoException || error instanceof ExcepcionApi) {
            return error;
        }
        if (error instanceof OptimisticLockException || error instanceof StaleStateException) {
            return new ConflictoDatosException(
                    "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
        }
        if (error instanceof ConstraintViolationException violacion) {
            if (violacion.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return new ConflictoDatosException("Ya existe un registro con esos datos.");
            }
            return new IllegalStateException(
                    "La operación viola una restricción de la base de datos.", violacion);
        }
        return new IllegalStateException(
                "No se pudo completar la operación. Revisa la conexión e inténtalo nuevamente.",
                error);
    }

    private static void propagar(Throwable error) {
        if (error instanceof RuntimeException runtime) throw runtime;
        throw (Error) error;
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ContextoTransaccion {
        private final Session session;
        private boolean rollbackOnly;
        private Throwable error;

    }
}
