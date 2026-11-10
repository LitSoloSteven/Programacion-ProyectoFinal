package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.ContratoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de Contratos de arrendamiento en Latch.
 * Aplica reglas de coherencia de fechas, montos y estados.
 */
public class ContratoService {

    private final ContratoDao contratoDao;

    public ContratoService() {
        this.contratoDao = new ContratoDao();
    }

    public ContratoService(ContratoDao contratoDao) {
        this.contratoDao = contratoDao;
    }

    public List<Contrato> listarTodos() throws SQLException {
        return contratoDao.listarTodos();
    }

    public List<Contrato> listarVigentes() throws SQLException {
        return contratoDao.listarVigentes();
    }

    public void guardar(Contrato c) throws SQLException {
        validarContrato(c);

        if (c.getId() == null) {
            contratoDao.insertar(c);
        } else {
            contratoDao.actualizar(c);
        }
    }

    public void terminarContrato(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un contrato válido.");
        }
        contratoDao.cambiarEstado(id, "Terminado");
    }

    public void eliminar(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un contrato válido.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede eliminar contratos físicamente.");
        }
        contratoDao.eliminar(id);
    }

    private void validarContrato(Contrato c) {
        if (c == null) {
            throw new IllegalArgumentException("La información del contrato no puede ser nula.");
        }
        if (c.getUnidadId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una unidad disponible.");
        }
        if (c.getInquilinoId() == null) {
            throw new IllegalArgumentException("Debe seleccionar un inquilino para el contrato.");
        }
        if (c.getInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio del contrato es obligatoria.");
        }
        if (c.getFin() == null) {
            throw new IllegalArgumentException("La fecha de finalización del contrato es obligatoria.");
        }
        // Validación de coherencia de fechas (mínimo 1 mes de duración según regla técnica)
        if (!c.getFin().isAfter(c.getInicio())) {
            throw new IllegalArgumentException("La fecha de finalización debe ser posterior a la fecha de inicio.");
        }
        if (c.getFin().isBefore(c.getInicio().plusMonths(1))) {
            throw new IllegalArgumentException("La duración mínima del contrato de arrendamiento debe ser de al menos 1 mes.");
        }
        if (c.getCanon() == null || c.getCanon().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El canon mensual debe ser un valor mayor a cero.");
        }
        if (c.getDiaPago() < 1 || c.getDiaPago() > 31) {
            throw new IllegalArgumentException("El día de pago mensual debe estar entre 1 y 31.");
        }
        if (c.getDepositoPactado() != null && c.getDepositoPactado().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El depósito en garantía no puede ser un valor negativo.");
        }
        if (c.getEstado() == null || c.getEstado().isBlank()) {
            throw new IllegalArgumentException("Debe especificar el estado del contrato.");
        }
    }
}
