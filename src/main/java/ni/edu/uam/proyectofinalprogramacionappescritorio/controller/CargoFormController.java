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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CargoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorAsincrono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Campos, validación en vivo y extracción de datos del diálogo de cargo. */
public class CargoFormController {
    public record Datos(ContratoResponse contrato, String tipo, BigDecimal importe, String estado,
                        String concepto, LocalDate periodo, LocalDate emision, LocalDate vencimiento) { }

    @FXML private ComboBox<ContratoResponse> cmbContrato;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtImporte;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField txtConcepto;
    @FXML private DatePicker dpPeriodo;
    @FXML private DatePicker dpFechaEmision;
    @FXML private DatePicker dpVenceEl;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorContrato;
    @FXML private Label lblErrorImporte;
    @FXML private Label lblErrorConcepto;
    @FXML private Label lblErrorFechas;

    private final Set<Control> tocados = new HashSet<>();
    private final ValidadorAsincrono validadorImporte = new ValidadorAsincrono();
    private ClienteValidaciones clienteValidaciones;
    private boolean inicializando = true;
    private String estadoInicial = "";

    @FXML
    public void initialize() {
        cmbContrato.setConverter(new StringConverter<>() {
            @Override public String toString(ContratoResponse c) {
                return c == null ? "" : c.inquilinoNombre() + " · " + c.unidadInfo();
            }
            @Override public ContratoResponse fromString(String s) { return null; }
        });
        cmbTipo.setItems(FXCollections.observableArrayList("Reparación", "Servicio", "Otro", "Renta"));
        cmbTipo.getSelectionModel().selectFirst();
        cmbEstado.setItems(FXCollections.observableArrayList("Pendiente", "Pagado", "Cancelado"));
        cmbEstado.getSelectionModel().selectFirst();
        LocalDate hoy = LocalDate.now(ConstantesLatch.ZONA_OPERATIVA);
        dpPeriodo.setValue(hoy.withDayOfMonth(1));
        dpFechaEmision.setValue(hoy);
        dpVenceEl.setValue(hoy.plusDays(5));
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
        marcarLimpio();
    }

    public void establecerValidadorServidor(ClienteValidaciones clienteValidaciones) {
        this.clienteValidaciones = clienteValidaciones;
    }

    public void establecerContratos(List<ContratoResponse> contratos) {
        inicializando = true;
        try {
            cmbContrato.setItems(FXCollections.observableArrayList(contratos));
            if (!contratos.isEmpty()) cmbContrato.getSelectionModel().selectFirst();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    public void cargar(CargoDetalleResponse cargo) {
        inicializando = true;
        try {
            if ("Mora".equals(cargo.tipo()) && !cmbTipo.getItems().contains("Mora")) {
                cmbTipo.getItems().add("Mora");
                cmbTipo.setDisable(true);
            }
            cmbTipo.setValue(cargo.tipo());
            cmbEstado.setValue(cargo.estado());
            txtImporte.setText(cargo.importe() == null ? "" : cargo.importe());
            txtConcepto.setText(cargo.concepto());
            dpPeriodo.setValue(parsearPeriodo(cargo.periodo()));
            dpFechaEmision.setValue(LocalDate.parse(cargo.fechaEmision()));
            dpVenceEl.setValue(LocalDate.parse(cargo.venceEl()));
            cmbContrato.getItems().stream()
                    .filter(contrato -> Objects.equals(contrato.id(), cargo.contratoId()))
                    .findFirst().ifPresent(cmbContrato::setValue);
            tocados.clear();
            validadorImporte.reiniciar();
            limpiarErrores();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    private static LocalDate parsearPeriodo(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now(ConstantesLatch.ZONA_OPERATIVA).withDayOfMonth(1);
        String limpio = raw.strip();
        return limpio.length() == 7 ? LocalDate.parse(limpio + "-01") : LocalDate.parse(limpio);
    }

    public boolean validar() {
        tocados.addAll(List.of(cmbContrato, txtImporte, txtConcepto, dpPeriodo, dpFechaEmision, dpVenceEl));
        boolean valido = validarContrato()
                & validarImporte()
                & validarConcepto()
                & validarFechas();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(cmbContrato, lblErrorContrato),
                    new CampoValidado(txtImporte, lblErrorImporte),
                    new CampoValidado(txtConcepto, lblErrorConcepto),
                    new CampoValidado(dpVenceEl, lblErrorFechas)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                    lblErrorContrato, lblErrorImporte, lblErrorConcepto, lblErrorFechas);
        }
        return valido;
    }

    public Datos datos() {
        return new Datos(cmbContrato.getValue(), cmbTipo.getValue(),
                new BigDecimal(txtImporte.getText().trim()), cmbEstado.getValue(),
                txtConcepto.getText().trim(), dpPeriodo.getValue().withDayOfMonth(1),
                dpFechaEmision.getValue(), dpVenceEl.getValue());
    }

    public void mostrarError(String mensaje) {
        lblResumenError.setText(mensaje);
        lblResumenError.setVisible(true);
        lblResumenError.setManaged(true);
    }

    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }

    private void configurarValidacionEnVivo() {
        ValidacionFormulario.vincularSelector(cmbContrato, () -> alEditar(cmbContrato, this::validarContrato));
        ValidacionFormulario.vincularTexto(txtImporte, () -> {
            alEditar(txtImporte, this::validarImporte);
            validarImporteEnServidor();
        });
        ValidacionFormulario.vincularTexto(txtConcepto, () -> alEditar(txtConcepto, this::validarConcepto));
        ValidacionFormulario.vincularSelector(dpPeriodo, () -> alEditar(dpPeriodo, this::validarFechas));
        ValidacionFormulario.vincularSelector(dpFechaEmision, () -> alEditar(dpFechaEmision, this::validarFechas));
        ValidacionFormulario.vincularSelector(dpVenceEl, () -> alEditar(dpVenceEl, this::validarFechas));
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorContrato, lblErrorImporte, lblErrorConcepto, lblErrorFechas);
    }

