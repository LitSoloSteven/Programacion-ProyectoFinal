package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.Node;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.base.JRBasePrintPage;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VistaPreviaReporteTest {

    @Test
    void renderizaPaginaRealConFondoBlancoYDimensionesPositivas() throws Exception {
        JasperPrint jasperPrint = generarJasperPrintUnidades();

        assertEquals(1, RenderizadorVistaPreviaReporte.totalPaginas(jasperPrint));
        assertEquals(0.50f, RenderizadorVistaPreviaReporte.acotarZoom(0.10f));
        assertEquals(2.00f, RenderizadorVistaPreviaReporte.acotarZoom(5.00f));
        assertEquals(1.00f, RenderizadorVistaPreviaReporte.acotarZoom(Float.NaN));

        WritableImage imagen = RenderizadorVistaPreviaReporte.renderizarPagina(jasperPrint, 0, 1.00f);
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        assertTrue(ancho > 100 && alto > 100, "Dimensiones inválidas: " + ancho + "x" + alto);

        PixelReader reader = imagen.getPixelReader();
        assertEquals(0xFFFFFFFF, reader.getArgb(0, 0), "La esquina debe tener fondo blanco opaco");

        boolean hayContenidoNoBlanco = false;
        for (int y = 0; y < alto && !hayContenidoNoBlanco; y += 4) {
            for (int x = 0; x < ancho; x += 4) {
                if (reader.getArgb(x, y) != 0xFFFFFFFF) {
                    hayContenidoNoBlanco = true;
                    break;
                }
            }
        }
        assertTrue(hayContenidoNoBlanco, "La página renderizada debe contener trazos visibles del reporte");
    }

    @Test
    void manejaDocumentoSinPaginasYIndiceFueraDeRango() {
        JasperPrint vacio = new JasperPrint();
        assertEquals(0, RenderizadorVistaPreviaReporte.totalPaginas(vacio));
        assertThrows(IllegalArgumentException.class,
                () -> RenderizadorVistaPreviaReporte.renderizarPagina(vacio, 0, 1.00f));

        JasperPrint unaPagina = new JasperPrint();
        JRPrintPage pagina = new JRBasePrintPage();
        unaPagina.addPage(pagina);
        unaPagina.setPageWidth(595);
        unaPagina.setPageHeight(842);

        assertThrows(IndexOutOfBoundsException.class,
                () -> RenderizadorVistaPreviaReporte.renderizarPagina(unaPagina, -1, 1.00f));
        assertThrows(IndexOutOfBoundsException.class,
                () -> RenderizadorVistaPreviaReporte.renderizarPagina(unaPagina, 1, 1.00f));
    }

    @Test
    void exportacionAtomicaEscribePdfValidoYSinTemporalesResiduales(@TempDir Path dir) throws Exception {
        JasperPrint jasperPrint = generarJasperPrintUnidades();
        Path destino = dir.resolve("reporte-unidades.pdf");

        VistaPreviaReporte.exportarPdfAtomico(jasperPrint, destino);

        assertTrue(Files.isRegularFile(destino));
        byte[] contenido = Files.readAllBytes(destino);
        assertTrue(contenido.length > 5);
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
    void catalogoIconosIncluyeReporteYDescargarParaTodosLosValores() {
        for (Iconos.Nombre nombre : Iconos.Nombre.values()) {
            Node icono = Iconos.crear(nombre, 16);
            assertNotNull(icono, "Icono nulo para " + nombre);
        }
    }

    private static JasperPrint generarJasperPrintUnidades() throws Exception {
        try (InputStream plantilla = VistaPreviaReporteTest.class.getResourceAsStream("/reportes/unidades.jrxml")) {
            assertNotNull(plantilla);
            JasperReport report = JasperCompileManager.compileReport(plantilla);
            Map<String, Object> params = new HashMap<>();
            params.put("TITULO_SISTEMA", "LATCH — Catálogo de Unidades");
            params.put("GENERADO_POR", "Prueba");
            params.put("TOTAL_UNIDADES", 1);
            params.put("UNIDADES_DISPONIBLES", 1);
            params.put("UNIDADES_OCUPADAS", 0);
            params.put("TOTAL_CANON_USD", "450.00");
            params.put("TOTAL_CANON_NIO", "0.00");

            var fila = new ReporteService.FilaUnidadReporte(
                    "Torre Norte", "U-01", "Apartamento", "Disponible",
                    new BigDecimal("450.00"), "USD", "Unidad de prueba");
            return JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(List.of(fila)));
        }
    }
}
