package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.ContratoDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelos.Contrato;
import java.util.List;

public class ContratoService {

    private final ContratoDao contratoDao = new ContratoDao();

    public void registrarContrato(Contrato contrato) {
        // Regla 1: Validar fechas lógicas
        if (contrato.getFechaInicio() != null && contrato.getFechaFin() != null) {
            if (contrato.getFechaInicio().isAfter(contrato.getFechaFin())) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
            }
        }

        // Regla 2: Ocupación única (Validar que la unidad no tenga otro contrato activo)
        List<Contrato> contratosActivos = contratoDao.obtenerContratosActivos();
        boolean unidadOcupada = contratosActivos.stream()
                .anyMatch(c -> c.getUnidad().getId().equals(contrato.getUnidad().getId()));

        if (unidadOcupada) {
            throw new IllegalStateException("Violación de regla: La unidad seleccionada ya posee un contrato activo.");
        }

        contratoDao.guardar(contrato);
    }

    public Contrato buscarPorId(Long id) {
        return contratoDao.buscarPorId(id);
    }

    public List<Contrato> obtenerContratosActivos() {
        return contratoDao.obtenerContratosActivos();
    }

    public void actualizarContrato(Contrato contrato) {
        contratoDao.actualizar(contrato);
    }
}