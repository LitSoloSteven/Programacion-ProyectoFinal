package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.Objects;

/**
 * Envuelve un {@link Label} de error existente en una barra contextual con botón
 * «Reintentar» opcional, evitando alertas modales bloqueantes durante recargas.
 */
public final class BarraErrorContextual {
    private final HBox contenedor;
    private final Label etiqueta;
    private final Button btnReintentar;

    private BarraErrorContextual(HBox contenedor, Label etiqueta, Button btnReintentar) {
        this.contenedor = contenedor;
        this.etiqueta = etiqueta;
        this.btnReintentar = btnReintentar;
    }

    public static BarraErrorContextual envolver(Label etiqueta) {
        Objects.requireNonNull(etiqueta, "etiqueta");
        Button btnReintentar = new Button("Reintentar");
        btnReintentar.getStyleClass().add("button-secondary");
        btnReintentar.setMinWidth(Region.USE_PREF_SIZE);
        btnReintentar.setVisible(false);
        btnReintentar.setManaged(false);

        HBox contenedor = new HBox(10);
        contenedor.getStyleClass().add("error-banner");
        contenedor.setAlignment(Pos.CENTER_LEFT);
        contenedor.setVisible(false);
        contenedor.setManaged(false);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(etiqueta, Priority.ALWAYS);

        if (etiqueta.getParent() instanceof Pane padre) {
            int indice = padre.getChildren().indexOf(etiqueta);
            padre.getChildren().remove(etiqueta);
            contenedor.getChildren().addAll(etiqueta, btnReintentar);
            if (indice >= 0 && indice <= padre.getChildren().size()) {
                padre.getChildren().add(indice, contenedor);
            } else {
                padre.getChildren().add(contenedor);
            }
        } else {
            contenedor.getChildren().addAll(etiqueta, btnReintentar);
        }

        etiqueta.setText("");
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
        return new BarraErrorContextual(contenedor, etiqueta, btnReintentar);
    }

    public void mostrar(String mensaje, Runnable accionReintento) {
        String limpio = MensajesUi.limpiarParaUsuario(mensaje);
        etiqueta.setText(limpio.isBlank() ? "No se pudo completar la operación. Vuelve a intentarlo." : limpio);
        etiqueta.setGraphic(Iconos.crear(Iconos.Nombre.ADVERTENCIA, 15));
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);

        boolean conReintento = accionReintento != null;
        btnReintentar.setVisible(conReintento);
        btnReintentar.setManaged(conReintento);
        btnReintentar.setOnAction(conReintento ? evento -> accionReintento.run() : null);

        contenedor.setVisible(true);
        contenedor.setManaged(true);
    }

    public void ocultar() {
        etiqueta.setText("");
        etiqueta.setGraphic(null);
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
        btnReintentar.setOnAction(null);
        btnReintentar.setVisible(false);
        btnReintentar.setManaged(false);
        contenedor.setVisible(false);
        contenedor.setManaged(false);
    }
}
