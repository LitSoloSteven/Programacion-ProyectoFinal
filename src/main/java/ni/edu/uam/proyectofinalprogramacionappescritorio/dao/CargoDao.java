
package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Cargo;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CargoDao {

    public void guardar(Cargo cargo) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(cargo);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al guardar el cargo", e);
        }
    }

    public Cargo buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Cargo.class, id);
        }
    }

    // HQL 2 OBLIGATORIA: Cargos pendientes por inquilino
    public List<Cargo> obtenerCargosPendientesPorInquilino(Long inquilinoId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT c FROM Cargo c " +
                                    "JOIN FETCH c.contrato ct " +
                                    "JOIN FETCH ct.inquilino i " +
                                    "WHERE c.estado = 'PENDIENTE' AND i.id = :inquilinoId " +
                                    "ORDER BY c.fechaVencimiento ASC", Cargo.class)
                    .setParameter("inquilinoId", inquilinoId)
                    .list();
        }
    }

    public void actualizar(Cargo cargo) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(cargo);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar el cargo", e);
        }
    }
}