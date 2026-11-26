package ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto;

public record GenerarReporteRequest(
        String tipo,
        String fechaCorte,
        String desde,
        String hasta,
        Integer umbralDias,
        String propiedadId,
        String unidadId,
        String contratoId,
        String inquilinoId,
        String pagoId,
        Long numeroRecibo,
        String periodo,
        String moneda,
        String tipoUnidad,
        String estado,
        String prioridad,
        String origenTasa,
        String texto,
        Boolean activo) { }
