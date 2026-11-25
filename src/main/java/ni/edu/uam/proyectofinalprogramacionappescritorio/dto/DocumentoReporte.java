package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.Objects;

public record DocumentoReporte(
        String titulo,
        String nombreArchivoSugerido,
        JasperPrint jasperPrint) {

    public DocumentoReporte {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Falta el título del reporte.");
        }
        if (nombreArchivoSugerido == null || nombreArchivoSugerido.isBlank()) {
            throw new IllegalArgumentException("Falta el nombre de archivo sugerido.");
        }
        Objects.requireNonNull(jasperPrint, "Falta el documento JasperPrint.");
    }
}
