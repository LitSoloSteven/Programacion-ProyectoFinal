package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.InquilinoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de inquilinos y validaciones de datos de contacto.
 */
public class InquilinoService {

    private final InquilinoDao inquilinoDao;

    public InquilinoService() {
        this.inquilinoDao = new InquilinoDao();
    }

    public InquilinoService(InquilinoDao inquilinoDao) {
        this.inquilinoDao = inquilinoDao;
    }

    public List<Inquilino> listarTodos() throws SQLException {
        return inquilinoDao.listarTodos();
    }

    public List<Inquilino> listarActivos() throws SQLException {
        return inquilinoDao.listarActivos();
    }

    public void guardar(Inquilino inq) throws SQLException {
        validarInquilino(inq);

        if (inq.getId() == null) {
            inquilinoDao.insertar(inq);
        } else {
            inquilinoDao.actualizar(inq);
        }
    }

    public void darDeBaja(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un inquilino válido.");
        }
        inquilinoDao.darDeBaja(id);
    }

    public void eliminar(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un inquilino válido.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede eliminar inquilinos físicamente.");
        }
        inquilinoDao.eliminar(id);
    }

    private void validarInquilino(Inquilino inq) {
        if (inq == null) {
            throw new IllegalArgumentException("La información del inquilino no puede ser nula.");
        }
        if (inq.getNombre() == null || inq.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del inquilino es obligatorio.");
        }
        if (inq.getNombre().trim().length() < 3 || inq.getNombre().trim().length() > 160) {
            throw new IllegalArgumentException("El nombre debe contener entre 3 y 160 caracteres.");
        }
        if (inq.getIdentificacion() != null && inq.getIdentificacion().trim().length() > 40) {
            throw new IllegalArgumentException("La identificación no puede exceder 40 caracteres.");
        }
        if (inq.getTelefono() != null && inq.getTelefono().trim().length() > 30) {
            throw new IllegalArgumentException("El teléfono no puede exceder 30 caracteres.");
        }
        if (inq.getCorreo() != null && !inq.getCorreo().trim().isEmpty()) {
            if (!inq.esCorreoValido()) {
                throw new IllegalArgumentException("El formato del correo electrónico es inválido.");
            }
            if (inq.getCorreo().trim().length() > 254) {
                throw new IllegalArgumentException("El correo electrónico no puede exceder 254 caracteres.");
            }
        }
        if (inq.getContactoEmergencia() != null && inq.getContactoEmergencia().trim().length() > 200) {
            throw new IllegalArgumentException("El contacto de emergencia no puede exceder 200 caracteres.");
        }
    }
}
