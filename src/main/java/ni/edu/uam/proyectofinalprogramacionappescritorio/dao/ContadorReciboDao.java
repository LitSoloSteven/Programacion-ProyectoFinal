package ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ContadorRecibo;
import org.hibernate.Session;
import java.util.Objects;

/**
 * Participa en la Session del Service, sin abrir ni confirmar transacciones.
 * El numero solo se confirma si el pago, el recibo y la auditoria se confirman
 * en esa misma transaccion. Un rollback revierte tambien el contador.
 */
public final class ContadorReciboDao {
    public long siguiente(Session session) {
        Objects.requireNonNull(session, "Falta la sesion Hibernate.");
        if (!session.isOpen() || !session.getTransaction().isActive()) {
            throw new IllegalStateException("Se requiere una transaccion financiera activa.");
        }
        ContadorRecibo contador = session.find(ContadorRecibo.class,
                ContadorRecibo.CLAVE, LockModeType.PESSIMISTIC_WRITE);
        if (contador == null) {
            throw new IllegalStateException("Falta inicializar el contador de recibos con el script SQL.");
        }
        return contador.siguiente();
    }
}
