package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import org.hibernate.SessionFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class HibernateTipoCambioDao implements TipoCambioDao {
    private final SessionFactory factory;
    public HibernateTipoCambioDao(SessionFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }
    @Override public void guardar(TipoCambio tasa) {
        Objects.requireNonNull(tasa);
        HibernateTransaccion.ejecutar(factory, session -> {
            session.persist(tasa);
            return null;
        });
    }
    @Override public List<TipoCambio> historial() {
        return HibernateTransaccion.ejecutar(factory, session -> session.createQuery(
                "from TipoCambio order by fecha desc, registradoEn desc, id desc",
                TipoCambio.class).getResultList());
    }
    @Override public Optional<TipoCambio> autorizadaPara(LocalDate fecha) {
        return HibernateTransaccion.ejecutar(factory, session -> session.createQuery(
                "from TipoCambio where fecha = :fecha and autorizadoPor is not null "
                        + "order by registradoEn desc, id desc", TipoCambio.class)
                .setParameter("fecha", Objects.requireNonNull(fecha))
                .setMaxResults(1).uniqueResultOptional());
    }
}
