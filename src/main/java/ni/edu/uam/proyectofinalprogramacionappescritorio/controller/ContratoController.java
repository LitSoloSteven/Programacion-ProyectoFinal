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
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.GarantiaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controlador de la vista de Contratos de arrendamiento.
 * Vincula unidades e inquilinos reales de PostgreSQL y valida coherencia contractual.
 */
public class ContratoController {

    @FXML
    private ComboBox<Unidad> cmbUnidad;

    @FXML
    private ComboBox<Inquilino> cmbInquilino;

    @FXML
    private DatePicker dpInicio;

    @FXML
    private DatePicker dpFin;

    @FXML
    private TextField txtCanon;

    @FXML
    private ComboBox<String> cmbMoneda;

    @FXML
    private TextField txtDiaPago;

    @FXML
    private TextField txtDeposito;

    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private Button btnTerminar;

    @FXML
    private Button btnEliminar;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableView<Contrato> tblContratos;

    @FXML
    private TableColumn<Contrato, String> colInquilino;

    @FXML
    private TableColumn<Contrato, String> colUnidad;

    @FXML
    private TableColumn<Contrato, LocalDate> colInicio;

    @FXML
    private TableColumn<Contrato, LocalDate> colFin;

    @FXML
    private TableColumn<Contrato, String> colCanon;

    @FXML
    private TableColumn<Contrato, Integer> colDiaPago;

    @FXML
    private TableColumn<Contrato, String> colDeposito;

    @FXML
    private TableColumn<Contrato, String> colEstado;

    @FXML
    private TableColumn<Contrato, String> colSaldoGarantia;

    @FXML
    private Button btnAjustarGarantia;

    private final ContratoService contratoService;
    private final UnidadService unidadService;
    private final InquilinoService inquilinoService;
    private final GarantiaService garantiaService;

    private final ObservableList<Contrato> listaContratos = FXCollections.observableArrayList();
    private FilteredList<Contrato> listaFiltrada;
    private Contrato contratoSeleccionado = null;

    private record Catalogos(List<Unidad> unidades, List<Inquilino> inquilinos) {
    }

    private record DatosTabla(Catalogos catalogos, List<Contrato> contratos) {
    }

    public ContratoController() {
        this(HelloApplication.instancia().getComposicion().contratos(),
                HelloApplication.instancia().getComposicion().unidades(),
                HelloApplication.instancia().getComposicion().inquilinos(),
                HelloApplication.instancia().getComposicion().garantias());
    }

    public ContratoController(ContratoService contratoService, UnidadService unidadService, InquilinoService inquilinoService) {
        this(contratoService, unidadService, inquilinoService, new GarantiaService());
    }

    public ContratoController(ContratoService contratoService, UnidadService unidadService,
                             InquilinoService inquilinoService, GarantiaService garantiaService) {
        this.contratoService = contratoService;
        this.unidadService = unidadService;
        this.inquilinoService = inquilinoService;
        this.garantiaService = garantiaService;
    }

