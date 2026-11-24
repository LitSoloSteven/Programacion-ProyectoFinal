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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;
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
import java.util.List;
import java.util.Locale;

/** Inventario de unidades con filtros locales y operaciones fuera del hilo FX. */
@RequiredArgsConstructor
public class UnidadController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnNuevo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnEliminar;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Unidad> tblUnidades;
    @FXML private TableColumn<Unidad, String> colPropiedad;
    @FXML private TableColumn<Unidad, String> colCodigo;
    @FXML private TableColumn<Unidad, String> colTipo;
    @FXML private TableColumn<Unidad, String> colEstado;
    @FXML private TableColumn<Unidad, String> colCanon;
    @FXML private TableColumn<Unidad, String> colDescripcion;
    @FXML private TableColumn<Unidad, Void> colAcciones;

    private final ReporteService reporteService = new ReporteService();
    private final ClienteCatalogos catalogos = new ClienteCatalogos(ClienteLatch.api());
    private final UnidadService unidadService;
    private final ObservableList<Unidad> registros = FXCollections.observableArrayList();
    private final FilteredList<Unidad> filtrados = new FilteredList<>(registros, item -> true);
    private List<Propiedad> propiedadesDisponibles = List.of();
    private Unidad seleccionada;
    private final boolean permisoEliminar = SesionUsuario.esPropietario();
    private long versionConsulta;

    private record DatosTabla(List<Propiedad> propiedades, List<Unidad> unidades) { }

    public UnidadController() {
        this(HelloApplication.instancia().getComposicion().unidades());
    }
    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.UNIDAD, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        btnNuevo.disableProperty().bind(btnRefrescar.disableProperty());
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona una unidad para eliminarla." : "Solo el propietario puede eliminar unidades."));
        btnEliminar.setDisable(true);
        tblUnidades.setItems(filtrados);
        tblUnidades.setPlaceholder(PanelEstadoTabla.crear("Cargando unidades", "El inventario aparecerá al terminar la consulta.", null, null));
        colPropiedad.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getPropiedadNombre()));
        colCodigo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getCodigo()));
        colTipo.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getTipo()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getEstado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        colCanon.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getCanonFormateado()));
        colCanon.setCellFactory(FormatoTabla.numerica());
        colDescripcion.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getDescripcion()));
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>(
                "Editar unidad", this::abrirEdicion, Iconos.Nombre.EDITAR)));
        tblUnidades.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
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
        filtrados.setPredicate(unidad -> filtro.isEmpty()
                || contiene(unidad.getCodigo(), filtro) || contiene(unidad.getPropiedadNombre(), filtro)
                || contiene(unidad.getTipo(), filtro) || contiene(unidad.getDescripcion(), filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblUnidades.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay unidades" : "No hay coincidencias",
                filtro.isEmpty() ? "Las unidades registradas aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? "Nueva unidad" : "Limpiar búsqueda",
                filtro.isEmpty() ? this::handleNuevo : this::limpiarFiltros));
    }

    public void cargarDatos() {
        btnRefrescar.setDisable(true);
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                return new DatosTabla(catalogos.propiedades(true), catalogos.unidades());
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            propiedadesDisponibles = datos.propiedades();
            registros.setAll(datos.unidades());
            aplicarFiltro(txtBuscar.getText());
            ocultarError();
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) {
                lblErrorConsulta.setText("No se pudieron cargar las unidades. Vuelve a intentarlo.");
                lblErrorConsulta.setVisible(true); lblErrorConsulta.setManaged(true);
                tblUnidades.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar las unidades",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
            btnRefrescar.setDisable(false);
        });
    }

    @FXML public void refrescarTabla() { cargarDatos(); }
    @FXML public void handleNuevo() {
        if (!btnNuevo.isDisabled()) abrirFormulario(null);
    }
    private void abrirEdicion(Unidad unidad) { abrirFormulario(unidad); }

    private void abrirFormulario(Unidad existente) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("unidad-form.fxml"));
            Node contenido = loader.load();
            UnidadFormController formulario = loader.getController();
            formulario.establecerPropiedades(propiedadesDisponibles);
            if (existente != null) formulario.cargar(existente);
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
                accion.setDisable(true); accion.setText("Guardando unidad…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        catalogos.guardarUnidad(nueva ? null : existente.getId(),
                                new UnidadRequest(datos.propiedad().getId().toString(), datos.codigo(),
                            datos.descripcion(), datos.tipo(), datos.estado(),
                            datos.canon() == null ? null : datos.canon().toPlainString(),
                            datos.moneda(), nueva ? null : existente.getVersion()));
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
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
        Unidad unidad = seleccionada;
        if (unidad == null || !permisoEliminar) return;
        String registro = "Unidad: " + unidad.getCodigo() + " · " + unidad.getPropiedadNombre();
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar unidad", registro,
                "El registro se eliminará físicamente si no tiene contratos o cargos asociados.", "Eliminar unidad")) return;
        btnEliminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { unidadService.eliminar(unidad.getId()); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            seleccionada = null;
            AlertaUtil.mostrarExito("Unidad eliminada", "El registro de la unidad se eliminó.");
            cargarDatos();
        }, error -> {
            btnEliminar.setDisable(seleccionada == null || !permisoEliminar);
            if (error instanceof SecurityException) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar unidades.");
            } else if (error instanceof IllegalStateException) {
                AlertaUtil.mostrarAdvertencia("La unidad tiene registros asociados",
                        "No se puede eliminar mientras tenga contratos o cargos. Puedes marcarla como Inactiva.");
            } else {
                AlertaUtil.mostrarError("No se pudo eliminar la unidad", "Revisa las relaciones asociadas e inténtalo de nuevo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }
    private void ocultarError() { lblErrorConsulta.setText(""); lblErrorConsulta.setVisible(false); lblErrorConsulta.setManaged(false); }
    private static boolean contiene(String texto, String filtro) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro);
    }
    private static String mensajeError(Throwable error) {
        if (error instanceof ErrorClienteException) return error.getMessage();
        if (error instanceof IllegalArgumentException || error instanceof SecurityException
                || error instanceof IllegalStateException) return error.getMessage();
        return "No se pudo guardar la unidad. Revisa los datos e inténtalo de nuevo.";
    }

    @FXML
    public void handleGenerarReporte() {
        List<Unidad> unidadesActuales = tblUnidades == null
                ? List.of()
                : List.copyOf(tblUnidades.getItems());

        if (unidadesActuales.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos",
                    "No hay unidades en la tabla para generar el reporte.");
            return;
        }

        if (btnGenerarReporte != null) {
            btnGenerarReporte.setDisable(true);
        }

        Task<byte[]> tareaGenerarPdf = new Task<>() {
            @Override
            protected byte[] call() {
                return reporteService.generarReporteUnidadesPdf(unidadesActuales);
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tareaGenerarPdf,
                pdfBytes -> {
                    if (btnGenerarReporte != null) {
                        btnGenerarReporte.setDisable(false);
                    }
                    if (pdfBytes == null || pdfBytes.length == 0) {
                        AlertaUtil.mostrarError("Error de reporte",
                                "El reporte de unidades está vacío.");
                        return;
                    }

                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Guardar reporte de unidades");
                    fileChooser.setInitialFileName("Reporte-Unidades.pdf");
                    fileChooser.getExtensionFilters().add(
                            new javafx.stage.FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf")
                    );

                    Window ventana = tblUnidades.getScene() == null
                            ? null
                            : tblUnidades.getScene().getWindow();
                    java.io.File archivoDestino = fileChooser.showSaveDialog(ventana);

                    if (archivoDestino != null) {
                        try {
                            java.nio.file.Files.write(archivoDestino.toPath(), pdfBytes);
                        } catch (Exception ex) {
                            AlertaUtil.mostrarError("Error al guardar",
                                    "No se pudo guardar el archivo PDF en disco.");
                            return;
                        }

                        AlertaUtil.mostrarExito("Reporte listo",
                                "El reporte de unidades se guardó correctamente.");

                        if (java.awt.Desktop.isDesktopSupported()
                                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                            try {
                                java.awt.Desktop.getDesktop().open(archivoDestino);
                            } catch (Exception ex) {
                                AlertaUtil.mostrarAdvertencia("No se pudo abrir automáticamente",
                                        "El PDF se guardó bien, pero no se pudo abrir el visor automáticamente.");
                            }
                        }
                    }
                },
                error -> {
                    if (btnGenerarReporte != null) {
                        btnGenerarReporte.setDisable(false);
                    }
                    AlertaUtil.mostrarError("Error al generar reporte", mensajeError(error));
                }
        );
    }



}
