package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class UnidadDao {

    // CREATE
    public void guardar(Unidad unidad) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(unidad);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al guardar la unidad", e);
        }
    }

    // READ
    public Unidad buscarPorId(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Unidad.class, id);
        }
    }

    // READ ALL
    public List<Unidad> obtenerTodas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Unidad", Unidad.class).list();
        }
    }

    // Método HQL extra: Buscar unidades por propiedad
    public List<Unidad> obtenerPorPropiedad(Long propiedadId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Unidad u WHERE u.propiedad.id = :id", Unidad.class)
                    .setParameter("id", propiedadId)
                    .list();
        }
    }

    // UPDATE
    public void actualizar(Unidad unidad) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(unidad);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al actualizar la unidad", e);
        }
    }

    // DELETE
    public void eliminar(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Unidad unidad = session.find(Unidad.class, id);
            if (unidad != null) {
                session.remove(unidad);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Error al eliminar la unidad", e);
        }
    }
}