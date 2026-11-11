package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.GarantiaDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Garantia;

public class GarantiaService {
    private final GarantiaDao garantiaDao = new GarantiaDao();

    public void guardar(Garantia garantia) {
        if (garantia.getContrato() == null) {
            throw new IllegalArgumentException("La garantía debe estar asociada a un contrato.");
        }
        garantiaDao.guardar(garantia);
    }
    public Garantia buscarPorId(Long id) { return garantiaDao.buscarPorId(id); }
    public Garantia buscarPorContrato(Long contratoId) { return garantiaDao.buscarPorContrato(contratoId); }
    public void actualizar(Garantia garantia) { garantiaDao.actualizar(garantia); }
    public void eliminar(Long id) { garantiaDao.eliminar(id); }
}