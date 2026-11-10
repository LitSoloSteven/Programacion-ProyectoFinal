package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.PagoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para el registro y gestión de pagos en Latch.
 */
public class PagoService {

    private final PagoDao pagoDao;

    public PagoService() {
        this.pagoDao = new PagoDao();
    }

    public PagoService(PagoDao pagoDao) {
        this.pagoDao = pagoDao;
    }

    public List<Pago> listarTodos() throws SQLException {
        return pagoDao.listarTodos();
    }

    public void registrarPago(Pago p) throws SQLException {
        validarPago(p);
        pagoDao.insertar(p);
    }

    public void anular(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un pago válido.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede anular pagos.");
        }
        pagoDao.anular(id);
    }

    public void eliminar(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un pago válido.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede eliminar pagos del sistema.");
        }
        pagoDao.eliminar(id);
    }

    private void validarPago(Pago p) {
        if (p == null) {
            throw new IllegalArgumentException("La información del pago no puede ser nula.");
        }
        if (p.getContratoId() == null) {
            throw new IllegalArgumentException("Debe seleccionar el contrato al que se le aplicará el pago.");
        }
        if (p.getFechaPago() == null) {
            throw new IllegalArgumentException("La fecha de realización del pago es obligatoria.");
        }
        if (p.getMontoOriginal() == null || p.getMontoOriginal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a pagar debe ser mayor a cero.");
        }
        if (p.getMetodo() == null || p.getMetodo().isBlank()) {
            throw new IllegalArgumentException("Debe especificar el método de pago utilizado.");
        }
    }
}
