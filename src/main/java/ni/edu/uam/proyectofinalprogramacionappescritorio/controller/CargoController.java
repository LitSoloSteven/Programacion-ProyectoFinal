package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CargoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    private record DatosTabla(List<Contrato> contratos, List<Cargo> cargos) {
    }

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

        dpPeriodo.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA).withDayOfMonth(1));
        dpFechaEmision.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
        dpVenceEl.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA).plusDays(5));
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
        Task<List<Contrato>> tarea = new Task<>() {
            @Override
            protected List<Contrato> call() throws SQLException {
                return contratoService.listarVigentes();
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, this::mostrarContratos,
                error -> AlertaUtil.mostrarError("Error",
                        "No fue posible cargar los contratos vigentes de PostgreSQL."));
    }

    public void cargarDatos() {
        Task<List<Cargo>> tarea = new Task<>() {
            @Override
            protected List<Cargo> call() throws SQLException {
                return cargoService.listarTodos();
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, listaCargos::setAll,
                error -> AlertaUtil.mostrarError("Error al cargar cargos",
                        "No fue posible consultar las obligaciones desde PostgreSQL."));
    }

    @FXML
    public void refrescarTabla() {
        Task<DatosTabla> tarea = new Task<>() {
            @Override
            protected DatosTabla call() throws SQLException {
                return new DatosTabla(contratoService.listarVigentes(), cargoService.listarTodos());
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            mostrarContratos(datos.contratos());
            listaCargos.setAll(datos.cargos());
            limpiarFormulario();
            AlertaUtil.mostrarExito("Tabla actualizada",
                    "El registro de cargos ha sido sincronizado con PostgreSQL.");
        }, error -> AlertaUtil.mostrarError("Error al cargar cargos",
                "No fue posible sincronizar el registro de cargos desde PostgreSQL."));
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

        boolean nuevo = cargoSeleccionado == null;
        CargoService.Solicitud solicitud = new CargoService.Solicitud(
                nuevo ? null : cargoSeleccionado.getId(),
                nuevo ? 0 : cargoSeleccionado.getVersion(),
                contrato.getId(), tipo, periodo.withDayOfMonth(1), emision,
                vence, importe, concepto.trim(), estado);
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                cargoService.guardar(solicitud);
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            if (nuevo) {
                AlertaUtil.mostrarExito("Registro exitoso", "El cargo financiero fue registrado correctamente en PostgreSQL.");
            } else {
                AlertaUtil.mostrarExito("Actualización exitosa", "El cargo financiero fue actualizado.");
            }

            cargarDatos();
            limpiarFormulario();
        }, this::mostrarErrorCargo);
    }

    @FXML
    public void handleEliminar() {
        if (cargoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un cargo de la tabla para eliminar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar anulación",
                "¿Está seguro de que desea anular este cargo de " + cargoSeleccionado.getImporteFormateado() + "?"
        );

        if (confirma) {
            UUID cargoId = cargoSeleccionado.getId();
            Task<Void> tarea = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    cargoService.anular(cargoId);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                AlertaUtil.mostrarExito("Cargo anulado", "El cargo fue anulado y auditado.");
                cargarDatos();
                limpiarFormulario();
            }, this::mostrarErrorCargo);
        }
    }

    private void mostrarContratos(List<Contrato> contratos) {
        cmbContrato.setItems(FXCollections.observableArrayList(contratos));
        if (!contratos.isEmpty()) cmbContrato.getSelectionModel().selectFirst();
    }

    private void mostrarErrorCargo(Throwable error) {
        if (error instanceof IllegalArgumentException argumento) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", argumento.getMessage());
        } else if (error instanceof SecurityException permiso) {
            AlertaUtil.mostrarAdvertencia("Permiso denegado", permiso.getMessage());
        } else if (error instanceof IllegalStateException persistencia) {
            AlertaUtil.mostrarError("Error de persistencia", persistencia.getMessage());
        } else {
            AlertaUtil.mostrarError("Error de persistencia", "No fue posible guardar o anular el cargo en PostgreSQL.");
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
        dpPeriodo.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA).withDayOfMonth(1));
        dpFechaEmision.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
        dpVenceEl.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA).plusDays(5));
        tblCargos.getSelectionModel().clearSelection();
    }

    private void seleccionarCargo(Cargo c) {
        this.cargoSeleccionado = c;
        cmbTipo.setValue(c.getTipo());
        cmbEstado.setValue(c.getEstadoPersistido());
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
