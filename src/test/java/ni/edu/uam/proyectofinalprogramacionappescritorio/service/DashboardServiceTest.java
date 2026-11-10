package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardServiceTest {

    private static DashboardService dashboardService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        dashboardService = new DashboardService();
    }

    @Test
    void testObtenerMetricasReales() {
        assertDoesNotThrow(() -> {
            Map<String, Object> metricas = dashboardService.obtenerMetricas();
            assertNotNull(metricas);
            assertTrue(metricas.containsKey("totalPropiedades"));
            assertTrue(metricas.containsKey("totalUnidades"));
            assertTrue(metricas.containsKey("totalInquilinos"));
            assertTrue(metricas.containsKey("totalContratosVigentes"));

            int propiedades = (int) metricas.get("totalPropiedades");
            int unidades = (int) metricas.get("totalUnidades");
            int inquilinos = (int) metricas.get("totalInquilinos");

            assertTrue(propiedades >= 0, "Propiedades debe ser >= 0");
            assertTrue(unidades >= 0, "Unidades debe ser >= 0");
            assertTrue(inquilinos >= 0, "Inquilinos debe ser >= 0");
        });
    }
}
