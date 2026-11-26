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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.BarraErrorContextual;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogosAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RecargadorVista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Inventario de unidades con filtros locales y operaciones HTTP fuera del hilo FX. */
public class UnidadController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnEliminar;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<UnidadResponse> tblUnidades;
    @FXML private TableColumn<UnidadResponse, String> colPropiedad;
    @FXML private TableColumn<UnidadResponse, String> colCodigo;
    @FXML private TableColumn<UnidadResponse, String> colTipo;
    @FXML private TableColumn<UnidadResponse, String> colEstado;
    @FXML private TableColumn<UnidadResponse, String> colCanon;
    @FXML private TableColumn<UnidadResponse, String> colDescripcion;
    @FXML private TableColumn<UnidadResponse, Void> colAcciones;

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ObservableList<UnidadResponse> registros = FXCollections.observableArrayList();
    private final FilteredList<UnidadResponse> filtrados = new FilteredList<>(registros, item -> true);
    private List<PropiedadResponse> propiedadesDisponibles = List.of();
    private UnidadResponse seleccionada;
    private final boolean permisoEliminar = SesionCliente.getActual().esPropietario();
    private RecargadorVista recargador;
    private BarraErrorContextual barraError;

    private record DatosTabla(List<PropiedadResponse> propiedades, List<UnidadResponse> unidades) { }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.UNIDAD, 20));
        recargador = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        if (permisoEliminar) {
            btnNuevo.disableProperty().bind(recargador.cargandoProperty());
        } else {
            btnNuevo.setDisable(true);
            btnNuevo.setTooltip(new Tooltip("Solo el propietario puede registrar unidades."));
        }
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona una unidad para eliminarla." : "Solo el propietario puede eliminar unidades."));
        btnEliminar.setDisable(true);
        tblUnidades.setItems(filtrados);
        tblUnidades.setPlaceholder(PanelEstadoTabla.crear("Cargando unidades", "El inventario aparecerá al terminar la consulta.", null, null));
        colPropiedad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().propiedadNombre()));
        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigo()));
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().tipo()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        colCanon.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearCanon(c.getValue())));
        colCanon.setCellFactory(FormatoTabla.numerica());
        colDescripcion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().descripcion()));
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Editar unidad", this::abrirEdicion, Iconos.Nombre.EDITAR),
                new AccionesTabla.Accion<>("Fotografía", this::abrirFotografia, Iconos.Nombre.VER)));
        tblUnidades.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionada = actual;
            btnEliminar.setDisable(actual == null || !permisoEliminar);
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro(actual));
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        aplicarFiltro("");
        cargarDatos();
    }

    public void aplicarFiltroInicial(String texto) {
        if (texto == null || texto.isBlank() || txtBuscar == null) {
            return;
        }
        txtBuscar.setText(texto.strip());
        aplicarFiltro(texto.strip());
    }

    private void aplicarFiltro(String texto) {
        String filtro = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(unidad -> filtro.isEmpty()
                || contiene(unidad.codigo(), filtro) || contiene(unidad.propiedadNombre(), filtro)
                || contiene(unidad.tipo(), filtro) || contiene(unidad.descripcion(), filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblUnidades.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay unidades" : "No hay coincidencias",
                filtro.isEmpty() ? "Las unidades registradas aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? (permisoEliminar ? "Nueva unidad" : null) : "Limpiar búsqueda",
                filtro.isEmpty() ? (permisoEliminar ? this::handleNuevo : null) : this::limpiarFiltros));
    }

    public void cargarDatos() {
        recargador.recargar(
                () -> new DatosTabla(catalogos.propiedades(true), catalogos.unidades()),
                datos -> {
                    propiedadesDisponibles = datos.propiedades();
                    RecargadorVista.conservarSeleccionPorId(tblUnidades, UnidadResponse::id, () -> {
                        registros.setAll(datos.unidades());
                        aplicarFiltro(txtBuscar.getText());
                    });
                    ocultarError();
                },
                error -> {
                    barraError.mostrar("No se pudieron cargar las unidades. Vuelve a intentarlo.", this::cargarDatos);
                    if (registros.isEmpty()) {
                        tblUnidades.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar las unidades",
                                "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
                    }
                });
    }

    @FXML public void handleNuevo() {
        if (permisoEliminar && !btnNuevo.isDisabled()) abrirFormulario(null);
    }
    private void abrirEdicion(UnidadResponse unidad) { abrirFormulario(unidad); }

    private void abrirFotografia(UnidadResponse unidad) {
        if (unidad == null || unidad.id() == null) return;
        Window propietario = tblUnidades.getScene() == null ? null : tblUnidades.getScene().getWindow();
        DialogosAdjuntos.mostrarFotoUnidad(
                propietario,
                unidad.codigo() + " · " + unidad.propiedadNombre(),
                ClienteLatch.adjuntos(),
                UUID.fromString(unidad.id()));
    }

    private void abrirFormulario(UnidadResponse existente) {
        if (existente == null && !permisoEliminar) return;
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("unidad-form.fxml"));
            Node contenido = loader.load();
            UnidadFormController formulario = loader.getController();
            formulario.establecerValidadorServidor(ClienteLatch.validaciones());
            formulario.establecerPropiedades(propiedadesDisponibles);
            if (existente != null) {
                formulario.cargar(existente);
                formulario.aplicarModoAsistente(!permisoEliminar);
            }
            Window propietario = tblUnidades.getScene() == null ? null : tblUnidades.getScene().getWindow();
            boolean nueva = existente == null;
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(nueva ? "Nueva unidad" : "Editar unidad",
                    contenido, propietario, 680, 640, nueva ? "Guardar unidad" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nueva ? btnNuevo : tblUnidades, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                UnidadFormController.Datos datos = formulario.datos();
                UUID id = nueva ? null : UUID.fromString(existente.id());
                accion.setDisable(true); accion.setText("Guardando unidad…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() {
                        catalogos.guardarUnidad(id, new UnidadRequest(
                                datos.propiedad().id(), datos.codigo(), datos.descripcion(),
                                datos.tipo(), datos.estado(),
                                datos.canon() == null ? null : datos.canon().toPlainString(),
                                datos.moneda(), nueva ? null : existente.version()));
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                    formulario.marcarLimpio(); DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito(nueva ? "Unidad registrada" : "Unidad actualizada",
                            nueva ? "La unidad quedó registrada." : "Los cambios quedaron guardados.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText(nueva ? "Guardar unidad" : "Guardar cambios");
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
        UnidadResponse unidad = seleccionada;
        if (unidad == null || !permisoEliminar) return;
        String registro = "Unidad: " + unidad.codigo() + " · " + unidad.propiedadNombre();
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar unidad", registro,
                "El registro se eliminará físicamente si no tiene contratos o cargos asociados.", "Eliminar unidad")) return;
        btnEliminar.setDisable(true);
        UUID id = UUID.fromString(unidad.id());
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                catalogos.eliminarUnidad(id);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            seleccionada = null;
            AlertaUtil.mostrarExito("Unidad eliminada", "El registro de la unidad se eliminó.");
            cargarDatos();
        }, error -> {
            btnEliminar.setDisable(seleccionada == null || !permisoEliminar);
            if (error instanceof ErrorClienteException api && api.getEstado() == 403) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar unidades.");
            } else if (error instanceof ErrorClienteException api && api.getEstado() == 409) {
                AlertaUtil.mostrarAdvertencia("La unidad tiene registros asociados",
                        api.getMessage() + " Puedes marcarla como Inactiva.");
            } else {
                AlertaUtil.mostrarError("No se pudo eliminar la unidad", "Revisa las relaciones asociadas e inténtalo de nuevo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }
    private void ocultarError() { barraError.ocultar(); }
    private static boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro);
    }
    private static String formatearCanon(UnidadResponse unidad) {
        if (unidad == null || unidad.canonReferencia() == null || unidad.canonReferencia().isBlank()) return "";
        return FormatoMoneda.formatear(unidad.moneda(), new BigDecimal(unidad.canonReferencia()));
    }
    private static String mensajeError(Throwable error) {
        return MensajesUi.traducir(error, "No se pudo guardar la unidad. Revisa los datos e inténtalo de nuevo.");
    }
}
