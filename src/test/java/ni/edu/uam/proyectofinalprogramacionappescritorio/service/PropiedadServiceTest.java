package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PropiedadServiceTest {

    private static PropiedadService propiedadService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        propiedadService = new PropiedadService();

        // Configurar usuario de prueba como Propietario
        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin Test", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    @Test
    void testListarPropiedades() {
        assertDoesNotThrow(() -> {
            List<Propiedad> lista = propiedadService.listarTodas();
            assertNotNull(lista);
            assertFalse(lista.isEmpty(), "Debe haber propiedades cargadas");
        });
    }

    @Test
    void testGuardarYEliminarPropiedad() {
        assertDoesNotThrow(() -> {
            Propiedad nueva = new Propiedad();
            nueva.setNombre("Edificio Prueba JUnit");
            nueva.setDireccion("Pista Jean Paul Genie, Managua");
            nueva.setActiva(true);

            propiedadService.guardar(nueva);
            assertNotNull(nueva.getId());

            // Actualizar
            nueva.setNombre("Edificio Prueba JUnit Modificado");
            propiedadService.guardar(nueva);

            // Eliminar
            propiedadService.eliminar(nueva.getId());
        });
    }

    @Test
    void testValidacionPropiedadInvalida() {
        Propiedad invalida = new Propiedad();
        invalida.setNombre("");
        invalida.setDireccion("");

        assertThrows(IllegalArgumentException.class, () -> {
            propiedadService.guardar(invalida);
        });
    }
}
