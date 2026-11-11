package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.InquilinoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Inquilino;
import java.util.List;

public class InquilinoService {
    private final InquilinoDao inquilinoDao = new InquilinoDao();

    public void guardar(Inquilino inquilino) {
        if (inquilino.getNombre() == null || inquilino.getNombre().isEmpty()) {
            throw new IllegalArgumentException("El nombre del inquilino es obligatorio.");
        }
        inquilinoDao.guardar(inquilino);
    }
    public Inquilino buscarPorId(Long id) { return inquilinoDao.buscarPorId(id); }
    public List<Inquilino> obtenerTodos() { return inquilinoDao.obtenerTodos(); }
    public void actualizar(Inquilino inquilino) { inquilinoDao.actualizar(inquilino); }
    public void eliminar(Long id) { inquilinoDao.eliminar(id); }
}