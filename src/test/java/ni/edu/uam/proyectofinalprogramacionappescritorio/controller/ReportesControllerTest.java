package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ReportesControllerTest {
    private static final Pattern CODIGOS_TECNICOS = Pattern.compile("\\b(REP-\\d+|RN-\\d+|H[1-6]|M0\\d|M1\\d|PAN-\\d+|ERR-\\d+)\\b");

    @Test
    void catalogoCubreLos19DocumentosSinDuplicadosNiCodigosTecnicos() {
        List<String> codigosEsperados = List.of(
                "RECIBO", "ESTADO_CUENTA", "DASHBOARD_RESUMEN", "CARTERA_VENCIDA", "CARGOS_CARTERA",
                "PAGOS_TABLA", "HISTORIAL_PAGOS", "INGRESOS_GASTOS", "GARANTIAS_DEPOSITOS",
                "LIQUIDACION_GARANTIAS", "PROPIEDADES_UNIDADES", "UNIDADES_CATALOGO", "OCUPACION",
                "INQUILINOS_CATALOGO", "CONTRATOS_OCUPACION", "CONTRATOS_POR_VENCER",
                "MANTENIMIENTO", "TASAS_CATALOGO", "TASAS_UTILIZADAS"
        );

        ReportesController.TipoReporte[] valores = ReportesController.TipoReporte.values();
        assertEquals(19, valores.length, "El Centro debe exponer exactamente los 19 documentos del inventario");

        Set<String> codigosEnEnum = new HashSet<>();
        Set<String> etiquetas = new HashSet<>();
        for (ReportesController.TipoReporte tipo : valores) {
            assertTrue(codigosEnEnum.add(tipo.codigoApi()), "Código duplicado: " + tipo.codigoApi());
            assertTrue(etiquetas.add(tipo.etiqueta()), "Etiqueta duplicada: " + tipo.etiqueta());
            assertFalse(CODIGOS_TECNICOS.matcher(tipo.etiqueta()).find(),
                    "La etiqueta no debe exponer códigos técnicos: " + tipo.etiqueta());
            assertFalse(CODIGOS_TECNICOS.matcher(tipo.descripcion()).find(),
                    "La descripción no debe exponer códigos técnicos: " + tipo.descripcion());
            assertNotEquals(ReportesController.CategoriaReporte.TODAS, tipo.categoria(),
                    tipo.name() + " debe pertenecer a una categoría concreta");

            if (tipo == ReportesController.TipoReporte.RECIBO) {
                assertFalse(tipo.admiteXlsx(), "El recibo solo admite PDF");
            } else {
                assertTrue(tipo.admiteXlsx(), tipo.name() + " debe admitir PDF y XLSX");
            }
        }
        assertEquals(new HashSet<>(codigosEsperados), codigosEnEnum);
    }

    @Test
    void nombresArchivoSugeridosSonDescriptivosYSinCodigosTecnicos() {
        LocalDate corte = LocalDate.of(2026, 11, 26);
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 30);

        assertEquals("Recibo-105.pdf", ReportesController.nombreArchivoSugerido(
                ReportesController.TipoReporte.RECIBO, false, corte, desde, hasta, 105L));
        assertEquals("Recibo-105.pdf", ReportesController.nombreArchivoSugerido(
                ReportesController.TipoReporte.RECIBO, true, corte, desde, hasta, 105L));
        assertEquals("Estado-Cuenta-2026-11-26.xlsx", ReportesController.nombreArchivoSugerido(
                ReportesController.TipoReporte.ESTADO_CUENTA, true, corte, desde, hasta, null));
        assertEquals("Reporte-Ingresos-Gastos-2026-11-01_2026-11-30.pdf", ReportesController.nombreArchivoSugerido(
                ReportesController.TipoReporte.INGRESOS_GASTOS, false, corte, desde, hasta, null));

        for (ReportesController.TipoReporte tipo : ReportesController.TipoReporte.values()) {
            String pdf = ReportesController.nombreArchivoSugerido(tipo, false, corte, desde, hasta, 10L);
            assertTrue(pdf.endsWith(".pdf"), pdf);
            assertFalse(pdf.contains("REP-"), pdf);
            if (tipo.admiteXlsx()) {
                String xlsx = ReportesController.nombreArchivoSugerido(tipo, true, corte, desde, hasta, 10L);
                assertTrue(xlsx.endsWith(".xlsx"), xlsx);
                assertFalse(xlsx.contains("REP-"), xlsx);
            }
        }

        long totalPorCategorias = Arrays.stream(ReportesController.CategoriaReporte.values())
                .filter(c -> c != ReportesController.CategoriaReporte.TODAS)
                .mapToLong(c -> Arrays.stream(ReportesController.TipoReporte.values())
                        .filter(t -> t.categoria() == c)
                        .count())
                .sum();
        assertEquals(19L, totalPorCategorias);
    }
}
