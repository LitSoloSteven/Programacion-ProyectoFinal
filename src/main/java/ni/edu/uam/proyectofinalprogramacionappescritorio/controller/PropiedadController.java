package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.BarraErrorContextual;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RecargadorVista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Lista de propiedades comunicada exclusivamente mediante ClienteCatalogos HTTP. */
public class PropiedadController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnEliminar;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<PropiedadResponse> tblPropiedades;
    @FXML private TableColumn<PropiedadResponse, String> colNombre;
    @FXML private TableColumn<PropiedadResponse, String> colDireccion;
    @FXML private TableColumn<PropiedadResponse, String> colEstado;
    @FXML private TableColumn<PropiedadResponse, String> colFecha;
    @FXML private TableColumn<PropiedadResponse, Void> colAcciones;

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ObservableList<PropiedadResponse> registros = FXCollections.observableArrayList();
    private final FilteredList<PropiedadResponse> filtrados = new FilteredList<>(registros, item -> true);
    private PropiedadResponse seleccionada;
    private final boolean permisoEliminar = SesionCliente.getActual().esPropietario();
    private RecargadorVista recargador;
    private BarraErrorContextual barraError;

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PROPIEDAD, 20));
        recargador = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
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
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colDireccion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().direccion()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activa() ? "Activa" : "Inactiva"));
        colEstado.setCellFactory(FormatoTabla.estado());
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().creadoEn()));
        colFecha.setCellFactory(FormatoTabla.fechaHoraIso(ConstantesLatch.ZONA_OPERATIVA));
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
                || propiedad.nombre().toLowerCase(Locale.ROOT).contains(filtro)
                || propiedad.direccion().toLowerCase(Locale.ROOT).contains(filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblPropiedades.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay propiedades" : "No hay coincidencias",
                filtro.isEmpty() ? "Las propiedades registradas aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? (permisoEliminar ? "Nueva propiedad" : null) : "Limpiar búsqueda",
                filtro.isEmpty() ? (permisoEliminar ? this::handleNuevo : null) : this::limpiarFiltros));
    }

    public void cargarDatos() {
        recargador.recargar(() -> catalogos.propiedades(false), datos -> {
            RecargadorVista.conservarSeleccionPorId(tblPropiedades, PropiedadResponse::id, () -> {
                registros.setAll(datos);
                aplicarFiltro(txtBuscar.getText());
            });
            ocultarError();
        }, error -> {
            barraError.mostrar("No se pudieron cargar las propiedades. Vuelve a intentarlo.", this::cargarDatos);
            if (registros.isEmpty()) {
                tblPropiedades.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar las propiedades",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
        });
    }

    @FXML public void handleNuevo() { if (permisoEliminar) abrirFormulario(null); }
    private void abrirEdicion(PropiedadResponse propiedad) { if (permisoEliminar) abrirFormulario(propiedad); }

    private void abrirFormulario(PropiedadResponse existente) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("propiedad-form.fxml"));
            Node contenido = loader.load();
            PropiedadFormController formulario = loader.getController();
            formulario.establecerValidadorServidor(ClienteLatch.validaciones());
            if (existente != null) formulario.cargar(existente);
            Window propietario = tblPropiedades.getScene() == null ? null : tblPropiedades.getScene().getWindow();
            boolean nueva = existente == null;
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                    nueva ? "Nueva propiedad" : "Editar propiedad", contenido, propietario, 620, 600,
                    nueva ? "Guardar propiedad" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nueva ? btnNuevo : tblPropiedades, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                PropiedadFormController.Datos datos = formulario.datos();
                UUID id = nueva ? null : UUID.fromString(existente.id());
                accion.setDisable(true);
                accion.setText("Guardando propiedad…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        catalogos.guardarPropiedad(id, new PropiedadRequest(
                                datos.nombre(), datos.direccion(), datos.activa(),
                                datos.nombrePropietario(), datos.correoPropietario(),
                                datos.telefonoPropietario(), nueva ? null : existente.version()));
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
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
        PropiedadResponse propiedad = seleccionada;
        if (propiedad == null || !permisoEliminar) return;
        String registro = "Propiedad: " + propiedad.nombre() + " · " + propiedad.direccion();
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar propiedad", registro,
                "El registro se eliminará físicamente si no tiene unidades asociadas.", "Eliminar propiedad")) return;
        btnEliminar.setDisable(true);
        UUID id = UUID.fromString(propiedad.id());
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                catalogos.eliminarPropiedad(id);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            seleccionada = null;
            AlertaUtil.mostrarExito("Propiedad eliminada", "El registro de la propiedad se eliminó.");
            cargarDatos();
        }, error -> {
            btnEliminar.setDisable(seleccionada == null || !permisoEliminar);
            if (error instanceof ErrorClienteException api && api.getEstado() == 409) {
                AlertaUtil.mostrarAdvertencia("La propiedad tiene unidades", api.getMessage()
                        + " Puedes desactivarla desde el formulario de propiedad.");
            } else if (error instanceof ErrorClienteException api && api.getEstado() == 403) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar propiedades.");
            } else {
                AlertaUtil.mostrarError("No se pudo eliminar la propiedad", "Revisa las relaciones asociadas e inténtalo de nuevo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }
    private void ocultarError() { barraError.ocultar(); }
    private static String mensajeError(Throwable error) {
        return MensajesUi.traducir(error, "No se pudo guardar la propiedad. Revisa los datos e inténtalo de nuevo.");
    }
}
