package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.GarantiaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
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
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Lista, acciones contractuales y guardado del diálogo de contrato. */
public class ContratoController {
    @FXML private VBox rootView;
    @FXML private HBox eliminarTooltipHost;
    @FXML private HBox garantiaTooltipHost;
    @FXML private Button btnNuevo;
    @FXML private Button btnRefrescar;
    @FXML private Button btnLimpiar;
    @FXML private Button btnTerminar;
    @FXML private Button btnEliminar;
    @FXML private Button btnAjustarGarantia;
    @FXML private Label lblTitulo;
    @FXML private Label lblContador;
    @FXML private Label lblErrorConsulta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Contrato> tblContratos;
    @FXML private TableColumn<Contrato, String> colInquilino;
    @FXML private TableColumn<Contrato, String> colUnidad;
    @FXML private TableColumn<Contrato, LocalDate> colInicio;
    @FXML private TableColumn<Contrato, LocalDate> colFin;
    @FXML private TableColumn<Contrato, String> colCanon;
    @FXML private TableColumn<Contrato, Integer> colDiaPago;
    @FXML private TableColumn<Contrato, String> colDeposito;
    @FXML private TableColumn<Contrato, String> colSaldoGarantia;
    @FXML private TableColumn<Contrato, String> colEstado;
    @FXML private TableColumn<Contrato, Void> colAcciones;

    private final ContratoService contratoService;
    private final UnidadService unidadService;
    private final InquilinoService inquilinoService;
    private final GarantiaService garantiaService;
    private final ObservableList<Contrato> registros = FXCollections.observableArrayList();
    private final FilteredList<Contrato> filtrados = new FilteredList<>(registros, item -> true);
    private List<Unidad> unidadesDisponibles = List.of();
    private List<Inquilino> inquilinosDisponibles = List.of();
    private Contrato seleccionado;
    private final boolean permisoEliminar = SesionUsuario.esPropietario();
    private final boolean permisoAjustarGarantia = SesionUsuario.esPropietario();
    private long versionConsulta;

    private record Catalogos(List<Unidad> unidades, List<Inquilino> inquilinos) { }
    private record DatosTabla(Catalogos catalogos, List<Contrato> contratos) { }

    public ContratoController() {
        this(HelloApplication.instancia().getComposicion().contratos(),
                HelloApplication.instancia().getComposicion().unidades(),
                HelloApplication.instancia().getComposicion().inquilinos(),
                HelloApplication.instancia().getComposicion().garantias());
    }
    public ContratoController(ContratoService contratoService, UnidadService unidadService,
                              InquilinoService inquilinoService) {
        this(contratoService, unidadService, inquilinoService, new GarantiaService());
    }
    public ContratoController(ContratoService contratoService, UnidadService unidadService,
                              InquilinoService inquilinoService, GarantiaService garantiaService) {
        this.contratoService = contratoService;
        this.unidadService = unidadService;
        this.inquilinoService = inquilinoService;
        this.garantiaService = garantiaService;
    }

