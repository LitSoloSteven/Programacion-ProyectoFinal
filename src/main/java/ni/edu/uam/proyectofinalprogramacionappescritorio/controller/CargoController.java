package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CargoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Lista paginada de cargos y responsable de las llamadas del diálogo financiero. */
public class CargoController {
    @FXML private VBox rootView;
    @FXML private HBox anularTooltipHost;
    @FXML private Button btnAnular;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
    @FXML private Button btnRefrescar;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private Label lblPaginacion;
    @FXML private Label lblTitulo;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Cargo> tblCargos;
    @FXML private TableColumn<Cargo, String> colContrato;
    @FXML private TableColumn<Cargo, String> colTipo;
    @FXML private TableColumn<Cargo, String> colConcepto;
    @FXML private TableColumn<Cargo, String> colImporte;
    @FXML private TableColumn<Cargo, LocalDate> colVence;
    @FXML private TableColumn<Cargo, String> colEstado;
    @FXML private TableColumn<Cargo, Void> colAcciones;

    private final CargoService cargoService;
    private final ContratoService contratoService;
    private final ObservableList<Cargo> listaCargos = FXCollections.observableArrayList();
    private List<Contrato> contratosDisponibles = List.of();
    private Cargo cargoSeleccionado;
    private int paginaActual = 1;
    private long versionConsulta;
    private boolean permisoAnular;

    private record DatosTabla(List<Contrato> contratos, PaginaResponse<Cargo> cargos) { }

    public CargoController() {
        this(HelloApplication.instancia().getComposicion().cargos(),
                HelloApplication.instancia().getComposicion().contratos());
    }

    public CargoController(CargoService cargoService, ContratoService contratoService) {
        this.cargoService = cargoService;
        this.contratoService = contratoService;
    }

