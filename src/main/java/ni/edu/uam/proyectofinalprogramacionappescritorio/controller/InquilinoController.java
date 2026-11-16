package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Directorio local de inquilinos y responsable de las operaciones del servicio. */
public class InquilinoController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnDarBaja;
    @FXML private Button btnEliminar;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Inquilino> tblInquilinos;
    @FXML private TableColumn<Inquilino, String> colNombre;
    @FXML private TableColumn<Inquilino, String> colIdentificacion;
    @FXML private TableColumn<Inquilino, String> colTelefono;
    @FXML private TableColumn<Inquilino, String> colCorreo;
    @FXML private TableColumn<Inquilino, String> colEmergencia;
    @FXML private TableColumn<Inquilino, String> colEstado;
    @FXML private TableColumn<Inquilino, Void> colAcciones;

    private final InquilinoService inquilinoService;
    private final ObservableList<Inquilino> registros = FXCollections.observableArrayList();
    private final FilteredList<Inquilino> filtrados = new FilteredList<>(registros, item -> true);
    private Inquilino seleccionada;
    private final boolean permisoEliminar = SesionUsuario.esPropietario();
    private long versionConsulta;

    public InquilinoController() { this(HelloApplication.instancia().getComposicion().inquilinos()); }
    public InquilinoController(InquilinoService inquilinoService) { this.inquilinoService = inquilinoService; }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PERSONAS, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnDarBaja, Iconos.Nombre.EDITAR, 16);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona un inquilino para eliminar el registro." : "Solo el propietario puede eliminar inquilinos."));
        btnDarBaja.setDisable(true);
        btnEliminar.setDisable(true);
        tblInquilinos.setItems(filtrados);
        tblInquilinos.setPlaceholder(PanelEstadoTabla.crear("Cargando inquilinos", "El directorio aparecerá al terminar la consulta.", null, null));
        colNombre.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getNombre()));
        colIdentificacion.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getIdentificacion()));
        colTelefono.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getTelefono()));
        colCorreo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getCorreo()));
        colEmergencia.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getContactoEmergencia()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getEstadoTexto()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>(
                "Editar inquilino", this::abrirEdicion, Iconos.Nombre.EDITAR)));
        tblInquilinos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionada = actual;
            btnDarBaja.setDisable(actual == null || !actual.isActivo());
            btnEliminar.setDisable(actual == null || !permisoEliminar);
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro(actual));
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        aplicarFiltro("");
        cargarDatos();
    }

    private void aplicarFiltro(String texto) {
        String filtro = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(i -> filtro.isEmpty() || contiene(i.getNombre(), filtro)
                || contiene(i.getIdentificacion(), filtro) || contiene(i.getTelefono(), filtro)
                || contiene(i.getCorreo(), filtro) || contiene(i.getContactoEmergencia(), filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblInquilinos.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay inquilinos" : "No hay coincidencias",
                filtro.isEmpty() ? "Los inquilinos registrados aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? "Nuevo inquilino" : "Limpiar búsqueda",
                filtro.isEmpty() ? this::handleNuevo : this::limpiarFiltros));
    }

    public void cargarDatos() {
        btnRefrescar.setDisable(true);
        long version = ++versionConsulta;
        Task<List<Inquilino>> tarea = new Task<>() {
            @Override protected List<Inquilino> call() throws SQLException { return inquilinoService.listarTodos(); }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            registros.setAll(datos);
            aplicarFiltro(txtBuscar.getText());
            ocultarError();
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) {
                lblErrorConsulta.setText("No se pudieron cargar los inquilinos. Vuelve a intentarlo.");
                lblErrorConsulta.setVisible(true); lblErrorConsulta.setManaged(true);
                tblInquilinos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los inquilinos",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
            btnRefrescar.setDisable(false);
        });
    }

    @FXML public void refrescarTabla() { cargarDatos(); }
    @FXML public void handleNuevo() { abrirFormulario(null); }
    private void abrirEdicion(Inquilino inquilino) { abrirFormulario(inquilino); }

    private void abrirFormulario(Inquilino existente) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("inquilino-form.fxml"));
            Node contenido = loader.load();
            InquilinoFormController formulario = loader.getController();
            if (existente != null) formulario.cargar(existente);
            Window propietario = tblInquilinos.getScene() == null ? null : tblInquilinos.getScene().getWindow();
            boolean nuevo = existente == null;
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(nuevo ? "Nuevo inquilino" : "Editar inquilino",
                    contenido, propietario, 620, 620, nuevo ? "Guardar inquilino" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nuevo ? btnNuevo : tblInquilinos, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                InquilinoFormController.Datos datos = formulario.datos();
                accion.setDisable(true); accion.setText("Guardando inquilino…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        if (nuevo) {
                            ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino inquilino = new ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino();
                            inquilino.setNombre(datos.nombre());
                            inquilino.setIdentificacion(datos.identificacion());
                            inquilino.setTelefono(datos.telefono());
                            inquilino.setCorreo(datos.correo());
                            inquilino.setContactoEmergencia(datos.emergencia());
                            inquilino.setActivo(datos.activo());
                            inquilinoService.guardar(inquilino);
                        } else {
                            inquilinoService.actualizar(existente.getId(), existente.getVersion(), datos.nombre(),
                                    datos.identificacion(), datos.telefono(), datos.correo(), datos.emergencia(), datos.activo());
                        }
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                    formulario.marcarLimpio(); DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito(nuevo ? "Inquilino registrado" : "Inquilino actualizado",
                            nuevo ? "El inquilino quedó registrado." : "Los cambios quedaron guardados.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText(nuevo ? "Guardar inquilino" : "Guardar cambios");
                    formulario.mostrarError(mensajeError(error));
                });
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleDarDeBaja() {
        Inquilino inquilino = seleccionada;
        if (inquilino == null || !inquilino.isActivo()) return;
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Deshabilitar inquilino",
                inquilino.getNombre() + (inquilino.getIdentificacion() == null ? "" : " · " + inquilino.getIdentificacion()),
                "El inquilino quedará inactivo; su historial se conservará.", "Deshabilitar")) return;
        btnDarBaja.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { inquilinoService.darDeBaja(inquilino.getId()); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Inquilino deshabilitado", "El inquilino quedó inactivo.");
            cargarDatos();
        }, error -> {
            btnDarBaja.setDisable(false);
            AlertaUtil.mostrarError("No se pudo deshabilitar el inquilino", "Vuelve a intentarlo.");
        });
    }

    @FXML public void handleEliminar() {
        Inquilino inquilino = seleccionada;
        if (inquilino == null || !permisoEliminar) return;
        String registro = "Inquilino: " + inquilino.getNombre()
                + (inquilino.getIdentificacion() == null ? "" : " · " + inquilino.getIdentificacion());
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar registro de inquilino", registro,
                "El registro se eliminará físicamente y no podrá restaurarse.", "Eliminar registro")) return;
        btnEliminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { inquilinoService.eliminar(inquilino.getId()); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Registro eliminado", "El registro del inquilino se eliminó.");
            cargarDatos();
        }, error -> {
            btnEliminar.setDisable(false);
            if (error instanceof SecurityException) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar registros de inquilinos.");
            } else {
                AlertaUtil.mostrarAdvertencia("No se puede eliminar el registro",
                        "El inquilino tiene contratos o historial asociado. Usa Deshabilitar para conservarlo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }
    private void ocultarError() { lblErrorConsulta.setText(""); lblErrorConsulta.setVisible(false); lblErrorConsulta.setManaged(false); }
    private static boolean contiene(String valor, String filtro) { return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro); }
    private static String mensajeError(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof SecurityException
                || error instanceof IllegalStateException) return error.getMessage();
        return "No se pudo guardar el inquilino. Revisa los datos e inténtalo de nuevo.";
    }
}
