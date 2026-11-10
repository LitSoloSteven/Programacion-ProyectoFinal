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

public class CargoYPagoServiceTest {

    private static CargoService cargoService;
    private static PagoService pagoService;
    private static ContratoService contratoService;
    private static UnidadService unidadService;
    private static InquilinoService inquilinoService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        cargoService = new CargoService();
        pagoService = new PagoService();
        contratoService = new ContratoService();
        unidadService = new UnidadService();
        inquilinoService = new InquilinoService();

        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin Test", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    private Contrato crearContratoPrueba() throws Exception {
        List<Unidad> unidades = unidadService.listarTodas();
        List<Inquilino> inquilinos = inquilinoService.listarActivos();

        Contrato c = new Contrato();
        c.setUnidadId(unidades.get(0).getId());
        c.setInquilinoId(inquilinos.get(0).getId());
        c.setInicio(LocalDate.now());
        c.setFin(LocalDate.now().plusMonths(3));
        c.setCanon(new BigDecimal("450.00"));
        c.setMoneda("USD");
        c.setDiaPago(5);
        c.setDepositoPactado(BigDecimal.ZERO);
        c.setEstado("Vigente");

        contratoService.guardar(c);
        return c;
    }

    @Test
    void testGuardarYEliminarCargo() {
        assertDoesNotThrow(() -> {
            Contrato contrato = crearContratoPrueba();

            Cargo nuevo = new Cargo();
            nuevo.setContratoId(contrato.getId());
            nuevo.setTipo("Renta");
            nuevo.setPeriodo(LocalDate.now().withDayOfMonth(1));
            nuevo.setFechaEmision(LocalDate.now());
            nuevo.setVenceEl(LocalDate.now().plusDays(5));
            nuevo.setImporte(new BigDecimal("450.00"));
            nuevo.setConcepto("Cobro de Renta JUnit");
            nuevo.setEstado("Pendiente");

            cargoService.guardar(nuevo);
            assertNotNull(nuevo.getId());

            // Eliminar cargo y contrato
            cargoService.eliminar(nuevo.getId());
            contratoService.eliminar(contrato.getId());
        });
    }

    @Test
    void testRegistrarYAnularPago() {
        assertDoesNotThrow(() -> {
            Contrato contrato = crearContratoPrueba();

            Pago pago = new Pago();
            pago.setContratoId(contrato.getId());
            pago.setFechaPago(LocalDate.now());
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("450.00"));
            pago.setMontoContrato(new BigDecimal("450.00"));
            pago.setMetodo("Efectivo");
            pago.setReferencia("REC-TEST-01");
            pago.setNotas("Prueba de pago");
            pago.setEstado("Registrado");

            pagoService.registrarPago(pago);
            assertNotNull(pago.getId());

            // Anular
            pagoService.anular(pago.getId());

            // Limpieza
            pagoService.eliminar(pago.getId());
            contratoService.eliminar(contrato.getId());
        });
    }

    @Test
    void testValidacionImporteInvalido() {
        Cargo invalido = new Cargo();
        invalido.setContratoId(UUID.randomUUID());
        invalido.setImporte(new BigDecimal("-50.00"));

        assertThrows(IllegalArgumentException.class, () -> {
            cargoService.guardar(invalido);
        });
    }
}
