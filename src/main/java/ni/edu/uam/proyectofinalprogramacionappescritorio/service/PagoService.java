package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.PagoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Pago;
import java.math.BigDecimal;
import java.util.List;

public class PagoService {

    private final PagoDao pagoDao = new PagoDao();

    public void registrarPago(Pago pago) {
        // Validación: El monto debe ser numérico y mayor que cero
        if (pago.getMonto() == null || pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero.");
        }

        if (pago.getUuid() == null || pago.getUuid().isEmpty()) {
            throw new IllegalArgumentException("El pago debe contener un UUID para garantizar la idempotencia.");
        }

        pagoDao.guardar(pago);
    }

    public Pago buscarPorId(Long id) {
        return pagoDao.buscarPorId(id);
    }

    public List<Pago> obtenerPagosPorContrato(Long contratoId) {
        return pagoDao.obtenerPagosPorContrato(contratoId);
    }

    // Regla: Los pagos son inmutables, se anulan lógicamente, no se eliminan de la base de datos
    public void anularPago(Long pagoId) {
        pagoDao.anularPago(pagoId);
    }
}