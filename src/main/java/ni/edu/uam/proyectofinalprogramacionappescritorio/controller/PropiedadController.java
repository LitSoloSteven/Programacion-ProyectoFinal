package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import lombok.RequiredArgsConstructor;

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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

/** Lista de propiedades y responsable del ciclo de guardado con PropiedadService. */
@RequiredArgsConstructor
public class PropiedadController {
    @FXML private VBox rootView;
    @FXML private Button btnGenerarReportePropiedades;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnEliminar;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Propiedad> tblPropiedades;
    @FXML private TableColumn<Propiedad, String> colNombre;
    @FXML private TableColumn<Propiedad, String> colDireccion;
    @FXML private TableColumn<Propiedad, String> colEstado;
    @FXML private TableColumn<Propiedad, Timestamp> colFecha;
    @FXML private TableColumn<Propiedad, Void> colAcciones;

    private final ClienteCatalogos catalogos = new ClienteCatalogos(ClienteLatch.api());
    private final ReporteService reporteService = new ReporteService();
    private final PropiedadService propiedadService;
    private final ObservableList<Propiedad> registros = FXCollections.observableArrayList();
    private final FilteredList<Propiedad> filtrados = new FilteredList<>(registros, item -> true);
    private Propiedad seleccionada;
    private final boolean permisoEliminar = SesionUsuario.esPropietario();
    private long versionConsulta;

