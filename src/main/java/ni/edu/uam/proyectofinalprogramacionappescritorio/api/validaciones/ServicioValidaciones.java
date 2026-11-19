package ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto.ValidacionResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.Permiso;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PermisoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@RequiredArgsConstructor
public final class ServicioValidaciones {
    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();

    public ValidacionResponse validar(ValidacionRequest solicitud, SesionUsuario identidad) {
        SesionUsuario.ejecutarComo(identidad, () -> tx.ejecutar(sesion -> {
            permisos.exigir(sesion, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            return null;
        }));
        if (ValidadorEntrada.excedeLongitudValor(solicitud.valor())) {
            throw ExcepcionApi.solicitudInvalida("El valor no puede superar "
                    + ValidadorEntrada.LONGITUD_MAXIMA_VALOR + " caracteres.");
        }
        String tipo = solicitud.tipo() == null
                ? "" : solicitud.tipo().strip().toLowerCase(Locale.ROOT);
        return resultado(tipo, solicitud);
    }

    private static ValidacionResponse resultado(String tipo, ValidacionRequest solicitud) {
        return switch (tipo) {
            case "correo" -> correo(solicitud.valor());
            case "telefono" -> telefono(solicitud.valor());
            case "cedula" -> cedula(solicitud.valor());
            case "monto" -> monto(solicitud.valor());
            case "rangofechas" -> rangoFechas(solicitud);
            default -> throw ExcepcionApi.solicitudInvalida(
                    "El tipo debe ser correo, telefono, cedula, monto o rangoFechas.");
        };
    }

    private static ValidacionResponse correo(String valor) {
        if (ValidadorEntrada.esVacio(valor)) {
            return new ValidacionResponse(false, "El correo es obligatorio.");
        }
        return ValidadorEntrada.esCorreoValido(valor)
                ? valido("El correo tiene un formato válido.")
                : new ValidacionResponse(false, "El correo no tiene un formato válido.");
    }

    private static ValidacionResponse telefono(String valor) {
        if (ValidadorEntrada.esVacio(valor)) {
            return new ValidacionResponse(false, "El teléfono es obligatorio.");
        }
        return ValidadorEntrada.esTelefonoValido(valor)
                ? valido("El teléfono tiene un formato válido.")
                : new ValidacionResponse(false, "El teléfono no tiene un formato válido.");
    }

    private static ValidacionResponse cedula(String valor) {
        if (ValidadorEntrada.esVacio(valor)) {
            return new ValidacionResponse(false, "La cédula es obligatoria.");
        }
        return ValidadorEntrada.esCedulaValida(valor)
                ? valido("La cédula tiene un formato válido.")
                : new ValidacionResponse(false, "La cédula no tiene un formato válido.");
    }

    private static ValidacionResponse monto(String valor) {
        if (ValidadorEntrada.esVacio(valor)) {
            return new ValidacionResponse(false, "El monto es obligatorio.");
        }
        return ValidadorEntrada.esMontoValido(valor)
                ? valido("El monto es un decimal positivo con dos decimales como máximo.")
                : new ValidacionResponse(false,
                        "El monto debe ser un decimal positivo con dos decimales como máximo.");
    }

    private static ValidacionResponse rangoFechas(ValidacionRequest solicitud) {
        LocalDate inicio = fecha(solicitud.inicio());
        if (inicio == null) {
            return new ValidacionResponse(false,
                    "La fecha inicial es obligatoria y debe tener el formato AAAA-MM-DD.");
        }
        LocalDate fin = fecha(solicitud.fin());
        if (fin == null) {
            return new ValidacionResponse(false,
                    "La fecha final es obligatoria y debe tener el formato AAAA-MM-DD.");
        }
        int meses = solicitud.mesesMinimos() == null ? 0 : solicitud.mesesMinimos();
        if (meses < 0) {
            return new ValidacionResponse(false, "Los meses mínimos no pueden ser negativos.");
        }
        try {
            ValidadorEntrada.validarRangoFechas(inicio, fin, meses);
            return valido("El rango de fechas es válido.");
        } catch (IllegalArgumentException e) {
            return new ValidacionResponse(false, e.getMessage());
        }
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static ValidacionResponse valido(String mensaje) {
        return new ValidacionResponse(true, mensaje);
    }
}