    @FXML
    public void initialize() {
        inicializarCombos();
        configurarTabla();
        configurarFiltro();
        aplicarControlPermisos();

        cargarCatalogosReales();
        cargarDatos();

        tblContratos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarContrato(newVal);
                btnAjustarGarantia.setDisable(newVal.getGarantiaSeguimiento() == null);
            } else {
                btnAjustarGarantia.setDisable(true);
            }
        });

        // Al seleccionar una unidad, autocompletar el canon y la moneda
        cmbUnidad.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && contratoSeleccionado == null) {
                if (newVal.getCanonReferencia() != null) {
                    txtCanon.setText(newVal.getCanonReferencia().toPlainString());
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

        cmbEstado.setItems(FXCollections.observableArrayList("Vigente", "Borrador", "Por vencer", "Vencido", "Terminado", "Cancelado"));
        cmbEstado.getSelectionModel().selectFirst();

        txtDiaPago.setText("5");
    }

    private void configurarTabla() {
        colInquilino.setCellValueFactory(new PropertyValueFactory<>("inquilinoNombre"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadInfo"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("fin"));
        colCanon.setCellValueFactory(new PropertyValueFactory<>("canonFormateado"));
        colDiaPago.setCellValueFactory(new PropertyValueFactory<>("diaPago"));
        colDeposito.setCellValueFactory(new PropertyValueFactory<>("depositoFormateado"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colSaldoGarantia.setCellValueFactory(new PropertyValueFactory<>("garantiaSaldoFormateado"));

        listaFiltrada = new FilteredList<>(listaContratos, c -> true);
        tblContratos.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            listaFiltrada.setPredicate(c -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filtro = newVal.toLowerCase().trim();
                boolean matchInq = c.getInquilinoNombre() != null && c.getInquilinoNombre().toLowerCase().contains(filtro);
                boolean matchUni = c.getUnidadInfo() != null && c.getUnidadInfo().toLowerCase().contains(filtro);
                boolean matchEst = c.getEstado() != null && c.getEstado().toLowerCase().contains(filtro);
                return matchInq || matchUni || matchEst;
            });
        });
    }

    private void aplicarControlPermisos() {
        boolean esAdmin = SesionUsuario.esPropietario();
        btnEliminar.setVisible(esAdmin);
        btnEliminar.setManaged(esAdmin);
    }

    public void cargarCatalogosReales() {
        Task<Catalogos> tarea = new Task<>() {
            @Override
            protected Catalogos call() throws SQLException {
                return new Catalogos(unidadService.listarTodas(), inquilinoService.listarActivos());
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, this::mostrarCatalogos,
                error -> AlertaUtil.mostrarError("Error al cargar catálogos",
                        "No fue posible consultar unidades e inquilinos desde PostgreSQL."));
    }

    public void cargarDatos() {
        Task<List<Contrato>> tarea = new Task<>() {
            @Override
            protected List<Contrato> call() throws SQLException {
                return contratoService.listarTodos();
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, listaContratos::setAll,
                error -> AlertaUtil.mostrarError("Error al cargar contratos",
                        "No fue posible consultar los contratos desde PostgreSQL."));
    }

    @FXML
    public void refrescarTabla() {
        Task<DatosTabla> tarea = new Task<>() {
            @Override
            protected DatosTabla call() throws SQLException {
                Catalogos catalogos = new Catalogos(unidadService.listarTodas(), inquilinoService.listarActivos());
                return new DatosTabla(catalogos, contratoService.listarTodos());
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            mostrarCatalogos(datos.catalogos());
            listaContratos.setAll(datos.contratos());
            limpiarFormulario();
            AlertaUtil.mostrarExito("Tabla actualizada",
                    "Los contratos han sido sincronizados con PostgreSQL.");
        }, error -> AlertaUtil.mostrarError("Error al cargar contratos",
                "No fue posible sincronizar contratos y catálogos desde PostgreSQL."));
    }

    @FXML
    public void handleGuardar() {
        Unidad unidad = cmbUnidad.getValue();
        Inquilino inquilino = cmbInquilino.getValue();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();
        String strCanon = txtCanon.getText();
        String moneda = cmbMoneda.getValue();
        String strDiaPago = txtDiaPago.getText();
        String strDeposito = txtDeposito.getText();
        String estado = cmbEstado.getValue();

        // Validaciones de relaciones
        if (unidad == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe seleccionar una unidad inmobiliaria para el contrato.");
            cmbUnidad.requestFocus();
            return;
        }

        if (inquilino == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe seleccionar un inquilino arrendatario.");
            cmbInquilino.requestFocus();
            return;
        }

        // Validaciones de fechas
        if (inicio == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Seleccione la fecha de inicio del contrato.");
            dpInicio.requestFocus();
            return;
        }

        if (fin == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Seleccione la fecha de finalización del contrato.");
            dpFin.requestFocus();
            return;
        }

        if (!fin.isAfter(inicio)) {
            AlertaUtil.mostrarAdvertencia("Incoherencia en fechas", "La fecha final debe ser posterior a la fecha de inicio.");
            dpFin.requestFocus();
            return;
        }

        if (fin.isBefore(inicio.plusMonths(1))) {
            AlertaUtil.mostrarAdvertencia("Duración insuficiente", "El contrato debe tener una vigencia mínima de 1 mes.");
            dpFin.requestFocus();
            return;
        }

        // Validación de canon
        BigDecimal canon;
        try {
            canon = new BigDecimal(strCanon.trim());
            if (canon.compareTo(BigDecimal.ZERO) <= 0) {
                AlertaUtil.mostrarAdvertencia("Monto inválido", "El canon mensual debe ser mayor a cero.");
                txtCanon.requestFocus();
                return;
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Formato numérico inválido", "Ingrese un canon mensual válido (ej. 450.00).");
            txtCanon.requestFocus();
            return;
        }

        // Validación de día de pago
        int diaPago;
        try {
            diaPago = Integer.parseInt(strDiaPago.trim());
            if (diaPago < 1 || diaPago > 31) {
                AlertaUtil.mostrarAdvertencia("Día inválido", "El día de pago debe estar entre 1 y 31.");
                txtDiaPago.requestFocus();
                return;
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Formato inválido", "Ingrese un número entero para el día de pago (1 - 31).");
            txtDiaPago.requestFocus();
            return;
        }

        // Depósito
        BigDecimal deposito = BigDecimal.ZERO;
        if (strDeposito != null && !strDeposito.trim().isEmpty()) {
            try {
                deposito = new BigDecimal(strDeposito.trim());
                if (deposito.compareTo(BigDecimal.ZERO) < 0) {
                    AlertaUtil.mostrarAdvertencia("Monto inválido", "El depósito en garantía no puede ser negativo.");
                    txtDeposito.requestFocus();
                    return;
                }
            } catch (Exception e) {
                AlertaUtil.mostrarAdvertencia("Formato inválido", "Ingrese un monto válido para el depósito de garantía.");
                txtDeposito.requestFocus();
                return;
            }
        }

        boolean nuevo = contratoSeleccionado == null;
        UUID contratoId = nuevo ? null : contratoSeleccionado.getId();
        long version = nuevo ? 0 : contratoSeleccionado.getVersion();
        UUID unidadId = unidad.getId();
        UUID inquilinoId = inquilino.getId();
        BigDecimal canonGuardado = canon;
        BigDecimal depositoGuardado = deposito;
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                if (nuevo) {
                    // Nuevo contrato
                    Contrato contrato = new Contrato();
                    contrato.setUnidadId(unidadId);
                    contrato.setInquilinoId(inquilinoId);
                    contrato.setInicio(inicio);
                    contrato.setFin(fin);
                    contrato.setCanon(canonGuardado);
                    contrato.setMoneda(moneda);
                    contrato.setDiaPago(diaPago);
                    contrato.setDepositoPactado(depositoGuardado);
                    contrato.setEstado(estado);
                    contratoService.guardar(contrato);
                } else {
                    contratoService.actualizar(contratoId, version, unidadId,
                            inquilinoId, inicio, fin, canonGuardado, moneda, diaPago,
                            depositoGuardado, estado);
                }
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            if (nuevo) {
                AlertaUtil.mostrarExito("Registro exitoso", "El contrato de arrendamiento ha sido registrado en PostgreSQL.");
            } else {
                AlertaUtil.mostrarExito("Actualización exitosa", "Los términos del contrato fueron actualizados.");
            }
            cargarDatos();
            limpiarFormulario();
        }, error -> mostrarErrorContrato(error,
                "No se pudo guardar el contrato en PostgreSQL. Verifique los datos ingresados."));
    }

    @FXML
    public void handleTerminarContrato() {
        if (contratoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un contrato de la tabla para finalizarlo.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Finalizar contrato",
                "¿Desea marcar como Terminado el contrato de " + contratoSeleccionado.getInquilinoNombre() + "?"
        );

        if (confirma) {
            UUID contratoId = contratoSeleccionado.getId();
            Task<Void> tarea = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    contratoService.terminarContrato(contratoId);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                AlertaUtil.mostrarExito("Contrato finalizado", "El estado del contrato ha cambiado a 'Terminado'.");
                cargarDatos();
                limpiarFormulario();
            }, error -> mostrarErrorContrato(error,
                    "No fue posible finalizar el contrato en la base de datos."));
        }
    }

    @FXML
    public void handleAjustarGarantia() {
        if (contratoSeleccionado == null || contratoSeleccionado.getGarantiaSeguimiento() == null) {
            AlertaUtil.mostrarAdvertencia("Garantía no disponible", "Seleccione un contrato con seguimiento de garantía.");
            return;
        }
        BigDecimal saldoActual = contratoSeleccionado.getGarantiaSeguimiento().getSaldoActual();
        TextInputDialog dialogo = new TextInputDialog(saldoActual.toPlainString());
        dialogo.setTitle("Ajustar saldo de garantía");
        dialogo.setHeaderText("Contrato de " + contratoSeleccionado.getInquilinoNombre());
        dialogo.setContentText("Nuevo saldo retenido (0 a " + contratoSeleccionado.getGarantiaSeguimiento().getMontoInicial() + "): ");
        dialogo.showAndWait().ifPresent(valor -> {
            UUID garantiaId = contratoSeleccionado.getGarantiaSeguimiento().getId();
            BigDecimal nuevoSaldo;
            try {
                nuevoSaldo = new BigDecimal(valor.strip());
            } catch (NumberFormatException e) {
                AlertaUtil.mostrarAdvertencia("Saldo inválido", "Ingrese un monto decimal válido.");
                return;
            }
            Task<Void> tarea = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    garantiaService.ajustarSaldo(garantiaId, nuevoSaldo);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                cargarDatos();
                AlertaUtil.mostrarExito("Garantía actualizada", "El saldo retenido se actualizó correctamente.");
            }, error -> mostrarErrorGarantia(error));
        });
    }

    @FXML
    public void handleEliminar() {
        if (contratoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un contrato de la tabla para eliminar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de que desea eliminar permanentemente este contrato? Esta acción es irreversible."
        );

        if (confirma) {
            UUID contratoId = contratoSeleccionado.getId();
            Task<Void> tarea = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    contratoService.eliminar(contratoId);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                AlertaUtil.mostrarExito("Registro eliminado", "El contrato fue eliminado de PostgreSQL.");
                cargarDatos();
                limpiarFormulario();
            }, error -> {
                if (error instanceof SecurityException permiso) {
                    AlertaUtil.mostrarAdvertencia("Permiso denegado", permiso.getMessage());
                } else {
                    AlertaUtil.mostrarError("No se pudo eliminar",
                            "Verifique que el contrato no tenga cargos o pagos asociados y que PostgreSQL esté disponible.");
                }
            });
        }
    }

    private void mostrarCatalogos(Catalogos catalogos) {
        cmbUnidad.setItems(FXCollections.observableArrayList(catalogos.unidades()));
        cmbInquilino.setItems(FXCollections.observableArrayList(catalogos.inquilinos()));
    }

    private void mostrarErrorContrato(Throwable error, String errorPersistencia) {
        if (error instanceof IllegalArgumentException argumento) {
            AlertaUtil.mostrarAdvertencia("Validación de contrato", argumento.getMessage());
        } else if (error instanceof SecurityException permiso) {
            AlertaUtil.mostrarAdvertencia("Permiso denegado", permiso.getMessage());
        } else if (error instanceof IllegalStateException persistencia) {
            AlertaUtil.mostrarError("Error de persistencia", persistencia.getMessage());
        } else {
            AlertaUtil.mostrarError("Error de persistencia", errorPersistencia);
        }
    }

    private void mostrarErrorGarantia(Throwable error) {
        if (error instanceof IllegalArgumentException argumento) {
            AlertaUtil.mostrarAdvertencia("Saldo inválido",
                    argumento.getMessage() == null ? "Ingrese un monto decimal válido." : argumento.getMessage());
        } else if (error instanceof SecurityException permiso) {
            AlertaUtil.mostrarAdvertencia("Permiso denegado", permiso.getMessage());
        } else if (error instanceof IllegalStateException persistencia) {
            AlertaUtil.mostrarError("No se pudo actualizar", persistencia.getMessage());
        } else {
            AlertaUtil.mostrarError("No se pudo actualizar", "No fue posible guardar el saldo de la garantía.");
        }
    }

    @FXML
    public void limpiarFormulario() {
        contratoSeleccionado = null;
        btnAjustarGarantia.setDisable(true);
        cmbUnidad.getSelectionModel().clearSelection();
        cmbInquilino.getSelectionModel().clearSelection();
        dpInicio.setValue(null);
        dpFin.setValue(null);
        txtCanon.clear();
        txtDeposito.clear();
        txtDiaPago.setText("5");
        if (!cmbMoneda.getItems().isEmpty()) cmbMoneda.getSelectionModel().selectFirst();
        if (!cmbEstado.getItems().isEmpty()) cmbEstado.setValue("Vigente");
        tblContratos.getSelectionModel().clearSelection();
    }

    private void seleccionarContrato(Contrato c) {
        this.contratoSeleccionado = c;
        dpInicio.setValue(c.getInicio());
        dpFin.setValue(c.getFin());
        txtCanon.setText(c.getCanon() != null ? c.getCanon().toPlainString() : "");
        txtDeposito.setText(c.getDepositoPactado() != null ? c.getDepositoPactado().toPlainString() : "");
        txtDiaPago.setText(String.valueOf(c.getDiaPago()));
        cmbMoneda.setValue(c.getMoneda());
        cmbEstado.setValue(c.getEstado());

        for (Unidad u : cmbUnidad.getItems()) {
            if (u.getId().equals(c.getUnidadId())) {
                cmbUnidad.setValue(u);
                break;
            }
        }

        for (Inquilino i : cmbInquilino.getItems()) {
            if (i.getId().equals(c.getInquilinoId())) {
                cmbInquilino.setValue(i);
                break;
            }
        }
    }
}
