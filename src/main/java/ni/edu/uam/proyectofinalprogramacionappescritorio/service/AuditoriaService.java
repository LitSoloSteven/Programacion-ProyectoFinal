package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;
import java.util.UUID;

public final class AuditoriaService {
    // Participa en la transacción de negocio: si ésta falla, tampoco queda una auditoría de éxito.
    public void registrar(Session session, Usuario actor, UUID correlacion, String accion, String referencia) {
        session.persist(new Auditoria(actor, correlacion, accion, referencia));
    }
}
