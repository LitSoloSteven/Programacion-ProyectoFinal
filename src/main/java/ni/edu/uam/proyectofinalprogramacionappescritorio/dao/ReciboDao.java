package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import org.hibernate.Session;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia y consultas HQL de recibos.
 * Recibe la Session del Service: no abre conexiones ni confirma transacciones.
 * Asi el Service puede guardar pago, recibo, contador y auditoria atomicamente.
 * Los permisos y la asignacion del consecutivo corresponden al Service.
 */
public final class ReciboDao {
    private static final int MAXIMO_POR_PAGINA = 200;

    public void guardar(Session session, Recibo recibo) {
        exigirTransaccion(session);
        session.persist(Objects.requireNonNull(recibo, "Falta el recibo."));
    }

    public Optional<Recibo> buscarPorId(Session session, UUID id) {
        exigirTransaccion(session);
        return Optional.ofNullable(session.find(Recibo.class,
                Objects.requireNonNull(id, "Falta el identificador.")));
    }

    public Optional<Recibo> buscarPorPago(Session session, UUID pagoId) {
        exigirTransaccion(session);
        return session.createQuery("from Recibo where pagoId = :pagoId", Recibo.class)
                .setParameter("pagoId", Objects.requireNonNull(pagoId, "Falta el pago."))
                .uniqueResultOptional();
    }

    public Optional<Recibo> buscarPorNumero(Session session, long numero) {
        exigirTransaccion(session);
        if (numero <= 0) throw new IllegalArgumentException("El numero debe ser positivo.");
        return session.createQuery("from Recibo where numero = :numero", Recibo.class)
                .setParameter("numero", numero)
                .uniqueResultOptional();
    }

    /** Incluye recibos anulados para conservar el historial del contrato. */
    public List<Recibo> listarPorContrato(Session session, UUID contratoId,
                                         int desplazamiento, int cantidad) {
        exigirTransaccion(session);
        Objects.requireNonNull(contratoId, "Falta el contrato.");
        if (desplazamiento < 0 || cantidad < 1 || cantidad > MAXIMO_POR_PAGINA) {
            throw new IllegalArgumentException("La pagina debe contener entre 1 y 200 registros y un desplazamiento no negativo.");
        }
        return List.copyOf(session.createQuery(
                "from Recibo where contratoId = :contratoId order by fechaPago desc, numero desc",
                Recibo.class)
                .setParameter("contratoId", contratoId)
                .setFirstResult(desplazamiento)
                .setMaxResults(cantidad)
                .getResultList());
    }

    private void exigirTransaccion(Session session) {
        Objects.requireNonNull(session, "Falta la sesion Hibernate.");
        if (!session.isOpen() || !session.getTransaction().isActive()) {
            throw new IllegalStateException("El Service debe proporcionar una sesion con transaccion activa.");
        }
    }
}
