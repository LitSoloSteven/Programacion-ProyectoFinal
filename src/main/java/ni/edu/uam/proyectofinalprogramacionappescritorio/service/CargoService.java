package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.CargoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de cargos y obligaciones financieras en Latch.
 */
public class CargoService {

    private final CargoDao cargoDao;

    public CargoService() {
        this.cargoDao = new CargoDao();
    }

    public CargoService(CargoDao cargoDao) {
        this.cargoDao = cargoDao;
    }

    public List<Cargo> listarTodos() throws SQLException {
        return cargoDao.listarTodos();
    }

    public void guardar(Cargo c) throws SQLException {
        validarCargo(c);

        if (c.getId() == null) {
            cargoDao.insertar(c);
        } else {
            cargoDao.actualizar(c);
        }
    }

    public void eliminar(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un cargo válido.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede eliminar cargos físicamente.");
        }
        cargoDao.eliminar(id);
    }

    private void validarCargo(Cargo c) {
        if (c == null) {
            throw new IllegalArgumentException("La información del cargo no puede ser nula.");
        }
        if (c.getContratoId() == null) {
            throw new IllegalArgumentException("Debe asociar el cargo a un contrato de arrendamiento.");
        }
        if (c.getPeriodo() == null) {
            throw new IllegalArgumentException("El periodo de facturación es obligatorio.");
        }
        if (c.getFechaEmision() == null) {
            throw new IllegalArgumentException("La fecha de emisión del cargo es obligatoria.");
        }
        if (c.getVenceEl() == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria.");
        }
        if (c.getVenceEl().isBefore(c.getFechaEmision())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de emisión.");
        }
        if (c.getImporte() == null || c.getImporte().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe del cargo debe ser mayor a cero.");
        }
        if (c.getConcepto() == null || c.getConcepto().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar el concepto o detalle del cobro.");
        }
    }
}
