package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorAsincrono;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Campos y validación en vivo del diálogo de unidad. */
public class UnidadFormController {
    public record Datos(PropiedadResponse propiedad, String codigo, String descripcion, String tipo,
                        String estado, BigDecimal canon, String moneda) { }

    @FXML private ComboBox<PropiedadResponse> cmbPropiedad;
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

    private final Set<Control> tocados = new HashSet<>();
    private final ValidadorAsincrono validadorCanon = new ValidadorAsincrono();
    private ClienteValidaciones clienteValidaciones;
    private boolean inicializando = true;
    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbPropiedad.setConverter(new StringConverter<>() {
            @Override public String toString(PropiedadResponse p) {
                return p == null ? "" : p.nombre();
            }
            @Override public PropiedadResponse fromString(String s) { return null; }
        });
        cmbTipo.setItems(FXCollections.observableArrayList("Apartamento", "Casa", "Cuarto", "Local comercial"));
        cmbTipo.getSelectionModel().selectFirst();
        cmbEstado.setItems(FXCollections.observableArrayList("Disponible", "En mantenimiento", "Inactiva"));
        cmbEstado.getSelectionModel().selectFirst();
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
        marcarLimpio();
    }

    public void establecerValidadorServidor(ClienteValidaciones clienteValidaciones) {
        this.clienteValidaciones = clienteValidaciones;
    }

    public void establecerPropiedades(List<PropiedadResponse> propiedades) {
        inicializando = true;
        try {
            cmbPropiedad.setItems(FXCollections.observableArrayList(propiedades));
            if (!propiedades.isEmpty()) cmbPropiedad.getSelectionModel().selectFirst();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    public void cargar(UnidadResponse unidad) {
        inicializando = true;
        try {
            txtCodigo.setText(unidad.codigo());
            txtDescripcion.setText(unidad.descripcion());
            cmbTipo.setValue(unidad.tipo());
            cmbEstado.setValue(unidad.estado());
            txtCanon.setText(unidad.canonReferencia() == null ? "" : unidad.canonReferencia());
            cmbMoneda.setValue(unidad.moneda());
            cmbPropiedad.getItems().stream()
                    .filter(p -> Objects.equals(p.id(), unidad.propiedadId()))
                    .findFirst().ifPresent(cmbPropiedad::setValue);
            tocados.clear();
            validadorCanon.reiniciar();
            limpiarErrores();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    public void aplicarModoAsistente(boolean soloCondiciones) {
        cmbPropiedad.setDisable(soloCondiciones);
        txtCodigo.setDisable(soloCondiciones);
        cmbTipo.setDisable(soloCondiciones);
        cmbEstado.setDisable(soloCondiciones);
    }

    public boolean validar() {
        tocados.addAll(List.of(cmbPropiedad, txtCodigo, txtCanon, txtDescripcion));
        boolean valido = validarPropiedad()
                & validarCodigo()
                & validarCanon()
                & validarDescripcion();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(cmbPropiedad, lblErrorPropiedad),
                    new CampoValidado(txtCodigo, lblErrorCodigo),
                    new CampoValidado(txtCanon, lblErrorCanon),
                    new CampoValidado(txtDescripcion, lblErrorDescripcion)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                    lblErrorPropiedad, lblErrorCodigo, lblErrorCanon, lblErrorDescripcion);
        }
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

    private void configurarValidacionEnVivo() {
        ValidacionFormulario.vincularSelector(cmbPropiedad, () -> alEditar(cmbPropiedad, this::validarPropiedad));
        ValidacionFormulario.vincularTexto(txtCodigo, () -> alEditar(txtCodigo, this::validarCodigo));
        ValidacionFormulario.vincularTexto(txtCanon, () -> {
            alEditar(txtCanon, this::validarCanon);
            validarCanonEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtDescripcion, () -> alEditar(txtDescripcion, this::validarDescripcion));
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorPropiedad, lblErrorCodigo, lblErrorCanon, lblErrorDescripcion);
    }

    private boolean validarPropiedad() {
        String msg = cmbPropiedad.getValue() == null ? "Selecciona una propiedad activa." : null;
        ValidacionFormulario.marcarCampo(cmbPropiedad, lblErrorPropiedad, msg);
        return msg == null;
    }

    private boolean validarCodigo() {
        String valor = txtCodigo.getText();
        String msg = null;
        if (valor == null || valor.isBlank()) {
            msg = "Escribe el código de la unidad.";
        } else if (valor.strip().length() > 30) {
            msg = "El código admite hasta 30 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtCodigo, lblErrorCodigo, msg);
        return msg == null;
    }

    private boolean validarCanon() {
        String msg = null;
        try {
            String raw = txtCanon.getText() == null ? "" : txtCanon.getText().trim();
            BigDecimal canon = new BigDecimal(raw);
            if (canon.signum() <= 0 || canon.stripTrailingZeros().scale() > 2) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException error) {
            msg = "Escribe un canon positivo con hasta dos decimales.";
        }
        ValidacionFormulario.marcarCampo(txtCanon, lblErrorCanon, msg);
        return msg == null;
    }

    private boolean validarDescripcion() {
        String descripcion = txtDescripcion.getText();
        String msg = descripcion != null && descripcion.length() > 255
                ? "La descripción admite hasta 255 caracteres." : null;
        ValidacionFormulario.marcarCampo(txtDescripcion, lblErrorDescripcion, msg);
        return msg == null;
    }

    private void validarCanonEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        String monto = txtCanon.getText() == null ? "" : txtCanon.getText().trim();
        if (monto.isEmpty()) {
            validadorCanon.cancelar();
            return;
        }
        try {
            BigDecimal canon = new BigDecimal(monto);
            if (canon.signum() <= 0 || canon.stripTrailingZeros().scale() > 2) {
                validadorCanon.cancelar();
                return;
            }
        } catch (RuntimeException ex) {
            validadorCanon.cancelar();
            return;
        }
        validadorCanon.solicitar(
                monto,
                () -> !inicializando && txtCanon.getScene() != null
                        && monto.equals(txtCanon.getText() == null ? "" : txtCanon.getText().trim()),
                () -> clienteValidaciones.monto(monto),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtCanon, lblErrorCanon, res.mensaje());
                    }
                },
                err -> { });
    }

    private String instantanea() {
        PropiedadResponse p = cmbPropiedad.getValue();
        return (p == null ? "" : p.id()) + "|" + txtCodigo.getText() + "|" + txtDescripcion.getText() + "|"
                + cmbTipo.getValue() + "|" + cmbEstado.getValue() + "|" + txtCanon.getText() + "|" + cmbMoneda.getValue();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(cmbPropiedad, lblErrorPropiedad),
                new CampoValidado(txtCodigo, lblErrorCodigo),
                new CampoValidado(txtCanon, lblErrorCanon),
                new CampoValidado(txtDescripcion, lblErrorDescripcion));
    }
}
