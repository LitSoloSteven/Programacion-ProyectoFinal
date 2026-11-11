package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Propietario;
// Importa tu HibernateUtil dependiendo del paquete exacto donde se encuentre
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PropietarioDao {

    // CREATE (persist)
    public void guardar(Propietario propietario) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(propietario);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al guardar el propietario", e);
        }
    }

    // READ (find)
    public Propietario buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Propietario.class, id);
        }
    }

    // READ ALL (HQL básico)
    public List<Propietario> obtenerTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Propietario", Propietario.class).list();
        }
    }

    // UPDATE (merge)
    public void actualizar(Propietario propietario) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(propietario);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar el propietario", e);
        }
    }

    // DELETE (remove)
    public void eliminar(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Propietario propietario = session.find(Propietario.class, id);
            if (propietario != null) {
                session.remove(propietario);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al eliminar el propietario", e);
        }
    }
}