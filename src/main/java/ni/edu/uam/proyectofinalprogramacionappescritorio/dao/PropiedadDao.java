package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PropiedadDao {

    // CREATE
    public void guardar(Propiedad propiedad) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(propiedad);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al guardar la propiedad", e);
        }
    }

    // READ
    public Propiedad buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Propiedad.class, id);
        }
    }

    // READ ALL
    public List<Propiedad> obtenerTodas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Propiedad", Propiedad.class).list();
        }
    }

    // Método HQL extra: Buscar propiedades por propietario
    public List<Propiedad> obtenerPorPropietario(Long propietarioId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Propiedad p WHERE p.propietario.id = :id", Propiedad.class)
                    .setParameter("id", propietarioId)
                    .list();
        }
    }

    // UPDATE
    public void actualizar(Propiedad propiedad) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(propiedad);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar la propiedad", e);
        }
    }

    // DELETE
    public void eliminar(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Propiedad propiedad = session.find(Propiedad.class, id);
            if (propiedad != null) {
                session.remove(propiedad);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al eliminar la propiedad", e);
        }
    }
}