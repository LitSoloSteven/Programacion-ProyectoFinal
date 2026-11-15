package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PagoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private Button btnPaginaAnterior;

    @FXML
    private Button btnPaginaSiguiente;

    @FXML
    private Label lblPaginacion;

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
    private final ClientePagos clientePagos;

    private final Map<PagoRequest, UUID> clavesPagosPendientes = new HashMap<>();

    private final ObservableList<Pago> listaPagos = FXCollections.observableArrayList();
    private Pago pagoSeleccionado = null;
    private int paginaActual = 1;
    private long versionConsulta;

    private record DatosTabla(List<Contrato> contratos, PaginaResponse<Pago> pagos) {
    }

    public PagoController() {
        this(HelloApplication.instancia().getComposicion().pagos(),
                HelloApplication.instancia().getComposicion().contratos(), new ClientePagos());
    }

    public PagoController(PagoService pagoService, ContratoService contratoService) {
        this(pagoService, contratoService, new ClientePagos());
    }

    public PagoController(PagoService pagoService, ContratoService contratoService,
                          ClientePagos clientePagos) {
        this.pagoService = pagoService;
        this.contratoService = contratoService;
        this.clientePagos = clientePagos;
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

        dpFechaPago.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
    }

    private void configurarTabla() {
        colContrato.setCellValueFactory(new PropertyValueFactory<>("contratoInfo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaPago"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("montoFormateado"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodo"));
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referencia"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tblPagos.setItems(listaPagos);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            paginaActual = 1;
            cargarDatos();
        });
    }

    private void aplicarControlPermisos() {
        boolean esAdmin = SesionUsuario.esPropietario();
        btnAnular.setVisible(esAdmin);
        btnAnular.setManaged(esAdmin);
    }

    public void cargarContratosReales() {
        Task<List<Contrato>> tarea = new Task<>() {
            @Override
            protected List<Contrato> call() throws SQLException {
                return contratoService.listarVigentes();
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, contratos -> {
            cmbContrato.setItems(FXCollections.observableArrayList(contratos));
            if (!contratos.isEmpty()) cmbContrato.getSelectionModel().selectFirst();
        }, error -> AlertaUtil.mostrarError("Error",
                "No se pudieron obtener los contratos vigentes desde PostgreSQL."));
    }

    public void cargarDatos() {
        int pagina = paginaActual;
        String texto = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<PaginaResponse<Pago>> tarea = new Task<>() {
            @Override
            protected PaginaResponse<Pago> call() {
                return pagoService.listarTodos(pagina, PagoService.TAMANO_PAGINA_MAXIMO, texto);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, resultado -> {
            if (version == versionConsulta) mostrarPagina(resultado);
        }, error -> {
            if (version == versionConsulta) AlertaUtil.mostrarError("Error al cargar pagos",
                    "No fue posible consultar el historial de pagos desde PostgreSQL.");
        });
    }

    @FXML
    public void refrescarTabla() {
        int pagina = paginaActual;
        String texto = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override
            protected DatosTabla call() throws SQLException {
                return new DatosTabla(contratoService.listarVigentes(),
                        pagoService.listarTodos(pagina, PagoService.TAMANO_PAGINA_MAXIMO, texto));
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            cmbContrato.setItems(FXCollections.observableArrayList(datos.contratos()));
            if (!datos.contratos().isEmpty()) cmbContrato.getSelectionModel().selectFirst();
            mostrarPagina(datos.pagos());
            limpiarFormulario();
            AlertaUtil.mostrarExito("Tabla actualizada",
                    "El historial de pagos ha sido sincronizado con PostgreSQL.");
        }, error -> {
            if (version == versionConsulta) AlertaUtil.mostrarError("Error al cargar pagos",
                    "No fue posible sincronizar el historial de pagos desde PostgreSQL.");
        });
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

        String referenciaLimpia = referencia != null && !referencia.isBlank()
                ? referencia.trim() : null;
        String notasLimpias = notas != null && !notas.isBlank() ? notas.trim() : null;
        PagoRequest solicitud = new PagoRequest(contrato.getId().toString(),
                fecha.toString(), moneda, monto.toPlainString(), metodo,
                referenciaLimpia, List.of(), notasLimpias);
        UUID clave = clavesPagosPendientes.computeIfAbsent(solicitud,
                ignorada -> UUID.randomUUID());
        Task<PagoRegistroResponse> tarea = new Task<>() {
            @Override
            protected PagoRegistroResponse call() {
                return clientePagos.registrar(solicitud, clave);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, registro -> {
            clavesPagosPendientes.remove(solicitud, clave);
            AlertaUtil.mostrarExito("Pago Registrado",
                    "El pago por " + String.format("%.2f %s", monto, moneda)
                            + " fue guardado. Recibo #" + registro.numeroRecibo() + ".");
            paginaActual = 1;
            cargarDatos();
            limpiarFormulario();
        }, this::mostrarErrorPago);
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
            UUID pagoId = pagoSeleccionado.getId();
            Task<Void> tarea = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    pagoService.anular(pagoId);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                AlertaUtil.mostrarExito("Pago anulado", "El estado del pago fue cambiado a 'Anulado'.");
                cargarDatos();
                limpiarFormulario();
            }, this::mostrarErrorPago);
        }
    }

    private void mostrarErrorPago(Throwable error) {
        if (error instanceof ErrorClienteException cliente) {
            AlertaUtil.mostrarError("No se pudo confirmar el pago",
                    cliente.getMessage() + " Si el resultado es incierto, reintenta sin cambiar los datos.");
        } else if (error instanceof IllegalArgumentException argumento) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", argumento.getMessage());
        } else if (error instanceof SecurityException permiso) {
            AlertaUtil.mostrarAdvertencia("Permiso denegado", permiso.getMessage());
        } else if (error instanceof IllegalStateException persistencia) {
            AlertaUtil.mostrarError("Error de persistencia", persistencia.getMessage());
        } else {
            AlertaUtil.mostrarError("Error", "No fue posible completar la operación de pago en PostgreSQL.");
        }
    }

    private void mostrarPagina(PaginaResponse<Pago> pagina) {
        int paginas = (int) Math.max(1, (pagina.total() + pagina.tamano() - 1) / pagina.tamano());
        if (paginaActual > paginas) {
            paginaActual = paginas;
            cargarDatos();
            return;
        }
        listaPagos.setAll(pagina.elementos());
        lblPaginacion.setText("Página " + paginaActual + " de " + paginas
                + " · " + pagina.total() + " pagos");
        btnPaginaAnterior.setDisable(paginaActual <= 1);
        btnPaginaSiguiente.setDisable(paginaActual >= paginas);
    }

    @FXML
    public void paginaAnterior() {
        if (paginaActual <= 1) return;
        paginaActual--;
        cargarDatos();
    }

    @FXML
    public void paginaSiguiente() {
        paginaActual++;
        cargarDatos();
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
        dpFechaPago.setValue(LocalDate.now(RelojService.ZONA_OPERATIVA));
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
