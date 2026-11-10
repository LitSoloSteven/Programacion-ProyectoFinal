package ni.edu.uam.proyectofinalprogramacionappescritorio;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas funcionales integradas que valida los flujos CRUD completos
 * de las vistas principales contra datos reales en PostgreSQL 18.
 */
public class FlujosCrudVistasTest {

    private static PropiedadService propiedadService;
    private static UnidadService unidadService;
    private static InquilinoService inquilinoService;
    private static ContratoService contratoService;
    private static CargoService cargoService;
    private static PagoService pagoService;
    private static DashboardService dashboardService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        propiedadService = new PropiedadService();
        unidadService = new UnidadService();
        inquilinoService = new InquilinoService();
        contratoService = new ContratoService();
        cargoService = new CargoService();
        pagoService = new PagoService();
        dashboardService = new DashboardService();

        // Autenticar sesión de prueba con rol Propietario
        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin QA", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    @Test
    @DisplayName("Flujo CRUD 1: Propiedad — Crear -> Guardar -> Visualizar -> Editar -> Actualizar -> Eliminar")
    void testFlujoCrudPropiedad() {
        assertDoesNotThrow(() -> {
            // 1. Crear
            Propiedad p = new Propiedad();
            p.setNombre("Residencial Los Sauces QA");
            p.setDireccion("Carretera Sur Km 14, Managua");
            p.setActiva(true);

            propiedadService.guardar(p);
            assertNotNull(p.getId(), "El ID generado en PostgreSQL debe estar presente");

            // 2. Visualizar / Consultar
            List<Propiedad> lista = propiedadService.listarTodas();
            assertTrue(lista.stream().anyMatch(item -> item.getId().equals(p.getId())));

            // 3. Editar y Actualizar
            p.setDireccion("Carretera Sur Km 14.5, Entrada Los Sauces, Managua");
            propiedadService.guardar(p);

            // 4. Eliminar
            propiedadService.eliminar(p.getId());
            List<Propiedad> listaDespues = propiedadService.listarTodas();
            assertFalse(listaDespues.stream().anyMatch(item -> item.getId().equals(p.getId())));
        });
    }

    @Test
    @DisplayName("Flujo CRUD 2: Unidad — Asociar a Propiedad -> Crear -> Guardar -> Visualizar -> Editar")
    void testFlujoCrudUnidad() {
        assertDoesNotThrow(() -> {
            List<Propiedad> props = propiedadService.listarActivas();
            assertFalse(props.isEmpty(), "Debe existir al menos una propiedad para asociar la unidad");
            Propiedad prop = props.get(0);

            // 1. Crear
            Unidad u = new Unidad();
            u.setPropiedadId(prop.getId());
            u.setCodigo("U-QA-101");
            u.setTipo("Apartamento");
            u.setEstado("Disponible");
            u.setCanonReferencia(new BigDecimal("420.00"));
            u.setMoneda("USD");

            unidadService.guardar(u);
            assertNotNull(u.getId());

            // 2. Visualizar
            List<Unidad> unidades = unidadService.listarTodas();
            assertTrue(unidades.stream().anyMatch(item -> item.getId().equals(u.getId())));

            // 3. Editar
            u.setCanonReferencia(new BigDecimal("450.00"));
            unidadService.guardar(u);

            // 4. Limpieza
            unidadService.eliminar(u.getId());
        });
    }

    @Test
    @DisplayName("Flujo CRUD 3: Inquilino — Crear -> Validar -> Guardar -> Consultar -> Editar -> Baja")
    void testFlujoCrudInquilino() {
        assertDoesNotThrow(() -> {
            Inquilino inq = new Inquilino();
            inq.setNombre("Mariana Fernanda Lopez");
            inq.setIdentificacion("001-200590-0003W");
            inq.setTelefono("+505 8765-4321");
            inq.setCorreo("mariana.lopez@correo.ni");
            inq.setContactoEmergencia("Pedro Lopez - +505 8999-1122");
            inq.setActivo(true);

            inquilinoService.guardar(inq);
            assertNotNull(inq.getId());

            // Consultar
            List<Inquilino> activos = inquilinoService.listarActivos();
            assertTrue(activos.stream().anyMatch(i -> i.getId().equals(inq.getId())));

            // Editar
            inq.setTelefono("+505 8888-2233");
            inquilinoService.guardar(inq);

            // Baja lógica
            inquilinoService.darDeBaja(inq.getId());

            // Eliminar físico final
            inquilinoService.eliminar(inq.getId());
        });
    }

