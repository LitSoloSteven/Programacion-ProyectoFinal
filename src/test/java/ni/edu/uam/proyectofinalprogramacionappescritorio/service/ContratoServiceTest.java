package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ContratoServiceTest {

    private static ContratoService contratoService;
    private static UnidadService unidadService;
    private static InquilinoService inquilinoService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        contratoService = new ContratoService();
        unidadService = new UnidadService();
        inquilinoService = new InquilinoService();

        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin Test", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    @Test
    void testGuardarTerminarYEliminarContrato() {
        assertDoesNotThrow(() -> {
            List<Unidad> unidades = unidadService.listarTodas();
            List<Inquilino> inquilinos = inquilinoService.listarActivos();
            assertFalse(unidades.isEmpty());
            assertFalse(inquilinos.isEmpty());

            Contrato nuevo = new Contrato();
            nuevo.setUnidadId(unidades.get(0).getId());
            nuevo.setInquilinoId(inquilinos.get(0).getId());
            nuevo.setInicio(LocalDate.now());
            nuevo.setFin(LocalDate.now().plusMonths(6));
            nuevo.setCanon(new BigDecimal("500.00"));
            nuevo.setMoneda("USD");
            nuevo.setDiaPago(5);
            nuevo.setDepositoPactado(new BigDecimal("500.00"));
            nuevo.setEstado("Vigente");

            contratoService.guardar(nuevo);
            assertNotNull(nuevo.getId());

            // Terminar
            contratoService.terminarContrato(nuevo.getId());

            // Eliminar
            contratoService.eliminar(nuevo.getId());
        });
    }

    @Test
    void testValidacionFechasIncoherentes() {
        Contrato invalido = new Contrato();
        invalido.setUnidadId(UUID.randomUUID());
        invalido.setInquilinoId(UUID.randomUUID());
        invalido.setInicio(LocalDate.of(2026, 5, 1));
        invalido.setFin(LocalDate.of(2026, 4, 1)); // Fin antes que inicio
        invalido.setCanon(new BigDecimal("300.00"));

        assertThrows(IllegalArgumentException.class, () -> {
            contratoService.guardar(invalido);
        });
    }

    @Test
    void testValidacionDuracionMinimaUnMes() {
        Contrato invalido = new Contrato();
        invalido.setUnidadId(UUID.randomUUID());
        invalido.setInquilinoId(UUID.randomUUID());
        invalido.setInicio(LocalDate.of(2026, 5, 1));
        invalido.setFin(LocalDate.of(2026, 5, 10)); // Solo 10 días
        invalido.setCanon(new BigDecimal("300.00"));

        assertThrows(IllegalArgumentException.class, () -> {
            contratoService.guardar(invalido);
        });
    }
}
