package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CarteraResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.DistribucionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario.CampoValidado;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorAsincrono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Campos, validación en vivo y revisión visual del formulario de pago. */
public class PagoFormController {
    public record Datos(ContratoResponse contrato, BigDecimal monto, String moneda, LocalDate fecha,
                        String metodo, String referencia, String notas,
                        List<DistribucionRequest> distribucion, boolean aplicacionAutomatica) { }

    private record FilaCargoManual(CarteraResponse.Item item, CheckBox check, TextField montoField) { }

    @FXML private ComboBox<ContratoResponse> cmbContrato;
    @FXML private TextField txtMonto;
    @FXML private ComboBox<String> cmbMoneda;
    @FXML private DatePicker dpFechaPago;
    @FXML private ComboBox<String> cmbMetodo;
    @FXML private TextField txtReferencia;
    @FXML private TextField txtNotas;
    @FXML private RadioButton rbAplicacionAutomatica;
    @FXML private RadioButton rbAplicacionManual;
    @FXML private VBox panelDistribucionManual;
    @FXML private Label lblInfoCargos;
    @FXML private VBox listaCargosContainer;
    @FXML private Label lblErrorDistribucion;
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
    @FXML private Label lblRevisionAplicacion;

    private final List<FilaCargoManual> filasManuales = new ArrayList<>();
    private final Set<Control> tocados = new HashSet<>();
    private final ValidadorAsincrono validadorMonto = new ValidadorAsincrono();
    private ClientePagos clientePagos;
    private ClienteValidaciones clienteValidaciones;
    private boolean inicializando = true;
    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbContrato.setConverter(new StringConverter<>() {
            @Override public String toString(ContratoResponse c) {
                return c == null ? "" : c.inquilinoNombre() + " · " + c.unidadInfo();
            }
            @Override public ContratoResponse fromString(String s) { return null; }
        });
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        cmbMetodo.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia", "Depósito bancario", "Otro"));
        cmbMetodo.getSelectionModel().selectFirst();
        dpFechaPago.setValue(LocalDate.now(ConstantesLatch.ZONA_OPERATIVA));
        ToggleGroup grupo = new ToggleGroup();
        rbAplicacionAutomatica.setToggleGroup(grupo);
        rbAplicacionManual.setToggleGroup(grupo);
        rbAplicacionAutomatica.setSelected(true);
        grupo.selectedToggleProperty().addListener((obs, ant, act) -> {
            boolean manual = act == rbAplicacionManual;
            panelDistribucionManual.setVisible(manual);
            panelDistribucionManual.setManaged(manual);
            if (manual) {
                cargarCargosContrato();
            } else {
                ValidacionFormulario.marcarCampo(null, lblErrorDistribucion, null);
            }
            sincronizarResumen();
        });
        mostrarEdicion();
        limpiarErrores();
        configurarValidacionEnVivo();
        inicializando = false;
        marcarLimpio();
    }

    public void establecerClientePagos(ClientePagos clientePagos) {
        this.clientePagos = clientePagos;
    }

    public void establecerValidadorServidor(ClienteValidaciones clienteValidaciones) {
        this.clienteValidaciones = clienteValidaciones;
    }

    public void establecerContratos(List<ContratoResponse> contratos) {
        inicializando = true;
        try {
            cmbContrato.setItems(FXCollections.observableArrayList(contratos));
            if (!contratos.isEmpty()) {
                cmbContrato.getSelectionModel().selectFirst();
                ContratoResponse primero = cmbContrato.getValue();
                if (primero != null) {
                    if (primero.canon() != null) txtMonto.setText(primero.canon());
                    if (primero.moneda() != null) cmbMoneda.setValue(primero.moneda());
                }
            }
            marcarLimpio();
        } finally {
            inicializando = false;
        }
    }

    private void cargarCargosContrato() {
        ContratoResponse contrato = cmbContrato.getValue();
        listaCargosContainer.getChildren().clear();
        filasManuales.clear();
        if (contrato == null || clientePagos == null) {
            lblInfoCargos.setText("Selecciona un contrato para consultar sus cargos pendientes.");
            return;
        }
        lblInfoCargos.setText("Cargando cargos abiertos del contrato…");
        String contratoId = contrato.id();
        Task<CarteraResponse> tarea = new Task<>() {
            @Override protected CarteraResponse call() {
                return clientePagos.consultarCartera(null, contratoId, null, null, null);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, cartera -> {
            if (!Objects.equals(cmbContrato.getValue(), contrato)) return;
            listaCargosContainer.getChildren().clear();
            filasManuales.clear();
            if (cartera.items().isEmpty()) {
                lblInfoCargos.setText("El contrato no tiene cargos pendientes; el pago quedará como saldo a favor.");
                ValidacionFormulario.marcarCampo(null, lblErrorDistribucion, null);
                return;
            }
            lblInfoCargos.setText("Selecciona uno o más cargos y el importe a aplicar en " + contrato.moneda() + ".");
            for (CarteraResponse.Item item : cartera.items()) {
                CheckBox check = new CheckBox(item.tipo() + " · " + item.concepto()
                        + " (vence " + item.venceEl() + ", saldo " + item.saldo() + " " + item.moneda() + ")");
                check.setWrapText(true);
                HBox.setHgrow(check, Priority.ALWAYS);
                TextField txtImporte = new TextField(item.saldo());
                txtImporte.setPrefWidth(110);
                txtImporte.getStyleClass().add("field");
                txtImporte.setDisable(true);
                check.selectedProperty().addListener((o, ant, sel) -> {
                    txtImporte.setDisable(!sel);
                    validarDistribucion(false);
                    sincronizarResumen();
                });
                ValidacionFormulario.vincularTexto(txtImporte, () -> {
                    if (check.isSelected()) {
                        validarDistribucion(false);
                        sincronizarResumen();
                    }
                });
                HBox fila = new HBox(10, check, txtImporte);
                listaCargosContainer.getChildren().add(fila);
                filasManuales.add(new FilaCargoManual(item, check, txtImporte));
            }
        }, error -> lblInfoCargos.setText("No se pudieron cargar los cargos abiertos."));
    }

    public boolean validar() {
        tocados.addAll(List.of(cmbContrato, txtMonto, dpFechaPago, txtReferencia));
        boolean valido = validarContrato()
                & validarMonto()
                & validarFecha()
                & validarReferencia()
                & validarDistribucion(true);
        if (!valido) {
            ValidacionFormulario.mostrarYEnfocar(lblResumenError, List.of(
                    new CampoValidado(cmbContrato, lblErrorContrato),
                    new CampoValidado(txtMonto, lblErrorMonto),
                    new CampoValidado(dpFechaPago, lblErrorFecha),
                    new CampoValidado(txtReferencia, lblErrorReferencia),
                    new CampoValidado(rbAplicacionManual, lblErrorDistribucion)));
        } else {
            sincronizarResumen();
        }
        return valido;
    }

    public Datos datos() {
        String referencia = txtReferencia.getText();
        String notas = txtNotas.getText();
        boolean automatica = rbAplicacionAutomatica.isSelected();
        List<DistribucionRequest> distribucion = new ArrayList<>();
        if (!automatica) {
            for (FilaCargoManual fila : filasManuales) {
                if (fila.check().isSelected()) {
                    distribucion.add(new DistribucionRequest(
                            fila.item().cargoId(), new BigDecimal(fila.montoField().getText().trim()).toPlainString()));
                }
            }
        }
        return new Datos(cmbContrato.getValue(), new BigDecimal(txtMonto.getText().trim()),
                cmbMoneda.getValue(), dpFechaPago.getValue(), cmbMetodo.getValue(),
                referencia == null || referencia.isBlank() ? null : referencia.trim(),
                notas == null || notas.isBlank() ? null : notas.trim(),
                List.copyOf(distribucion), automatica);
    }

    public void mostrarRevision() {
        Datos datos = datos();
        lblRevisionContrato.setText(datos.contrato().inquilinoNombre() + " · " + datos.contrato().unidadInfo());
        lblRevisionImporte.setText(String.format("%.2f %s", datos.monto(), datos.moneda()));
        lblRevisionFecha.setText(datos.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblRevisionMetodo.setText(datos.metodo());
        lblRevisionReferencia.setText(datos.referencia() == null ? "Sin referencia" : datos.referencia());
        lblRevisionNotas.setText(datos.notas() == null ? "Sin observaciones" : datos.notas());
        lblRevisionAplicacion.setText(datos.aplicacionAutomatica()
                ? "Automática (vencidos más antiguos)"
                : "Manual (" + datos.distribucion().size() + " cargos)");
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

    private void configurarValidacionEnVivo() {
        cmbContrato.valueProperty().addListener((obs, anterior, contrato) -> {
            if (inicializando) return;
            if (contrato != null) {
                if (contrato.canon() != null) txtMonto.setText(contrato.canon());
                if (contrato.moneda() != null) cmbMoneda.setValue(contrato.moneda());
                if (rbAplicacionManual.isSelected()) cargarCargosContrato();
            }
            alEditar(cmbContrato, () -> {
                validarContrato();
                validarDistribucion(false);
            });
        });
        ValidacionFormulario.vincularTexto(txtMonto, () -> {
            alEditar(txtMonto, () -> {
                validarMonto();
                if (rbAplicacionManual.isSelected() && !filasManuales.isEmpty()) {
                    validarDistribucion(false);
                }
            });
            validarMontoEnServidor();
        });
        ValidacionFormulario.vincularSelector(cmbMoneda, () -> {
            if (!inicializando && rbAplicacionManual.isSelected() && !filasManuales.isEmpty()) {
                validarDistribucion(false);
                sincronizarResumen();
            }
        });
        ValidacionFormulario.vincularSelector(dpFechaPago, () -> alEditar(dpFechaPago, this::validarFecha));
        ValidacionFormulario.vincularTexto(txtReferencia, () -> alEditar(txtReferencia, this::validarReferencia));
    }

    private void alEditar(Control control, Runnable validador) {
        if (inicializando) return;
        tocados.add(control);
        validador.run();
        sincronizarResumen();
    }

    private void sincronizarResumen() {
        ValidacionFormulario.sincronizarResumenSiVisible(lblResumenError,
                lblErrorContrato, lblErrorMonto, lblErrorFecha, lblErrorReferencia, lblErrorDistribucion);
    }

    private boolean validarContrato() {
        String msg = cmbContrato.getValue() == null
                ? "Selecciona el contrato al que se aplicará el pago." : null;
        ValidacionFormulario.marcarCampo(cmbContrato, lblErrorContrato, msg);
        return msg == null;
    }

    private BigDecimal parsearMontoValido() {
        try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
            if (monto.signum() <= 0 || monto.abs().compareTo(new BigDecimal("1000000000000")) > 0
                    || monto.stripTrailingZeros().scale() > 2) {
                return null;
            }
            return monto;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private boolean validarMonto() {
        BigDecimal monto = parsearMontoValido();
        String msg = monto == null ? "Escribe un monto positivo con hasta dos decimales." : null;
        ValidacionFormulario.marcarCampo(txtMonto, lblErrorMonto, msg);
        return msg == null;
    }

    private boolean validarFecha() {
        String msg = dpFechaPago.getValue() == null ? "Selecciona la fecha del pago." : null;
        ValidacionFormulario.marcarCampo(dpFechaPago, lblErrorFecha, msg);
        return msg == null;
    }

    private boolean validarReferencia() {
        String referencia = txtReferencia.getText();
        String msg = referencia != null && referencia.length() > 100
                ? "La referencia admite hasta 100 caracteres." : null;
        ValidacionFormulario.marcarCampo(txtReferencia, lblErrorReferencia, msg);
        return msg == null;
    }

    private boolean validarDistribucion(boolean exigirSeleccion) {
        String msg = null;
        if (rbAplicacionManual.isSelected() && !filasManuales.isEmpty()) {
            BigDecimal sumaManual = BigDecimal.ZERO;
            int seleccionados = 0;
            for (FilaCargoManual fila : filasManuales) {
                fila.montoField().getStyleClass().remove(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                if (!fila.check().isSelected()) continue;
                seleccionados++;
                try {
                    BigDecimal abono = new BigDecimal(fila.montoField().getText().trim());
                    BigDecimal saldo = new BigDecimal(fila.item().saldo());
                    if (abono.signum() <= 0 || abono.stripTrailingZeros().scale() > 2 || abono.compareTo(saldo) > 0) {
                        throw new IllegalArgumentException();
                    }
                    sumaManual = sumaManual.add(abono);
                } catch (RuntimeException ex) {
                    if (!fila.montoField().getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO)) {
                        fila.montoField().getStyleClass().add(ValidacionFormulario.CLASE_CAMPO_INVALIDO);
                    }
                    msg = "Cada cargo seleccionado debe tener un monto positivo menor o igual a su saldo.";
                    break;
                }
            }
            if (msg == null && exigirSeleccion && seleccionados == 0) {
                msg = "Selecciona al menos un cargo o elige la aplicación automática.";
            }
            BigDecimal montoPago = parsearMontoValido();
            if (msg == null && montoPago != null && cmbContrato.getValue() != null
                    && Objects.equals(cmbMoneda.getValue(), cmbContrato.getValue().moneda())
                    && sumaManual.compareTo(montoPago) > 0) {
                msg = "La suma asignada supera el monto del pago.";
            }
        }
        ValidacionFormulario.marcarCampo(null, lblErrorDistribucion, msg);
        return msg == null;
    }

    private void validarMontoEnServidor() {
        if (inicializando || clienteValidaciones == null) return;
        BigDecimal monto = parsearMontoValido();
        if (monto == null) {
            validadorMonto.cancelar();
            return;
        }
        String texto = txtMonto.getText().trim();
        validadorMonto.solicitar(
                texto,
                () -> !inicializando && txtMonto.getScene() != null
                        && texto.equals(txtMonto.getText() == null ? "" : txtMonto.getText().trim()),
                () -> clienteValidaciones.monto(texto),
                res -> {
                    if (res != null && !res.valido()) {
                        ValidacionFormulario.marcarCampo(txtMonto, lblErrorMonto, res.mensaje());
                    }
                },
                err -> { });
    }

    private String instantanea() {
        ContratoResponse c = cmbContrato.getValue();
        return (c == null ? "" : c.id()) + "|" + txtMonto.getText() + "|" + cmbMoneda.getValue()
                + "|" + dpFechaPago.getValue() + "|" + cmbMetodo.getValue() + "|"
                + txtReferencia.getText() + "|" + txtNotas.getText() + "|" + rbAplicacionAutomatica.isSelected();
    }

    private void limpiarErrores() {
        ValidacionFormulario.limpiarCampos(lblResumenError,
                new CampoValidado(cmbContrato, lblErrorContrato),
                new CampoValidado(txtMonto, lblErrorMonto),
                new CampoValidado(dpFechaPago, lblErrorFecha),
                new CampoValidado(txtReferencia, lblErrorReferencia),
                new CampoValidado(null, lblErrorDistribucion));
    }
}
