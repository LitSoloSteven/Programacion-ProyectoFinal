package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.PropiedadDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para la gestión de propiedades inmobiliarias.
 */
public class PropiedadService {

    private final PropiedadDao propiedadDao;

    public PropiedadService() {
        this.propiedadDao = new PropiedadDao();
    }

    public PropiedadService(PropiedadDao propiedadDao) {
        this.propiedadDao = propiedadDao;
    }

    public List<Propiedad> listarTodas() throws SQLException {
        return propiedadDao.listarTodas();
    }

    public List<Propiedad> listarActivas() throws SQLException {
        return propiedadDao.listarActivas();
    }

    public void guardar(Propiedad propiedad) throws SQLException {
        validarPropiedad(propiedad);

        if (propiedad.getId() == null) {
            propiedadDao.insertar(propiedad);
        } else {
            propiedadDao.actualizar(propiedad);
        }
    }

    public void eliminar(UUID id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar una propiedad válida.");
        }
        if (!SesionUsuario.esPropietario()) {
            throw new SecurityException("Permiso denegado: solo el Propietario puede eliminar propiedades físicamente.");
        }
        propiedadDao.eliminar(id);
    }

    public void cambiarEstado(UUID id, boolean activa) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar una propiedad válida.");
        }
        propiedadDao.cambiarEstado(id, activa);
    }

    private void validarPropiedad(Propiedad p) {
        if (p == null) {
            throw new IllegalArgumentException("La información de la propiedad no puede ser nula.");
        }
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la propiedad es obligatorio.");
        }
        if (p.getNombre().trim().length() < 3 || p.getNombre().trim().length() > 120) {
            throw new IllegalArgumentException("El nombre debe tener entre 3 y 120 caracteres.");
        }
        if (p.getDireccion() == null || p.getDireccion().trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección de la propiedad es obligatoria.");
        }
        if (p.getDireccion().trim().length() < 5 || p.getDireccion().trim().length() > 300) {
            throw new IllegalArgumentException("La dirección debe tener entre 5 y 300 caracteres.");
        }
    }
}
