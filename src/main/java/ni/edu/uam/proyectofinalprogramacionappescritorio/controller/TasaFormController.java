package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto.TasaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.FormatoTasa;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Campos y validación en vivo de los dos diálogos del módulo de tasas. */
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

    private final Set<Control> tocados = new HashSet<>();
    private TasaResponse tasaSeleccionada;
    private boolean inicializando = true;
    private String estadoInicial = "";

    @FXML public void initialize() {
        dpFecha.setValue(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA));
        lblBandaInformativa.setText("Banda de referencia: "
                + FormatoTasa.formatear(ConstantesLatch.BANDA_TASA_MINIMA) + "–"
                + FormatoTasa.formatear(ConstantesLatch.BANDA_TASA_MAXIMA) + " NIO/USD.");
        mostrarRegistro();
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
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
        inicializando = true;
        try {
            tasaSeleccionada = tasa;
            panelRegistro.setVisible(false);
            panelRegistro.setManaged(false);
            panelAutorizacion.setVisible(true);
            panelAutorizacion.setManaged(true);
            lblTasaSeleccionada.setText(fechaVisible(tasa.fecha()) + " · "
                    + FormatoTasa.formatear(new BigDecimal(tasa.nioPorUsd())) + " NIO/USD");
            lblFuenteSeleccionada.setText("Fuente: " + tasa.fuente());
            txtMotivo.clear();
            tocados.clear();
            limpiarErrores();
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    public boolean validarRegistro() {
        tocados.addAll(List.of(dpFecha, txtTasa, txtFuente));
        boolean valido = validarFecha()
                & validarTasa()
                & validarFuente();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(dpFecha, lblErrorFecha),
                    new CampoValidado(txtTasa, lblErrorTasa),
                    new CampoValidado(txtFuente, lblErrorFuente)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                    lblErrorFecha, lblErrorTasa, lblErrorFuente, lblErrorMotivo);
        }
        return valido;
    }

    public boolean validarMotivo() {
        tocados.add(txtMotivo);
        boolean valido = validarTextoMotivo();
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(txtMotivo, lblErrorMotivo)));
        } else {
            ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError, lblErrorMotivo);
        }
        return valido;
    }

    public Datos datos() {
        return new Datos(dpFecha.getValue(), new BigDecimal(txtTasa.getText().strip().replace(',', '.')),
                txtFuente.getText().strip());
    }

    public String motivo() { return txtMotivo.getText().strip(); }
    public void enlazarMotivoConAccion(Button accion) {
        accion.setDisable(false);
    }
    public TasaResponse tasaSeleccionada() { return tasaSeleccionada; }
    public boolean hayCambios() { return !estadoInicial.equals(instantanea()); }
    public void marcarLimpio() { estadoInicial = instantanea(); }

    public void mostrarError(String mensaje) {
        lblResumenError.setText(mensaje);
        lblResumenError.setVisible(true);
        lblResumenError.setManaged(true);
    }

    private void configurarValidacionEnVivo() {
        ValidacionFormulario.vincularSelector(dpFecha, () -> alEditar(dpFecha, this::validarFecha));
        ValidacionFormulario.vincularTexto(txtTasa, () -> alEditar(txtTasa, this::validarTasa));
        ValidacionFormulario.vincularTexto(txtFuente, () -> alEditar(txtFuente, this::validarFuente));
        ValidacionFormulario.vincularTexto(txtMotivo, () -> alEditar(txtMotivo, this::validarTextoMotivo));
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorFecha, lblErrorTasa, lblErrorFuente, lblErrorMotivo);
    }

    private boolean validarFecha() {
        LocalDate fecha = dpFecha.getValue();
        String msg = null;
        if (fecha == null) {
            msg = "Selecciona la fecha de la tasa.";
        } else if (fecha.isAfter(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA))) {
            msg = "La tasa no puede corresponder a una fecha futura.";
        }
        ValidacionFormulario.marcarCampo(dpFecha, lblErrorFecha, msg);
        return msg == null;
    }

    private boolean validarTasa() {
        String msg = null;
        try {
            BigDecimal valor = new BigDecimal(txtTasa.getText().strip().replace(',', '.'));
            if (valor.signum() <= 0 || valor.stripTrailingZeros().scale() > 8) {
                throw new NumberFormatException();
            }
        } catch (RuntimeException error) {
            msg = "Escribe una tasa decimal mayor que cero con hasta ocho decimales.";
        }
        ValidacionFormulario.marcarCampo(txtTasa, lblErrorTasa, msg);
        return msg == null;
    }

    private boolean validarFuente() {
        String fuente = txtFuente.getText();
        String msg = null;
        if (fuente == null || fuente.isBlank()) {
            msg = "Indica la fuente de la tasa.";
        } else if (fuente.length() > 500) {
            msg = "La fuente admite hasta 500 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtFuente, lblErrorFuente, msg);
        return msg == null;
    }

    private boolean validarTextoMotivo() {
        String motivo = txtMotivo.getText();
        String msg = null;
        if (motivo == null || motivo.isBlank()) {
            msg = "Escribe el motivo de autorización.";
        } else if (motivo.length() > 500) {
            msg = "El motivo admite hasta 500 caracteres.";
        }
        ValidacionFormulario.marcarCampo(txtMotivo, lblErrorMotivo, msg);
        return msg == null;
    }

    private String instantanea() {
        return dpFecha.getValue() + "|" + txtTasa.getText() + "|" + txtFuente.getText() + "|" + txtMotivo.getText();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(dpFecha, lblErrorFecha),
                new CampoValidado(txtTasa, lblErrorTasa),
                new CampoValidado(txtFuente, lblErrorFuente),
                new CampoValidado(txtMotivo, lblErrorMotivo));
    }

    private static String fechaVisible(String fecha) {
        return LocalDate.parse(fecha).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
