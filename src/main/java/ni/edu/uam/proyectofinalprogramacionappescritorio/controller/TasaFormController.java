package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTasa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Campos y validación de los dos diálogos del módulo de tasas. */
public class TasaFormController {
    public record Datos(LocalDate fecha, BigDecimal tasa, String fuente) { }

    @FXML private VBox panelRegistro;
    @FXML private VBox panelAutorizacion;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtTasa;
    @FXML private TextField txtFuente;
    @FXML private TextField txtMotivo;
    @FXML private Label lblTasaSeleccionada;
    @FXML private Label lblFuenteSeleccionada;
    @FXML private Label lblBandaInformativa;
    @FXML private Label lblResumenError;
    @FXML private Label lblErrorFecha;
    @FXML private Label lblErrorTasa;
    @FXML private Label lblErrorFuente;
    @FXML private Label lblErrorMotivo;

    private TasaResponse tasaSeleccionada;
    private String estadoInicial = "";

    @FXML public void initialize() {
        dpFecha.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
        lblBandaInformativa.setText("Banda de referencia: "
                + FormatoTasa.formatear(TipoCambio.BANDA_MINIMA) + "–"
                + FormatoTasa.formatear(TipoCambio.BANDA_MAXIMA) + " NIO/USD.");
        mostrarRegistro();
        limpiarErrores();
        marcarLimpio();
    }

    public void mostrarRegistro() {
        tasaSeleccionada = null;
        panelAutorizacion.setVisible(false);
        panelAutorizacion.setManaged(false);
        panelRegistro.setVisible(true);
        panelRegistro.setManaged(true);
    }

    public void mostrarAutorizacion(TasaResponse tasa) {
        tasaSeleccionada = tasa;
        panelRegistro.setVisible(false);
        panelRegistro.setManaged(false);
        panelAutorizacion.setVisible(true);
        panelAutorizacion.setManaged(true);
        lblTasaSeleccionada.setText(fechaVisible(tasa.fecha()) + " · "
                + FormatoTasa.formatear(new BigDecimal(tasa.nioPorUsd())) + " NIO/USD");
        lblFuenteSeleccionada.setText("Fuente: " + tasa.fuente());
        txtMotivo.clear();
        limpiarErrores();
        marcarLimpio();
    }

    public boolean validarRegistro() {
        limpiarErrores();
        boolean valido = true;
        int errores = 0;
        if (dpFecha.getValue() == null) {
            error(lblErrorFecha, "Selecciona la fecha de la tasa.");
            errores++; valido = false;
        } else if (dpFecha.getValue().isAfter(LocalDate.now(RelojService.ZONA_OPERATIVA))) {
            error(lblErrorFecha, "La tasa no puede corresponder a una fecha futura.");
            errores++; valido = false;
        }
        try {
            BigDecimal valor = new BigDecimal(txtTasa.getText().strip().replace(',', '.'));
            if (valor.signum() <= 0) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorTasa, "Escribe una tasa decimal mayor que cero.");
            errores++; valido = false;
        }
        if (txtFuente.getText() == null || txtFuente.getText().isBlank()) {
            error(lblErrorFuente, "Indica la fuente de la tasa.");
            errores++; valido = false;
        } else if (txtFuente.getText().length() > 500) {
            error(lblErrorFuente, "La fuente admite hasta 500 caracteres.");
            errores++; valido = false;
        }
        if (errores > 1) mostrarError("Revisa los campos señalados antes de continuar.");
        return valido;
    }

    public boolean validarMotivo() {
        limpiarErrores();
        if (txtMotivo.getText() == null || txtMotivo.getText().isBlank()) {
            error(lblErrorMotivo, "Escribe el motivo de autorización.");
            return false;
        }
        if (txtMotivo.getText().length() > 500) {
            error(lblErrorMotivo, "El motivo admite hasta 500 caracteres.");
            return false;
        }
        return true;
    }

    public Datos datos() {
        return new Datos(dpFecha.getValue(), new BigDecimal(txtTasa.getText().strip().replace(',', '.')),
                txtFuente.getText().strip());
    }

    public String motivo() { return txtMotivo.getText().strip(); }
    public void enlazarMotivoConAccion(Button accion) {
        accion.setDisable(txtMotivo.getText() == null || txtMotivo.getText().isBlank());
        txtMotivo.textProperty().addListener((obs, anterior, actual) ->
                accion.setDisable(actual == null || actual.isBlank()));
    }
    public TasaResponse tasaSeleccionada() { return tasaSeleccionada; }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }

    public void mostrarError(String mensaje) {
        lblResumenError.setText(mensaje);
        lblResumenError.setVisible(true);
        lblResumenError.setManaged(true);
    }

    private String instantanea() {
        return dpFecha.getValue() + "|" + txtTasa.getText() + "|" + txtFuente.getText() + "|" + txtMotivo.getText();
    }

    private void limpiarErrores() {
        for (Label etiqueta : List.of(lblResumenError, lblErrorFecha, lblErrorTasa, lblErrorFuente, lblErrorMotivo)) {
            etiqueta.setText("");
            etiqueta.setVisible(false);
            etiqueta.setManaged(false);
        }
    }

    private void error(Label etiqueta, String mensaje) {
        etiqueta.setText(mensaje);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
    }

    private static String fechaVisible(String fecha) {
        return LocalDate.parse(fecha).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
