package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;

import java.util.List;

/** Datos, validación y borrador del diálogo de propiedad. */
public class PropiedadFormController {
    public record Datos(String nombre, String direccion, boolean activa, String nombrePropietario,
                        String correoPropietario, String telefonoPropietario) { }

    @FXML private TextField txtNombre;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtNombrePropietario;
    @FXML private TextField txtCorreoPropietario;
    @FXML private TextField txtTelefonoPropietario;
    @FXML private CheckBox chkActiva;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorNombre;
    @FXML private Label lblErrorDireccion;
    @FXML private Label lblErrorPropietario;
    @FXML private Label lblErrorCorreo;

    private String estadoInicial = "";

    @FXML public void initialize() {
        chkActiva.setSelected(true);
        limpiarErrores();
        marcarLimpio();
    }

    public void cargar(Propiedad propiedad) {
        txtNombre.setText(propiedad.getNombre());
        txtDireccion.setText(propiedad.getDireccion());
        chkActiva.setSelected(propiedad.isActiva());
        if (propiedad.getPropietario() != null) {
            txtNombrePropietario.setText(propiedad.getPropietario().getNombre());
            txtCorreoPropietario.setText(propiedad.getPropietario().getCorreo());
            txtTelefonoPropietario.setText(propiedad.getPropietario().getTelefono());
        }
        marcarLimpio();
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        int errores = 0;
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) {
            error(lblErrorNombre, "Escribe el nombre del inmueble."); errores++; valido = false;
        } else if (txtNombre.getText().strip().length() > 120) {
            error(lblErrorNombre, "El nombre admite hasta 120 caracteres."); errores++; valido = false;
        }
        if (txtDireccion.getText() == null || txtDireccion.getText().isBlank()) {
            error(lblErrorDireccion, "Escribe la dirección completa."); errores++; valido = false;
        } else if (txtDireccion.getText().strip().length() > 300) {
            error(lblErrorDireccion, "La dirección admite hasta 300 caracteres."); errores++; valido = false;
        }
        if (txtNombrePropietario.getText() == null || txtNombrePropietario.getText().isBlank()) {
            error(lblErrorPropietario, "Escribe el nombre del propietario."); errores++; valido = false;
        }
        String correo = txtCorreoPropietario.getText();
        if (correo == null || correo.isBlank() || !correo.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            error(lblErrorCorreo, "Escribe un correo válido para el propietario."); errores++; valido = false;
        }
        if (errores > 1) mostrarError("Revisa los campos señalados antes de guardar.");
        return valido;
    }

    public Datos datos() {
        return new Datos(txtNombre.getText().strip(), txtDireccion.getText().strip(), chkActiva.isSelected(),
                txtNombrePropietario.getText().strip(), txtCorreoPropietario.getText().strip(),
                txtTelefonoPropietario.getText() == null || txtTelefonoPropietario.getText().isBlank()
                        ? null : txtTelefonoPropietario.getText().strip());
    }

    public void mostrarError(String mensaje) {
        lblResumenError.setText(mensaje);
        lblResumenError.setManaged(true);
        lblResumenError.setVisible(true);
    }

    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }

    private String instantanea() {
        return txtNombre.getText() + "|" + txtDireccion.getText() + "|" + chkActiva.isSelected() + "|"
                + txtNombrePropietario.getText() + "|" + txtCorreoPropietario.getText() + "|" + txtTelefonoPropietario.getText();
    }

    private void limpiarErrores() {
        for (Label etiqueta : List.of(lblResumenError, lblErrorNombre, lblErrorDireccion, lblErrorPropietario, lblErrorCorreo)) {
            etiqueta.setText(""); etiqueta.setVisible(false); etiqueta.setManaged(false);
        }
    }

    private void error(Label etiqueta, String mensaje) {
        etiqueta.setText(mensaje); etiqueta.setVisible(true); etiqueta.setManaged(true);
    }
}
