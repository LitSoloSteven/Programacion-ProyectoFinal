package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;

import java.util.List;

/** Campos, validación y datos del formulario de inquilino. */
public class InquilinoFormController {
    public record Datos(String nombre, String identificacion, String telefono, String correo,
                        String emergencia, boolean activo) { }

    @FXML private TextField txtNombre;
    @FXML private TextField txtIdentificacion;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtContactoEmergencia;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorNombre;
    @FXML private Label lblErrorIdentificacion;
    @FXML private Label lblErrorTelefono;
    @FXML private Label lblErrorCorreo;
    @FXML private Label lblErrorEmergencia;

    private String estadoInicial = "";

    @FXML public void initialize() { chkActivo.setSelected(true); limpiarErrores(); marcarLimpio(); }

    public void cargar(ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino inquilino) {
        txtNombre.setText(inquilino.getNombre());
        txtIdentificacion.setText(inquilino.getIdentificacion());
        txtTelefono.setText(inquilino.getTelefono());
        txtCorreo.setText(inquilino.getCorreo());
        txtContactoEmergencia.setText(inquilino.getContactoEmergencia());
        chkActivo.setSelected(inquilino.isActivo());
        marcarLimpio();
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        int errores = 0;
        String nombre = txtNombre.getText();
        if (nombre == null || nombre.isBlank()) {
            error(lblErrorNombre, "Escribe el nombre completo."); errores++; valido = false;
        } else if (nombre.strip().length() > 160) {
            error(lblErrorNombre, "El nombre admite hasta 160 caracteres."); errores++; valido = false;
        }
        if (txtIdentificacion.getText() != null && txtIdentificacion.getText().strip().length() > 40) {
            error(lblErrorIdentificacion, "La identificación admite hasta 40 caracteres."); errores++; valido = false;
        }
        if (!ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada.esTelefonoValido(txtTelefono.getText())) {
            error(lblErrorTelefono, "Escribe un teléfono válido con 7 a 15 dígitos; puedes usar +, espacios o guiones."); errores++; valido = false;
        }
        String correo = txtCorreo.getText();
        if (correo != null && !correo.isBlank()
                && (correo.strip().length() > 254 || !correo.strip().matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"))) {
            error(lblErrorCorreo, "Escribe un correo válido de hasta 254 caracteres."); errores++; valido = false;
        }
        if (txtContactoEmergencia.getText() != null && txtContactoEmergencia.getText().strip().length() > 200) {
            error(lblErrorEmergencia, "El contacto de emergencia admite hasta 200 caracteres."); errores++; valido = false;
        }
        if (!valido) ValidacionFormulario.mostrar(lblResumenError, lblErrorNombre, lblErrorIdentificacion, lblErrorTelefono, lblErrorCorreo, lblErrorEmergencia);
        return valido;
    }

    public Datos datos() {
        return new Datos(txtNombre.getText().strip(), opcional(txtIdentificacion.getText()),
                opcional(txtTelefono.getText()), opcional(txtCorreo.getText()),
                opcional(txtContactoEmergencia.getText()), chkActivo.isSelected());
    }

    public void mostrarError(String mensaje) { lblResumenError.setText(mensaje); lblResumenError.setVisible(true); lblResumenError.setManaged(true); }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }
    private String instantanea() {
        return txtNombre.getText() + "|" + txtIdentificacion.getText() + "|" + txtTelefono.getText() + "|"
                + txtCorreo.getText() + "|" + txtContactoEmergencia.getText() + "|" + chkActivo.isSelected();
    }
    private void limpiarErrores() {
        for (Label label : List.of(lblResumenError, lblErrorNombre, lblErrorIdentificacion,
                lblErrorTelefono, lblErrorCorreo, lblErrorEmergencia)) {
            label.setText(""); label.setVisible(false); label.setManaged(true);
        }
    }
    private void error(Label label, String mensaje) { label.setText(mensaje); label.setVisible(true); label.setManaged(true); }
    private static String opcional(String texto) { return texto == null || texto.isBlank() ? null : texto.strip(); }
}
