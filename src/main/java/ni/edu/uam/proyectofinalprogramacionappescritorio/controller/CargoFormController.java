package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Campos, validación y extracción de datos del diálogo de cargo. */
public class CargoFormController {
    public record Datos(Contrato contrato, String tipo, BigDecimal importe, String estado,
                        String concepto, LocalDate periodo, LocalDate emision, LocalDate vencimiento) { }

    @FXML private ComboBox<Contrato> cmbContrato;
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

    private String estadoInicial = "";

    @FXML
    public void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList("Renta", "Mora", "Reparación", "Servicio", "Otro"));
        cmbTipo.getSelectionModel().selectFirst();
        cmbEstado.setItems(FXCollections.observableArrayList("Pendiente", "Pagado", "Cancelado"));
        cmbEstado.getSelectionModel().selectFirst();
        LocalDate hoy = LocalDate.now(RelojService.ZONA_OPERATIVA);
        dpPeriodo.setValue(hoy.withDayOfMonth(1));
        dpFechaEmision.setValue(hoy);
        dpVenceEl.setValue(hoy.plusDays(5));
        limpiarErrores();
        marcarLimpio();
    }

    public void establecerContratos(List<Contrato> contratos) {
        cmbContrato.setItems(FXCollections.observableArrayList(contratos));
        if (!contratos.isEmpty()) cmbContrato.getSelectionModel().selectFirst();
        marcarLimpio();
    }

    public void cargar(Cargo cargo) {
        cmbTipo.setValue(cargo.getTipo());
        cmbEstado.setValue(cargo.getEstadoPersistido());
        txtImporte.setText(cargo.getImporte() == null ? "" : cargo.getImporte().toPlainString());
        txtConcepto.setText(cargo.getConcepto());
        dpPeriodo.setValue(cargo.getPeriodo());
        dpFechaEmision.setValue(cargo.getFechaEmision());
        dpVenceEl.setValue(cargo.getVenceEl());
        cmbContrato.getItems().stream()
                .filter(contrato -> contrato.getId().equals(cargo.getContratoId()))
                .findFirst().ifPresent(cmbContrato::setValue);
        limpiarErrores();
        marcarLimpio();
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        int cantidad = 0;
        if (cmbContrato.getValue() == null) {
            error(lblErrorContrato, "Selecciona el contrato.");
            cantidad++; valido = false;
        }
        BigDecimal importe = null;
        try {
            importe = new BigDecimal(txtImporte.getText().trim());
            if (importe.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorImporte, "Escribe un importe decimal mayor que cero.");
            cantidad++; valido = false;
        }
        if (txtConcepto.getText() == null || txtConcepto.getText().isBlank()) {
            error(lblErrorConcepto, "Escribe el concepto del cargo.");
            cantidad++; valido = false;
        }
        if (dpPeriodo.getValue() == null || dpFechaEmision.getValue() == null || dpVenceEl.getValue() == null
                || dpVenceEl.getValue().isBefore(dpFechaEmision.getValue())) {
            error(lblErrorFechas, "Completa las fechas y verifica que el vencimiento no preceda la emisión.");
            cantidad++; valido = false;
        }
        if (cantidad > 1) mostrarError("Revisa los campos señalados antes de guardar.");
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

    private String instantanea() {
        return String.valueOf(cmbContrato.getValue()) + "|" + cmbTipo.getValue() + "|"
                + txtImporte.getText() + "|" + cmbEstado.getValue() + "|" + txtConcepto.getText()
                + "|" + dpPeriodo.getValue() + "|" + dpFechaEmision.getValue() + "|" + dpVenceEl.getValue();
    }

    private void limpiarErrores() {
        for (Label etiqueta : List.of(lblResumenError, lblErrorContrato, lblErrorImporte,
                lblErrorConcepto, lblErrorFechas)) {
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

