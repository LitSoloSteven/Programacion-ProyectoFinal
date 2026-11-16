package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Campos, validación y datos del diálogo de contrato. */
public class ContratoFormController {
    public record Datos(Unidad unidad, Inquilino inquilino, LocalDate inicio, LocalDate fin,
                        BigDecimal canon, String moneda, int diaPago, BigDecimal deposito, String estado) { }

    @FXML private ComboBox<Unidad> cmbUnidad;
    @FXML private ComboBox<Inquilino> cmbInquilino;
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

    private boolean editando;
    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        cmbEstado.setItems(FXCollections.observableArrayList("Vigente", "Borrador", "Por vencer", "Vencido", "Terminado", "Cancelado"));
        cmbEstado.getSelectionModel().selectFirst();
        txtDiaPago.setText("5");
        cmbUnidad.valueProperty().addListener((obs, anterior, unidad) -> {
            if (unidad != null && !editando) {
                if (unidad.getCanonReferencia() != null) txtCanon.setText(unidad.getCanonReferencia().toPlainString());
                if (unidad.getMoneda() != null) cmbMoneda.setValue(unidad.getMoneda());
            }
        });
        limpiarErrores();
        marcarLimpio();
    }

    public void establecerCatalogos(List<Unidad> unidades, List<Inquilino> inquilinos) {
        cmbUnidad.setItems(FXCollections.observableArrayList(unidades));
        cmbInquilino.setItems(FXCollections.observableArrayList(inquilinos));
        if (!unidades.isEmpty()) cmbUnidad.getSelectionModel().selectFirst();
        marcarLimpio();
    }

    public void cargar(Contrato contrato) {
        editando = true;
        cmbUnidad.getItems().stream().filter(u -> u.getId().equals(contrato.getUnidadId())).findFirst().ifPresent(cmbUnidad::setValue);
        cmbInquilino.getItems().stream().filter(i -> i.getId().equals(contrato.getInquilinoId())).findFirst().ifPresent(cmbInquilino::setValue);
        dpInicio.setValue(contrato.getInicio());
        dpFin.setValue(contrato.getFin());
        txtCanon.setText(contrato.getCanon() == null ? "" : contrato.getCanon().toPlainString());
        cmbMoneda.setValue(contrato.getMoneda());
        txtDiaPago.setText(String.valueOf(contrato.getDiaPago()));
        txtDeposito.setText(contrato.getDepositoPactado() == null ? "" : contrato.getDepositoPactado().toPlainString());
        cmbEstado.setValue(contrato.getEstado());
        editando = false;
        limpiarErrores();
        marcarLimpio();
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        int errores = 0;
        if (cmbUnidad.getValue() == null || cmbInquilino.getValue() == null) {
            error(lblErrorPartes, "Selecciona una unidad y un inquilino activos."); errores++; valido = false;
        }
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();
        if (inicio == null || fin == null || !fin.isAfter(inicio) || fin.isBefore(inicio.plusMonths(1))) {
            error(lblErrorFechas, "Completa las fechas y verifica una vigencia mínima de un mes."); errores++; valido = false;
        }
        try {
            BigDecimal canon = new BigDecimal(txtCanon.getText().trim());
            if (canon.signum() <= 0) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorCanon, "Escribe un canon mensual mayor que cero."); errores++; valido = false;
        }
        try {
            int dia = Integer.parseInt(txtDiaPago.getText().trim());
            if (dia < 1 || dia > 31) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorDiaPago, "El día de pago debe estar entre 1 y 31."); errores++; valido = false;
        }
        try {
            BigDecimal deposito = txtDeposito.getText() == null || txtDeposito.getText().isBlank()
                    ? BigDecimal.ZERO : new BigDecimal(txtDeposito.getText().trim());
            if (deposito.signum() < 0) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorDeposito, "El depósito debe ser cero o mayor."); errores++; valido = false;
        }
        if (errores > 1) mostrarError("Revisa los campos señalados antes de guardar.");
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
    private String instantanea() {
        return cmbUnidad.getValue() + "|" + cmbInquilino.getValue() + "|" + dpInicio.getValue() + "|" + dpFin.getValue()
                + "|" + txtCanon.getText() + "|" + cmbMoneda.getValue() + "|" + txtDiaPago.getText()
                + "|" + txtDeposito.getText() + "|" + cmbEstado.getValue();
    }
    private void limpiarErrores() {
        for (Label label : List.of(lblResumenError, lblErrorPartes, lblErrorFechas, lblErrorCanon, lblErrorDiaPago, lblErrorDeposito)) {
            label.setText(""); label.setVisible(false); label.setManaged(true);
        }
    }
    private void error(Label label, String mensaje) { label.setText(mensaje); label.setVisible(true); label.setManaged(true); }
}
