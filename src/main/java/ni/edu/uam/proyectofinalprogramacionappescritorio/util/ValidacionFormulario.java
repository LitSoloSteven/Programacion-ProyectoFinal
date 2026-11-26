package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Conserva el detalle por campo, aplica feedback en vivo y enfoca errores de guardado. */
public final class ValidacionFormulario {
    public static final String CLASE_CAMPO_INVALIDO = "field-invalid";
    private static final String CLASE_BANNER_ERROR = "error-banner";

    public record CampoValidado(Control control, Label error) { }

    private ValidacionFormulario() { }

    public static void vincularTexto(TextInputControl campo, Runnable alCambiar) {
        if (campo == null || alCambiar == null) return;
        campo.textProperty().addListener((obs, anterior, actual) -> {
            if (!Objects.equals(anterior, actual)) {
                alCambiar.run();
            }
        });
    }

    public static void vincularSelector(ComboBoxBase<?> campo, Runnable alCambiar) {
        if (campo == null || alCambiar == null) return;
        campo.valueProperty().addListener((obs, anterior, actual) -> {
            if (!Objects.equals(anterior, actual)) {
                alCambiar.run();
            }
        });
        if (campo instanceof DatePicker picker && picker.getEditor() != null) {
            picker.getEditor().textProperty().addListener((obs, anterior, actual) -> {
                if (!Objects.equals(anterior, actual)) {
                    alCambiar.run();
                }
            });
        }
    }

    public static void marcarCampo(Control campo, Label etiquetaError, String mensaje) {
        String limpio = MensajesUi.limpiarParaUsuario(mensaje);
        boolean conError = !limpio.isBlank();
        if (etiquetaError != null) {
            etiquetaError.setText(conError ? limpio : "");
            etiquetaError.setVisible(conError);
            etiquetaError.setManaged(true);
            etiquetaError.setGraphic(conError ? Iconos.crear(Iconos.Nombre.ADVERTENCIA, 13) : null);
        }
        if (campo != null) {
            if (conError) {
                if (!campo.getStyleClass().contains(CLASE_CAMPO_INVALIDO)) {
                    campo.getStyleClass().add(CLASE_CAMPO_INVALIDO);
                }
            } else {
                campo.getStyleClass().remove(CLASE_CAMPO_INVALIDO);
            }
        }
    }

    public static void limpiarCampos(Label resumen, CampoValidado... campos) {
        if (resumen != null) {
            resumen.setText("");
            resumen.setVisible(false);
            resumen.setManaged(false);
            resumen.setGraphic(null);
            resumen.getStyleClass().remove(CLASE_BANNER_ERROR);
        }
        if (campos != null) {
            for (CampoValidado campo : campos) {
                if (campo != null) {
                    marcarCampo(campo.control(), campo.error(), null);
                }
            }
        }
    }

    public static void sincronizarResumenSiVisible(Label resumen, Label... campos) {
        if (resumen == null || !resumen.isVisible()) return;
        String detalle = concatenarErrores(campos);
        if (detalle.isBlank()) {
            resumen.setText("");
            resumen.setVisible(false);
            resumen.setManaged(false);
            resumen.setGraphic(null);
            resumen.getStyleClass().remove(CLASE_BANNER_ERROR);
        } else {
            resumen.setText(detalle);
        }
    }

    public static void mostrar(Label resumen, Label... campos) {
        String detalle = concatenarErrores(campos);
        if (detalle.isBlank()) return;
        aplicarResumen(resumen, detalle);
        if (resumen != null) {
            resumen.requestFocus();
        }
    }

    public static void mostrarYEnfocar(Label resumen, List<CampoValidado> campos) {
        if (campos == null || campos.isEmpty()) return;
        Label[] etiquetas = campos.stream()
                .map(CampoValidado::error)
                .filter(Objects::nonNull)
                .toArray(Label[]::new);
        String detalle = concatenarErrores(etiquetas);
        if (detalle.isBlank()) return;
        aplicarResumen(resumen, detalle);
        Control primeroInvalido = campos.stream()
                .filter(c -> c.error() != null && c.error().isVisible()
                        && c.control() != null && !c.control().isDisabled() && c.control().isVisible())
                .map(CampoValidado::control)
                .findFirst()
                .orElse(null);
        if (primeroInvalido != null) {
            primeroInvalido.requestFocus();
        } else if (resumen != null) {
            resumen.requestFocus();
        }
    }

    private static void aplicarResumen(Label resumen, String detalle) {
        if (resumen == null) return;
        resumen.setText(detalle);
        resumen.setVisible(true);
        resumen.setManaged(true);
        resumen.setFocusTraversable(true);
        resumen.setGraphic(Iconos.crear(Iconos.Nombre.ADVERTENCIA, 14));
        if (!resumen.getStyleClass().contains(CLASE_BANNER_ERROR)) {
            resumen.getStyleClass().add(CLASE_BANNER_ERROR);
        }
        for (Node nodo = resumen; nodo != null; nodo = nodo.getParent()) {
            if (nodo instanceof ScrollPane scroll) scroll.setVvalue(scroll.getVmin());
        }
    }

    private static String concatenarErrores(Label... campos) {
        if (campos == null) return "";
        return Arrays.stream(campos)
                .filter(Objects::nonNull)
                .filter(Label::isVisible)
                .map(Label::getText)
                .filter(texto -> texto != null && !texto.isBlank())
                .distinct()
                .collect(Collectors.joining("\n"));
    }
}
