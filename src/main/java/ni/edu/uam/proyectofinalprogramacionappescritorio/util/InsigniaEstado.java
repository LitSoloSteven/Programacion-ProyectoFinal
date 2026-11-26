package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.Node;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Insignia textual e iconográfica para estados de dominio conocidos o sin mapear. */
public final class InsigniaEstado {
    private record Estado(String texto, String icono, String clase) { }

    private static final Map<String, Estado> ESTADOS = Map.ofEntries(
            Map.entry("pagado", new Estado("Pagado", "CONFIRMACION", "status-emphasis")),
            Map.entry("parcial", new Estado("Parcial", "RELOJ", "status-pending")),
            Map.entry("vencido", new Estado("Vencido", "ADVERTENCIA", "status-pending")),
            Map.entry("pendiente", new Estado("Pendiente", "RELOJ", "status-pending")),
            Map.entry("pendiente de autorizacion", new Estado("Pendiente de autorización", "RELOJ", "status-pending")),
            Map.entry("anulado", new Estado("Anulado", "CERRAR", "status-secondary")),
            Map.entry("cancelado", new Estado("Cancelado", "CERRAR", "status-secondary")),
            Map.entry("registrado", new Estado("Registrado", "CONFIRMACION", "status-neutral")),
            Map.entry("vigente", new Estado("Vigente", "CONFIRMACION", "status-emphasis")),
            Map.entry("borrador", new Estado("Borrador", "EDITAR", "status-neutral")),
            Map.entry("por vencer", new Estado("Por vencer", "RELOJ", "status-pending")),
            Map.entry("terminado", new Estado("Terminado", "CONFIRMACION", "status-secondary")),
            Map.entry("disponible", new Estado("Disponible", "CONFIRMACION", "status-emphasis")),
            Map.entry("en mantenimiento", new Estado("En mantenimiento", "ADVERTENCIA", "status-pending")),
            Map.entry("inactiva", new Estado("Inactiva", "PUNTO", "status-secondary")),
            Map.entry("inactivo", new Estado("Inactivo", "PUNTO", "status-secondary")),
            Map.entry("activo", new Estado("Activo", "CONFIRMACION", "status-emphasis")),
            Map.entry("activa", new Estado("Activa", "CONFIRMACION", "status-emphasis")),
            Map.entry("autorizada", new Estado("Autorizada", "CONFIRMACION", "status-emphasis")),
            Map.entry("abierta", new Estado("Abierta", "ADVERTENCIA", "status-pending")),
            Map.entry("en proceso", new Estado("En proceso", "RELOJ", "status-pending")),
            Map.entry("resuelta", new Estado("Resuelta", "CONFIRMACION", "status-emphasis")),
            Map.entry("baja", new Estado("Baja", "PUNTO", "status-neutral")),
            Map.entry("media", new Estado("Media", "RELOJ", "status-pending")),
            Map.entry("alta", new Estado("Alta", "ADVERTENCIA", "status-emphasis"))
    );

    private InsigniaEstado() { }

    public static String textoVisible(String valor) {
        return estadoVisible(valor).texto();
    }

    private static Estado estadoVisible(String valor) {
        String crudo = valor == null || valor.isBlank() ? "Sin estado" : valor.trim();
        Estado estado = ESTADOS.get(normalizar(crudo));
        return estado == null ? new Estado(crudo, "PUNTO", "status-neutral") : estado;
    }

    public static HBox crear(String valor) {
        String crudo = valor == null || valor.isBlank() ? "Sin estado" : valor.trim();
        Estado estado = ESTADOS.get(normalizar(crudo));
        Estado visible = estadoVisible(crudo);
        HBox chip = new HBox(5);
        chip.getStyleClass().addAll("status-chip", visible.clase());
        chip.setAccessibleRole(AccessibleRole.TEXT);
        chip.setAccessibleText(visible.texto());
        Node icono = Iconos.crear(Iconos.Nombre.valueOf(visible.icono()), 13);
        Label texto = new Label(visible.texto());
        texto.getStyleClass().add("status-label");
        chip.getChildren().addAll(icono, texto);
        chip.setUserData(estado != null);
        return chip;
    }

    private static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
