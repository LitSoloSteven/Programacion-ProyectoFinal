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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.HistorialInquilinoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AccionesTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AtajosLista;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogosAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PanelEstadoTabla;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** Directorio local de inquilinos y responsable de las operaciones del servicio. */
@RequiredArgsConstructor
public class InquilinoController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnGenerarReporte;
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

    private final ReporteService reporteService = new ReporteService();
    private final ClienteCatalogos catalogos = new ClienteCatalogos(ClienteLatch.api());
    private final InquilinoService inquilinoService;
    private final ObservableList<Inquilino> registros = FXCollections.observableArrayList();
    private final FilteredList<Inquilino> filtrados = new FilteredList<>(registros, item -> true);
    private Inquilino seleccionada;
    private final boolean permisoEliminar = SesionUsuario.esPropietario();
    private long versionConsulta;

    public InquilinoController() { this(HelloApplication.instancia().getComposicion().inquilinos()); }
    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PERSONAS, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnGenerarReporte != null) {
            Iconos.asignar(btnGenerarReporte, Iconos.Nombre.REPORTE, 16);
            btnGenerarReporte.setTooltip(new Tooltip("Vista previa y exportación PDF del directorio de inquilinos"));
        }
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnDarBaja, Iconos.Nombre.EDITAR, 16);
        if (!permisoEliminar) {
            btnDarBaja.setTooltip(new Tooltip("Solo el propietario puede desactivar inquilinos."));
        }
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
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Editar inquilino", this::abrirEdicion, Iconos.Nombre.EDITAR),
                new AccionesTabla.Accion<>("Ver historial", this::abrirHistorial, Iconos.Nombre.VER),
                new AccionesTabla.Accion<>("Adjuntos", this::abrirAdjuntos, Iconos.Nombre.REPORTE)));
        tblInquilinos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionada = actual;
            btnDarBaja.setDisable(actual == null || !actual.isActivo() || !permisoEliminar);
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
            @Override protected List<Inquilino> call() throws SQLException { return catalogos.inquilinos(false); }
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

    private void abrirAdjuntos(Inquilino inquilino) {
        if (inquilino == null || inquilino.getId() == null) return;
        Window propietario = tblInquilinos.getScene() == null ? null : tblInquilinos.getScene().getWindow();
        var adjuntos = HelloApplication.instancia().getComposicion().adjuntos();
        SesionUsuario identidad = SesionUsuario.getActual();
        DialogosAdjuntos.mostrarGestorAdjuntos(
                propietario,
                "Adjuntos de " + inquilino.getNombre(),
                "Los documentos opcionales (PDF, PNG o JPEG, máximo 5 MiB) se conservan para respaldo del expediente.",
                false,
                adjuntos,
                () -> adjuntos.listarPorInquilino(identidad, inquilino.getId()),
                archivo -> adjuntos.adjuntarDocumentoInquilino(identidad, inquilino.getId(), archivo));
    }

    private void abrirHistorial(Inquilino inquilino) {
        if (inquilino == null) return;
        Task<HistorialInquilinoDTO> tarea = new Task<>() {
            @Override protected HistorialInquilinoDTO call() {
                return inquilinoService.consultarHistorial(inquilino.getId());
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, historial -> {
            StringBuilder detalle = new StringBuilder();
            detalle.append("Inquilino: ").append(historial.nombre());
            if (historial.identificacion() != null) {
                detalle.append(" (").append(historial.identificacion()).append(")");
            }
            detalle.append(" · ").append(historial.activo() ? "Activo" : "Inactivo").append("\n\n");
            detalle.append("CONTRATOS (").append(historial.contratos().size()).append(")\n");
            for (var c : historial.contratos()) {
                detalle.append("• ").append(c.unidad()).append(" | ").append(c.inicio()).append(" → ").append(c.fin())
                        .append(" | ").append(c.canon()).append(" ").append(c.moneda())
                        .append(" | ").append(c.estado()).append("\n");
            }
            detalle.append("\nCARGOS (").append(historial.cargos().size()).append(")\n");
            for (var cg : historial.cargos()) {
                detalle.append("• ").append(cg.tipo()).append(" | ").append(cg.periodo())
                        .append(" | vence ").append(cg.venceEl())
                        .append(" | ").append(cg.importe()).append(" ").append(cg.moneda())
                        .append(" | ").append(cg.estado()).append("\n");
            }
            detalle.append("\nPAGOS Y RECIBOS (").append(historial.pagos().size()).append(")\n");
            for (var p : historial.pagos()) {
                detalle.append("• ").append(p.fechaPago()).append(" | ")
                        .append(p.montoOriginal()).append(" ").append(p.monedaOriginal())
                        .append(" (contrato: ").append(p.montoContrato()).append(" ").append(p.monedaContrato()).append(")")
                        .append(" | ").append(p.metodo()).append(" | ").append(p.estado());
                if (p.numeroRecibo() != null) detalle.append(" | Recibo #").append(p.numeroRecibo());
                detalle.append("\n");
            }

            TextArea area = new TextArea(detalle.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefSize(660, 420);
            Window propietario = tblInquilinos.getScene() == null ? null : tblInquilinos.getScene().getWindow();
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                    "Historial del inquilino", area, propietario, 700, 520, "Cerrar");
            dialogo.getDialogPane().getButtonTypes().remove(1);
            dialogo.showAndWait();
        }, error -> AlertaUtil.mostrarError("No se pudo cargar el historial",
                mensajeError(error)));
    }

    private void abrirFormulario(Inquilino existente) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("inquilino-form.fxml"));
            Node contenido = loader.load();
            InquilinoFormController formulario = loader.getController();
            if (existente != null) formulario.cargar(existente);
            formulario.aplicarPermisoEstado(permisoEliminar);
            Window propietario = tblInquilinos.getScene() == null ? null : tblInquilinos.getScene().getWindow();
            boolean nuevo = existente == null;
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(nuevo ? "Nuevo inquilino" : "Editar inquilino",
                    contenido, propietario, 620, 620, nuevo ? "Guardar inquilino" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nuevo ? btnNuevo : tblInquilinos, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            String[] identificacionConfirmada = {null};
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                InquilinoFormController.Datos datos = formulario.datos();
                accion.setDisable(true);
                accion.setText("Guardando inquilino…");

                Runnable ejecutarGuardado = () -> {
                    Task<Void> tarea = new Task<>() {
                        @Override protected Void call() throws SQLException {
                            catalogos.guardarInquilino(nuevo ? null : existente.getId(),
                                    new InquilinoRequest(datos.nombre(), datos.identificacion(), datos.telefono(),
                                            datos.correo(), datos.emergencia(), datos.activo(),
                                            nuevo ? null : existente.getVersion()));
                            return null;
                        }
                    };
                    TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                        formulario.marcarLimpio();
                        DialogoFormulario.cerrar(dialogo);
                        AlertaUtil.mostrarExito(nuevo ? "Inquilino registrado" : "Inquilino actualizada",
                                nuevo ? "El inquilino quedó registrado." : "Los cambios quedaron guardados.");
                        cargarDatos();
                    }, error -> {
                        accion.setDisable(false);
                        accion.setText(nuevo ? "Guardar inquilino" : "Guardar cambios");
                        if (!nuevo && error instanceof ErrorClienteException api && api.getEstado() == 409) {
                            DialogoFormulario.cerrar(dialogo);
                            cargarDatos();
                            AlertaUtil.mostrarAdvertencia("No se guardaron los cambios",
                                    api.getMessage() + " Abre el registro actualizado para volver a editarlo.");
                            return;
                        }
                        formulario.mostrarError(mensajeError(error));
                    });
                };

                String ident = datos.identificacion();
                boolean yaConfirmada = ident == null || ident.equalsIgnoreCase(identificacionConfirmada[0]);
                if (yaConfirmada) {
                    ejecutarGuardado.run();
                } else {
                    Task<List<Inquilino>> verificar = new Task<>() {
                        @Override protected List<Inquilino> call() {
                            return inquilinoService.buscarCoincidenciasIdentificacion(
                                    ident, nuevo ? null : existente.getId());
                        }
                    };
                    TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), verificar, coincidencias -> {
                        if (!coincidencias.isEmpty()) {
                            Inquilino previo = coincidencias.getFirst();
                            boolean continuar = AlertaUtil.mostrarConfirmacion(
                                    "Identificación ya registrada",
                                    "Ya existe un expediente con esa identificación: " + previo.getNombre()
                                            + " (" + previo.getIdentificacion() + "). ¿Deseas continuar de todos modos?");
                            if (!continuar) {
                                accion.setDisable(false);
                                accion.setText(nuevo ? "Guardar inquilino" : "Guardar cambios");
                                return;
                            }
                            identificacionConfirmada[0] = ident;
                        }
                        ejecutarGuardado.run();
                    }, error -> ejecutarGuardado.run());
                }
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleDarDeBaja() {
        Inquilino inquilino = seleccionada;
        if (inquilino == null || !inquilino.isActivo() || !permisoEliminar) return;
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Deshabilitar inquilino",
                inquilino.getNombre() + (inquilino.getIdentificacion() == null ? "" : " · " + inquilino.getIdentificacion()),
                "El inquilino quedará inactivo; su historial se conservará.", "Deshabilitar")) return;
        btnDarBaja.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { catalogos.darDeBajaInquilino(inquilino.getId()); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Inquilino deshabilitado", "El inquilino quedó inactivo.");
            cargarDatos();
        }, error -> {
            btnDarBaja.setDisable(seleccionada == null || !seleccionada.isActivo() || !permisoEliminar);
            if (error instanceof ErrorClienteException) {
                AlertaUtil.mostrarAdvertencia("No se pudo deshabilitar el inquilino", error.getMessage());
            } else {
                AlertaUtil.mostrarError("No se pudo deshabilitar el inquilino", "Vuelve a intentarlo.");
            }
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
        if (error instanceof ErrorClienteException) return error.getMessage();
        if (error instanceof IllegalArgumentException || error instanceof SecurityException
                || error instanceof IllegalStateException) return error.getMessage();
        return "No se pudo guardar el inquilino. Revisa los datos e inténtalo de nuevo.";
    }

    @FXML
    public void handleGenerarReporte() {
        List<Inquilino> inquilinosActuales = tblInquilinos == null
                ? List.of()
                : List.copyOf(tblInquilinos.getItems());

        if (inquilinosActuales.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos",
                    "No hay inquilinos en la tabla para generar el reporte.");
            return;
        }

        Window ventana = tblInquilinos == null || tblInquilinos.getScene() == null
                ? null
                : tblInquilinos.getScene().getWindow();
        VistaPreviaReporte.generarYMostrar(btnGenerarReporte, ventana,
                () -> reporteService.generarDocumentoInquilinos(inquilinosActuales));
    }




}
