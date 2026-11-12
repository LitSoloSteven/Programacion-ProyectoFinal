package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import org.hibernate.Session;
import java.util.*;

/** Respeta el mapeo de main: numero Long y relacion OneToOne con Pago (UUID). */
public final class ReciboDao {
    public void guardar(Session session, Recibo recibo) {
        exigirTransaccion(session);
        session.persist(Objects.requireNonNull(recibo, "Falta el recibo."));
    }
    public Optional<Recibo> buscarPorId(Session session, Long numero) {
        Objects.requireNonNull(numero, "Falta el numero.");
        return buscarPorNumero(session, numero);
    }
    public Optional<Recibo> buscarPorNumero(Session session, long numero) {
        exigirTransaccion(session);
        if (numero <= 0) throw new IllegalArgumentException("El numero debe ser positivo.");
        return session.createQuery("select r from Recibo r join fetch r.pago where r.numero = :numero", Recibo.class)
                .setParameter("numero", numero).uniqueResultOptional();
    }
    public Optional<Recibo> buscarPorPago(Session session, UUID pagoId) {
        exigirTransaccion(session);
        return session.createQuery("select r from Recibo r join fetch r.pago p where p.id = :pagoId", Recibo.class)
                .setParameter("pagoId", Objects.requireNonNull(pagoId, "Falta el pago."))
                .uniqueResultOptional();
    }
    public List<Recibo> listarPorContrato(Session session, UUID contratoId, int desplazamiento, int cantidad) {
        exigirTransaccion(session);
        Objects.requireNonNull(contratoId, "Falta el contrato.");
        if (desplazamiento < 0 || cantidad < 1 || cantidad > 200) {
            throw new IllegalArgumentException("La pagina debe contener entre 1 y 200 registros y un desplazamiento no negativo.");
        }
        return List.copyOf(session.createQuery("select r from Recibo r join fetch r.pago p "
                + "where p.contratoId = :contratoId order by p.fechaPago desc, r.numero desc", Recibo.class)
                .setParameter("contratoId", contratoId).setFirstResult(desplazamiento)
                .setMaxResults(cantidad).getResultList());
    }
    private void exigirTransaccion(Session session) {
        Objects.requireNonNull(session, "Falta la sesion Hibernate.");
        if (!session.isOpen() || !session.getTransaction().isActive()) {
            throw new IllegalStateException("El Service debe proporcionar una transaccion activa.");
        }
    }
}