    private boolean validarContrato() {
        String msg = cmbContrato.getValue() == null ? "Selecciona el contrato." : null;
        ValidacionFormulario.marcarCampo(cmbContrato, lblErrorContrato, msg);
        return msg == null;
    }

    private boolean validarImporte() {
        String msg = null;
        try {
            BigDecimal importe = new BigDecimal(txtImporte.getText().trim());
            if (importe.compareTo(BigDecimal.ZERO) <= 0 || importe.stripTrailingZeros().scale() > 2) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException error) {
            msg = "Escribe un importe decimal mayor que cero con hasta dos decimales.";
        }
        ValidacionFormulario.marcarCampo(txtImporte, lblErrorImporte, msg);
        return msg == null;
    }

    private boolean validarConcepto() {
        String valor = txtConcepto.getText();
        String msg = null;
        if (valor == null || valor.isBlank()) {
            msg = "Escribe el concepto del cargo.";
        } else if (valor.strip().length() > 255) {
            msg = "El concepto admite hasta 255 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtConcepto, lblErrorConcepto, msg);
        return msg == null;
    }

    private boolean validarFechas() {
        LocalDate periodo = dpPeriodo.getValue();
        LocalDate emision = dpFechaEmision.getValue();
        LocalDate vence = dpVenceEl.getValue();
        String msg = null;
        if (periodo == null || emision == null || vence == null) {
            msg = "Completa el periodo, la fecha de emisión y el vencimiento.";
        } else if (vence.isBefore(emision)) {
            msg = "La fecha de vencimiento debe ser igual o posterior a la fecha de emisión.";
        }
        if (msg == null) {
            ValidacionFormulario.marcarCampo(dpPeriodo, lblErrorFechas, null);
            ValidacionFormulario.marcarCampo(dpFechaEmision, null, null);
            ValidacionFormulario.marcarCampo(dpVenceEl, null, null);
        } else {
            ValidacionFormulario.marcarCampo(dpVenceEl, lblErrorFechas, msg);
            if (periodo == null) {
                if (!dpPeriodo.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                    dpPeriodo.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                }
            } else {
                dpPeriodo.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            }
            if (emision == null) {
                if (!dpFechaEmision.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                    dpFechaEmision.getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                }
            } else {
                dpFechaEmision.getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
            }
        }
        return msg == null;
    }

    private void validarImporteEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        String monto = txtImporte.getText() == null ? "" : txtImporte.getText().trim();
        if (monto.isEmpty()) {
            validadorImporte.cancelar();
            return;
        }
        try {
            BigDecimal importe = new BigDecimal(monto);
            if (importe.compareTo(BigDecimal.ZERO) <= 0 || importe.stripTrailingZeros().scale() > 2) {
                validadorImporte.cancelar();
                return;
            }
        } catch (RuntimeException ex) {
            validadorImporte.cancelar();
            return;
        }
        validadorImporte.solicitar(
                monto,
                () -> !inicializando && txtImporte.getScene() != null
                        && monto.equals(txtImporte.getText() == null ? "" : txtImporte.getText().trim()),
                () -> clienteValidaciones.monto(monto),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtImporte, lblErrorImporte, res.mensaje());
                    }
                },
                err -> { });
    }

    private String instantanea() {
        ContratoResponse c = cmbContrato.getValue();
        return (c == null ? "" : c.id()) + "|" + cmbTipo.getValue() + "|"
                + txtImporte.getText() + "|" + cmbEstado.getValue() + "|" + txtConcepto.getText()
                + "|" + dpPeriodo.getValue() + "|" + dpFechaEmision.getValue() + "|" + dpVenceEl.getValue();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(cmbContrato, lblErrorContrato),
                new CampoValidado(txtImporte, lblErrorImporte),
                new CampoValidado(txtConcepto, lblErrorConcepto),
                new CampoValidado(dpPeriodo, lblErrorFechas),
                new CampoValidado(dpFechaEmision, lblErrorFechas),
                new CampoValidado(dpVenceEl, lblErrorFechas));
    }
}
