package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UnidadServiceTest {

    private static UnidadService unidadService;
    private static PropiedadService propiedadService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        unidadService = new UnidadService();
        propiedadService = new PropiedadService();

        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin Test", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    @Test
    void testListarUnidades() {
        assertDoesNotThrow(() -> {
            List<Unidad> lista = unidadService.listarTodas();
            assertNotNull(lista);
            assertFalse(lista.isEmpty(), "Debe haber unidades iniciales registradas");
        });
    }

    @Test
    void testGuardarYEliminarUnidad() {
        assertDoesNotThrow(() -> {
            List<Propiedad> props = propiedadService.listarActivas();
            assertFalse(props.isEmpty());
            Propiedad prop = props.get(0);

            Unidad nueva = new Unidad();
            nueva.setPropiedadId(prop.getId());
            nueva.setCodigo("TEST-99");
            nueva.setTipo("Apartamento");
            nueva.setEstado("Disponible");
            nueva.setCanonReferencia(new BigDecimal("350.00"));
            nueva.setMoneda("USD");

            unidadService.guardar(nueva);
            assertNotNull(nueva.getId());

            // Actualizar
            nueva.setCanonReferencia(new BigDecimal("400.00"));
            unidadService.guardar(nueva);

            // Eliminar
            unidadService.eliminar(nueva.getId());
        });
    }

    @Test
    void testValidacionUnidadInvalida() {
        Unidad invalida = new Unidad();
        invalida.setPropiedadId(null);
        invalida.setCodigo("");
        invalida.setCanonReferencia(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> {
            unidadService.guardar(invalida);
        });
    }
}
