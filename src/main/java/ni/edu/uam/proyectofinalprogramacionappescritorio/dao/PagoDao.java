package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Pago;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PagoDao {

    public void guardar(Pago pago) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(pago);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al registrar el pago", e);
        }
    }

    public Pago buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Pago.class, id);
        }
    }

    public List<Pago> obtenerPagosPorContrato(Long contratoId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Pago p WHERE p.contrato.id = :contratoId ORDER BY p.fechaPago DESC", Pago.class)
                    .setParameter("contratoId", contratoId)
                    .list();
        }
    }

    public void actualizar(Pago pago) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(pago);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar el pago", e);
        }
    }

    // BAJA LÓGICA: Se evita el borrado físico para mantener integridad financiera
    public void anularPago(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Pago pago = session.find(Pago.class, id);
            if (pago != null && !pago.getEstado().equals("ANULADO")) {
                pago.setEstado("ANULADO");
                session.merge(pago);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al anular el pago", e);
        }
    }
}