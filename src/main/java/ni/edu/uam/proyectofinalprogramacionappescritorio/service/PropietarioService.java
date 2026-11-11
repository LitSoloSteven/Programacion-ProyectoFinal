package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.PropietarioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Propietario;
import java.util.List;

public class PropietarioService {
    private final PropietarioDao propietarioDao = new PropietarioDao();

    public void guardar(Propietario propietario) {
        if (propietario.getNombre() == null || propietario.getNombre().isEmpty()) {
            throw new IllegalArgumentException("El nombre del propietario es obligatorio.");
        }
        propietarioDao.guardar(propietario);
    }
    public Propietario buscarPorId(Long id) { return propietarioDao.buscarPorId(id); }
    public List<Propietario> obtenerTodos() { return propietarioDao.obtenerTodos(); }
    public void actualizar(Propietario propietario) { propietarioDao.actualizar(propietario); }
    public void eliminar(Long id) { propietarioDao.eliminar(id); }
}