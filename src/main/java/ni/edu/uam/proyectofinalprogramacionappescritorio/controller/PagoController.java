package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PagoService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador de la vista de Pagos de Latch.
 * Registra cobros y actualiza el historial en tiempo real con PostgreSQL 18.
 */
public class PagoController {

    @FXML
    private ComboBox<Contrato> cmbContrato;

    @FXML
    private TextField txtMonto;

    @FXML
    private ComboBox<String> cmbMoneda;

    @FXML
    private DatePicker dpFechaPago;

    @FXML
    private ComboBox<String> cmbMetodo;

    @FXML
    private TextField txtReferencia;

    @FXML
    private TextField txtNotas;

    @FXML
    private Button btnAnular;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableView<Pago> tblPagos;

    @FXML
    private TableColumn<Pago, String> colContrato;

    @FXML
    private TableColumn<Pago, LocalDate> colFecha;

    @FXML
    private TableColumn<Pago, String> colMonto;

    @FXML
    private TableColumn<Pago, String> colMetodo;

    @FXML
    private TableColumn<Pago, String> colReferencia;

    @FXML
    private TableColumn<Pago, String> colEstado;

    private final PagoService pagoService;
    private final ContratoService contratoService;

    private final ObservableList<Pago> listaPagos = FXCollections.observableArrayList();
    private FilteredList<Pago> listaFiltrada;
    private Pago pagoSeleccionado = null;

    public PagoController() {
        this.pagoService = new PagoService();
        this.contratoService = new ContratoService();
    }

    public PagoController(PagoService pagoService, ContratoService contratoService) {
        this.pagoService = pagoService;
        this.contratoService = contratoService;
    }

