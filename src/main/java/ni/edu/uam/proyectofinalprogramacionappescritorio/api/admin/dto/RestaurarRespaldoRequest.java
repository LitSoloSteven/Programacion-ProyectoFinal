package ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto;

public record RestaurarRespaldoRequest(
        String archivoRespaldo,
        String password,
        String destinoJdbcUrl,
        String destinoUsuario,
        String destinoPassword,
        String directorioAdjuntosDestino,
        boolean modoMantenimientoActivo) { }