    public PropiedadController() { this(HelloApplication.instancia().getComposicion().propiedades()); }
    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PROPIEDAD, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnGenerarReportePropiedades != null) {
            Iconos.asignar(btnGenerarReportePropiedades, Iconos.Nombre.REPORTE, 16);
            btnGenerarReportePropiedades.setTooltip(new Tooltip("Vista previa y exportación PDF del inventario de propiedades y unidades"));
        }
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        if (!permisoEliminar) {
            btnNuevo.setDisable(true);
            btnNuevo.setTooltip(new Tooltip("Solo el propietario puede registrar propiedades."));
        }
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona una propiedad para eliminarla." : "Solo el propietario puede eliminar propiedades."));
        btnEliminar.setDisable(true);
        tblPropiedades.setItems(filtrados);
        tblPropiedades.setPlaceholder(PanelEstadoTabla.crear("Cargando propiedades",
                "El listado aparecerá al terminar la consulta.", null, null));
        colNombre.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getNombre()));
        colDireccion.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getDireccion()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getEstadoTexto()));
        colEstado.setCellFactory(FormatoTabla.estado());
        colFecha.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().getCreadoEn()));
        colFecha.setCellFactory(FormatoTabla.fechaHora());
        AccionesTabla.configurar(colAcciones, permisoEliminar
                ? List.of(new AccionesTabla.Accion<>("Editar propiedad", this::abrirEdicion, Iconos.Nombre.EDITAR))
                : List.of());
        tblPropiedades.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionada = actual;
            btnEliminar.setDisable(actual == null || !permisoEliminar);
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro(actual));
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        aplicarFiltro("");
        cargarDatos();
    }

    private void aplicarFiltro(String texto) {
        String filtro = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(propiedad -> filtro.isEmpty()
                || propiedad.getNombre().toLowerCase(Locale.ROOT).contains(filtro)
                || propiedad.getDireccion().toLowerCase(Locale.ROOT).contains(filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblPropiedades.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay propiedades" : "No hay coincidencias",
                filtro.isEmpty() ? "Las propiedades registradas aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? (permisoEliminar ? "Nueva propiedad" : null) : "Limpiar búsqueda",
                filtro.isEmpty() ? (permisoEliminar ? this::handleNuevo : null) : this::limpiarFiltros));
    }

    public void cargarDatos() {
        btnRefrescar.setDisable(true);
        long version = ++versionConsulta;
        Task<List<Propiedad>> tarea = new Task<>() {
            @Override protected List<Propiedad> call() throws SQLException { return catalogos.propiedades(false); }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            registros.setAll(datos);
            aplicarFiltro(txtBuscar.getText());
            ocultarError();
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) {
                lblErrorConsulta.setText("No se pudieron cargar las propiedades. Vuelve a intentarlo.");
                lblErrorConsulta.setVisible(true);
                lblErrorConsulta.setManaged(true);
                tblPropiedades.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar las propiedades",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
            btnRefrescar.setDisable(false);
        });
    }

    @FXML public void refrescarTabla() { cargarDatos(); }
    @FXML public void handleNuevo() { if (permisoEliminar) abrirFormulario(null); }
    private void abrirEdicion(Propiedad propiedad) { if (permisoEliminar) abrirFormulario(propiedad); }

    private void abrirFormulario(Propiedad existente) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("propiedad-form.fxml"));
            Node contenido = loader.load();
            PropiedadFormController formulario = loader.getController();
            if (existente != null) formulario.cargar(existente);
            Window propietario = tblPropiedades.getScene() == null ? null : tblPropiedades.getScene().getWindow();
            boolean nueva = existente == null;
            Dialog<javafx.scene.control.ButtonType> dialogo = DialogoFormulario.crear(
                    nueva ? "Nueva propiedad" : "Editar propiedad", contenido, propietario, 620, 600,
                    nueva ? "Guardar propiedad" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nueva ? btnNuevo : tblPropiedades, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                PropiedadFormController.Datos datos = formulario.datos();
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        catalogos.guardarPropiedad(nueva ? null : existente.getId(),
                                new PropiedadRequest(datos.nombre(), datos.direccion(),
                            datos.activa(), datos.nombrePropietario(), datos.correoPropietario(),
                            datos.telefonoPropietario(), nueva ? null : existente.getVersion()));
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                    formulario.marcarLimpio();
                    DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito(nueva ? "Propiedad registrada" : "Propiedad actualizada",
                            nueva ? "La propiedad quedó registrada." : "Los cambios quedaron guardados.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false);
                    accion.setText(nueva ? "Guardar propiedad" : "Guardar cambios");
                    if (!nueva && error instanceof ErrorClienteException api && api.getEstado() == 409) {
                        DialogoFormulario.cerrar(dialogo);
                        cargarDatos();
                        AlertaUtil.mostrarAdvertencia("No se guardaron los cambios",
                                api.getMessage() + " Abre el registro actualizado para volver a editarlo.");
                        return;
                    }
                    formulario.mostrarError(mensajeError(error));
                });
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleEliminar() {
        Propiedad propiedad = seleccionada;
        if (propiedad == null || !permisoEliminar) return;
        String registro = "Propiedad: " + propiedad.getNombre() + " · " + propiedad.getDireccion();
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar propiedad", registro,
                "El registro se eliminará físicamente si no tiene unidades asociadas.", "Eliminar propiedad")) return;
        btnEliminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { propiedadService.eliminar(propiedad.getId()); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            seleccionada = null;
            AlertaUtil.mostrarExito("Propiedad eliminada", "El registro de la propiedad se eliminó.");
            cargarDatos();
        }, error -> {
            btnEliminar.setDisable(seleccionada == null || !permisoEliminar);
            if (error instanceof ConflictoDatosException) {
                AlertaUtil.mostrarAdvertencia("La propiedad tiene unidades", error.getMessage()
                        + " Puedes desactivarla desde el formulario de propiedad.");
            } else if (error instanceof SecurityException) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar propiedades.");
            } else {
                AlertaUtil.mostrarError("No se pudo eliminar la propiedad", "Revisa las relaciones asociadas e inténtalo de nuevo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }
    private void ocultarError() { lblErrorConsulta.setText(""); lblErrorConsulta.setVisible(false); lblErrorConsulta.setManaged(false); }
    private static String mensajeError(Throwable error) {
        if (error instanceof ErrorClienteException) return error.getMessage();
        if (error instanceof IllegalArgumentException || error instanceof SecurityException
                || error instanceof IllegalStateException) return error.getMessage();
        return "No se pudo guardar la propiedad. Revisa los datos e inténtalo de nuevo.";
    }

    @FXML
    public void handleGenerarReportePropiedades() {
        Window ventana = (tblPropiedades != null && tblPropiedades.getScene() != null)
                ? tblPropiedades.getScene().getWindow()
                : null;
        VistaPreviaReporte.generarYMostrar(btnGenerarReportePropiedades, ventana,
                reporteService::generarDocumentoPropiedades);
    }



}
