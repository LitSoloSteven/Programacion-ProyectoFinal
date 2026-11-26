package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.ValidadorEntrada;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorAsincrono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Campos, validación en vivo y datos del formulario de inquilino. */
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
    @FXML private Label lblAvisoIdentificacion;
    @FXML private Label lblErrorTelefono;
    @FXML private Label lblErrorCorreo;
    @FXML private Label lblErrorEmergencia;

    private final Set<Control> tocados = new HashSet<>();
    private final ValidadorAsincrono validadorCorreo = new ValidadorAsincrono();
    private final ValidadorAsincrono validadorTelefono = new ValidadorAsincrono();
    private final ValidadorAsincrono validadorIdentificacion = new ValidadorAsincrono();
    private ClienteValidaciones clienteValidaciones;
    private ClienteCatalogos clienteCatalogos;
    private UUID inquilinoIdExcluido;
    private boolean inicializando = true;
    private String estadoInicial = "";

    @FXML public void initialize() {
        chkActivo.setSelected(true);
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
        marcarLimpio();
    }

    public void establecerValidadorServidor(ClienteValidaciones clienteValidaciones,
                                            ClienteCatalogos clienteCatalogos,
                                            UUID inquilinoIdExcluido) {
        this.clienteValidaciones = clienteValidaciones;
        this.clienteCatalogos = clienteCatalogos;
        this.inquilinoIdExcluido = inquilinoIdExcluido;
    }

    public void cargar(InquilinoResponse inquilino) {
        inicializando = true;
        try {
            txtNombre.setText(inquilino.nombre());
            txtIdentificacion.setText(inquilino.identificacion());
            txtTelefono.setText(inquilino.telefono());
            txtCorreo.setText(inquilino.correo());
            cargarContactoEmergencia(inquilino.contactoEmergencia());
            chkActivo.setSelected(inquilino.activo());
            tocados.clear();
            validadorCorreo.reiniciar();
            validadorTelefono.reiniciar();
            validadorIdentificacion.reiniciar();
            limpiarErrores();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
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
        tocados.addAll(List.of(txtNombre, txtIdentificacion, txtTelefono, txtCorreo,
                txtEmergenciaNombre, txtEmergenciaTelefono));
        boolean valido = validarNombre()
                & validarIdentificacion()
                & validarTelefono()
                & validarCorreo()
                & validarEmergencia();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(txtNombre, lblErrorNombre),
                    new CampoValidado(txtIdentificacion, lblErrorIdentificacion),
                    new CampoValidado(txtTelefono, lblErrorTelefono),
                    new CampoValidado(txtCorreo, lblErrorCorreo),
                    new CampoValidado(txtEmergenciaTelefono, lblErrorEmergencia)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                    lblErrorNombre, lblErrorIdentificacion, lblErrorTelefono, lblErrorCorreo, lblErrorEmergencia);
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

    private void configurarValidacionEnVivo() {
        ValidacionFormulario.vincularTexto(txtNombre, () -> alEditar(txtNombre, this::validarNombre));
        ValidacionFormulario.vincularTexto(txtIdentificacion, () -> {
            alEditar(txtIdentificacion, this::validarIdentificacion);
            buscarCoincidenciasIdentificacion();
        });
        ValidacionFormulario.vincularTexto(txtTelefono, () -> {
            alEditar(txtTelefono, this::validarTelefono);
            validarTelefonoEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtCorreo, () -> {
            alEditar(txtCorreo, this::validarCorreo);
            validarCorreoEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtEmergenciaNombre, () -> alEditar(txtEmergenciaNombre, this::validarEmergencia));
        ValidacionFormulario.vincularTexto(txtEmergenciaTelefono, () -> alEditar(txtEmergenciaTelefono, this::validarEmergencia));
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorNombre, lblErrorIdentificacion, lblErrorTelefono, lblErrorCorreo, lblErrorEmergencia);
    }

    private boolean validarNombre() {
        String nombre = txtNombre.getText();
        String msg = null;
        if (nombre == null || nombre.isBlank()) {
            msg = "Escribe el nombre completo.";
        } else if (nombre.strip().length() > 160) {
            msg = "El nombre admite hasta 160 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtNombre, lblErrorNombre, msg);
        return msg == null;
    }

    private boolean validarIdentificacion() {
        String ident = txtIdentificacion.getText();
        String msg = ident != null && ident.strip().length() > 40
                ? "La identificación admite hasta 40 caracteres." : null;
        ValidacionFormulario.marcarCampo(txtIdentificacion, lblErrorIdentificacion, msg);
        return msg == null;
    }

    private boolean validarTelefono() {
        String msg = !ValidadorEntrada.esTelefonoValido(txtTelefono.getText())
                ? "Escribe un teléfono válido con 7 a 15 dígitos; puedes usar +, espacios o guiones."
                : null;
        ValidacionFormulario.marcarCampo(txtTelefono, lblErrorTelefono, msg);
        return msg == null;
    }

    private boolean validarCorreo() {
        String correo = txtCorreo.getText();
        String msg = null;
        if (correo != null && !correo.isBlank()
                && (correo.strip().length() > 254 || !ValidadorEntrada.esCorreoValido(correo.strip()))) {
            msg = "Escribe un correo válido de hasta 254 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtCorreo, lblErrorCorreo, msg);
        return msg == null;
    }

    private boolean validarEmergencia() {
        String emergTel = txtEmergenciaTelefono.getText();
        String msg = null;
        if (emergTel != null && !emergTel.isBlank() && !ValidadorEntrada.esTelefonoValido(emergTel)) {
            msg = "El teléfono de emergencia debe tener de 7 a 15 dígitos.";
        } else {
            String combinado = combinarEmergencia();
            if (combinado != null && combinado.length() > 200) {
                msg = "El contacto de emergencia admite hasta 200 caracteres.";
            }
        }
        ValidacionFormulario.marcarCampo(txtEmergenciaTelefono, lblErrorEmergencia, msg);
        return msg == null;
    }

    private void validarCorreoEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().strip();
        if (correo.isEmpty() || correo.length() > 254 || !ValidadorEntrada.esCorreoValido(correo)) {
            validadorCorreo.cancelar();
            return;
        }
        validadorCorreo.solicitar(
                correo,
                () -> !inicializando && txtCorreo.getScene() != null
                        && correo.equals(txtCorreo.getText() == null ? "" : txtCorreo.getText().strip()),
                () -> clienteValidaciones.correo(correo),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtCorreo, lblErrorCorreo, res.mensaje());
                    }
                },
                err -> { });
    }

    private void validarTelefonoEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        String tel = txtTelefono.getText() == null ? "" : txtTelefono.getText().strip();
        if (tel.isEmpty() || !ValidadorEntrada.esTelefonoValido(tel)) {
            validadorTelefono.cancelar();
            return;
        }
        validadorTelefono.solicitar(
                tel,
                () -> !inicializando && txtTelefono.getScene() != null
                        && tel.equals(txtTelefono.getText() == null ? "" : txtTelefono.getText().strip()),
                () -> clienteValidaciones.telefono(tel),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtTelefono, lblErrorTelefono, res.mensaje());
                    }
                },
                err -> { });
    }

    private void buscarCoincidenciasIdentificacion() {
        if (lblAvisoIdentificacion == null) return;
        String ident = txtIdentificacion.getText() == null ? "" : txtIdentificacion.getText().strip();
        if (inicializando || clienteCatalogos == null || ident.length() < 4 || ident.length() > 40) {
            validadorIdentificacion.cancelar();
            ocultarAvisoIdentificacion();
            return;
        }
        validadorIdentificacion.solicitar(
                ident,
                () -> !inicializando && txtIdentificacion.getScene() != null
                        && ident.equals(txtIdentificacion.getText() == null ? "" : txtIdentificacion.getText().strip()),
                () -> clienteCatalogos.coincidenciasIdentificacion(ident, inquilinoIdExcluido),
                lista -> {
                    if (lista == null || lista.isEmpty()) {
                        ocultarAvisoIdentificacion();
                    } else {
                        InquilinoResponse previo = lista.getFirst();
                        lblAvisoIdentificacion.setText("Ya existe un expediente con esa identificación: "
                                + previo.nombre() + ".");
                        lblAvisoIdentificacion.setVisible(true);
                        lblAvisoIdentificacion.setManaged(true);
                    }
                },
                err -> ocultarAvisoIdentificacion());
    }

    private void ocultarAvisoIdentificacion() {
        if (lblAvisoIdentificacion != null) {
            lblAvisoIdentificacion.setText("");
            lblAvisoIdentificacion.setVisible(false);
            lblAvisoIdentificacion.setManaged(false);
        }
    }

    private String instantanea() {
        return txtNombre.getText() + "|" + txtIdentificacion.getText() + "|" + txtTelefono.getText() + "|"
                + txtCorreo.getText() + "|" + txtEmergenciaNombre.getText() + "|"
                + txtEmergenciaTelefono.getText() + "|" + chkActivo.isSelected();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(txtNombre, lblErrorNombre),
                new CampoValidado(txtIdentificacion, lblErrorIdentificacion),
                new CampoValidado(txtTelefono, lblErrorTelefono),
                new CampoValidado(txtCorreo, lblErrorCorreo),
                new CampoValidado(txtEmergenciaTelefono, lblErrorEmergencia));
        ocultarAvisoIdentificacion();
    }

    private static String opcional(String texto) { return texto == null || texto.isBlank() ? null : texto.strip(); }
}
