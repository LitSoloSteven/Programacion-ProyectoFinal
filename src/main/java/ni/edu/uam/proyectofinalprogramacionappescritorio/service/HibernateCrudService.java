package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import org.hibernate.Session;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class HibernateCrudService {
    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();

    HibernateCrudService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    <T> List<T> listar(Class<T> tipo, String hql) {
        return tx.ejecutar(s -> {
            permisos.exigir(s, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return s.createQuery(hql, tipo).getResultList();
        });
    }

    <T> void guardar(T item, Function<T, UUID> id, Consumer<T> validar) {
        validar.accept(item);
        if (id.apply(item) != null) {
            throw new IllegalArgumentException(
                    "No se guardan entidades desconectadas; actualice la entidad administrada.");
        }
        tx.ejecutar(s -> {
            permisos.exigir(s, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            s.persist(item);
            return null;
        });
    }

    <T> void cambiar(Class<T> tipo, UUID id, Consumer<T> cambio) {
        tx.ejecutar(s -> {
            permisos.exigir(s, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            T item = s.find(tipo, id);
            if (item == null) throw new IllegalArgumentException("El registro ya no existe.");
            cambio.accept(item);
            return null;
        });
    }

    <T> void eliminar(Class<T> tipo, UUID id, String permiso) {
        tx.ejecutar(s -> {
            permisos.exigir(s, SesionUsuario.getActual(), permiso);
            T item = s.find(tipo, id);
            if (item != null) s.remove(item);
            return null;
        });
    }
}
