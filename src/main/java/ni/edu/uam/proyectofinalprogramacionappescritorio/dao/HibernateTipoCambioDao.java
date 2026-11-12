package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import org.hibernate.Session;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Sin transacciones independientes: el Service controla permisos y commit. */
public final class HibernateTipoCambioDao implements TipoCambioDao {
    @Override public void guardar(Session session, TipoCambio tasa) {
        exigirTransaccion(session);
        session.persist(Objects.requireNonNull(tasa));
    }
    @Override public List<TipoCambio> historial(Session session) {
        exigirTransaccion(session);
        return session.createQuery("from TipoCambio order by fecha desc, registradoEn desc, id desc",
                TipoCambio.class).getResultList();
    }
    @Override public Optional<TipoCambio> autorizadaPara(Session session, LocalDate fecha) {
        exigirTransaccion(session);
        return session.createQuery("from TipoCambio where fecha = :fecha and autorizadoPor is not null "
                + "order by registradoEn desc, id desc", TipoCambio.class)
                .setParameter("fecha", Objects.requireNonNull(fecha)).setMaxResults(1).uniqueResultOptional();
    }
    private void exigirTransaccion(Session session) {
        Objects.requireNonNull(session, "Falta la sesion.");
        if (!session.isOpen() || !session.getTransaction().isActive()) {
            throw new IllegalStateException("El Service debe proporcionar una transaccion activa.");
        }
    }
}
