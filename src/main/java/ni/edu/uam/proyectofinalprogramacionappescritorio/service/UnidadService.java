package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UnidadDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de unidades inmobiliarias.
 */
public class UnidadService {

    private final UnidadDao unidadDao;

    public UnidadService() {
        this.unidadDao = new UnidadDao();
    }

    public UnidadService(UnidadDao unidadDao) {
        this.unidadDao = unidadDao;
    }

    public List<Unidad> listarTodas() throws SQLException {
        return unidadDao.listarTodas();
    }

    public List<Unidad> listarDisponibles() throws SQLException {
        return unidadDao.listarDisponibles();
    }

    public void guardar(Unidad u) throws SQLException {
        validarUnidad(u);

        if (u.getId() == null) {
            unidadDao.insertar(u);
        } else {
            unidadDao.actualizar(u);
        }
    }

    public void eliminar(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar una unidad válida.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede eliminar unidades físicamente.");
        }
        unidadDao.eliminar(id);
    }

    private void validarUnidad(Unidad u) {
        if (u == null) {
            throw new IllegalArgumentException("La información de la unidad no puede ser nula.");
        }
        if (u.getPropiedadId() == null) {
            throw new IllegalArgumentException("Debe seleccionar la propiedad asociada a la unidad.");
        }
        if (u.getCodigo() == null || u.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("El código de la unidad es obligatorio.");
        }
        if (u.getCodigo().trim().length() < 1 || u.getCodigo().trim().length() > 30) {
            throw new IllegalArgumentException("El código debe tener entre 1 y 30 caracteres.");
        }
        if (u.getCanonReferencia() == null || u.getCanonReferencia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El canon de referencia debe ser un valor numérico mayor a 0.");
        }
        if (u.getTipo() == null || u.getTipo().isBlank()) {
            throw new IllegalArgumentException("Debe especificar el tipo de unidad.");
        }
        if (u.getEstado() == null || u.getEstado().isBlank()) {
            throw new IllegalArgumentException("Debe especificar el estado de la unidad.");
        }
    }
}
