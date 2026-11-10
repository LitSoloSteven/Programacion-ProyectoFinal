package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DashboardDao;

import java.sql.SQLException;
import java.util.Map;

/**
 * Servicio que provee indicadores y totales reales para el Dashboard de Latch.
 */
public class DashboardService {

    private final DashboardDao dashboardDao;

    public DashboardService() {
        this.dashboardDao = new DashboardDao();
    }

    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    public Map<String, Object> obtenerMetricas() throws SQLException {
        return dashboardDao.obtenerMetricas();
    }
}
