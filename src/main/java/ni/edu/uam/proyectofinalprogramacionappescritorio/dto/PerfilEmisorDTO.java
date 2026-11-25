package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.util.ArrayList;
import java.util.List;

/** Datos del perfil emisor conservados en el parámetro histórico "emisor". */
public record PerfilEmisorDTO(
        String nombre,
        String identificacion,
        String direccion,
        String telefono,
        String correo) {

    public String resumenVisible() {
        List<String> partes = new ArrayList<>();
        agregarSiPresente(partes, nombre);
        agregarSiPresente(partes, identificacion);
        agregarSiPresente(partes, direccion);
        agregarSiPresente(partes, telefono);
        agregarSiPresente(partes, correo);
        return String.join(" · ", partes);
    }

    private static void agregarSiPresente(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.strip());
        }
    }
}
