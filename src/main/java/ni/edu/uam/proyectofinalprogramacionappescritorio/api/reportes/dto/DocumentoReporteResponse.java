package ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto;

public record DocumentoReporteResponse(
        String documentoId,
        String titulo,
        String nombreArchivoPdf,
        String nombreArchivoXlsx,
        boolean admiteXlsx,
        int totalPaginas,
        int anchoPagina) { }
