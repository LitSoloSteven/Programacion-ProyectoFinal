package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.CargoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Cargo;
import java.math.BigDecimal;
import java.util.List;

public class CargoService {
    private final CargoDao cargoDao = new CargoDao();

    public void guardar(Cargo cargo) {
        if (cargo.getMonto() == null || cargo.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del cargo debe ser mayor a cero.");
        }
        cargoDao.guardar(cargo);
    }
    public Cargo buscarPorId(Long id) { return cargoDao.buscarPorId(id); }
    public List<Cargo> obtenerCargosPendientesPorInquilino(Long inquilinoId) { return cargoDao.obtenerCargosPendientesPorInquilino(inquilinoId); }
    public void actualizar(Cargo cargo) { cargoDao.actualizar(cargo); }
}