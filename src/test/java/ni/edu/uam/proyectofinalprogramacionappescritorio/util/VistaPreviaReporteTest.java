package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto.DocumentoReporteResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.VistaPreviaReporteController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del cliente JavaFX para exportación atómica local, zoom/H5 e iconos sin dependencias de servidor. */
class VistaPreviaReporteTest {

    @Test
    void exportacionAtomicaEscribeArchivoYSinTemporalesResiduales(@TempDir Path dir) throws Exception {
        Path destino = dir.resolve("reporte-unidades.pdf");

        VistaPreviaReporte.exportarAtomico(destino,
                out -> out.write("%PDF-1.4\n%Prueba Latch\n".getBytes(StandardCharsets.US_ASCII)));

        assertTrue(Files.isRegularFile(destino));
        byte[] contenido = Files.readAllBytes(destino);
        assertEquals("%PDF-", new String(contenido, 0, 5, StandardCharsets.US_ASCII));

        try (Stream<Path> archivos = Files.list(dir)) {
            long temporales = archivos
                    .filter(p -> p.getFileName().toString().endsWith(".tmp"))
                    .count();
            assertEquals(0L, temporales, "No deben quedar archivos temporales en el directorio destino");
        }
    }

    @Test
    void exportacionAtomicaConservaArchivoPrevioAnteFalloSimulado(@TempDir Path dir) throws IOException {
        Path destino = dir.resolve("existente.pdf");
        byte[] previo = "%PDF-previo-intacto".getBytes(StandardCharsets.US_ASCII);
        Files.write(destino, previo);

        assertThrows(IOException.class, () -> VistaPreviaReporte.exportarAtomico(destino, out -> {
            out.write("%PDF-parcial".getBytes(StandardCharsets.US_ASCII));
            throw new IOException("Fallo simulado de escritura");
        }));

        assertArrayEquals(previo, Files.readAllBytes(destino));
        try (Stream<Path> archivos = Files.list(dir)) {
            long temporales = archivos
                    .filter(p -> p.getFileName().toString().endsWith(".tmp"))
                    .count();
            assertEquals(0L, temporales, "El archivo temporal debe eliminarse tras fallar");
        }
    }

    @Test
    void resuelveCarreraCuandoCancelacionLlegaDespuesDePublicar(@TempDir Path dir) throws IOException {
        Path destino = dir.resolve("carrera.pdf");
        AtomicBoolean cancelar = new AtomicBoolean(false);

        var res = VistaPreviaReporte.exportarAtomicoConCancelacion(destino, cancelar::get, out -> {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            cancelar.set(false);
        });
        assertFalse(res.publicadoAntesDeCancelar());
        assertTrue(Files.isRegularFile(res.rutaFinal()));
    }

    @Test
    void gestorH5ExigeInstanciaUnicaYLiberaAlCerrar() throws InterruptedException {
        try (var ejecucion = GestorH5Reportes.iniciar("Prueba H5")) {
            assertTrue(GestorH5Reportes.hayActividad());
            assertThrows(IllegalStateException.class, () -> GestorH5Reportes.iniciar("Segunda instancia"));
            assertTrue(GestorH5Reportes.solicitarCancelacionActiva());
            assertTrue(ejecucion.estaCancelada());
        }
        assertFalse(GestorH5Reportes.hayActividad());
        assertTrue(GestorH5Reportes.esperarTerminacion(Duration.ofSeconds(1)));
    }

    @Test
    void acotarZoomMantieneLimitesContrato() {
        assertEquals(ConstantesLatch.ZOOM_MIN, ConstantesLatch.acotarZoom(0.10f));
        assertEquals(ConstantesLatch.ZOOM_MAX, ConstantesLatch.acotarZoom(5.00f));
        assertEquals(ConstantesLatch.ZOOM_INICIAL, ConstantesLatch.acotarZoom(Float.NaN));
    }

    @Test
    void catalogoIconosIncluyeReporteYDescargarParaTodosLosValores() {
        for (Iconos.Nombre nombre : Iconos.Nombre.values()) {
            Node icono = Iconos.crear(nombre, 16);
            assertNotNull(icono, "Icono nulo para " + nombre);
        }
    }

    @Test
    void vistaPreviaOcultaXlsxCuandoNoSeAdmiteYManejaDocumentoSinPaginas() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException existente) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS));

        enFx(() -> {
            Platform.setImplicitExit(false);
            FXMLLoader loader = new FXMLLoader(
                    LatchApplication.class.getResource("vista-previa-reporte.fxml"));
            Parent root = loader.load();
            VistaPreviaReporteController controller = loader.getController();

            Button btnXlsx = (Button) root.lookup("#btnExportarXlsx");
            Button btnPdf = (Button) root.lookup("#btnExportarPdf");
            Button btnAnterior = (Button) root.lookup("#btnAnterior");
            Button btnSiguiente = (Button) root.lookup("#btnSiguiente");
            Label lblEstado = (Label) root.lookup("#lblEstado");

            controller.inicializarDocumento(new DocumentoReporteResponse(
                    "doc-recibo-vacio", "Recibo #101", "Recibo-101.pdf", null, false, 0, 595));
            assertFalse(btnXlsx.isVisible());
            assertFalse(btnXlsx.isManaged());
            assertTrue(btnPdf.isDisabled());
            assertTrue(btnAnterior.isDisabled());
            assertTrue(btnSiguiente.isDisabled());
            assertTrue(lblEstado.isVisible());
            assertTrue(lblEstado.getText().contains("no contiene páginas"));

            controller.inicializarDocumento(new DocumentoReporteResponse(
                    "doc-tabular-vacio", "Estado de cuenta", "Estado-Cuenta.pdf", "Estado-Cuenta.xlsx", true, 0, 595));
            assertTrue(btnXlsx.isVisible());
            assertTrue(btnXlsx.isManaged());
            assertTrue(btnXlsx.isDisabled());
            return null;
        });
    }

    private static <T> T enFx(Callable<T> tarea) throws Exception {
        FutureTask<T> futuro = new FutureTask<>(tarea);
        Platform.runLater(futuro);
        return futuro.get(20, TimeUnit.SECONDS);
    }
}
