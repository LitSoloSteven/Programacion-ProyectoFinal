package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InsigniaEstadoTest {
    private record DatosChip(String textoAccesible, String textoVisible, boolean claseChip, int nodos) { }

    @org.junit.jupiter.api.BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException yaIniciado) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS), "JavaFX debe iniciar");
    }

    @Test
    void cubreLosEstadosDelDominioConUnaEtiquetaVisible() {
        List<String> estados = List.of(
                "Pagado", "Parcial", "Vencido", "Pendiente", "Anulado", "Cancelado",
                "Registrado", "Vigente", "Borrador", "Por vencer", "Terminado",
                "Disponible", "En mantenimiento", "Inactiva", "Activo", "Inactivo",
                "Activa", "Autorizada", "Pendiente de autorización");
        for (String estado : estados) {
            DatosChip chip = enHiloFx(() -> {
                HBox nodo = InsigniaEstado.crear(estado);
                return new DatosChip(nodo.getAccessibleText(),
                        ((Label) nodo.getChildren().get(1)).getText(),
                        nodo.getStyleClass().contains("status-chip"), nodo.getChildren().size());
            });
            assertFalse(chip.textoAccesible().isBlank(), estado);
            assertTrue(chip.claseChip(), estado);
            assertEquals(chip.textoAccesible(), chip.textoVisible(), estado);
            assertEquals(2, chip.nodos(), estado);
        }
        assertEquals("Pendiente de autorización",
                InsigniaEstado.textoVisible("Pendiente de autorización"));
    }

    @Test
    void estadoNoMapeadoConservaSuTextoCrudo() {
        assertEquals("Estado nuevo", InsigniaEstado.textoVisible("Estado nuevo"));
    }

    private static <T> T enHiloFx(java.util.concurrent.Callable<T> tarea) {
        FutureTask<T> futura = new FutureTask<>(tarea);
        Platform.runLater(futura);
        try {
            return futura.get(5, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new AssertionError("No se pudo verificar el chip en JavaFX", error);
        }
    }
}
