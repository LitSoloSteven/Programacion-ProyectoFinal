package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.Locale;
import java.util.Objects;

public record DocumentoReporte(
        String titulo,
        String nombreArchivoSugerido,
        JasperPrint jasperPrint,
        TablaReporteXlsx datosXlsx) {

    public DocumentoReporte {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Falta el título del reporte.");
        }
        if (nombreArchivoSugerido == null || nombreArchivoSugerido.isBlank()) {
            throw new IllegalArgumentException("Falta el nombre de archivo sugerido.");
        }
        Objects.requireNonNull(jasperPrint, "Falta el documento JasperPrint.");
    }

    public DocumentoReporte(String titulo, String nombreArchivoSugerido, JasperPrint jasperPrint) {
        this(titulo, nombreArchivoSugerido, jasperPrint, null);
    }

    public boolean admiteXlsx() {
        return datosXlsx != null;
    }

    public String nombreArchivoXlsxSugerido() {
        String base = nombreArchivoSugerido.strip();
        if (base.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return base.substring(0, base.length() - 4) + ".xlsx";
        }
        if (base.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return base;
        }
        return base + ".xlsx";
    }
}
