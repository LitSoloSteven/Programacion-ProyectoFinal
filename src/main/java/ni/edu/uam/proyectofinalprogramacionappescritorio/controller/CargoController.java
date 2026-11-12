package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CargoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador de la vista de Cargos.
 * Maneja emisión y consulta de cobros vinculados a contratos reales de PostgreSQL.
 */
public class CargoController {

    @FXML
    private ComboBox<Contrato> cmbContrato;

    @FXML
    private ComboBox<String> cmbTipo;

    @FXML
    private TextField txtImporte;

    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private TextField txtConcepto;

    @FXML
    private DatePicker dpPeriodo;

    @FXML
    private DatePicker dpFechaEmision;

    @FXML
    private DatePicker dpVenceEl;

    @FXML
    private Button btnEliminar;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableView<Cargo> tblCargos;

    @FXML
    private TableColumn<Cargo, String> colContrato;

    @FXML
    private TableColumn<Cargo, String> colTipo;

    @FXML
    private TableColumn<Cargo, String> colConcepto;

    @FXML
    private TableColumn<Cargo, String> colImporte;

    @FXML
    private TableColumn<Cargo, LocalDate> colVence;

    @FXML
    private TableColumn<Cargo, String> colEstado;

    private final CargoService cargoService;
    private final ContratoService contratoService;

    private final ObservableList<Cargo> listaCargos = FXCollections.observableArrayList();
    private FilteredList<Cargo> listaFiltrada;
    private Cargo cargoSeleccionado = null;

    public CargoController() {
        this.cargoService = new CargoService();
        this.contratoService = new ContratoService();
    }

