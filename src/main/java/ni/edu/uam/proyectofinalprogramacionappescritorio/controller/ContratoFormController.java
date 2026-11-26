package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorAsincrono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Campos, validación en vivo y datos del diálogo de contrato. */
public class ContratoFormController {
    public record Datos(UnidadResponse unidad, InquilinoResponse inquilino, LocalDate inicio, LocalDate fin,
                        BigDecimal canon, String moneda, int diaPago, BigDecimal deposito, String estado) { }

    @FXML private ComboBox<UnidadResponse> cmbUnidad;
    @FXML private ComboBox<InquilinoResponse> cmbInquilino;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private TextField txtCanon;
    @FXML private ComboBox<String> cmbMoneda;
    @FXML private TextField txtDiaPago;
    @FXML private TextField txtDeposito;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorPartes;
    @FXML private Label lblErrorFechas;
    @FXML private Label lblErrorCanon;
    @FXML private Label lblErrorDiaPago;
    @FXML private Label lblErrorDeposito;

    private final Set<Control> tocados = new HashSet<>();
    private final ValidadorAsincrono validadorRango = new ValidadorAsincrono();
    private final ValidadorAsincrono validadorCanon = new ValidadorAsincrono();
    private ClienteValidaciones clienteValidaciones;
    private boolean inicializando = true;
    private boolean editando;
    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbUnidad.setConverter(new StringConverter<>() {
            @Override public String toString(UnidadResponse u) {
                return u == null ? "" : u.codigo() + " · " + u.propiedadNombre();
            }
            @Override public UnidadResponse fromString(String s) { return null; }
        });
        cmbInquilino.setConverter(new StringConverter<>() {
            @Override public String toString(InquilinoResponse i) {
                return i == null ? "" : i.nombre();
            }
            @Override public InquilinoResponse fromString(String s) { return null; }
        });
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        cmbEstado.setItems(FXCollections.observableArrayList("Borrador", "Vigente", "Por vencer", "Vencido", "Terminado", "Cancelado"));
        cmbEstado.getSelectionModel().selectFirst();
        txtDiaPago.setText("5");
        cmbUnidad.valueProperty().addListener((obs, anterior, unidad) -> {
            if (unidad != null && !editando && !inicializando) {
                if (unidad.canonReferencia() != null) txtCanon.setText(unidad.canonReferencia());
                if (unidad.moneda() != null) cmbMoneda.setValue(unidad.moneda());
            }
        });
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
        marcarLimpio();
    }

    public void establecerValidadorServidor(ClienteValidaciones clienteValidaciones) {
        this.clienteValidaciones = clienteValidaciones;
    }

    public void establecerCatalogos(List<UnidadResponse> unidades, List<InquilinoResponse> inquilinos) {
        inicializando = true;
        try {
            cmbUnidad.setItems(FXCollections.observableArrayList(unidades));
            cmbInquilino.setItems(FXCollections.observableArrayList(inquilinos));
            if (!unidades.isEmpty()) {
                cmbUnidad.getSelectionModel().selectFirst();
                UnidadResponse primera = cmbUnidad.getValue();
                if (primera != null) {
                    if (primera.canonReferencia() != null) txtCanon.setText(primera.canonReferencia());
                    if (primera.moneda() != null) cmbMoneda.setValue(primera.moneda());
                }
            }
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    public void cargar(ContratoResponse contrato) {
        inicializando = true;
        editando = true;
        try {
            cmbUnidad.getItems().stream()
                    .filter(u -> Objects.equals(u.id(), contrato.unidadId()))
                    .findFirst().ifPresent(cmbUnidad::setValue);
            cmbInquilino.getItems().stream()
                    .filter(i -> Objects.equals(i.id(), contrato.inquilinoId()))
                    .findFirst().ifPresent(cmbInquilino::setValue);
            dpInicio.setValue(LocalDate.parse(contrato.inicio()));
            dpFin.setValue(LocalDate.parse(contrato.fin()));
            txtCanon.setText(contrato.canon() == null ? "" : contrato.canon());
            cmbMoneda.setValue(contrato.moneda());
            txtDiaPago.setText(String.valueOf(contrato.diaPago()));
            txtDeposito.setText(contrato.depositoPactado() == null ? "" : contrato.depositoPactado());
            cmbEstado.setValue(contrato.estado());
            tocados.clear();
            validadorRango.reiniciar();
            validadorCanon.reiniciar();
            limpiarErrores();
            marcarLimpio();
        } finally {
            editando = false;
            inicializando = false;
        }
    }

    public boolean validar() {
        tocados.addAll(List.of(cmbUnidad, cmbInquilino, dpInicio, dpFin, txtCanon, txtDiaPago, txtDeposito));
        boolean valido = validarPartes()
                & validarFechas()
                & validarCanon()
                & validarDiaPago()
                & validarDeposito();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(cmbUnidad, lblErrorPartes),
                    new CampoValidado(dpInicio, lblErrorFechas),
                    new CampoValidado(txtCanon, lblErrorCanon),
                    new CampoValidado(txtDiaPago, lblErrorDiaPago),
                    new CampoValidado(txtDeposito, lblErrorDeposito)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                    lblErrorPartes, lblErrorFechas, lblErrorCanon, lblErrorDiaPago, lblErrorDeposito);
        }
        return valido;
    }

    public Datos datos() {
        BigDecimal deposito = txtDeposito.getText() == null || txtDeposito.getText().isBlank()
                ? BigDecimal.ZERO : new BigDecimal(txtDeposito.getText().trim());
        return new Datos(cmbUnidad.getValue(), cmbInquilino.getValue(), dpInicio.getValue(), dpFin.getValue(),
                new BigDecimal(txtCanon.getText().trim()), cmbMoneda.getValue(),
                Integer.parseInt(txtDiaPago.getText().trim()), deposito, cmbEstado.getValue());
    }

    public void mostrarError(String mensaje) { lblResumenError.setText(mensaje); lblResumenError.setVisible(true); lblResumenError.setManaged(true); }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }

    private void configurarValidacionEnVivo() {
        ValidacionFormulario.vincularSelector(cmbUnidad, () -> alEditar(cmbUnidad, this::validarPartes));
        ValidacionFormulario.vincularSelector(cmbInquilino, () -> alEditar(cmbInquilino, this::validarPartes));
        ValidacionFormulario.vincularSelector(dpInicio, () -> {
            alEditar(dpInicio, this::validarFechas);
            validarRangoEnServidor();
        });
        ValidacionFormulario.vincularSelector(dpFin, () -> {
            alEditar(dpFin, this::validarFechas);
            validarRangoEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtCanon, () -> {
            alEditar(txtCanon, this::validarCanon);
            validarCanonEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtDiaPago, () -> alEditar(txtDiaPago, this::validarDiaPago));
        ValidacionFormulario.vincularTexto(txtDeposito, () -> alEditar(txtDeposito, this::validarDeposito));
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorPartes, lblErrorFechas, lblErrorCanon, lblErrorDiaPago, lblErrorDeposito);
    }

    private boolean validarPartes() {
        String msg = (cmbUnidad.getValue() == null || cmbInquilino.getValue() == null)
                ? "Selecciona una unidad y un inquilino activos." : null;
        if (msg == null) {
            ValidacionFormulario.marcarCampo(cmbUnidad, lblErrorPartes, null);
            ValidacionFormulario.marcarCampo(cmbInquilino, null, null);
        } else {
            ValidacionFormulario.marcarCampo(cmbUnidad.getValue() == null ? cmbUnidad : cmbInquilino, lblErrorPartes, msg);
            if (cmbUnidad.getValue() != null) cmbUnidad.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            if (cmbInquilino.getValue() != null) cmbInquilino.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
        }
        return msg == null;
    }

    private boolean validarFechas() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();
        String msg = null;
        if (inicio == null || fin == null) {
            msg = "Completa las fechas de inicio y finalización.";
        } else if (!fin.isAfter(inicio)) {
            msg = "La fecha de finalización debe ser posterior a la fecha de inicio.";
        } else if (fin.isBefore(inicio.plusMonths(1))) {
            msg = "La vigencia del contrato debe ser de al menos un mes.";
        }
        if (msg == null) {
            ValidacionFormulario.marcarCampo(dpInicio, lblErrorFechas, null);
            ValidacionFormulario.marcarCampo(dpFin, null, null);
        } else {
            ValidacionFormulario.marcarCampo(dpFin, lblErrorFechas, msg);
            if (inicio == null) {
                ValidacionFormulario.marcarCampo(dpInicio, lblErrorFechas, msg);
            } else {
                dpInicio.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            }
        }
        return msg == null;
    }

    private boolean validarCanon() {
        String msg = null;
        try {
            BigDecimal canon = new BigDecimal(txtCanon.getText().trim());
            if (canon.signum() <= 0 || canon.stripTrailingZeros().scale() > 2) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException error) {
            msg = "Escribe un canon mensual mayor que cero con hasta dos decimales.";
        }
        ValidacionFormulario.marcarCampo(txtCanon, lblErrorCanon, msg);
        return msg == null;
    }

    private boolean validarDiaPago() {
        String msg = null;
        try {
            int dia = Integer.parseInt(txtDiaPago.getText().trim());
            if (dia < 1 || dia > 31) throw new NumberFormatException();
        } catch (RuntimeException error) {
            msg = "El día de pago debe estar entre 1 y 31.";
        }
        ValidacionFormulario.marcarCampo(txtDiaPago, lblErrorDiaPago, msg);
        return msg == null;
    }

    private boolean validarDeposito() {
        String msg = null;
        try {
            BigDecimal deposito = txtDeposito.getText() == null || txtDeposito.getText().isBlank()
                    ? BigDecimal.ZERO : new BigDecimal(txtDeposito.getText().trim());
            if (deposito.signum() < 0 || deposito.stripTrailingZeros().scale() > 2) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException error) {
            msg = "El depósito debe ser cero o mayor con hasta dos decimales.";
        }
        ValidacionFormulario.marcarCampo(txtDeposito, lblErrorDeposito, msg);
        return msg == null;
    }

    private void validarRangoEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();
        if (inicio == null || fin == null || !fin.isAfter(inicio) || fin.isBefore(inicio.plusMonths(1))) {
            validadorRango.cancelar();
            return;
        }
        String clave = inicio + "|" + fin;
        validadorRango.solicitar(
                clave,
                () -> !inicializando && dpFin.getScene() != null
                        && Objects.equals(inicio, dpInicio.getValue())
                        && Objects.equals(fin, dpFin.getValue()),
                () -> clienteValidaciones.rangoFechas(inicio.toString(), fin.toString(), 1),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(dpFin, lblErrorFechas, res.mensaje());
                    }
                },
                err -> { });
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
        UnidadResponse u = cmbUnidad.getValue();
        InquilinoResponse i = cmbInquilino.getValue();
        return (u == null ? "" : u.id()) + "|" + (i == null ? "" : i.id())
                + "|" + dpInicio.getValue() + "|" + dpFin.getValue()
                + "|" + txtCanon.getText() + "|" + cmbMoneda.getValue() + "|" + txtDiaPago.getText()
                + "|" + txtDeposito.getText() + "|" + cmbEstado.getValue();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(cmbUnidad, lblErrorPartes),
                new CampoValidado(cmbInquilino, lblErrorPartes),
                new CampoValidado(dpInicio, lblErrorFechas),
                new CampoValidado(dpFin, lblErrorFechas),
                new CampoValidado(txtCanon, lblErrorCanon),
                new CampoValidado(txtDiaPago, lblErrorDiaPago),
                new CampoValidado(txtDeposito, lblErrorDeposito));
    }
}
