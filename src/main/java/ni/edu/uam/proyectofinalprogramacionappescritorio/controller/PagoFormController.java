package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Campos, validación y revisión visual del formulario de pago. */
public class PagoFormController {
    public record Datos(Contrato contrato, BigDecimal monto, String moneda, LocalDate fecha,
                        String metodo, String referencia, String notas) { }

    @FXML private ComboBox<Contrato> cmbContrato;
    @FXML private TextField txtMonto;
    @FXML private ComboBox<String> cmbMoneda;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<String> cmbMetodo;
    @FXML private TextField txtReferencia;
    @FXML private TextField txtNotas;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorContrato;
    @FXML private Label lblErrorMonto;
    @FXML private Label lblErrorFecha;
    @FXML private Label lblErrorReferencia;
    @FXML private VBox panelEdicion;
    @FXML private VBox panelRevision;
    @FXML private Label lblRevisionContrato;
    @FXML private Label lblRevisionImporte;
    @FXML private Label lblRevisionFecha;
    @FXML private Label lblRevisionMetodo;
    @FXML private Label lblRevisionReferencia;
    @FXML private Label lblRevisionNotas;

    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        cmbMetodo.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia", "Depósito bancario", "Otro"));
        cmbMetodo.getSelectionModel().selectFirst();
        dpFechaPago.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
        mostrarEdicion();
        limpiarErrores();
        marcarLimpio();
    }

    public void establecerContratos(List<Contrato> contratos) {
        cmbContrato.setItems(FXCollections.observableArrayList(contratos));
        cmbContrato.valueProperty().addListener((obs, anterior, contrato) -> {
            if (contrato == null) return;
            if (contrato.getCanon() != null) txtMonto.setText(contrato.getCanon().toPlainString());
            if (contrato.getMoneda() != null) cmbMoneda.setValue(contrato.getMoneda());
        });
        if (!contratos.isEmpty()) cmbContrato.getSelectionModel().selectFirst();
        marcarLimpio();
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        int errores = 0;
        if (cmbContrato.getValue() == null) {
            error(lblErrorContrato, "Selecciona el contrato al que se aplicará el pago.");
            errores++; valido = false;
        }
        try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
            if (monto.signum() <= 0 || monto.abs().compareTo(new BigDecimal("1000000000000")) > 0
                    || monto.stripTrailingZeros().scale() > 2) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorMonto, "Escribe un monto positivo con hasta dos decimales.");
            errores++; valido = false;
        }
        if (dpFechaPago.getValue() == null) {
            error(lblErrorFecha, "Selecciona la fecha del pago.");
            errores++; valido = false;
        }
        String referencia = txtReferencia.getText();
        if (referencia != null && referencia.length() > 100) {
            error(lblErrorReferencia, "La referencia admite hasta 100 caracteres.");
            errores++; valido = false;
        }
        if (!valido) ValidacionFormulario.mostrar(lblResumenError, lblErrorContrato, lblErrorMonto, lblErrorFecha, lblErrorReferencia);
        return valido;
    }

    public Datos datos() {
        String referencia = txtReferencia.getText();
        String notas = txtNotas.getText();
        return new Datos(cmbContrato.getValue(), new BigDecimal(txtMonto.getText().trim()),
                cmbMoneda.getValue(), dpFechaPago.getValue(), cmbMetodo.getValue(),
                referencia == null || referencia.isBlank() ? null : referencia.trim(),
                notas == null || notas.isBlank() ? null : notas.trim());
    }

    public void mostrarRevision() {
        Datos datos = datos();
        lblRevisionContrato.setText(datos.contrato().toString());
        lblRevisionImporte.setText(String.format("%.2f %s", datos.monto(), datos.moneda()));
        lblRevisionFecha.setText(datos.fecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblRevisionMetodo.setText(datos.metodo());
        lblRevisionReferencia.setText(datos.referencia() == null ? "Sin referencia" : datos.referencia());
        lblRevisionNotas.setText(datos.notas() == null ? "Sin observaciones" : datos.notas());
        panelEdicion.setVisible(false);
        panelEdicion.setManaged(false);
        panelRevision.setVisible(true);
        panelRevision.setManaged(true);
        lblResumenError.setText("");
        lblResumenError.setVisible(false);
        lblResumenError.setManaged(false);
    }

    @FXML public void mostrarEdicion() {
        panelRevision.setVisible(false);
        panelRevision.setManaged(false);
        panelEdicion.setVisible(true);
        panelEdicion.setManaged(true);
    }

    public void mostrarError(String mensaje) {
        lblResumenError.setText(mensaje);
        lblResumenError.setVisible(true);
        lblResumenError.setManaged(true);
    }

    public void marcarLimpio() { estadoInicial = instantanea(); }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }

    private String instantanea() {
        return String.valueOf(cmbContrato.getValue()) + "|" + txtMonto.getText() + "|" + cmbMoneda.getValue()
                + "|" + dpFechaPago.getValue() + "|" + cmbMetodo.getValue() + "|"
                + txtReferencia.getText() + "|" + txtNotas.getText();
    }

    private void limpiarErrores() {
        for (Label etiqueta : List.of(lblResumenError, lblErrorContrato, lblErrorMonto, lblErrorFecha, lblErrorReferencia)) {
            etiqueta.setText("");
            etiqueta.setVisible(false);
            etiqueta.setManaged(true);
        }
    }

    private void error(Label etiqueta, String mensaje) {
        etiqueta.setText(mensaje);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
    }
}
