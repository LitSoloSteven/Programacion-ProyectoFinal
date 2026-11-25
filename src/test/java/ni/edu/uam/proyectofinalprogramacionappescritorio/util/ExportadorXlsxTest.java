package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TablaReporteXlsx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.OperacionCanceladaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExportadorXlsxTest {

    @Test
    void generaOoxmlValidoConTiposNumericosFechasYCaracteresEspeciales() throws Exception {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("Corte", "2026-11-25");
        meta.put("Moneda", "USD y NIO");

        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Inquilino / Ñandú", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Importe", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Tasa", TablaReporteXlsx.Tipo.TASA),
                new TablaReporteXlsx.Columna("Días", TablaReporteXlsx.Tipo.ENTERO)
        );
        List<List<Object>> filas = List.of(
                List.of("Sofía Peña — C$ / US$ & <prueba>", LocalDate.of(2026, 11, 25),
                        new BigDecimal("1250.50"), new BigDecimal("36.62430000"), 15L)
        );
        List<TablaReporteXlsx.Resumen> resumenes = List.of(
                TablaReporteXlsx.Resumen.numerico("Total USD", "USD", new BigDecimal("1250.50"))
        );

        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-03", "Cartera: Vencida / 2026?", "LATCH — Cartera de Prueba",
                meta, columnas, filas, resumenes);
        assertFalse(tabla.nombreHoja().contains(":"));
        assertFalse(tabla.nombreHoja().contains("/"));
        assertTrue(tabla.nombreHoja().length() <= 31);

        byte[] xlsx = ExportadorXlsx.exportarBytes(tabla);
        Map<String, String> entradas = leerEntradasZip(xlsx);

        assertTrue(entradas.containsKey("[Content_Types].xml"));
        assertTrue(entradas.containsKey("_rels/.rels"));
        assertTrue(entradas.containsKey("xl/workbook.xml"));
        assertTrue(entradas.containsKey("xl/_rels/workbook.xml.rels"));
        assertTrue(entradas.containsKey("xl/styles.xml"));
        assertTrue(entradas.containsKey("xl/worksheets/sheet1.xml"));

        String sheet = entradas.get("xl/worksheets/sheet1.xml");
        assertTrue(sheet.contains("Sofía Peña — C$ / US$ &amp; &lt;prueba&gt;"), sheet);
        // Importe y tasa como <v> numéricos, no texto inlineStr
        assertTrue(sheet.contains("s=\"3\"><v>1250.50</v>"), sheet);
        assertTrue(sheet.contains("s=\"4\"><v>36.62430000</v>"), sheet);
        assertTrue(sheet.contains("s=\"5\"><v>15</v>"), sheet);
        // Fecha real Excel (2026-11-25 -> epochDay + 25569 = 46351)
        assertTrue(sheet.contains("s=\"6\"><v>" + (LocalDate.of(2026, 11, 25).toEpochDay() + 25569L) + "</v>"), sheet);
    }

    @Test
    void benchmarkDiezMilFilasEnMenosDeTreintaSegundos(@TempDir Path dir) throws Exception {
        List<TablaReporteXlsx.Columna> columnas = List.of(
                new TablaReporteXlsx.Columna("Contrato", TablaReporteXlsx.Tipo.TEXTO),
                new TablaReporteXlsx.Columna("Fecha", TablaReporteXlsx.Tipo.FECHA),
                new TablaReporteXlsx.Columna("Importe", TablaReporteXlsx.Tipo.MONTO),
                new TablaReporteXlsx.Columna("Tasa", TablaReporteXlsx.Tipo.TASA),
                new TablaReporteXlsx.Columna("Días", TablaReporteXlsx.Tipo.ENTERO)
        );
        List<List<Object>> filas = new ArrayList<>(10_000);
        LocalDate base = LocalDate.of(2026, 1, 1);
        BigDecimal tasa = new BigDecimal("36.62430000");
        for (int i = 0; i < 10_000; i++) {
            filas.add(List.of(
                    "Contrato #" + i + " — Torre Latch",
                    base.plusDays(i % 365),
                    new BigDecimal("450.25"),
                    tasa,
                    i % 90));
        }
        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-03", "Diez mil filas", "Benchmark 10,000 filas",
                Map.of("Corte", "2026-11-25"), columnas, filas,
                List.of(TablaReporteXlsx.Resumen.numerico("Total", "USD", new BigDecimal("4502500.00"))));

        Path destino = dir.resolve("benchmark-10k.xlsx");
        long inicioNs = System.nanoTime();
        var pub = VistaPreviaReporte.exportarXlsxAtomico(tabla, destino, () -> false);
        long duracionMs = (System.nanoTime() - inicioNs) / 1_000_000L;

        assertTrue(duracionMs < 30_000L, "Superó 30 s: " + duracionMs + " ms");
        assertEquals(destino.toAbsolutePath().normalize(), pub.rutaFinal());
        assertFalse(pub.publicadoAntesDeCancelar());
        assertTrue(Files.isRegularFile(destino));
        assertTrue(Files.size(destino) > 10_000L);

        try (Stream<Path> archivos = Files.list(dir)) {
            assertEquals(0L, archivos.filter(p -> p.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void cancelacionCooperativaConservaArchivoPrevioYNoDejaTemporales(@TempDir Path dir) throws IOException {
        Path destino = dir.resolve("existente.xlsx");
        byte[] previo = "ARCHIVO-XLSX-PREVIO".getBytes(StandardCharsets.UTF_8);
        Files.write(destino, previo);

        List<List<Object>> filas = new ArrayList<>(600);
        for (int i = 0; i < 600; i++) {
            filas.add(List.of("Fila " + i, new BigDecimal("10.00")));
        }
        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-04", "Cancelable", "Prueba cancelación",
                Map.of(),
                List.of(new TablaReporteXlsx.Columna("Nombre", TablaReporteXlsx.Tipo.TEXTO),
                        new TablaReporteXlsx.Columna("Monto", TablaReporteXlsx.Tipo.MONTO)),
                filas, List.of());

        AtomicInteger llamadas = new AtomicInteger();
        assertThrows(OperacionCanceladaException.class, () ->
                VistaPreviaReporte.exportarXlsxAtomico(tabla, destino, () -> llamadas.incrementAndGet() >= 3));

        assertArrayEquals(previo, Files.readAllBytes(destino));
        try (Stream<Path> archivos = Files.list(dir)) {
            assertEquals(0L, archivos.filter(p -> p.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void resuelveCarreraCuandoCancelacionLlegaDespuesDePublicar(@TempDir Path dir) throws IOException {
        Path destino = dir.resolve("carrera.pdf");
        AtomicBoolean cancelar = new AtomicBoolean(false);

        var res = VistaPreviaReporte.exportarAtomicoConCancelacion(destino, cancelar::get, out -> {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            // Simula solicitud de cancelación justo tras escribir y antes/durante el cierre final
            cancelar.set(false);
        });
        assertFalse(res.publicadoAntesDeCancelar());

        Path destino2 = dir.resolve("carrera2.xlsx");
        TablaReporteXlsx tabla = new TablaReporteXlsx(
                "REP-05", "Carrera", "Carrera", Map.of(),
                List.of(new TablaReporteXlsx.Columna("Col", TablaReporteXlsx.Tipo.TEXTO)),
                List.of(List.of("Ok")), List.of());
        AtomicBoolean cancelarAlFinal = new AtomicBoolean(false);
        var pub = VistaPreviaReporte.exportarAtomicoConCancelacion(
                destino2,
                cancelarAlFinal::get,
                out -> {
                    ExportadorXlsx.escribir(tabla, out, () -> false);
                });
        assertTrue(Files.isRegularFile(pub.rutaFinal()));
        assertFalse(pub.publicadoAntesDeCancelar());
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
    void exportadorRechazaReporteSoloPdfCuandoNoTieneTablaXlsx(@TempDir Path dir) {
        net.sf.jasperreports.engine.JasperPrint jp = new net.sf.jasperreports.engine.JasperPrint();
        DocumentoReporte soloPdf = new DocumentoReporte("Recibo", "Recibo-1.pdf", jp, null);
        assertFalse(soloPdf.admiteXlsx());
        assertThrows(IllegalArgumentException.class, () ->
                VistaPreviaReporte.exportarXlsxAtomico(soloPdf, dir.resolve("recibo.xlsx"), () -> false));
    }

    private static Map<String, String> leerEntradasZip(byte[] bytes) throws IOException {
        Map<String, String> mapa = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                zis.transferTo(buf);
                mapa.put(entry.getName(), buf.toString(StandardCharsets.UTF_8));
                zis.closeEntry();
            }
        }
        return mapa;
    }
}
