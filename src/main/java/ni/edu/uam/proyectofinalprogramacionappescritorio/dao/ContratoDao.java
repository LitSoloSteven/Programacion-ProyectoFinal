package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Contrato;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ContratoDao {

    public void guardar(Contrato contrato) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(contrato);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al guardar el contrato", e);
        }
    }

    public Contrato buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Contrato.class, id);
        }
    }

    // HQL 1 OBLIGATORIA: Consulta de contratos activos con todas sus relaciones
    public List<Contrato> obtenerContratosActivos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT c FROM Contrato c " +
                                    "JOIN FETCH c.unidad u " +
                                    "JOIN FETCH u.propiedad p " +
                                    "JOIN FETCH c.inquilino i " +
                                    "WHERE c.estado IN ('VIGENTE', 'POR_VENCER') " +
                                    "ORDER BY p.nombre, u.codigo", Contrato.class)
                    .list();
        }
    }

    public void actualizar(Contrato contrato) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(contrato);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar el contrato", e);
        }
    }
}