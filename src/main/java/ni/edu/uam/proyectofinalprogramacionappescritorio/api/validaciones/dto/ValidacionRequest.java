package ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto;

/**
 * Solicitud de validación de una entrada. {@code tipo} admite correo, telefono,
 * cedula, monto o rangoFechas; {@code valor} se usa en los tres primeros y en
 * monto; {@code inicio}/{@code fin} se usan en rangoFechas.
 */
public record ValidacionRequest(
        String tipo,
        String valor,
        String inicio,
        String fin,
        Integer mesesMinimos) {
}