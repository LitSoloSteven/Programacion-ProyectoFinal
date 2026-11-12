package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import org.hibernate.Session;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Participa en la misma Session del Service y su comprobacion de permisos. */
public interface TipoCambioDao {
    void guardar(Session session, TipoCambio tasa);
    List<TipoCambio> historial(Session session);
    Optional<TipoCambio> autorizadaPara(Session session, LocalDate fecha);
}
