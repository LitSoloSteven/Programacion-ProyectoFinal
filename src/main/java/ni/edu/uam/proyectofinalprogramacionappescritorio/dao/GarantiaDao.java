package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Garantia;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class GarantiaDao {

    // CREATE
    public void guardar(Garantia garantia) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(garantia);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al registrar la garantía", e);
        }
    }

    // READ
    public Garantia buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Garantia.class, id);
        }
    }

    // Método específico por la relación 1:1 con Contrato
    public Garantia buscarPorContrato(Long contratoId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Garantia g WHERE g.contrato.id = :contratoId", Garantia.class)
                    .setParameter("contratoId", contratoId)
                    .uniqueResult();
        }
    }

    // UPDATE
    public void actualizar(Garantia garantia) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(garantia);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar la garantía", e);
        }
    }

    // DELETE
    public void eliminar(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Garantia garantia = session.find(Garantia.class, id);
            if (garantia != null) {
                session.remove(garantia);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al eliminar la garantía", e);
        }
    }
}