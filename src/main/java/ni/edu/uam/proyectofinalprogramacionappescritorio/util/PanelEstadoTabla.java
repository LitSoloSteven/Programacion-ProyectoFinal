package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/** Vistas breves para vacío, carga, error y falta de permiso en tablas. */
public final class PanelEstadoTabla {
    private PanelEstadoTabla() { }

    public static Node crear(String titulo, String detalle, String textoAccion, Runnable accion) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("table-state");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("table-state-title");
        encabezado.setMaxWidth(Double.MAX_VALUE);
        encabezado.setAlignment(Pos.CENTER);
        encabezado.setTextAlignment(TextAlignment.CENTER);
        Label descripcion = new Label(detalle);
        descripcion.getStyleClass().add("table-state-text");
        descripcion.setAlignment(Pos.CENTER);
        descripcion.setTextAlignment(TextAlignment.CENTER);
        descripcion.setWrapText(true);
        descripcion.setMaxWidth(520);
        panel.getChildren().addAll(encabezado, descripcion);
        if (textoAccion != null && accion != null) {
            Button boton = new Button(textoAccion);
            boton.getStyleClass().add("button-secondary");
            boton.setOnAction(evento -> accion.run());
            panel.getChildren().add(boton);
        }
        return panel;
    }
}

