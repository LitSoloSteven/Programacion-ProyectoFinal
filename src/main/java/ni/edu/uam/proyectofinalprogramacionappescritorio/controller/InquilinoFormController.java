package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada;

import java.util.List;

/** Campos, validación y datos del formulario de inquilino. */
public class InquilinoFormController {
    public record Datos(String nombre, String identificacion, String telefono, String correo,
                        String emergencia, boolean activo) { }

    private static final String SEPARADOR_EMERGENCIA = " — ";

    @FXML private TextField txtNombre;
    @FXML private TextField txtIdentificacion;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtEmergenciaNombre;
    @FXML private TextField txtEmergenciaTelefono;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorNombre;
    @FXML private Label lblErrorIdentificacion;
    @FXML private Label lblErrorTelefono;
    @FXML private Label lblErrorCorreo;
    @FXML private Label lblErrorEmergencia;

    private String estadoInicial = "";

    @FXML public void initialize() { chkActivo.setSelected(true); limpiarErrores(); marcarLimpio(); }

    public void cargar(Inquilino inquilino) {
        txtNombre.setText(inquilino.getNombre());
        txtIdentificacion.setText(inquilino.getIdentificacion());
        txtTelefono.setText(inquilino.getTelefono());
        txtCorreo.setText(inquilino.getCorreo());
        cargarContactoEmergencia(inquilino.getContactoEmergencia());
        chkActivo.setSelected(inquilino.isActivo());
        marcarLimpio();
    }

    private void cargarContactoEmergencia(String contacto) {
        if (contacto == null || contacto.isBlank()) {
            txtEmergenciaNombre.setText("");
            txtEmergenciaTelefono.setText("");
            return;
        }
        int sep = contacto.indexOf(SEPARADOR_EMERGENCIA);
        if (sep >= 0) {
            txtEmergenciaNombre.setText(contacto.substring(0, sep).strip());
            txtEmergenciaTelefono.setText(contacto.substring(sep + SEPARADOR_EMERGENCIA.length()).strip());
        } else {
            txtEmergenciaNombre.setText(contacto.strip());
            txtEmergenciaTelefono.setText("");
        }
    }

    public void aplicarPermisoEstado(boolean puedeCambiarEstado) {
        chkActivo.setDisable(!puedeCambiarEstado);
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        String nombre = txtNombre.getText();
        if (nombre == null || nombre.isBlank()) {
            error(lblErrorNombre, "Escribe el nombre completo.");
            valido = false;
        } else if (nombre.strip().length() > 160) {
            error(lblErrorNombre, "El nombre admite hasta 160 caracteres.");
            valido = false;
        }
        if (txtIdentificacion.getText() != null && txtIdentificacion.getText().strip().length() > 40) {
            error(lblErrorIdentificacion, "La identificación admite hasta 40 caracteres.");
            valido = false;
        }
        if (!ValidadorEntrada.esTelefonoValido(txtTelefono.getText())) {
            error(lblErrorTelefono, "Escribe un teléfono válido con 7 a 15 dígitos; puedes usar +, espacios o guiones.");
            valido = false;
        }
        String correo = txtCorreo.getText();
        if (correo != null && !correo.isBlank()
                && (correo.strip().length() > 254 || !ValidadorEntrada.esCorreoValido(correo.strip()))) {
            error(lblErrorCorreo, "Escribe un correo válido de hasta 254 caracteres.");
            valido = false;
        }
        String emergTel = txtEmergenciaTelefono.getText();
        if (emergTel != null && !emergTel.isBlank() && !ValidadorEntrada.esTelefonoValido(emergTel)) {
            error(lblErrorEmergencia, "El teléfono de emergencia debe tener de 7 a 15 dígitos.");
            valido = false;
        } else {
            String combinado = combinarEmergencia();
            if (combinado != null && combinado.length() > 200) {
                error(lblErrorEmergencia, "El contacto de emergencia admite hasta 200 caracteres.");
                valido = false;
            }
        }
        if (!valido) {
            ValidacionFormulario.mostrar(lblResumenError, lblErrorNombre, lblErrorIdentificacion,
                    lblErrorTelefono, lblErrorCorreo, lblErrorEmergencia);
        }
        return valido;
    }

    public Datos datos() {
        return new Datos(txtNombre.getText().strip(), opcional(txtIdentificacion.getText()),
                opcional(txtTelefono.getText()), opcional(txtCorreo.getText()),
                combinarEmergencia(), chkActivo.isSelected());
    }

    private String combinarEmergencia() {
        String nom = opcional(txtEmergenciaNombre.getText());
        String tel = opcional(txtEmergenciaTelefono.getText());
        if (nom == null && tel == null) return null;
        if (nom != null && tel != null) return nom + SEPARADOR_EMERGENCIA + tel;
        return nom != null ? nom : tel;
    }

    public void mostrarError(String mensaje) { lblResumenError.setText(mensaje); lblResumenError.setVisible(true); lblResumenError.setManaged(true); }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }
    private String instantanea() {
        return txtNombre.getText() + "|" + txtIdentificacion.getText() + "|" + txtTelefono.getText() + "|"
                + txtCorreo.getText() + "|" + txtEmergenciaNombre.getText() + "|"
                + txtEmergenciaTelefono.getText() + "|" + chkActivo.isSelected();
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