    @Test
    @DisplayName("Flujo CRUD 4: Contrato, Cargo y Pago — Cadena contractual y financiera completa")
    void testFlujoContratoCargoYPagoCompleto() {
        assertDoesNotThrow(() -> {
            List<Unidad> unidades = unidadService.listarTodas();
            List<Inquilino> inquilinos = inquilinoService.listarActivos();

            // 1. Contrato
            Contrato contrato = new Contrato();
            contrato.setUnidadId(unidades.get(0).getId());
            contrato.setInquilinoId(inquilinos.get(0).getId());
            contrato.setInicio(LocalDate.now());
            contrato.setFin(LocalDate.now().plusMonths(12));
            contrato.setCanon(new BigDecimal("500.00"));
            contrato.setMoneda("USD");
            contrato.setDiaPago(5);
            contrato.setDepositoPactado(new BigDecimal("500.00"));
            contrato.setEstado("Vigente");

            contratoService.guardar(contrato);
            assertNotNull(contrato.getId());

            // 2. Cargo
            Cargo cargo = new Cargo();
            cargo.setContratoId(contrato.getId());
            cargo.setTipo("Renta");
            cargo.setPeriodo(LocalDate.now().withDayOfMonth(1));
            cargo.setFechaEmision(LocalDate.now());
            cargo.setVenceEl(LocalDate.now().plusDays(5));
            cargo.setImporte(new BigDecimal("500.00"));
            cargo.setConcepto("Renta mensual periodo actual");
            cargo.setEstado("Pendiente");

            cargoService.guardar(cargo);
            assertNotNull(cargo.getId());

            // 3. Pago
            Pago pago = new Pago();
            pago.setContratoId(contrato.getId());
            pago.setFechaPago(LocalDate.now());
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("500.00"));
            pago.setMontoContrato(new BigDecimal("500.00"));
            pago.setMetodo("Transferencia");
            pago.setReferencia("BAC-TR-88123");
            pago.setNotas("Cancelación de renta mes corriente");
            pago.setEstado("Registrado");

            pagoService.registrarPago(pago);
            assertNotNull(pago.getId());

            // 4. Verificación de métricas en Dashboard
            Map<String, Object> metricas = dashboardService.obtenerMetricas();
            assertTrue((int) metricas.get("totalContratosVigentes") > 0);

            // 5. Limpieza segura en cascada inversa
            pagoService.eliminar(pago.getId());
            cargoService.eliminar(cargo.getId());
            contratoService.eliminar(contrato.getId());
        });
    }

    @Test
    @DisplayName("Flujo Validaciones de Error: Evitar persistencia de datos corruptos")
    void testValidacionesNegativasEvitanPersistencia() {
        // Intento de guardar contrato con fecha final anterior a inicio
        Contrato cInvalido = new Contrato();
        cInvalido.setUnidadId(UUID.randomUUID());
        cInvalido.setInquilinoId(UUID.randomUUID());
        cInvalido.setInicio(LocalDate.of(2026, 12, 1));
        cInvalido.setFin(LocalDate.of(2026, 6, 1)); // Incoherente
        cInvalido.setCanon(new BigDecimal("300.00"));

        assertThrows(IllegalArgumentException.class, () -> contratoService.guardar(cInvalido));

        // Intento de guardar pago con monto cero o negativo
        Pago pInvalido = new Pago();
        pInvalido.setContratoId(UUID.randomUUID());
        pInvalido.setMontoOriginal(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> pagoService.registrarPago(pInvalido));
    }
}