    @FXML
    public void initialize() {
        inicializarCombos();
        configurarTabla();
        configurarFiltro();
        aplicarControlPermisos();

        cargarContratosReales();
        cargarDatos();

        tblPagos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarPago(newVal);
            }
        });

        // Al seleccionar contrato, precargar el monto con el canon y su moneda
        cmbContrato.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && pagoSeleccionado == null) {
                if (newVal.getCanon() != null) {
                    txtMonto.setText(newVal.getCanon().toPlainString());
                }
                if (newVal.getMoneda() != null) {
                    cmbMoneda.setValue(newVal.getMoneda());
                }
            }
        });
    }

    private void inicializarCombos() {
        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();

        cmbMetodo.setItems(FXCollections.observableArrayList("Efectivo", "Transferencia", "Depósito bancario", "Otro"));
        cmbMetodo.getSelectionModel().selectFirst();

        dpFechaPago.setValue(LocalDate.now());
    }

    private void configurarTabla() {
        colContrato.setCellValueFactory(new PropertyValueFactory<>("contratoInfo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaPago"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("montoFormateado"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodo"));
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referencia"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        listaFiltrada = new FilteredList<>(listaPagos, p -> true);
        tblPagos.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            listaFiltrada.setPredicate(p -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filtro = newVal.toLowerCase().trim();
                boolean matchCon = p.getContratoInfo() != null && p.getContratoInfo().toLowerCase().contains(filtro);
                boolean matchRef = p.getReferencia() != null && p.getReferencia().toLowerCase().contains(filtro);
                boolean matchMet = p.getMetodo() != null && p.getMetodo().toLowerCase().contains(filtro);
                return matchCon || matchRef || matchMet;
            });
        });
    }

    private void aplicarControlPermisos() {
        boolean esAdmin = SesionUsuario.esPropietario();
        btnAnular.setVisible(esAdmin);
        btnAnular.setManaged(esAdmin);
    }

    public void cargarContratosReales() {
        try {
            List<Contrato> contratos = contratoService.listarVigentes();
            cmbContrato.setItems(FXCollections.observableArrayList(contratos));
            if (!contratos.isEmpty()) {
                cmbContrato.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No se pudieron obtener los contratos vigentes desde PostgreSQL.");
        }
    }

    public void cargarDatos() {
        try {
            List<Pago> datos = pagoService.listarTodos();
            listaPagos.setAll(datos);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al cargar pagos", "No fue posible consultar el historial de pagos desde PostgreSQL.");
        }
    }

    @FXML
    public void refrescarTabla() {
        cargarContratosReales();
        cargarDatos();
        limpiarFormulario();
        AlertaUtil.mostrarExito("Tabla actualizada", "El historial de pagos ha sido sincronizado con PostgreSQL.");
    }

    @FXML
    public void handleRegistrar() {
        Contrato contrato = cmbContrato.getValue();
        String strMonto = txtMonto.getText();
        String moneda = cmbMoneda.getValue();
        LocalDate fecha = dpFechaPago.getValue();
        String metodo = cmbMetodo.getValue();
        String referencia = txtReferencia.getText();
        String notas = txtNotas.getText();

        if (contrato == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe seleccionar el contrato al que se le aplicará el pago.");
            cmbContrato.requestFocus();
            return;
        }

        if (fecha == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Seleccione la fecha de realización del pago.");
            dpFechaPago.requestFocus();
            return;
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(strMonto.trim());
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                AlertaUtil.mostrarAdvertencia("Monto inválido", "El monto a pagar debe ser mayor a cero.");
                txtMonto.requestFocus();
                return;
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Formato inválido", "Ingrese un monto decimal válido para el pago.");
            txtMonto.requestFocus();
            return;
        }

        try {
            Pago nuevo = new Pago();
            nuevo.setContratoId(contrato.getId());
            nuevo.setFechaPago(fecha);
            nuevo.setMonedaOriginal(moneda);
            nuevo.setMontoOriginal(monto);
            nuevo.setMontoContrato(monto);
            nuevo.setMetodo(metodo);
            nuevo.setReferencia(referencia != null && !referencia.isBlank() ? referencia.trim() : null);
            nuevo.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);
            nuevo.setEstado("Registrado");

            pagoService.registrarPago(nuevo);

            AlertaUtil.mostrarExito(
                    "Pago Registrado",
                    "El pago por " + String.format("%.2f %s", monto, moneda) + " fue guardado satisfactoriamente en PostgreSQL."
            );

            cargarDatos();
            limpiarFormulario();
        } catch (IllegalArgumentException e) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de persistencia", "No se pudo registrar el pago en la base de datos.");
        }
    }

    @FXML
    public void handleAnular() {
        if (pagoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un pago de la tabla para anular.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar anulación",
                "¿Está seguro de que desea anular el pago por " + pagoSeleccionado.getMontoFormateado() + "?"
        );

        if (confirma) {
            try {
                pagoService.anular(pagoSeleccionado.getId());
                AlertaUtil.mostrarExito("Pago anulado", "El estado del pago fue cambiado a 'Anulado'.");
                cargarDatos();
                limpiarFormulario();
            } catch (SecurityException e) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error", "No fue posible anular el pago en PostgreSQL.");
            }
        }
    }

    @FXML
    public void limpiarFormulario() {
        pagoSeleccionado = null;
        if (!cmbContrato.getItems().isEmpty()) {
            cmbContrato.getSelectionModel().selectFirst();
        } else {
            cmbContrato.getSelectionModel().clearSelection();
        }
        txtMonto.clear();
        if (!cmbMoneda.getItems().isEmpty()) cmbMoneda.getSelectionModel().selectFirst();
        dpFechaPago.setValue(LocalDate.now());
        if (!cmbMetodo.getItems().isEmpty()) cmbMetodo.getSelectionModel().selectFirst();
        txtReferencia.clear();
        txtNotas.clear();
        tblPagos.getSelectionModel().clearSelection();
    }

    private void seleccionarPago(Pago p) {
        this.pagoSeleccionado = p;
        txtMonto.setText(p.getMontoOriginal() != null ? p.getMontoOriginal().toPlainString() : "");
        cmbMoneda.setValue(p.getMonedaOriginal());
        dpFechaPago.setValue(p.getFechaPago());
        cmbMetodo.setValue(p.getMetodo());
        txtReferencia.setText(p.getReferencia() != null ? p.getReferencia() : "");
        txtNotas.setText(p.getNotas() != null ? p.getNotas() : "");

        for (Contrato c : cmbContrato.getItems()) {
            if (c.getId().equals(p.getContratoId())) {
                cmbContrato.setValue(c);
                break;
            }
        }
    }
}
