package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia: permite integrar Hibernate sin acoplarlo a la UI. */
public interface TipoCambioDao {
    void guardar(TipoCambio tasa);
    List<TipoCambio> historial();
    Optional<TipoCambio> autorizadaPara(LocalDate fecha);
}
