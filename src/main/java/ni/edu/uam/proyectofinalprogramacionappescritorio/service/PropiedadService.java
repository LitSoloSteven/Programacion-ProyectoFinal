package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.PropiedadDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Propiedad;
import java.util.List;

public class PropiedadService {
    private final PropiedadDao propiedadDao = new PropiedadDao();

    public void guardar(Propiedad propiedad) {
        if (propiedad.getPropietario() == null) {
            throw new IllegalArgumentException("La propiedad debe estar asignada a un propietario.");
        }
        propiedadDao.guardar(propiedad);
    }
    public Propiedad buscarPorId(Long id) { return propiedadDao.buscarPorId(id); }
    public List<Propiedad> obtenerTodas() { return propiedadDao.obtenerTodas(); }
    public List<Propiedad> obtenerPorPropietario(Long propietarioId) { return propiedadDao.obtenerPorPropietario(propietarioId); }
    public void actualizar(Propiedad propiedad) { propiedadDao.actualizar(propiedad); }
    public void eliminar(Long id) { propiedadDao.eliminar(id); }
}