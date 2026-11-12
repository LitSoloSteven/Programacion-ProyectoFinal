package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import java.util.Objects;

/** Invocar dentro de la Session que recupera el recibo y su pago. */
public final class ReciboMapper {
    private ReciboMapper() { }
    public static ReciboDTO aDTO(Recibo recibo) {
        return Objects.requireNonNull(recibo, "Falta el recibo.").presentar();
    }
}
