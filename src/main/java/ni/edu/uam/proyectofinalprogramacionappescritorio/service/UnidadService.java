package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.UnidadDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Unidad;
import java.util.List;

public class UnidadService {
    private final UnidadDao unidadDao = new UnidadDao();

    public void guardar(Unidad unidad) {
        if (unidad.getPropiedad() == null) {
            throw new IllegalArgumentException("La unidad debe pertenecer a una propiedad.");
        }
        unidadDao.guardar(unidad);
    }
    public Unidad buscarPorId(Long id) { return unidadDao.buscarPorId(id); }
    public List<Unidad> obtenerTodas() { return unidadDao.obtenerTodas(); }
    public List<Unidad> obtenerPorPropiedad(Long propiedadId) { return unidadDao.obtenerPorPropiedad(propiedadId); }
    public void actualizar(Unidad unidad) { unidadDao.actualizar(unidad); }
    public void eliminar(Long id) { unidadDao.eliminar(id); }
}