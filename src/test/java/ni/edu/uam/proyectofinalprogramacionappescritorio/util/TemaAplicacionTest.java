package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemaAplicacionTest {
    private static final class AlmacenMemoria implements AlmacenPreferencias {
        final Map<String, String> datos = new ConcurrentHashMap<>();
        boolean fallarLectura;
        boolean fallarEscritura;

        @Override
        public Optional<String> leer(String clave) {
            if (fallarLectura) throw new IllegalStateException("Lectura denegada simulada");
            return Optional.ofNullable(datos.get(clave));
        }

        @Override
        public void guardar(String clave, String valor) {
            if (fallarEscritura) throw new IllegalStateException("Escritura denegada simulada");
            datos.put(clave, valor);
        }
    }

    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException existente) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS));
        enFx(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    @AfterEach
    void restaurar() {
        TemaAplicacion.registrarObservadorAviso(null);
        TemaAplicacion.restaurarAlmacenPorDefecto();
    }

    @Test
    void usaClaroEnSeisIniciosSinPreferenciaValidaOFalloDeLectura() {
        AlmacenMemoria almacen = new AlmacenMemoria();

        // 1. Pre-acceso vacío
        TemaAplicacion.configurarAlmacen(almacen);
        assertEquals(TemaAplicacion.Tema.CLARO, TemaAplicacion.getTema());

        // 2. Pre-acceso corrupto
        almacen.datos.put(TemaAplicacion.CLAVE_ULTIMO_TEMA, "NEON_INVALIDO");
        TemaAplicacion.cargarPreferenciaPreAcceso();
        assertEquals(TemaAplicacion.Tema.CLARO, TemaAplicacion.getTema());

        // 3. Usuario 1 sin preferencia guardada
        TemaAplicacion.cargarPreferenciasUsuario(101L);
        assertEquals(TemaAplicacion.Tema.CLARO, TemaAplicacion.getTema());

        // 4. Usuario 2 con preferencia corrupta
        String idOpaco2 = AlmacenPreferencias.idOpacoUsuario(202L);
        almacen.datos.put("users/" + idOpaco2 + "/theme", "SEPIA");
        TemaAplicacion.cargarPreferenciasUsuario(202L);
        assertEquals(TemaAplicacion.Tema.CLARO, TemaAplicacion.getTema());

        // 5 y 6. Fallo de lectura en pre-acceso y en usuario autenticado
        almacen.fallarLectura = true;
        TemaAplicacion.cargarPreferenciaPreAcceso();
        assertEquals(TemaAplicacion.Tema.CLARO, TemaAplicacion.getTema());
        TemaAplicacion.cargarPreferenciasUsuario(101L);
        assertEquals(TemaAplicacion.Tema.CLARO, TemaAplicacion.getTema());
    }

    @Test
    void restauraPreferenciaPorIdOpacoSinExponerNombreNiCedulaEnDiezReiniciosPorCuenta() {
        AlmacenMemoria almacen = new AlmacenMemoria();
        TemaAplicacion.configurarAlmacen(almacen);

        Long usuarioA = 10L;
        Long usuarioB = 20L;
        for (int i = 0; i < 10; i++) {
            TemaAplicacion.Tema esperadoA = (i % 2 == 0) ? TemaAplicacion.Tema.OSCURO : TemaAplicacion.Tema.CLARO;
            TemaAplicacion.Tema esperadoB = (i % 2 == 0) ? TemaAplicacion.Tema.CLARO : TemaAplicacion.Tema.OSCURO;

            TemaAplicacion.cargarPreferenciasUsuario(usuarioA);
            TemaAplicacion.establecerTema(esperadoA);
            TemaAplicacion.establecerSinEfectos(i % 2 == 0);

            TemaAplicacion.cargarPreferenciasUsuario(usuarioB);
            TemaAplicacion.establecerTema(esperadoB);
            TemaAplicacion.establecerSinEfectos(i % 2 != 0);

            // Reinicio simulado pre-acceso conserva la última elección local (usuario B)
            TemaAplicacion.cargarPreferenciaPreAcceso();
            assertEquals(esperadoB, TemaAplicacion.getTema());

            // Al autenticar A y B recuperan sus preferencias individuales
            TemaAplicacion.cargarPreferenciasUsuario(usuarioA);
            assertEquals(esperadoA, TemaAplicacion.getTema());
            assertEquals(i % 2 == 0, TemaAplicacion.isSinEfectos());

            TemaAplicacion.cargarPreferenciasUsuario(usuarioB);
            assertEquals(esperadoB, TemaAplicacion.getTema());
            assertEquals(i % 2 != 0, TemaAplicacion.isSinEfectos());
        }

        for (String clave : almacen.datos.keySet()) {
            assertFalse(clave.contains("10") || clave.contains("20"),
                    "Las claves de preferencias deben usar UUID opaco, no IDs crudos ni nombres: " + clave);
        }
    }

    @Test
    void falloDeGuardadoConservaCambioEnSesionYEmiteAvisoNoBloqueante() throws Exception {
        AlmacenMemoria almacen = new AlmacenMemoria();
        TemaAplicacion.configurarAlmacen(almacen);
        TemaAplicacion.cargarPreferenciasUsuario(55L);

        AtomicInteger avisos = new AtomicInteger(0);
        TemaAplicacion.registrarObservadorAviso(msg -> avisos.incrementAndGet());

        almacen.fallarEscritura = true;
        TemaAplicacion.establecerTema(TemaAplicacion.Tema.OSCURO);
        TemaAplicacion.establecerSinEfectos(true);

        // Esperar pulso FX por si el observador se despachó vía Platform.runLater
        enFx(() -> null);

        assertEquals(TemaAplicacion.Tema.OSCURO, TemaAplicacion.getTema());
        assertTrue(TemaAplicacion.isSinEfectos());
        assertEquals(TemaAplicacion.MENSAJE_FALLO_GUARDADO,
                TemaAplicacion.ultimoAvisoGuardado().orElseThrow());
        assertEquals(1, avisos.get());
    }

    @Test
    void treintaCambiosDeTemaConservanVeinteCamposFocoHilosYSinEfectosEnMenosDe500Ms() throws Exception {
        AlmacenMemoria almacen = new AlmacenMemoria();
        TemaAplicacion.configurarAlmacen(almacen);

        enFx(() -> {
            VBox formulario = new VBox(4);
            List<TextField> campos = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                TextField tf = new TextField("valor-" + i);
                campos.add(tf);
                formulario.getChildren().add(tf);
            }
            Region aurora = new Region();
            aurora.getStyleClass().add("aurora-layer");
            StackPane root = new StackPane(aurora, formulario);
            Scene scene = new Scene(root, 800, 600);
            Stage stage = new Stage();
            stage.setScene(scene);
            TemaAplicacion.aplicar(scene);
            stage.show();
            try {
                campos.get(7).requestFocus();
                assertSame(campos.get(7), scene.getFocusOwner());

                int hilosAntes = Thread.activeCount();
                for (int i = 0; i < 30; i++) {
                    long inicio = System.nanoTime();
                    TemaAplicacion.establecerTema(i % 2 == 0 ? TemaAplicacion.Tema.OSCURO : TemaAplicacion.Tema.CLARO);
                    long duracionMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                    assertTrue(duracionMs <= 500, "Cambio de tema excedió 500 ms: " + duracionMs);
                }
                int hilosDespues = Thread.activeCount();
                assertTrue(hilosDespues <= hilosAntes,
                        "El cambio de tema no debe crear hilos adicionales (" + hilosAntes + " -> " + hilosDespues + ")");

                for (int i = 0; i < 20; i++) {
                    assertEquals("valor-" + i, campos.get(i).getText());
                }
                assertSame(campos.get(7), scene.getFocusOwner(), "El foco debe preservarse tras cambiar tema");

                TemaAplicacion.establecerSinEfectos(true);
                assertTrue(root.getStyleClass().contains("no-effects"));
                assertTrue(root.getStyleClass().contains("reduced-motion"));
                assertFalse(aurora.isVisible(), "La capa Aurora debe ocultarse en modalidad sin efectos");
                assertSame(campos.get(7), scene.getFocusOwner());

                TemaAplicacion.establecerSinEfectos(false);
                assertTrue(aurora.isVisible());
            } finally {
                stage.hide();
            }
            return null;
        });
    }

    private static <T> T enFx(Callable<T> tarea) throws Exception {
        FutureTask<T> futuro = new FutureTask<>(tarea);
        Platform.runLater(futuro);
        return futuro.get(15, TimeUnit.SECONDS);
    }
}