    @FXML public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.CONTRATO, 20));
        Iconos.asignar(btnRefrescar, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnNuevo, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnLimpiar, Iconos.Nombre.CERRAR, 14);
        Iconos.asignar(btnTerminar, Iconos.Nombre.CONFIRMACION, 16);
        Iconos.asignar(btnEliminar, Iconos.Nombre.ELIMINAR, 16);
        Iconos.asignar(btnAjustarGarantia, Iconos.Nombre.EDITAR, 16);
        Tooltip.install(eliminarTooltipHost, new Tooltip(permisoEliminar
                ? "Selecciona un contrato para eliminarlo." : "Solo el propietario puede eliminar contratos."));
        Tooltip.install(garantiaTooltipHost, new Tooltip(permisoAjustarGarantia
                ? "Selecciona un contrato con seguimiento de garantía." : "Solo el propietario puede ajustar garantías."));
        btnTerminar.setDisable(true);
        btnEliminar.setDisable(true);
        btnAjustarGarantia.setDisable(true);
        tblContratos.setItems(filtrados);
        tblContratos.setPlaceholder(PanelEstadoTabla.crear("Cargando contratos", "El listado aparecerá al terminar la consulta.", null, null));
        colInquilino.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getInquilinoNombre()));
        colUnidad.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getUnidadInfo()));
        colInicio.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().getInicio()));
        colInicio.setCellFactory(FormatoTabla.fecha());
        colFin.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().getFin()));
        colFin.setCellFactory(FormatoTabla.fecha());
        colCanon.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getCanonFormateado()));
        colCanon.setCellFactory(FormatoTabla.numerica());
        colDiaPago.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().getDiaPago()));
        colDiaPago.setCellFactory(FormatoTabla.entero());
        colDeposito.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getDepositoFormateado()));
        colDeposito.setCellFactory(FormatoTabla.numerica());
        colSaldoGarantia.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getGarantiaSaldoFormateado()));
        colSaldoGarantia.setCellFactory(FormatoTabla.numerica());
        colEstado.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().getEstado()));
        colEstado.setCellFactory(FormatoTabla.estado());
        AccionesTabla.configurar(colAcciones, List.of(new AccionesTabla.Accion<>(
                "Editar contrato", this::abrirEdicion, Iconos.Nombre.EDITAR)));
        tblContratos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccionado = actual;
            actualizarAcciones();
        });
        txtBuscar.textProperty().addListener((obs, anterior, actual) -> aplicarFiltro(actual));
        AtajosLista.instalar(rootView, txtBuscar, this::handleNuevo);
        aplicarFiltro("");
        cargarDatos();
    }

    private void aplicarFiltro(String texto) {
        String filtro = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        filtrados.setPredicate(contrato -> filtro.isEmpty()
                || contiene(contrato.getInquilinoNombre(), filtro) || contiene(contrato.getUnidadInfo(), filtro)
                || contiene(contrato.getEstado(), filtro));
        lblContador.setText(filtrados.size() + (filtrados.size() == 1 ? " registro" : " registros"));
        tblContratos.setPlaceholder(PanelEstadoTabla.crear(filtro.isEmpty() ? "Aún no hay contratos" : "No hay coincidencias",
                filtro.isEmpty() ? "Los contratos registrados aparecerán aquí." : "Prueba con otro término o limpia la búsqueda.",
                filtro.isEmpty() ? "Nuevo contrato" : "Limpiar búsqueda",
                filtro.isEmpty() ? this::handleNuevo : this::limpiarFiltros));
    }

    public void cargarCatalogosReales() { cargarDatos(); }
    public void cargarDatos() {
        btnRefrescar.setDisable(true);
        long version = ++versionConsulta;
        Task<DatosTabla> tarea = new Task<>() {
            @Override protected DatosTabla call() throws SQLException {
                Catalogos catalogos = new Catalogos(unidadService.listarTodas(), inquilinoService.listarActivos());
                return new DatosTabla(catalogos, contratoService.listarTodos());
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, datos -> {
            if (version != versionConsulta) return;
            mostrarCatalogos(datos.catalogos());
            registros.setAll(datos.contratos());
            aplicarFiltro(txtBuscar.getText());
            ocultarError();
            btnRefrescar.setDisable(false);
        }, error -> {
            if (version == versionConsulta) {
                lblErrorConsulta.setText("No se pudieron cargar los contratos y sus opciones. Vuelve a intentarlo.");
                lblErrorConsulta.setVisible(true); lblErrorConsulta.setManaged(true);
                tblContratos.setPlaceholder(PanelEstadoTabla.crear("No se pudieron cargar los contratos",
                        "Vuelve a intentarlo.", "Reintentar", this::cargarDatos));
            }
            btnRefrescar.setDisable(false);
        });
    }

    @FXML public void refrescarTabla() { cargarDatos(); }
    @FXML public void handleNuevo() { abrirFormulario(null); }
    private void abrirEdicion(Contrato contrato) { abrirFormulario(contrato); }

    private void abrirFormulario(Contrato existente) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("contrato-form.fxml"));
            Node contenido = loader.load();
            ContratoFormController formulario = loader.getController();
            formulario.establecerCatalogos(unidadesDisponibles, inquilinosDisponibles);
            if (existente != null) formulario.cargar(existente);
            Window propietario = tblContratos.getScene() == null ? null : tblContratos.getScene().getWindow();
            boolean nuevo = existente == null;
            Dialog<ButtonType> dialogo = DialogoFormulario.crear(nuevo ? "Nuevo contrato" : "Editar contrato",
                    contenido, propietario, 740, 680, nuevo ? "Guardar contrato" : "Guardar cambios");
            DialogoFormulario.protegerCambiosSinGuardar(dialogo, nuevo ? btnNuevo : tblContratos, formulario::hayCambios);
            ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
            Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
            accion.addEventFilter(ActionEvent.ACTION, evento -> {
                evento.consume();
                if (!formulario.validar()) return;
                ContratoFormController.Datos datos = formulario.datos();
                UUID contratoId = nuevo ? null : existente.getId();
                long version = nuevo ? 0 : existente.getVersion();
                UUID unidadId = datos.unidad().getId();
                UUID inquilinoId = datos.inquilino().getId();
                BigDecimal canonGuardado = datos.canon();
                BigDecimal depositoGuardado = datos.deposito();
                accion.setDisable(true); accion.setText("Guardando contrato…");
                Task<Void> tarea = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        if (nuevo) {
                            Contrato contrato = new Contrato();
                            contrato.setUnidadId(unidadId);
                            contrato.setInquilinoId(inquilinoId);
                            contrato.setInicio(datos.inicio());
                            contrato.setFin(datos.fin());
                            contrato.setCanon(canonGuardado);
                            contrato.setMoneda(datos.moneda());
                            contrato.setDiaPago(datos.diaPago());
                            contrato.setDepositoPactado(depositoGuardado);
                            contrato.setEstado(datos.estado());
                            contratoService.guardar(contrato);
                        } else {
                            contratoService.actualizar(contratoId, version, unidadId, inquilinoId,
                                    datos.inicio(), datos.fin(), canonGuardado, datos.moneda(), datos.diaPago(),
                                    depositoGuardado, datos.estado());
                        }
                        return null;
                    }
                };
                TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                    formulario.marcarLimpio(); DialogoFormulario.cerrar(dialogo);
                    AlertaUtil.mostrarExito(nuevo ? "Contrato registrado" : "Contrato actualizado",
                            nuevo ? "El contrato quedó registrado." : "Los términos del contrato quedaron guardados.");
                    cargarDatos();
                }, error -> {
                    accion.setDisable(false); accion.setText(nuevo ? "Guardar contrato" : "Guardar cambios");
                    formulario.mostrarError(mensajeError(error));
                });
            });
            dialogo.showAndWait();
        } catch (IOException error) {
            AlertaUtil.mostrarError("No se pudo abrir el formulario", "Vuelve a intentarlo.");
        }
    }

    @FXML public void handleTerminarContrato() {
        Contrato contrato = seleccionado;
        if (contrato == null || !sePuedeFinalizar(contrato)) return;
        String registro = "Contrato de " + contrato.getInquilinoNombre() + " · "
                + contrato.getUnidadInfo() + " · " + fecha(contrato.getInicio()) + " a " + fecha(contrato.getFin());
        if (!AlertaUtil.mostrarConfirmacion("Finalizar contrato", registro
                + "\nEl estado pasará a Terminado y la unidad dejará de estar reservada.", "Finalizar")) return;
        UUID contratoId = contrato.getId();
        btnTerminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { contratoService.terminarContrato(contratoId); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Contrato finalizado", "El contrato quedó marcado como Terminado.");
            cargarDatos();
        }, error -> {
            actualizarAcciones();
            AlertaUtil.mostrarError("No se pudo finalizar el contrato", mensajeError(error));
        });
    }

    @FXML public void handleAjustarGarantia() {
        Contrato contrato = seleccionado;
        if (!permisoAjustarGarantia || contrato == null || contrato.getGarantiaSeguimiento() == null) return;
        Garantia garantia = contrato.getGarantiaSeguimiento();
        UUID garantiaId = garantia.getId();
        BigDecimal maximo = garantia.getMontoInicial();
        TextField txtSaldo = new TextField(garantia.getSaldoActual().toPlainString());
        txtSaldo.getStyleClass().add("field");
        Label error = new Label("");
        error.getStyleClass().add("field-error");
        error.setMinHeight(18);
        error.setVisible(false);
        error.setManaged(false);
        VBox contenido = new VBox(8,
                new Label("Contrato de " + contrato.getInquilinoNombre()),
                new Label("Nuevo saldo retenido · máximo " + maximo.toPlainString() + " " + contrato.getMoneda()),
                txtSaldo, error);
        contenido.setPadding(new Insets(16));
        Window propietario = tblContratos.getScene() == null ? null : tblContratos.getScene().getWindow();
        Dialog<ButtonType> dialogo = DialogoFormulario.crear("Ajustar garantía", contenido, propietario,
                460, 340, "Guardar ajuste");
        String saldoInicial = txtSaldo.getText();
        DialogoFormulario.protegerCambiosSinGuardar(dialogo, btnAjustarGarantia,
                () -> !saldoInicial.equals(txtSaldo.getText()));
        ButtonType tipoAccion = dialogo.getDialogPane().getButtonTypes().getFirst();
        Button accion = (Button) dialogo.getDialogPane().lookupButton(tipoAccion);
        accion.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            BigDecimal nuevoSaldo;
            try {
                nuevoSaldo = new BigDecimal(txtSaldo.getText().strip());
                if (nuevoSaldo.signum() < 0 || nuevoSaldo.compareTo(maximo) > 0
                        || nuevoSaldo.stripTrailingZeros().scale() > 2) throw new NumberFormatException();
            } catch (RuntimeException invalido) {
                error.setText("El saldo debe estar entre cero y " + maximo.toPlainString()
                        + " y tener hasta dos decimales.");
                error.setVisible(true); error.setManaged(true);
                return;
            }
            accion.setDisable(true); accion.setText("Guardando ajuste…");
            BigDecimal saldoAjustado = nuevoSaldo;
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() throws SQLException {
                    garantiaService.ajustarSaldo(garantiaId, saldoAjustado);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Garantía actualizada", "El saldo de la garantía quedó actualizado.");
                cargarDatos();
            }, fallo -> {
                accion.setDisable(false); accion.setText("Guardar ajuste");
                error.setText(mensajeError(fallo)); error.setVisible(true); error.setManaged(true);
            });
        });
        dialogo.showAndWait();
    }

    @FXML public void handleEliminar() {
        Contrato contrato = seleccionado;
        if (contrato == null || !permisoEliminar) return;
        String registro = "Contrato: " + contrato.getInquilinoNombre() + " · " + contrato.getUnidadInfo()
                + " · " + fecha(contrato.getInicio()) + " a " + fecha(contrato.getFin());
        if (!AlertaUtil.mostrarConfirmacionDestructiva("Eliminar contrato", registro,
                "El registro se eliminará físicamente si no tiene cargos ni pagos asociados.", "Eliminar contrato")) return;
        UUID contratoId = contrato.getId();
        btnEliminar.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() throws SQLException { contratoService.eliminar(contratoId); return null; }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ignorado -> {
            AlertaUtil.mostrarExito("Contrato eliminado", "El registro del contrato se eliminó.");
            cargarDatos();
        }, error -> {
            actualizarAcciones();
            if (error instanceof SecurityException) {
                AlertaUtil.mostrarAdvertencia("No tienes permiso", "Solo el propietario puede eliminar contratos.");
            } else {
                AlertaUtil.mostrarError("No se pudo eliminar el contrato",
                        "Revisa si hay cargos o pagos asociados e inténtalo de nuevo.");
            }
        });
    }

    @FXML public void limpiarFiltros() { txtBuscar.clear(); }

    private void mostrarCatalogos(Catalogos catalogos) {
        unidadesDisponibles = catalogos.unidades();
        inquilinosDisponibles = catalogos.inquilinos();
    }

    private void actualizarAcciones() {
        btnTerminar.setDisable(seleccionado == null || !sePuedeFinalizar(seleccionado));
        btnEliminar.setDisable(seleccionado == null || !permisoEliminar);
        boolean puedeAjustar = seleccionado != null && seleccionado.getGarantiaSeguimiento() != null
                && permisoAjustarGarantia;
        btnAjustarGarantia.setDisable(!puedeAjustar);
        if (seleccionado != null && seleccionado.getGarantiaSeguimiento() == null) {
            Tooltip.install(garantiaTooltipHost, new Tooltip("Este contrato no tiene seguimiento de garantía."));
        } else if (!permisoAjustarGarantia) {
            Tooltip.install(garantiaTooltipHost, new Tooltip("Solo el propietario puede ajustar garantías."));
        } else {
            Tooltip.install(garantiaTooltipHost, new Tooltip("Selecciona un contrato con seguimiento de garantía."));
        }
    }

    private static boolean sePuedeFinalizar(Contrato contrato) {
        return !"Terminado".equalsIgnoreCase(contrato.getEstado())
                && !"Cancelado".equalsIgnoreCase(contrato.getEstado());
    }

    private void ocultarError() { lblErrorConsulta.setText(""); lblErrorConsulta.setVisible(false); lblErrorConsulta.setManaged(false); }
    private static boolean contiene(String texto, String filtro) { return texto != null && texto.toLowerCase(Locale.ROOT).contains(filtro); }
    private static String fecha(LocalDate fecha) { return fecha == null ? "—" : fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    private static String mensajeError(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof SecurityException || error instanceof IllegalStateException) {
            return error.getMessage();
        }
        return "Revisa los datos e inténtalo de nuevo.";
    }
}
