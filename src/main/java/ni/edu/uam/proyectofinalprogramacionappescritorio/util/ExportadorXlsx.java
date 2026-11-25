package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TablaReporteXlsx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.OperacionCanceladaException;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exportador OOXML (.xlsx) nativo en Java puro sin dependencias externas.
 * Conserva importes numéricos, tasas a 8 decimales, enteros y fechas reales de Excel.
 */
public final class ExportadorXlsx {

    private static final long EXCEL_EPOCH_OFFSET_DAYS = 25569L;

    private ExportadorXlsx() {
    }

    public static byte[] exportarBytes(TablaReporteXlsx tabla) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        try {
            escribir(tabla, out, null);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el libro XLSX en memoria.", e);
        }
    }

    public static void escribir(TablaReporteXlsx tabla,
                                OutputStream destino,
                                BooleanSupplier cancelado) throws IOException {
        Objects.requireNonNull(tabla, "Falta el contenido tabular del reporte.");
        Objects.requireNonNull(destino, "Falta el flujo de salida XLSX.");

        verificarCancelacion(cancelado);

        try (ZipOutputStream zip = new ZipOutputStream(destino, StandardCharsets.UTF_8)) {
            escribirEntradaTexto(zip, "[Content_Types].xml", contentTypesXml());
            escribirEntradaTexto(zip, "_rels/.rels", rootRelsXml());
            escribirEntradaTexto(zip, "xl/workbook.xml", workbookXml(tabla.nombreHoja()));
            escribirEntradaTexto(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            escribirEntradaTexto(zip, "xl/styles.xml", stylesXml());

            verificarCancelacion(cancelado);
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(zip, StandardCharsets.UTF_8), 65536);
            escribirHoja(tabla, writer, cancelado);
            writer.flush();
            zip.closeEntry();
            zip.finish();
        }
        verificarCancelacion(cancelado);
    }

    private static void escribirHoja(TablaReporteXlsx tabla,
                                     Writer w,
                                     BooleanSupplier cancelado) throws IOException {
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        w.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        w.write("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>");
        w.write("<sheetData>");

        int filaNum = 1;
        // Fila 1: título
        w.write("<row r=\"" + filaNum + "\">");
        escribirCeldaTexto(w, 0, filaNum, 1, tabla.titulo());
        w.write("</row>");
        filaNum++;

        // Metadatos / filtros / corte / moneda
        for (Map.Entry<String, String> meta : tabla.metadatos().entrySet()) {
            w.write("<row r=\"" + filaNum + "\">");
            escribirCeldaTexto(w, 0, filaNum, 8, meta.getKey());
            escribirCeldaTexto(w, 1, filaNum, 0, meta.getValue());
            w.write("</row>");
            filaNum++;
        }

        // Fila separadora en blanco
        filaNum++;

        // Encabezados de columnas
        List<TablaReporteXlsx.Columna> columnas = tabla.columnas();
        w.write("<row r=\"" + filaNum + "\">");
        for (int col = 0; col < columnas.size(); col++) {
            escribirCeldaTexto(w, col, filaNum, 2, columnas.get(col).encabezado());
        }
        w.write("</row>");
        filaNum++;

        // Filas de datos
        List<List<Object>> filas = tabla.filas();
        for (int idx = 0; idx < filas.size(); idx++) {
            if ((idx % 200) == 0) {
                verificarCancelacion(cancelado);
            }
            List<Object> celdas = filas.get(idx);
            w.write("<row r=\"" + filaNum + "\">");
            for (int col = 0; col < columnas.size(); col++) {
                Object valor = col < celdas.size() ? celdas.get(col) : null;
                TablaReporteXlsx.Tipo tipo = columnas.get(col).tipo();
                escribirCeldaDato(w, col, filaNum, tipo, valor);
            }
            w.write("</row>");
            filaNum++;
        }

        verificarCancelacion(cancelado);

        // Resúmenes / totales separados por moneda
        List<TablaReporteXlsx.Resumen> resumenes = tabla.resumenes();
        if (!resumenes.isEmpty()) {
            filaNum++;
            for (TablaReporteXlsx.Resumen resumen : resumenes) {
                w.write("<row r=\"" + filaNum + "\">");
                escribirCeldaTexto(w, 0, filaNum, 8, resumen.etiqueta());
                if (resumen.moneda() != null && !resumen.moneda().isBlank()) {
                    escribirCeldaTexto(w, 1, filaNum, 0, resumen.moneda());
                }
                if (resumen.valorNumerico() != null) {
                    BigDecimal redondeado = resumen.valorNumerico().setScale(2, RoundingMode.HALF_EVEN);
                    escribirCeldaNumero(w, 2, filaNum, 9, redondeado.toPlainString());
                } else if (resumen.textoAlterno() != null) {
                    escribirCeldaTexto(w, 2, filaNum, 8, resumen.textoAlterno());
                }
                w.write("</row>");
                filaNum++;
            }
        }

        w.write("</sheetData>");
        w.write("</worksheet>");
    }

    private static void escribirCeldaDato(Writer w,
                                          int col,
                                          int row,
                                          TablaReporteXlsx.Tipo tipo,
                                          Object valor) throws IOException {
        if (valor == null) {
            return;
        }
        switch (tipo) {
            case MONTO -> {
                BigDecimal bd = aBigDecimal(valor);
                if (bd != null) {
                    escribirCeldaNumero(w, col, row, 3, bd.setScale(2, RoundingMode.HALF_EVEN).toPlainString());
                } else {
                    escribirCeldaTexto(w, col, row, 0, String.valueOf(valor));
                }
            }
            case TASA -> {
                BigDecimal bd = aBigDecimal(valor);
                if (bd != null) {
                    escribirCeldaNumero(w, col, row, 4, bd.setScale(8, RoundingMode.HALF_EVEN).toPlainString());
                } else {
                    escribirCeldaTexto(w, col, row, 0, String.valueOf(valor));
                }
            }
            case ENTERO -> {
                if (valor instanceof Number num) {
                    escribirCeldaNumero(w, col, row, 5, Long.toString(num.longValue()));
                } else {
                    BigDecimal bd = aBigDecimal(valor);
                    if (bd != null) {
                        escribirCeldaNumero(w, col, row, 5, Long.toString(bd.longValue()));
                    } else {
                        escribirCeldaTexto(w, col, row, 0, String.valueOf(valor));
                    }
                }
            }
            case PORCENTAJE -> {
                BigDecimal bd = aBigDecimal(valor);
                if (bd != null) {
                    escribirCeldaNumero(w, col, row, 7, bd.setScale(2, RoundingMode.HALF_EVEN).toPlainString());
                } else {
                    escribirCeldaTexto(w, col, row, 0, String.valueOf(valor));
                }
            }
            case FECHA -> {
                if (valor instanceof LocalDate fecha) {
                    long serial = fecha.toEpochDay() + EXCEL_EPOCH_OFFSET_DAYS;
                    escribirCeldaNumero(w, col, row, 6, Long.toString(serial));
                } else {
                    escribirCeldaTexto(w, col, row, 0, String.valueOf(valor));
                }
            }
            case TEXTO -> escribirCeldaTexto(w, col, row, 0, String.valueOf(valor));
        }
    }

    private static BigDecimal aBigDecimal(Object valor) {
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        if (valor instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        if (valor instanceof String s && !s.isBlank()) {
            try {
                return new BigDecimal(s.strip());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static void escribirCeldaNumero(Writer w,
                                            int col,
                                            int row,
                                            int styleIndex,
                                            String numero) throws IOException {
        w.write("<c r=\"");
        w.write(refCelda(col, row));
        w.write("\" s=\"");
        w.write(Integer.toString(styleIndex));
        w.write("\"><v>");
        w.write(numero);
        w.write("</v></c>");
    }

    private static void escribirCeldaTexto(Writer w,
                                           int col,
                                           int row,
                                           int styleIndex,
                                           String texto) throws IOException {
        w.write("<c r=\"");
        w.write(refCelda(col, row));
        w.write("\" s=\"");
        w.write(Integer.toString(styleIndex));
        w.write("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">");
        w.write(escaparXml(texto));
        w.write("</t></is></c>");
    }

    static String refCelda(int col, int row) {
        StringBuilder sb = new StringBuilder(6);
        int c = col;
        do {
            sb.append((char) ('A' + (c % 26)));
            c = (c / 26) - 1;
        } while (c >= 0);
        sb.reverse();
        sb.append(row);
        return sb.toString();
    }

    static String escaparXml(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') {
                continue;
            }
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void verificarCancelacion(BooleanSupplier cancelado) {
        if (cancelado != null && cancelado.getAsBoolean()) {
            throw new OperacionCanceladaException();
        }
    }

    private static void escribirEntradaTexto(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """.strip();
    }

    private static String rootRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """.strip();
    }

    private static String workbookXml(String nombreHoja) {
        String hojaSegura = escaparXml(nombreHoja == null || nombreHoja.isBlank() ? "Reporte" : nombreHoja);
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="%s" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """.formatted(hojaSegura).strip();
    }

    private static String workbookRelsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """.strip();
    }

    private static String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <numFmts count="5">
                    <numFmt numFmtId="164" formatCode="#,##0.00"/>
                    <numFmt numFmtId="165" formatCode="0.00000000"/>
                    <numFmt numFmtId="166" formatCode="#,##0"/>
                    <numFmt numFmtId="167" formatCode="yyyy-mm-dd"/>
                    <numFmt numFmtId="168" formatCode="0.0&quot;%&quot;"/>
                  </numFmts>
                  <fonts count="3">
                    <font><sz val="10"/><name val="Calibri"/></font>
                    <font><b/><sz val="12"/><name val="Calibri"/></font>
                    <font><b/><sz val="10"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="3">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FFE2E8F0"/></patternFill></fill>
                  </fills>
                  <borders count="1">
                    <border><left/><right/><top/><bottom/><diagonal/></border>
                  </borders>
                  <cellStyleXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                  </cellStyleXfs>
                  <cellXfs count="10">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
                    <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                    <xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                    <xf numFmtId="166" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                    <xf numFmtId="167" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                    <xf numFmtId="168" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="164" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/>
                  </cellXfs>
                </styleSheet>
                """.strip();
    }
}
