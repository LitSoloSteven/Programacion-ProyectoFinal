package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;

import java.math.BigDecimal;
import java.util.List;

/** Campos y validación del diálogo de unidad. */
public class UnidadFormController {
    public record Datos(Propiedad propiedad, String codigo, String descripcion, String tipo,
                        String estado, BigDecimal canon, String moneda) { }

    @FXML private ComboBox<Propiedad> cmbPropiedad;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField txtCanon;
    @FXML private ComboBox<String> cmbMoneda;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorPropiedad;
    @FXML private Label lblErrorCodigo;
    @FXML private Label lblErrorCanon;
    @FXML private Label lblErrorDescripcion;

    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList("Apartamento", "Casa", "Cuarto", "Local comercial"));
        cmbTipo.getSelectionModel().selectFirst();
        cmbEstado.setItems(FXCollections.observableArrayList("Disponible", "En mantenimiento", "Inactiva"));
        cmbEstado.getSelectionModel().selectFirst();
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        limpiarErrores();
        marcarLimpio();
    }

    public void establecerPropiedades(List<Propiedad> propiedades) {
        cmbPropiedad.setItems(FXCollections.observableArrayList(propiedades));
        if (!propiedades.isEmpty()) cmbPropiedad.getSelectionModel().selectFirst();
        marcarLimpio();
    }

    public void cargar(Unidad unidad) {
        txtCodigo.setText(unidad.getCodigo());
        txtDescripcion.setText(unidad.getDescripcion());
        cmbTipo.setValue(unidad.getTipo());
        cmbEstado.setValue(unidad.getEstado());
        txtCanon.setText(unidad.getCanonReferencia() == null ? "" : unidad.getCanonReferencia().toPlainString());
        cmbMoneda.setValue(unidad.getMoneda());
        cmbPropiedad.getItems().stream().filter(p -> p.getId().equals(unidad.getPropiedadId()))
                .findFirst().ifPresent(cmbPropiedad::setValue);
        limpiarErrores();
        marcarLimpio();
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        int errores = 0;
        if (cmbPropiedad.getValue() == null) {
            error(lblErrorPropiedad, "Selecciona una propiedad activa."); errores++; valido = false;
        }
        if (txtCodigo.getText() == null || txtCodigo.getText().isBlank()) {
            error(lblErrorCodigo, "Escribe el código de la unidad."); errores++; valido = false;
        } else if (txtCodigo.getText().strip().length() > 30) {
            error(lblErrorCodigo, "El código admite hasta 30 caracteres."); errores++; valido = false;
        }
        try {
            BigDecimal canon = new BigDecimal(txtCanon.getText().trim());
            if (canon.signum() <= 0) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorCanon, "Escribe un canon positivo."); errores++; valido = false;
        }
        String descripcion = txtDescripcion.getText();
        if (descripcion != null && descripcion.length() > 255) {
            error(lblErrorDescripcion, "La descripción admite hasta 255 caracteres."); errores++; valido = false;
        }
        if (errores > 1) mostrarError("Revisa los campos señalados antes de guardar.");
        return valido;
    }

    public Datos datos() {
        String descripcion = txtDescripcion.getText();
        return new Datos(cmbPropiedad.getValue(), txtCodigo.getText().strip(),
                descripcion == null || descripcion.isBlank() ? null : descripcion.strip(),
                cmbTipo.getValue(), cmbEstado.getValue(), new BigDecimal(txtCanon.getText().trim()), cmbMoneda.getValue());
    }

    public void mostrarError(String mensaje) {
        lblResumenError.setText(mensaje); lblResumenError.setVisible(true); lblResumenError.setManaged(true);
    }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }

    private String instantanea() {
        return cmbPropiedad.getValue() + "|" + txtCodigo.getText() + "|" + txtDescripcion.getText() + "|"
                + cmbTipo.getValue() + "|" + cmbEstado.getValue() + "|" + txtCanon.getText() + "|" + cmbMoneda.getValue();
    }
    private void limpiarErrores() {
        for (Label label : List.of(lblResumenError, lblErrorPropiedad, lblErrorCodigo, lblErrorCanon, lblErrorDescripcion)) {
            label.setText(""); label.setVisible(false); label.setManaged(false);
        }
    }
    private void error(Label label, String mensaje) { label.setText(mensaje); label.setVisible(true); label.setManaged(true); }
}
