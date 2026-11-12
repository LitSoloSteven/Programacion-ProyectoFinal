package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.HibernateTransaccion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.ReciboDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboMapper;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Consultas de recibos para cuentas autenticadas de Propietario o Asistente.
 * Devuelve DTO y los construye antes de cerrar la sesion Hibernate.
 * La emision y la anulacion conjunta con pagos se integran por separado.
 */
public final class ReciboService {
    private final SessionFactory factory;
    private final Supplier<Usuario> usuarioActual;
    private final ReciboDao dao = new ReciboDao();

    public ReciboService(SessionFactory factory) {
        this(factory, SesionUsuario::getUsuarioActual);
    }

    public ReciboService(SessionFactory factory, Supplier<Usuario> usuarioActual) {
        this.factory = Objects.requireNonNull(factory, "Falta SessionFactory.");
        this.usuarioActual = Objects.requireNonNull(usuarioActual, "Falta el proveedor de usuario.");
    }

    public Optional<ReciboDTO> buscarPorId(UUID id) {
        exigirUsuario();
        Objects.requireNonNull(id, "Falta el identificador del recibo.");
        return HibernateTransaccion.ejecutar(factory, session ->
                dao.buscarPorId(session, id).map(ReciboMapper::aDTO));
    }

    public Optional<ReciboDTO> buscarPorPago(UUID pagoId) {
        exigirUsuario();
        Objects.requireNonNull(pagoId, "Falta el identificador del pago.");
        return HibernateTransaccion.ejecutar(factory, session ->
                dao.buscarPorPago(session, pagoId).map(ReciboMapper::aDTO));
    }

    public Optional<ReciboDTO> buscarPorNumero(long numero) {
        exigirUsuario();
        if (numero <= 0) throw new IllegalArgumentException("El numero debe ser positivo.");
        return HibernateTransaccion.ejecutar(factory, session ->
                dao.buscarPorNumero(session, numero).map(ReciboMapper::aDTO));
    }

    /** Incluye anulados y devuelve una lista inmutable con paginacion acotada. */
    public List<ReciboDTO> listarPorContrato(UUID contratoId, int desplazamiento, int cantidad) {
        exigirUsuario();
        Objects.requireNonNull(contratoId, "Falta el identificador del contrato.");
        if (desplazamiento < 0 || cantidad < 1 || cantidad > 200) {
            throw new IllegalArgumentException("La pagina debe contener entre 1 y 200 registros y un desplazamiento no negativo.");
        }
        return HibernateTransaccion.ejecutar(factory, session ->
                dao.listarPorContrato(session, contratoId, desplazamiento, cantidad)
                        .stream().map(ReciboMapper::aDTO).toList());
    }

    private void exigirUsuario() {
        Usuario actor = usuarioActual.get();
        if (actor == null || !actor.isActivo() || actor.getId() == null
                || !(actor.esPropietario() || "Asistente".equalsIgnoreCase(actor.getRolNombre()))) {
            throw new SecurityException("Se requiere una sesion activa y autorizada para consultar recibos.");
        }
    }
}
