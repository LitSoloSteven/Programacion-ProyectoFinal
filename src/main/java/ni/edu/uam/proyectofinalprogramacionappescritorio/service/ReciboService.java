package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.ReciboDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.util.*;
import java.util.function.*;

/** Consultas de recibos mediante la sesion y los permisos compartidos de main. */
public final class ReciboService {
    private final HibernateTransactions tx;
    private final Supplier<SesionUsuario> identidad;
    private final ReciboDao dao = new ReciboDao();
    private final PermisoService permisos = new PermisoService();
    public ReciboService(SessionFactory factory) { this(factory, SesionUsuario::getActual); }
    public ReciboService(SessionFactory factory, Supplier<SesionUsuario> identidad) {
        tx = new HibernateTransactions(factory);
        this.identidad = Objects.requireNonNull(identidad);
    }
    public Optional<ReciboDTO> buscarPorId(Long numero) {
        Objects.requireNonNull(numero, "Falta el numero.");
        return buscarPorNumero(numero);
    }
    public Optional<ReciboDTO> buscarPorNumero(long numero) {
        if (numero <= 0) throw new IllegalArgumentException("El numero debe ser positivo.");
        return consultar(s -> dao.buscarPorNumero(s, numero).map(ReciboMapper::aDTO));
    }
    public Optional<ReciboDTO> buscarPorPago(UUID pagoId) {
        Objects.requireNonNull(pagoId, "Falta el pago.");
        return consultar(s -> dao.buscarPorPago(s, pagoId).map(ReciboMapper::aDTO));
    }
    public List<ReciboDTO> listarPorContrato(UUID contratoId, int desplazamiento, int cantidad) {
        Objects.requireNonNull(contratoId, "Falta el contrato.");
        if (desplazamiento < 0 || cantidad < 1 || cantidad > 200) {
            throw new IllegalArgumentException("La pagina debe contener entre 1 y 200 registros y un desplazamiento no negativo.");
        }
        return consultar(s -> dao.listarPorContrato(s, contratoId, desplazamiento, cantidad)
                .stream().map(ReciboMapper::aDTO).toList());
    }
    private <T> T consultar(Function<Session, T> operacion) {
        return tx.ejecutar(s -> {
            permisos.exigir(s, identidad.get(), Permiso.CONSULTAR_REPORTES);
            return operacion.apply(s);
        });
    }
}
