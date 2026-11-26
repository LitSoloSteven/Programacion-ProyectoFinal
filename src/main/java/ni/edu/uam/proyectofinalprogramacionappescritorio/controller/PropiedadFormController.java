package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.ValidadorEntrada;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorAsincrono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Datos, validación en vivo y borrador del diálogo de propiedad. */
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
    @FXML private Label lblErrorTelefono;

    private final Set<Control> tocados = new HashSet<>();
    private final ValidadorAsincrono validadorCorreo = new ValidadorAsincrono();
    private final ValidadorAsincrono validadorTelefono = new ValidadorAsincrono();
    private ClienteValidaciones clienteValidaciones;
    private boolean inicializando = true;
    private String estadoInicial = "";

    @FXML public void initialize() {
        chkActiva.setSelected(true);
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
        marcarLimpio();
    }

    public void establecerValidadorServidor(ClienteValidaciones clienteValidaciones) {
        this.clienteValidaciones = clienteValidaciones;
    }

    public void cargar(PropiedadResponse propiedad) {
        inicializando = true;
        try {
            txtNombre.setText(propiedad.nombre());
            txtDireccion.setText(propiedad.direccion());
            chkActiva.setSelected(propiedad.activa());
            if (propiedad.propietarioNombre() != null) {
                txtNombrePropietario.setText(propiedad.propietarioNombre());
                txtCorreoPropietario.setText(propiedad.propietarioCorreo());
                txtTelefonoPropietario.setText(propiedad.propietarioTelefono());
            }
            tocados.clear();
            validadorCorreo.reiniciar();
            validadorTelefono.reiniciar();
            limpiarErrores();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    public boolean validar() {
        tocados.addAll(List.of(txtNombre, txtDireccion, txtNombrePropietario,
                txtCorreoPropietario, txtTelefonoPropietario));
        boolean valido = validarNombre()
                & validarDireccion()
                & validarNombrePropietario()
                & validarCorreo()
                & validarTelefono();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(txtNombre, lblErrorNombre),
                    new CampoValidado(txtDireccion, lblErrorDireccion),
                    new CampoValidado(txtNombrePropietario, lblErrorPropietario),
                    new CampoValidado(txtCorreoPropietario, lblErrorCorreo),
                    new CampoValidado(txtTelefonoPropietario, lblErrorTelefono)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                    lblErrorNombre, lblErrorDireccion, lblErrorPropietario, lblErrorCorreo, lblErrorTelefono);
        }
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

    private void configurarValidacionEnVivo() {
        ValidacionFormulario.vincularTexto(txtNombre, () -> alEditar(txtNombre, this::validarNombre));
        ValidacionFormulario.vincularTexto(txtDireccion, () -> alEditar(txtDireccion, this::validarDireccion));
        ValidacionFormulario.vincularTexto(txtNombrePropietario, () -> alEditar(txtNombrePropietario, this::validarNombrePropietario));
        ValidacionFormulario.vincularTexto(txtCorreoPropietario, () -> {
            alEditar(txtCorreoPropietario, this::validarCorreo);
            validarCorreoEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtTelefonoPropietario, () -> {
            alEditar(txtTelefonoPropietario, this::validarTelefono);
            validarTelefonoEnServidor();
        });
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorNombre, lblErrorDireccion, lblErrorPropietario, lblErrorCorreo, lblErrorTelefono);
    }

    private boolean validarNombre() {
        String valor = txtNombre.getText();
        String msg = null;
        if (valor == null || valor.isBlank()) {
            msg = "Escribe el nombre del inmueble.";
        } else if (valor.strip().length() > 120) {
            msg = "El nombre admite hasta 120 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtNombre, lblErrorNombre, msg);
        return msg == null;
    }

    private boolean validarDireccion() {
        String valor = txtDireccion.getText();
        String msg = null;
        if (valor == null || valor.isBlank()) {
            msg = "Escribe la dirección completa.";
        } else if (valor.strip().length() > 300) {
            msg = "La dirección admite hasta 300 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtDireccion, lblErrorDireccion, msg);
        return msg == null;
    }

    private boolean validarNombrePropietario() {
        String valor = txtNombrePropietario.getText();
        String msg = null;
        if (valor == null || valor.isBlank()) {
            msg = "Escribe el nombre del propietario.";
        } else if (valor.strip().length() > 100) {
            msg = "El nombre del propietario admite hasta 100 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtNombrePropietario, lblErrorPropietario, msg);
        return msg == null;
    }

    private boolean validarCorreo() {
        String correo = txtCorreoPropietario.getText();
        String msg = null;
        if (correo == null || correo.isBlank()
                || correo.strip().length() > 150
                || !ValidadorEntrada.esCorreoValido(correo.strip())) {
            msg = "Escribe un correo válido de hasta 150 caracteres para el propietario.";
        }
        ValidacionFormulario.marcarCampo(txtCorreoPropietario, lblErrorCorreo, msg);
        return msg == null;
    }

    private boolean validarTelefono() {
        String telefono = txtTelefonoPropietario.getText();
        String msg = null;
        if (telefono != null && !telefono.isBlank()) {
            if (telefono.strip().length() > 20) {
                msg = "El teléfono admite hasta 20 caracteres.";
            } else if (!ValidadorEntrada.esTelefonoValido(telefono)) {
                msg = "Escribe un teléfono válido con 7 a 15 dígitos.";
            }
        }
        ValidacionFormulario.marcarCampo(txtTelefonoPropietario, lblErrorTelefono, msg);
        return msg == null;
    }

    private void validarCorreoEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        String correo = txtCorreoPropietario.getText() == null ? "" : txtCorreoPropietario.getText().strip();
        if (correo.isEmpty() || correo.length() > 150 || !ValidadorEntrada.esCorreoValido(correo)) {
            validadorCorreo.cancelar();
            return;
        }
        validadorCorreo.solicitar(
                correo,
                () -> !inicializando && txtCorreoPropietario.getScene() != null
                        && correo.equals(txtCorreoPropietario.getText() == null ? "" : txtCorreoPropietario.getText().strip()),
                () -> clienteValidaciones.correo(correo),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtCorreoPropietario, lblErrorCorreo, res.mensaje());
                    }
                },
                err -> { });
    }

    private void validarTelefonoEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        String tel = txtTelefonoPropietario.getText() == null ? "" : txtTelefonoPropietario.getText().strip();
        if (tel.isEmpty() || tel.length() > 20 || !ValidadorEntrada.esTelefonoValido(tel)) {
            validadorTelefono.cancelar();
            return;
        }
        validadorTelefono.solicitar(
                tel,
                () -> !inicializando && txtTelefonoPropietario.getScene() != null
                        && tel.equals(txtTelefonoPropietario.getText() == null ? "" : txtTelefonoPropietario.getText().strip()),
                () -> clienteValidaciones.telefono(tel),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtTelefonoPropietario, lblErrorTelefono, res.mensaje());
                    }
                },
                err -> { });
    }

    private String instantanea() {
        return txtNombre.getText() + "|" + txtDireccion.getText() + "|" + chkActiva.isSelected() + "|"
                + txtNombrePropietario.getText() + "|" + txtCorreoPropietario.getText() + "|" + txtTelefonoPropietario.getText();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(txtNombre, lblErrorNombre),
                new CampoValidado(txtDireccion, lblErrorDireccion),
                new CampoValidado(txtNombrePropietario, lblErrorPropietario),
                new CampoValidado(txtCorreoPropietario, lblErrorCorreo),
                new CampoValidado(txtTelefonoPropietario, lblErrorTelefono));
    }
}
