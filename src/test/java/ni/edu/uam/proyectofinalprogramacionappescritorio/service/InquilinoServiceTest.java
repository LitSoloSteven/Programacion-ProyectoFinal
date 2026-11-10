package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class InquilinoServiceTest {

    private static InquilinoService inquilinoService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        inquilinoService = new InquilinoService();

        Usuario admin = new Usuario(UUID.randomUUID(), "Propietario", "admin", "Admin Test", true);
        SesionUsuario.setUsuarioActual(admin);
    }

    @Test
    void testListarInquilinos() {
        assertDoesNotThrow(() -> {
            List<Inquilino> lista = inquilinoService.listarTodos();
            assertNotNull(lista);
            assertFalse(lista.isEmpty(), "Debe haber inquilinos sintéticos registrados");
        });
    }

    @Test
    void testGuardarDarDeBajaYEliminar() {
        assertDoesNotThrow(() -> {
            Inquilino nuevo = new Inquilino();
            nuevo.setNombre("Test Inquilino JUnit");
            nuevo.setIdentificacion("001-010190-0001A");
            nuevo.setTelefono("+505 8888-9999");
            nuevo.setCorreo("inquilino.junit@test.com");
            nuevo.setActivo(true);

            inquilinoService.guardar(nuevo);
            assertNotNull(nuevo.getId());

            // Actualizar
            nuevo.setTelefono("+505 7777-6666");
            inquilinoService.guardar(nuevo);

            // Dar de baja
            inquilinoService.darDeBaja(nuevo.getId());

            // Eliminar
            inquilinoService.eliminar(nuevo.getId());
        });
    }

    @Test
    void testValidacionCorreoInvalido() {
        Inquilino invalido = new Inquilino();
        invalido.setNombre("Persona Prueba");
        invalido.setCorreo("correo-sin-arroba-ni-punto");

        assertThrows(IllegalArgumentException.class, () -> {
            inquilinoService.guardar(invalido);
        });
    }

    @Test
    void testValidacionNombreVacio() {
        Inquilino invalido = new Inquilino();
        invalido.setNombre("   ");

        assertThrows(IllegalArgumentException.class, () -> {
            inquilinoService.guardar(invalido);
        });
    }
}
