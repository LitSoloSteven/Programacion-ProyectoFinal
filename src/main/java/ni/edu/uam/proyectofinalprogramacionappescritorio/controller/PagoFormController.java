package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.CarteraResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.DistribucionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidacionFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Campos, validación y revisión visual del formulario de pago. */
public class PagoFormController {
    public record Datos(Contrato contrato, BigDecimal monto, String moneda, LocalDate fecha,
                        String metodo, String referencia, String notas,
                        List<DistribucionRequest> distribucion, boolean aplicacionAutomatica) { }

    private record FilaCargoManual(CarteraResponse.Item item, CheckBox check, TextField montoField) { }

    @FXML private ComboBox<Contrato> cmbContrato;
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
    private ClientePagos clientePagos;
    private String estadoInicial = "";

    @FXML public void initialize() {
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
        cmbMetodo.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia", "Depósito bancario", "Otro"));
        cmbMetodo.getSelectionModel().selectFirst();
        dpFechaPago.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
        ToggleGroup grupo = new ToggleGroup();
        rbAplicacionAutomatica.setToggleGroup(grupo);
        rbAplicacionManual.setToggleGroup(grupo);
        rbAplicacionAutomatica.setSelected(true);
        grupo.selectedToggleProperty().addListener((obs, ant, act) -> {
            boolean manual = act == rbAplicacionManual;
            panelDistribucionManual.setVisible(manual);
            panelDistribucionManual.setManaged(manual);
            if (manual) cargarCargosContrato();
        });
        mostrarEdicion();
        limpiarErrores();
        marcarLimpio();
    }

    public void establecerClientePagos(ClientePagos clientePagos) {
        this.clientePagos = clientePagos;
    }

    public void establecerContratos(List<Contrato> contratos) {
        cmbContrato.setItems(FXCollections.observableArrayList(contratos));
        cmbContrato.valueProperty().addListener((obs, anterior, contrato) -> {
            if (contrato == null) return;
            if (contrato.getCanon() != null) txtMonto.setText(contrato.getCanon().toPlainString());
            if (contrato.getMoneda() != null) cmbMoneda.setValue(contrato.getMoneda());
            if (rbAplicacionManual.isSelected()) cargarCargosContrato();
        });
        if (!contratos.isEmpty()) cmbContrato.getSelectionModel().selectFirst();
        marcarLimpio();
    }

    private void cargarCargosContrato() {
        Contrato contrato = cmbContrato.getValue();
        listaCargosContainer.getChildren().clear();
        filasManuales.clear();
        if (contrato == null || clientePagos == null) {
            lblInfoCargos.setText("Selecciona un contrato para consultar sus cargos pendientes.");
            return;
        }
        lblInfoCargos.setText("Cargando cargos abiertos del contrato…");
        String contratoId = contrato.getId().toString();
        Task<CarteraResponse> tarea = new Task<>() {
            @Override protected CarteraResponse call() {
                return clientePagos.consultarCartera(null, contratoId, null, null, null);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, cartera -> {
            if (!Objects.equals(cmbContrato.getValue(), contrato)) return;
            listaCargosContainer.getChildren().clear();
            filasManuales.clear();
            if (cartera.items().isEmpty()) {
                lblInfoCargos.setText("El contrato no tiene cargos pendientes; el pago quedará como saldo a favor.");
                return;
            }
            lblInfoCargos.setText("Selecciona uno o más cargos y el importe a aplicar en " + contrato.getMoneda() + ".");
            for (CarteraResponse.Item item : cartera.items()) {
                CheckBox check = new CheckBox(item.tipo() + " · " + item.concepto()
                        + " (vence " + item.venceEl() + ", saldo " + item.saldo() + " " + item.moneda() + ")");
                check.setWrapText(true);
                HBox.setHgrow(check, Priority.ALWAYS);
                TextField txtImporte = new TextField(item.saldo());
                txtImporte.setPrefWidth(110);
                txtImporte.getStyleClass().add("field");
                txtImporte.setDisable(true);
                check.selectedProperty().addListener((o, ant, sel) -> txtImporte.setDisable(!sel));
                HBox fila = new HBox(10, check, txtImporte);
                listaCargosContainer.getChildren().add(fila);
                filasManuales.add(new FilaCargoManual(item, check, txtImporte));
            }
        }, error -> lblInfoCargos.setText("No se pudieron cargar los cargos abiertos."));
    }

    public boolean validar() {
        limpiarErrores();
        boolean valido = true;
        if (cmbContrato.getValue() == null) {
            error(lblErrorContrato, "Selecciona el contrato al que se aplicará el pago.");
            valido = false;
        }
        BigDecimal montoPago = null;
        try {
            montoPago = new BigDecimal(txtMonto.getText().trim());
            if (montoPago.signum() <= 0 || montoPago.abs().compareTo(new BigDecimal("1000000000000")) > 0
                    || montoPago.stripTrailingZeros().scale() > 2) throw new NumberFormatException();
        } catch (RuntimeException error) {
            error(lblErrorMonto, "Escribe un monto positivo con hasta dos decimales.");
            valido = false;
        }
        if (dpFechaPago.getValue() == null) {
            error(lblErrorFecha, "Selecciona la fecha del pago.");
            valido = false;
        }
        String referencia = txtReferencia.getText();
        if (referencia != null && referencia.length() > 100) {
            error(lblErrorReferencia, "La referencia admite hasta 100 caracteres.");
            valido = false;
        }
        if (rbAplicacionManual.isSelected() && !filasManuales.isEmpty()) {
            BigDecimal sumaManual = BigDecimal.ZERO;
            int seleccionados = 0;
            for (FilaCargoManual fila : filasManuales) {
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
                    error(lblErrorDistribucion, "Cada cargo seleccionado debe tener un monto positivo menor o igual a su saldo.");
                    valido = false;
                    break;
                }
            }
            if (valido && seleccionados == 0) {
                error(lblErrorDistribucion, "Selecciona al menos un cargo o elige la aplicación automática.");
                valido = false;
            }
            if (valido && montoPago != null && cmbContrato.getValue() != null
                    && Objects.equals(cmbMoneda.getValue(), cmbContrato.getValue().getMoneda())
                    && sumaManual.compareTo(montoPago) > 0) {
                error(lblErrorDistribucion, "La suma asignada supera el monto del pago.");
                valido = false;
            }
        }
        if (!valido) ValidacionFormulario.mostrar(lblResumenError, lblErrorContrato, lblErrorMonto,
                lblErrorFecha, lblErrorReferencia, lblErrorDistribucion);
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
        lblRevisionContrato.setText(datos.contrato().toString());
        lblRevisionImporte.setText(String.format("%.2f %s", datos.monto(), datos.moneda()));
        lblRevisionFecha.setText(datos.fecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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

    private String instantanea() {
        return String.valueOf(cmbContrato.getValue()) + "|" + txtMonto.getText() + "|" + cmbMoneda.getValue()
                + "|" + dpFechaPago.getValue() + "|" + cmbMetodo.getValue() + "|"
                + txtReferencia.getText() + "|" + txtNotas.getText() + "|" + rbAplicacionAutomatica.isSelected();
    }

    private void limpiarErrores() {
        for (Label etiqueta : List.of(lblResumenError, lblErrorContrato, lblErrorMonto,
                lblErrorFecha, lblErrorReferencia, lblErrorDistribucion)) {
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
