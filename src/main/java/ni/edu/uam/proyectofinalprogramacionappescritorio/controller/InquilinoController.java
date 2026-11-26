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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.HistorialInquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Directorio local de inquilinos comunicado exclusivamente mediante API HTTP. */
public class InquilinoController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnLimpiar;
    @FXML private Button btnDarBaja;
    @FXML private Button btnEliminar;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<InquilinoResponse> tblInquilinos;
    @FXML private TableColumn<InquilinoResponse, String> colNombre;
    @FXML private TableColumn<InquilinoResponse, String> colIdentificacion;
    @FXML private TableColumn<InquilinoResponse, String> colTelefono;
    @FXML private TableColumn<InquilinoResponse, String> colCorreo;
    @FXML private TableColumn<InquilinoResponse, String> colEmergencia;
    @FXML private TableColumn<InquilinoResponse, String> colEstado;
    @FXML private TableColumn<InquilinoResponse, Void> colAcciones;

    private final ClienteCatalogos catalogos = ClienteLatch.catalogos();
    private final ObservableList<InquilinoResponse> registros = FXCollections.observableArrayList();
    private final FilteredList<InquilinoResponse> filtrados = new FilteredList<>(registros, item -> true);
    private InquilinoResponse seleccionada;
    private final boolean permisoEliminar = SesionCliente.getActual().esPropietario();
    private RecargadorVista recargador;
    private BarraErrorContextual barraError;

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PERSONAS, 20));
        recargador = RecargadorVista.paraNodo(rootView, LatchApplication.instancia().getTareas());
        barraError = BarraErrorContextual.envolver(lblErrorConsulta);
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
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colIdentificacion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().identificacion()));
        colTelefono.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().telefono()));
        colCorreo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().correo()));
        colEmergencia.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().contactoEmergencia()));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activo() ? "Activo" : "Inactivo"));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(
                new AccionesTabla.Accion<>("Editar inquilino", this::abrirEdicion, Iconos.Nombre.EDITAR),
                new AccionesTabla.Accion<>("Ver historial", this::abrirHistorial, Iconos.Nombre.VER),
                new AccionesTabla.Accion<>("Adjuntos", this::abrirAdjuntos, Iconos.Nombre.REPORTE)));
        tblInquilinos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionada = actual;
            btnDarBaja.setDisable(actual == null || !actual.activo() || !permisoEliminar);
            btnEliminar.setDisable(actual == null || !permisoEliminar);
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro(actual));
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        aplicarFiltro("");
        cargarDatos();
    }

    private void aplicarFiltro(String texto) {
        String filtro = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(i -> filtro.isEmpty() || contiene(i.nombre(), filtro)
                || contiene(i.identificacion(), filtro) || contiene(i.telefono(), filtro)
                || contiene(i.correo(), filtro) || contiene(i.contactoEmergencia(), filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblInquilinos.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay inquilinos" : "No hay coincidencias",
                filtro.isEmpty() ? "Los inquilinos registrados aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? "Nuevo inquilino" : "Limpiar búsqueda",
                filtro.isEmpty() ? this::handleNuevo : this::limpiarFiltros));
    }

    public void cargarDatos() {
        recargador.recargar(() -> catalogos.inquilinos(false), datos -> {
            RecargadorVista.conservarSeleccionPorId(tblInquilinos, InquilinoResponse::id, () -> {
                registros.setAll(datos);
                aplicarFiltro(txtBuscar.getText());
            });
            ocultarError();
        }, error -> {
            barraError.mostrar("No se pudieron cargar los inquilinos. Vuelve a intentarlo.", this::cargarDatos);
            if (registros.isEmpty()) {
                tblInquilinos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los inquilinos",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
        });
    }

    @FXML public void handleNuevo() { abrirFormulario(null); }
    private void abrirEdicion(InquilinoResponse inquilino) { abrirFormulario(inquilino); }

    private void abrirAdjuntos(InquilinoResponse inquilino) {
        if (inquilino == null || inquilino.id() == null) return;
        Window propietario = tblInquilinos.getScene() == null ? null : tblInquilinos.getScene().getWindow();
        ClienteAdjuntos adjuntos = ClienteLatch.adjuntos();
        UUID inquilinoId = UUID.fromString(inquilino.id());
        DialogosAdjuntos.mostrarGestorAdjuntos(
                propietario,
                "Adjuntos de " + inquilino.nombre(),
                "Los documentos opcionales (PDF, PNG o JPEG, máximo 5 MiB) se conservan para respaldo del expediente.",
                false,
                adjuntos,
                () -> adjuntos.listarPorInquilino(inquilinoId),
                archivo -> adjuntos.adjuntarDocumentoInquilino(inquilinoId, archivo));
    }

    private void abrirHistorial(InquilinoResponse inquilino) {
        if (inquilino == null || inquilino.id() == null) return;
        UUID inquilinoId = UUID.fromString(inquilino.id());
        Task<HistorialInquilinoResponse> tarea = new Task<>() {
            @Override protected HistorialInquilinoResponse call() {
                return catalogos.historialInquilino(inquilinoId);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, historial -> {
            if (rootView == null || rootView.getScene() == null || !SesionCliente.haySesionActiva()) return;
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
        }, error -> AlertaUtil.mostrarError("No se pudo cargar el historial", mensajeError(error)));
    }

    private void abrirFormulario(InquilinoResponse existente) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("inquilino-form.fxml"));
            Node contenido = loader.load();
            InquilinoFormController formulario = loader.getController();
            UUID existenteId = existente == null ? null : UUID.fromString(existente.id());
            formulario.establecerValidadorServidor(ClienteLatch.validaciones(), catalogos, existenteId);
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
                        @Override protected Void call() {
                            catalogos.guardarInquilino(existenteId,
                                    new InquilinoRequest(datos.nombre(), datos.identificacion(), datos.telefono(),
                                            datos.correo(), datos.emergencia(), datos.activo(),
                                            nuevo ? null : existente.version()));
                            return null;
                        }
                    };
                    TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
                        formulario.marcarLimpio();
                        DialogoFormulario.cerrar(dialogo);
                        AlertaUtil.mostrarExito(nuevo ? "Inquilino registrado" : "Inquilino actualizado",
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
                    Task<List<InquilinoResponse>> verificar = new Task<>() {
                        @Override protected List<InquilinoResponse> call() {
                            return catalogos.coincidenciasIdentificacion(ident, existenteId);
                        }
                    };
                    TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), verificar, coincidencias -> {
                        if (!coincidencias.isEmpty()) {
                            InquilinoResponse previo = coincidencias.getFirst();
                            boolean continuar = AlertaUtil.mostrarConfirmacion(
                                    "Identificación ya registrada",
                                    "Ya existe un expediente con esa identificación: " + previo.nombre()
                                            + " (" + previo.identificacion() + "). ¿Deseas continuar de todos modos?");
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
        InquilinoResponse inquilino = seleccionada;
        if (inquilino == null || !inquilino.activo() || !permisoEliminar) return;
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Deshabilitar inquilino",
                inquilino.nombre() + (inquilino.identificacion() == null ? "" : " · " + inquilino.identificacion()),
                "El inquilino quedará inactivo; su historial se conservará.", "Deshabilitar")) return;
        btnDarBaja.setDisable(true);
        UUID id = UUID.fromString(inquilino.id());
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                catalogos.darDeBajaInquilino(id);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Inquilino deshabilitado", "El inquilino quedó inactivo.");
            cargarDatos();
        }, error -> {
            btnDarBaja.setDisable(seleccionada == null || !seleccionada.activo() || !permisoEliminar);
            if (error instanceof ErrorClienteException) {
                AlertaUtil.mostrarAdvertencia("No se pudo deshabilitar el inquilino", error.getMessage());
            } else {
                AlertaUtil.mostrarError("No se pudo deshabilitar el inquilino", "Vuelve a intentarlo.");
            }
        });
    }

    @FXML public void handleEliminar() {
        InquilinoResponse inquilino = seleccionada;
        if (inquilino == null || !permisoEliminar) return;
        String registro = "Inquilino: " + inquilino.nombre()
                + (inquilino.identificacion() == null ? "" : " · " + inquilino.identificacion());
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar registro de inquilino", registro,
                "El registro se eliminará físicamente y no podrá restaurarse.", "Eliminar registro")) return;
        btnEliminar.setDisable(true);
        UUID id = UUID.fromString(inquilino.id());
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                catalogos.eliminarInquilino(id);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Registro eliminado", "El registro del inquilino se eliminó.");
            cargarDatos();
        }, error -> {
            btnEliminar.setDisable(false);
            if (error instanceof ErrorClienteException api && api.getEstado() == 403) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar registros de inquilinos.");
            } else {
                AlertaUtil.mostrarAdvertencia("No se puede eliminar el registro",
                        "El inquilino tiene contratos o historial asociado. Usa Deshabilitar para conservarlo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }
    private void ocultarError() { barraError.ocultar(); }
    private static boolean contiene(String valor, String filtro) { return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro); }
    private static String mensajeError(Throwable error) {
        return MensajesUi.traducir(error, "No se pudo guardar el inquilino. Revisa los datos e inténtalo de nuevo.");
    }
}