    public CargoController(CargoService cargoService, ContratoService contratoService) {
        this.cargoService = cargoService;
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

        tblCargos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarCargo(newVal);
            }
        });
    }

    private void inicializarCombos() {
        cmbTipo.setItems(FXCollections.observableArrayList("Renta", "Mora", "Reparación", "Servicio", "Otro"));
        cmbTipo.getSelectionModel().selectFirst();

        cmbEstado.setItems(FXCollections.observableArrayList("Pendiente", "Pagado", "Cancelado"));
        cmbEstado.getSelectionModel().selectFirst();

        dpPeriodo.setValue(LocalDate.now().withDayOfMonth(1));
        dpFechaEmision.setValue(LocalDate.now());
        dpVenceEl.setValue(LocalDate.now().plusDays(5));
    }

    private void configurarTabla() {
        colContrato.setCellValueFactory(new PropertyValueFactory<>("contratoInfo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colImporte.setCellValueFactory(new PropertyValueFactory<>("importeFormateado"));
        colVence.setCellValueFactory(new PropertyValueFactory<>("venceEl"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        listaFiltrada = new FilteredList<>(listaCargos, c -> true);
        tblCargos.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            listaFiltrada.setPredicate(c -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filtro = newVal.toLowerCase().trim();
                boolean matchCon = c.getContratoInfo() != null && c.getContratoInfo().toLowerCase().contains(filtro);
                boolean matchConcep = c.getConcepto() != null && c.getConcepto().toLowerCase().contains(filtro);
                boolean matchTip = c.getTipo() != null && c.getTipo().toLowerCase().contains(filtro);
                return matchCon || matchConcep || matchTip;
            });
        });
    }

    private void aplicarControlPermisos() {
        boolean esAdmin = SesionUsuario.esPropietario();
        btnEliminar.setVisible(esAdmin);
        btnEliminar.setManaged(esAdmin);
    }

    public void cargarContratosReales() {
        try {
            List<Contrato> contratos = contratoService.listarVigentes();
            cmbContrato.setItems(FXCollections.observableArrayList(contratos));
            if (!contratos.isEmpty()) {
                cmbContrato.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No fue posible cargar los contratos vigentes de PostgreSQL.");
        }
    }

    public void cargarDatos() {
        try {
            List<Cargo> datos = cargoService.listarTodos();
            listaCargos.setAll(datos);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al cargar cargos", "No fue posible consultar las obligaciones desde PostgreSQL.");
        }
    }

    @FXML
    public void refrescarTabla() {
        cargarContratosReales();
        cargarDatos();
        limpiarFormulario();
        AlertaUtil.mostrarExito("Tabla actualizada", "El registro de cargos ha sido sincronizado con PostgreSQL.");
    }

    @FXML
    public void handleGuardar() {
        Contrato contrato = cmbContrato.getValue();
        String tipo = cmbTipo.getValue();
        String strImporte = txtImporte.getText();
        String estado = cmbEstado.getValue();
        String concepto = txtConcepto.getText();
        LocalDate periodo = dpPeriodo.getValue();
        LocalDate emision = dpFechaEmision.getValue();
        LocalDate vence = dpVenceEl.getValue();

        if (contrato == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe seleccionar un contrato.");
            cmbContrato.requestFocus();
            return;
        }

        if (concepto == null || concepto.trim().isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe ingresar el concepto o descripción del cargo.");
            txtConcepto.requestFocus();
            return;
        }

        BigDecimal importe;
        try {
            importe = new BigDecimal(strImporte.trim());
            if (importe.compareTo(BigDecimal.ZERO) <= 0) {
                AlertaUtil.mostrarAdvertencia("Importe inválido", "El importe del cargo debe ser mayor a cero.");
                txtImporte.requestFocus();
                return;
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Formato numérico inválido", "Ingrese un valor numérico decimal válido para el importe.");
            txtImporte.requestFocus();
            return;
        }

        if (periodo == null || emision == null || vence == null) {
            AlertaUtil.mostrarAdvertencia("Fechas obligatorias", "Complete las fechas de periodo, emisión y vencimiento.");
            return;
        }

        if (vence.isBefore(emision)) {
            AlertaUtil.mostrarAdvertencia("Fechas incoherentes", "La fecha de vencimiento no puede ser anterior a la fecha de emisión.");
            dpVenceEl.requestFocus();
            return;
        }

        try {
            if (cargoSeleccionado == null) {
                // Nuevo cargo
                Cargo nuevo = new Cargo();
                nuevo.setContratoId(contrato.getId());
                nuevo.setTipo(tipo);
                nuevo.setImporte(importe);
                nuevo.setEstado(estado);
                nuevo.setConcepto(concepto.trim());
                nuevo.setPeriodo(periodo.withDayOfMonth(1));
                nuevo.setFechaEmision(emision);
                nuevo.setVenceEl(vence);

                cargoService.guardar(nuevo);
                AlertaUtil.mostrarExito("Registro exitoso", "El cargo financiero fue registrado correctamente en PostgreSQL.");
            } else {
                // Edición
                cargoSeleccionado.setContratoId(contrato.getId());
                cargoSeleccionado.setTipo(tipo);
                cargoSeleccionado.setImporte(importe);
                cargoSeleccionado.setEstado(estado);
                cargoSeleccionado.setConcepto(concepto.trim());
                cargoSeleccionado.setPeriodo(periodo.withDayOfMonth(1));
                cargoSeleccionado.setFechaEmision(emision);
                cargoSeleccionado.setVenceEl(vence);

                cargoService.guardar(cargoSeleccionado);
                AlertaUtil.mostrarExito("Actualización exitosa", "El cargo financiero fue actualizado.");
            }

            cargarDatos();
            limpiarFormulario();
        } catch (IllegalArgumentException e) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de persistencia", "No fue posible guardar el cargo en PostgreSQL.");
        }
    }

    @FXML
    public void handleEliminar() {
        if (cargoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un cargo de la tabla para eliminar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de que desea eliminar este cargo de " + cargoSeleccionado.getImporteFormateado() + "?"
        );

        if (confirma) {
            try {
                cargoService.eliminar(cargoSeleccionado.getId());
                AlertaUtil.mostrarExito("Registro eliminado", "El cargo fue eliminado de PostgreSQL.");
                cargarDatos();
                limpiarFormulario();
            } catch (SecurityException e) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error", "No fue posible eliminar el cargo.");
            }
        }
    }

    @FXML
    public void limpiarFormulario() {
        cargoSeleccionado = null;
        if (!cmbContrato.getItems().isEmpty()) {
            cmbContrato.getSelectionModel().selectFirst();
        } else {
            cmbContrato.getSelectionModel().clearSelection();
        }
        if (!cmbTipo.getItems().isEmpty()) cmbTipo.getSelectionModel().selectFirst();
        if (!cmbEstado.getItems().isEmpty()) cmbEstado.getSelectionModel().selectFirst();
        txtImporte.clear();
        txtConcepto.clear();
        dpPeriodo.setValue(LocalDate.now().withDayOfMonth(1));
        dpFechaEmision.setValue(LocalDate.now());
        dpVenceEl.setValue(LocalDate.now().plusDays(5));
        tblCargos.getSelectionModel().clearSelection();
    }

    private void seleccionarCargo(Cargo c) {
        this.cargoSeleccionado = c;
        cmbTipo.setValue(c.getTipo());
        cmbEstado.setValue(c.getEstado());
        txtImporte.setText(c.getImporte() != null ? c.getImporte().toPlainString() : "");
        txtConcepto.setText(c.getConcepto());
        dpPeriodo.setValue(c.getPeriodo());
        dpFechaEmision.setValue(c.getFechaEmision());
        dpVenceEl.setValue(c.getVenceEl());

        for (Contrato ct : cmbContrato.getItems()) {
            if (ct.getId().equals(c.getContratoId())) {
                cmbContrato.setValue(ct);
                break;
            }
        }
    }
}