    @FXML
    public void initialize() {
        permisoAnular = SesionUsuario.esPropietario();
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.CARGO, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnAnular, Iconos.Nombre.ELIMINAR, 16);
        btnAnular.setDisable(true);
        Tooltip.install(anularTooltipHost,
                new Tooltip(permisoAnular ? "Selecciona un cargo para anularlo." : "Solo el propietario puede anular cargos."));
        btnNuevo.setDisable(true);
        tblCargos.setItems(listaCargos);
        tblCargos.setPlaceholder(PanelEstadoTabla.crear("Cargando cargos", "La lista se actualizará al terminar la consulta.", null, null));
        colContrato.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(
                cell.getValue().getContratoInfo()));
        colTipo.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().getTipo()));
        colConcepto.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().getConcepto()));
        colImporte.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().getImporteFormateado()));
        colImporte.setCellFactory(FormatoTabla.numerica());
        colVence.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue().getVenceEl()));
        colVence.setCellFactory(FormatoTabla.fecha());
        colEstado.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue().getEstado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>(
                "Editar cargo", this::abrirEdicion, Iconos.Nombre.EDITAR)));
        tblCargos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            cargoSeleccionado = nuevo;
            btnAnular.setDisable(nuevo == null || !permisoAnular);
            if (nuevo == null) Tooltip.install(anularTooltipHost,
                    new Tooltip(permisoAnular ? "Selecciona un cargo para anularlo." : "Solo el propietario puede anular cargos."));
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> {
            paginaActual = 1;
            cargarPagina();
        });
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        cargarContratosYPagina();
    }

    private void cargarContratosYPagina() {
        long version = ++versionConsulta;
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                return new DatosTabla(contratoService.listarVigentes(),
                        cargoService.listarTodos(pagina, CargoService.TAMANO_PAGINA_MAXIMO, filtro));
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, resultado -> {
            if (version != versionConsulta) return;
            contratosDisponibles = resultado.contratos();
            btnNuevo.setDisable(contratosDisponibles.isEmpty());
            btnRefrescar.setDisable(false);
            if (contratosDisponibles.isEmpty()) {
                lblErrorConsulta.setText("No hay contratos vigentes para registrar cargos.");
                lblErrorConsulta.setVisible(true);
                lblErrorConsulta.setManaged(true);
            }
            mostrarPagina(resultado.cargos());
        }, error -> {
            if (version != versionConsulta) return;
            mostrarErrorConsulta(error);
            btnRefrescar.setDisable(false);
            tblCargos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los cargos",
                    "Revisa la conexión e inténtalo de nuevo.", "Reintentar", this::cargarContratosYPagina));
        });
    }

    private void cargarPagina() {
        int pagina = paginaActual;
        String filtro = txtBuscar.getText();
        long version = ++versionConsulta;
        Task<PaginaResponse<Cargo>> tarea = new Task<>() {
            @Override protected PaginaResponse<Cargo> call() {
                return cargoService.listarTodos(pagina, CargoService.TAMANO_PAGINA_MAXIMO, filtro);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, resultado -> {
            if (version == versionConsulta) mostrarPagina(resultado);
        }, error -> {
            if (version == versionConsulta) mostrarErrorConsulta(error);
        });
    }

    private void mostrarPagina(PaginaResponse<Cargo> resultado) {
        int paginas = (int) Math.max(1, (resultado.total() + resultado.tamano() - 1) / resultado.tamano());
        if (paginaActual > paginas) {
            paginaActual = paginas;
            cargarPagina();
            return;
        }
        listaCargos.setAll(resultado.elementos());
        long desde = resultado.total() == 0 ? 0 : (long) (paginaActual - 1) * resultado.tamano() + 1;
        long hasta = resultado.total() == 0 ? 0 : desde + resultado.elementos().size() - 1;
        lblPaginacion.setText(desde + "–" + hasta + " de " + resultado.total() + " cargos");
        lblContador.setText(resultado.total() + (resultado.total() == 1 ? " cargo" : " cargos"));
        btnPaginaAnterior.setDisable(paginaActual <= 1);
        btnPaginaSiguiente.setDisable(paginaActual >= paginas);
        tblCargos.setPlaceholder(PanelEstadoTabla.crear(
                txtBuscar.getText().isBlank() ? "Aún no hay cargos" : "No hay coincidencias",
                txtBuscar.getText().isBlank() ? "Los cargos registrados aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                txtBuscar.getText().isBlank() && !contratosDisponibles.isEmpty() ? "Nuevo cargo" : null,
                txtBuscar.getText().isBlank() && !contratosDisponibles.isEmpty() ? this::handleNuevo : null));
        lblErrorConsulta.setText("");
        lblErrorConsulta.setVisible(false);
        lblErrorConsulta.setManaged(false);
    }

    @FXML public void handleNuevo() { abrirFormulario(null); }

    private void abrirEdicion(Cargo cargo) { abrirFormulario(cargo); }

    private void abrirFormulario(Cargo cargo) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("cargo-form.fxml"));
            Node contenido = loader.load();
            CargoFormController formulario = loader.getController();
            formulario.establecerContratos(contratosDisponibles);
            if (cargo != null) formulario.cargar(cargo);
            Window propietario = tblCargos.getScene() == null ? null : tblCargos.getScene().getWindow();
            String textoAccion = cargo == null ? "Guardar cargo" : "Guardar cambios";
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(cargo == null ? "Nuevo cargo" : "Editar cargo",
                    contenido, propietario, 720, 680, textoAccion);
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, cargo == null ? btnNuevo : tblCargos,
                    formulario::hayCambios);
            ButtonType accion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(accion);
            botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                guardarEnDialogo(dialogo, botonGuardar, formulario, cargo);
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    private void guardarEnDialogo(Dialog<ButtonType> dialogo, Button botonGuardar,
                                  CargoFormController formulario, Cargo cargo) {
        if (!formulario.validar()) return;
        CargoFormController.Datos datos = formulario.datos();
        boolean nuevo = cargo == null;
        CargoService.Solicitud solicitud = new CargoService.Solicitud(
                nuevo ? null : cargo.getId(), nuevo ? 0 : cargo.getVersion(), datos.contrato().getId(),
                datos.tipo(), datos.periodo(), datos.emision(), datos.vencimiento(), datos.importe(),
                datos.concepto(), datos.estado());
        botonGuardar.setDisable(true);
        botonGuardar.setText("Guardando cargo…");
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException {
                cargoService.guardar(solicitud);
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            formulario.marcarLimpio();
            DialogoFormulario.cerrar(dialogo);
            AlertaUtil.mostrarExito(nuevo ? "Cargo registrado" : "Cargo actualizado",
                    nuevo ? "El cargo quedó registrado." : "Los cambios del cargo quedaron guardados.");
            paginaActual = 1;
            cargarPagina();
        }, error -> {
            botonGuardar.setDisable(false);
            botonGuardar.setText(nuevo ? "Guardar cargo" : "Guardar cambios");
            formulario.mostrarError(mensajeError(error));
        });
    }

    @FXML public void refrescarTabla() {
        btnRefrescar.setDisable(true);
        cargarContratosYPagina();
    }

    @FXML public void limpiarFiltros() {
        txtBuscar.clear();
        paginaActual = 1;
        cargarPagina();
    }

    @FXML public void paginaAnterior() {
        if (paginaActual <= 1) return;
        paginaActual--;
        cargarPagina();
    }

    @FXML public void paginaSiguiente() {
        paginaActual++;
        cargarPagina();
    }

    @FXML public void handleAnular() {
        Cargo cargo = cargoSeleccionado;
        if (cargo == null || !permisoAnular) return;
        String registro = "Cargo: " + cargo.getConcepto() + " · " + cargo.getImporteFormateado()
                + " · vencimiento " + cargo.getVenceEl() + " · " + cargo.getContratoInfo();
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Anular cargo", registro,
                "El cargo quedará cancelado y dejará de estar pendiente.", "Anular cargo")) return;
        UUID cargoId = cargo.getId();
        btnAnular.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException {
                cargoService.anular(cargoId);
                return null;
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            cargoSeleccionado = null;
            AlertaUtil.mostrarExito("Cargo anulado", "El cargo quedó anulado.");
            cargarPagina();
        }, error -> {
            btnAnular.setDisable(cargoSeleccionado == null || !permisoAnular);
            AlertaUtil.mostrarError("No se pudo anular el cargo", mensajeError(error));
        });
    }

    private void mostrarErrorConsulta(Throwable error) {
        lblErrorConsulta.setText("No se pudieron actualizar los cargos. " + mensajeError(error));
        lblErrorConsulta.setVisible(true);
        lblErrorConsulta.setManaged(true);
    }

    private static String mensajeError(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException
                || error instanceof SecurityException) return error.getMessage();
        return "Revisa los datos y vuelve a intentarlo.";
    }
}
