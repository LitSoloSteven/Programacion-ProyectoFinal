package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.DatabaseInitializer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private static AuthService authService;

    @BeforeAll
    static void setup() {
        DatabaseInitializer.inicializarBaseDeDatos();
        authService = new AuthService();
    }

    @Test
    void testAutenticacionExitosaAdmin() {
        assertDoesNotThrow(() -> {
            Usuario user = authService.iniciarSesion("admin", "admin123");
            assertNotNull(user);
            assertEquals("admin", user.getNombreUsuario());
            assertTrue(user.esPropietario());
        });
    }

    @Test
    void testAutenticacionExitosaAsistente() {
        assertDoesNotThrow(() -> {
            Usuario user = authService.iniciarSesion("asistente", "asistente123");
            assertNotNull(user);
            assertEquals("asistente", user.getNombreUsuario());
            assertFalse(user.esPropietario());
        });
    }

    @Test
    void testAutenticacionCredencialesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> {
            authService.iniciarSesion("admin", "clave_incorrecta");
        });
    }

    @Test
    void testAutenticacionCamposVacios() {
        assertThrows(IllegalArgumentException.class, () -> {
            authService.iniciarSesion("", "123");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            authService.iniciarSesion("admin", "");
        });
    }
}
