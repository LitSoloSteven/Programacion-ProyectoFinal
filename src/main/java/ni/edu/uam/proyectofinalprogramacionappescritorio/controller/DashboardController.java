package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.ComposicionAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TasaResumenResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ReporteService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MonitorInactividad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Notificaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TransicionesUI;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controlador del Dashboard principal de Latch.
 * Coordina la navegación entre módulos, presenta métricas del panel y
 * supervisa las preferencias locales y la inactividad de sesión.
 */
public class DashboardController {
    private static final DateTimeFormatter HORA_VISIBLE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CORTE_VISIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TODOS_LOS_PERIODOS = "Todos los períodos";

    @FXML private VBox sidebar;
    @FXML private VBox navContainer;
    @FXML private VBox userProfile;
    @FXML private Label lblContexto;
    @FXML private Label lblActualizado;
    @FXML private Label lblCorteGeneracion;
    @FXML private ComboBox<String> cmbPeriodoDashboard;
    @FXML private Button btnActualizar;
    @FXML private Button btnCambiarPassword;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnTema;
    @FXML private CheckBox chkReducirMovimiento;

    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    @FXML private Label lblTituloKpiOcupacion;
    @FXML private Label lblTituloKpiPropiedades;
    @FXML private Label lblTituloKpiInquilinos;
    @FXML private Label lblTituloKpiContratos;
    @FXML private Label lblTituloKpiPorVencer;
    @FXML private Label lblTituloKpiTasa;
    @FXML private Label lblTituloKpiCargos;
    @FXML private Label lblTituloKpiMora;
    @FXML private Label lblTituloKpiPagos;

    @FXML private VBox cardOcupacion;
    @FXML private VBox cardPropiedades;
    @FXML private VBox cardInquilinos;
    @FXML private VBox cardContratos;
    @FXML private VBox cardPorVencer;
    @FXML private VBox cardTasa;
    @FXML private VBox cardCargos;
    @FXML private VBox cardMora;
    @FXML private VBox cardPagos;

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavPropiedades;
    @FXML private Button btnNavUnidades;
    @FXML private Button btnNavInquilinos;
    @FXML private Button btnNavContratos;
    @FXML private Button btnNavCargos;
    @FXML private Button btnNavPagos;
    @FXML private Button btnNavMantenimiento;
    @FXML private Button btnNavTasas;
    @FXML private Button btnNavReportes;
    @FXML private Button btnNavAdmin;
    @FXML private Button btnCerrarSesion;

    @FXML private StackPane contentArea;
    @FXML private Region auroraFondo;
    @FXML private ScrollPane homeView;

    @FXML private Label lblPorcentajeOcupacion;
    @FXML private Label lblTotalUnidades;
    @FXML private Label lblTotalPropiedades;
    @FXML private Label lblTotalInquilinos;
    @FXML private Label lblTotalContratos;
    @FXML private Label lblContratosVencidos;
    @FXML private Label lblContratosPorVencer;
    @FXML private Label lblTasaValor;
    @FXML private Label lblTasaDetalle;
    @FXML private Label lblCargosPendientes;
    @FXML private Label lblCargosMontoUsd;
    @FXML private Label lblCargosMontoNio;
    @FXML private Label lblMoraCantidad;
    @FXML private Label lblMoraMontoUsd;
    @FXML private Label lblMoraMontoNio;
    @FXML private Label lblPagosRegistrados;
    @FXML private Label lblPagosMontoUsd;
    @FXML private Label lblPagosMontoNio;

    private final ReporteService reporteService = new ReporteService();
    private MetricasResponse ultimasMetricas;
    private MonitorInactividad monitorInactividad;
    private boolean inicializandoPeriodos;

    @FXML
    public void initialize() {
        SesionUsuario usuario = SesionUsuario.getActual();
        if (usuario != null) {
            lblUsuarioNombre.setText(usuario.getNombre());
            lblUsuarioRol.setText(usuario.getRol());
        } else {
            lblUsuarioNombre.setText("Usuario");
            lblUsuarioRol.setText("Invitado");
        }

        inicializarPeriodos();
        configurarNavegacion();
        configurarTarjetasAccionables();
        Iconos.asignar(btnActualizar, Iconos.Nombre.ACTUALIZAR, 16);
        if (btnCambiarPassword != null) {
            Iconos.asignar(btnCambiarPassword, Iconos.Nombre.EDITAR, 16);
            btnCambiarPassword.setTooltip(new Tooltip("Cambiar mi contraseña"));
        }
        if (btnGenerarReporte != null) {
            Iconos.asignar(btnGenerarReporte, Iconos.Nombre.REPORTE, 16);
            btnGenerarReporte.setTooltip(new Tooltip("Vista previa y exportación PDF del resumen del panel"));
        }
        asignarIconosKpi();
        Iconos.asignar(btnTema, Iconos.Nombre.TEMA, 16);
        btnTema.setAccessibleText("Cambiar tema de color");
        TemaAplicacion.registrarObservadorAviso(null);
        actualizarControlTema();
        Notificaciones.registrarAnfitrion(contentArea);
        iniciarMonitorInactividad();
        actualizarMetricas();
    }

    private void inicializarPeriodos() {
        if (cmbPeriodoDashboard == null) return;
        inicializandoPeriodos = true;
        try {
            List<String> opciones = new ArrayList<>();
            opciones.add(TODOS_LOS_PERIODOS);
            YearMonth actual = YearMonth.now(RelojService.ZONA_OPERATIVA);
            for (int i = 0; i < 12; i++) {
                opciones.add(actual.minusMonths(i).toString());
            }
            cmbPeriodoDashboard.setItems(FXCollections.observableArrayList(opciones));
            cmbPeriodoDashboard.getSelectionModel().selectFirst();
        } finally {
            inicializandoPeriodos = false;
        }
    }

    private void configurarTarjetasAccionables() {
        asociarAccionTeclado(cardOcupacion, this::abrirDetalleUnidades);
        asociarAccionTeclado(cardPropiedades, this::navegarPropiedades);
        asociarAccionTeclado(cardInquilinos, this::navegarInquilinos);
        asociarAccionTeclado(cardContratos, this::abrirDetalleContratosVigentes);
        asociarAccionTeclado(cardPorVencer, this::abrirDetalleContratosPorVencer);
        asociarAccionTeclado(cardTasa, this::navegarTasas);
        asociarAccionTeclado(cardCargos, this::abrirDetalleCargos);
        asociarAccionTeclado(cardMora, this::abrirDetalleMora);
        asociarAccionTeclado(cardPagos, this::abrirDetallePagos);
    }

    private static void asociarAccionTeclado(VBox tarjeta, Runnable accion) {
        if (tarjeta == null || accion == null) return;
        tarjeta.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER || evento.getCode() == KeyCode.SPACE) {
                accion.run();
                evento.consume();
            }
        });
    }

    private void iniciarMonitorInactividad() {
        if (contentArea == null) return;
        SesionUsuario identidad = SesionUsuario.getActual();
        if (identidad == null || !identidad.isVigente()) return;

        Task<Integer> leerMinutos = new Task<>() {
            @Override protected Integer call() {
                try {
                    return LatchApplication.instancia().getComposicion()
                            .configuracion().leerMinutosInactividad(identidad);
                } catch (RuntimeException error) {
                    return 15;
                }
            }
        };
        Runnable vincular = () -> {
            int minutos = leerMinutos.isDone() && leerMinutos.getValue() != null ? leerMinutos.getValue() : 15;
            if (contentArea.getScene() == null || SesionUsuario.getActual() != identidad || !identidad.isVigente()) return;
            detenerMonitorInactividad();
            monitorInactividad = MonitorInactividad.crear(
                    minutos,
                    () -> Notificaciones.mostrarAdvertencia(
                            "Tu sesión se cerrará por inactividad en 60 segundos. Interactúa para continuar."),
                    this::cerrarPorInactividad);
            monitorInactividad.vincularEscena(contentArea.getScene());
            LatchApplication.instancia().registrarMonitorInactividad(monitorInactividad);
        };
        contentArea.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena == null) {
                detenerMonitorInactividad();
            } else if (leerMinutos.isDone()) {
                vincular.run();
            }
        });
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), leerMinutos,
                minutos -> vincular.run(), error -> vincular.run());
    }

    private void detenerMonitorInactividad() {
        if (monitorInactividad != null) {
            monitorInactividad.detener();
            monitorInactividad = null;
        }
    }

    private void cerrarPorInactividad() {
        detenerMonitorInactividad();
        cerrarSesionRemota();
        SesionUsuario.cerrarSesion();
        TemaAplicacion.limpiarUsuarioActivo();
        Stage principal = contentArea != null && contentArea.getScene() != null
                && contentArea.getScene().getWindow() instanceof Stage st ? st : null;
        if (principal != null) {
            for (Window ventana : List.copyOf(Window.getWindows()).reversed()) {
                if (perteneceA(ventana, principal)) ventana.hide();
            }
            mostrarLoginEnStage(principal);
        }
        AlertaUtil.mostrarAdvertencia("Sesión cerrada por inactividad",
                "Tu sesión se cerró por inactividad. Vuelve a iniciar sesión para continuar.");
    }

    private static boolean perteneceA(Window ventana, Stage principal) {
        while (ventana instanceof Stage dialogo && dialogo.getOwner() != null) {
            ventana = dialogo.getOwner();
            if (ventana == principal) return true;
        }
        return false;
    }

    private void asignarIconosKpi() {
        Label[] titulos = {
                lblTituloKpiOcupacion, lblTituloKpiPropiedades, lblTituloKpiInquilinos,
                lblTituloKpiContratos, lblTituloKpiPorVencer, lblTituloKpiTasa,
                lblTituloKpiCargos, lblTituloKpiMora, lblTituloKpiPagos};
        Iconos.Nombre[] iconos = {
                Iconos.Nombre.UNIDAD, Iconos.Nombre.PROPIEDAD, Iconos.Nombre.PERSONAS,
                Iconos.Nombre.CONTRATO, Iconos.Nombre.RELOJ, Iconos.Nombre.TASA,
                Iconos.Nombre.CARGO, Iconos.Nombre.ADVERTENCIA, Iconos.Nombre.PAGO};
        for (int i = 0; i < titulos.length; i++) {
            if (titulos[i] == null) continue;
            Iconos.asignar(titulos[i], iconos[i], 14);
            titulos[i].setGraphicTextGap(6);
        }
    }

    @FXML
    public void handleCambiarTema() {
        TemaAplicacion.Tema nuevo = TemaAplicacion.getTema() == TemaAplicacion.Tema.OSCURO
                ? TemaAplicacion.Tema.CLARO : TemaAplicacion.Tema.OSCURO;
        TemaAplicacion.establecerTema(nuevo);
        actualizarControlTema();
    }

    @FXML
    public void handleReducirMovimiento() {
        TemaAplicacion.establecerSinEfectos(chkReducirMovimiento.isSelected());
        actualizarControlTema();
    }

    @FXML
    public void handleCambiarPeriodo() {
        if (!inicializandoPeriodos) {
            actualizarMetricas();
        }
    }

    private void actualizarControlTema() {
        boolean oscuro = TemaAplicacion.getTema() == TemaAplicacion.Tema.OSCURO;
        btnTema.setText(oscuro ? "Tema claro" : "Tema oscuro");
        btnTema.setTooltip(new Tooltip("Cambiar a tema " + (oscuro ? "claro" : "oscuro")));
        chkReducirMovimiento.setSelected(TemaAplicacion.isSinEfectos());
        chkReducirMovimiento.setTooltip(new Tooltip("Elimina animaciones y luces decorativas en ambos temas"));
    }

    private String periodoSeleccionado() {
        if (cmbPeriodoDashboard == null) return null;
        String valor = cmbPeriodoDashboard.getValue();
        return (valor == null || TODOS_LOS_PERIODOS.equals(valor)) ? null : valor;
    }

    @FXML
    public void actualizarMetricas() {
        if (btnActualizar != null) {
            btnActualizar.setDisable(true);
            btnActualizar.setText("Actualizando…");
        }
        String periodo = periodoSeleccionado();
        String ruta = periodo == null
                ? "/api/v1/dashboard/metricas"
                : "/api/v1/dashboard/metricas?periodo=" + URLEncoder.encode(periodo, StandardCharsets.UTF_8);
        Task<MetricasResponse> tarea = new Task<>() {
            @Override
            protected MetricasResponse call() {
                return ClienteLatch.api().get(ruta, MetricasResponse.class);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, metricas -> {
            ultimasMetricas = metricas;
            if (lblPorcentajeOcupacion != null) {
                lblPorcentajeOcupacion.setText((metricas.porcentajeOcupacion() == null ? "0.00" : metricas.porcentajeOcupacion()) + " %");
            }
            lblTotalUnidades.setText(metricas.unidadesOcupadas() + " de " + metricas.unidadesActivas()
                    + " activas (" + metricas.unidades() + " en inventario)");
            lblTotalPropiedades.setText(String.valueOf(metricas.propiedades()));
            lblTotalInquilinos.setText(String.valueOf(metricas.inquilinos()));
            lblTotalContratos.setText(String.valueOf(metricas.contratosVigentes()));
            if (lblContratosVencidos != null) {
                lblContratosVencidos.setText(metricas.contratosVencidos() > 0
                        ? metricas.contratosVencidos() + " vencidos pendientes de cierre"
                        : "Contratos en curso");
            }
            if (lblContratosPorVencer != null) {
                lblContratosPorVencer.setText(String.valueOf(metricas.contratosPorVencer()));
            }

            actualizarTasa(metricas.tasaDisponible());

            lblCargosPendientes.setText(String.valueOf(metricas.cargosPendientes().stream().mapToLong(TotalMonedaResponse::cantidad).sum()));
            actualizarImportesPorMoneda(metricas.cargosPendientes(), lblCargosMontoUsd, lblCargosMontoNio);

            if (lblMoraCantidad != null) {
                long cantMora = metricas.moraExigible() == null ? 0L
                        : metricas.moraExigible().stream().mapToLong(TotalMonedaResponse::cantidad).sum();
                lblMoraCantidad.setText(String.valueOf(cantMora));
                actualizarImportesPorMoneda(
                        metricas.moraExigible() == null ? List.of() : metricas.moraExigible(),
                        lblMoraMontoUsd, lblMoraMontoNio);
            }

            lblPagosRegistrados.setText(String.valueOf(metricas.pagosRegistrados().stream().mapToLong(TotalMonedaResponse::cantidad).sum()));
            actualizarImportesPorMoneda(metricas.pagosRegistrados(), lblPagosMontoUsd, lblPagosMontoNio);

            String horaCorte = formatearCorte(metricas.instanteGeneracion());
            lblActualizado.setText("Actualizado a las "
                    + HORA_VISIBLE.format(LocalTime.now(RelojService.ZONA_OPERATIVA)));
            if (lblCorteGeneracion != null) {
                lblCorteGeneracion.setText("Corte de generación: " + horaCorte
                        + (metricas.periodo() == null ? " · Todos los períodos" : " · Período " + metricas.periodo()));
            }
            if (btnActualizar != null) {
                btnActualizar.setDisable(false);
                btnActualizar.setText("Actualizar");
            }
        }, error -> {
            if (btnActualizar != null) {
                btnActualizar.setDisable(false);
                btnActualizar.setText("Actualizar");
            }
            if (error instanceof ErrorClienteException api && api.getEstado() == 403) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado",
                        "Esta cuenta no puede consultar estas métricas.");
            } else {
                AlertaUtil.mostrarError("No se pudo actualizar el panel",
                        "No fue posible cargar las métricas. Inténtalo nuevamente.");
            }
        });
    }

    private void actualizarTasa(TasaResumenResponse tasa) {
        if (lblTasaValor == null || lblTasaDetalle == null) return;
        if (tasa == null || !tasa.disponible() || tasa.valor() == null) {
            lblTasaValor.setText("Sin tasa disponible");
            lblTasaDetalle.setText("Sin tasa autorizada ni referencia H3 registrada");
            return;
        }
        lblTasaValor.setText(tasa.valor());
        String antiguedad = formatearAntiguedad(tasa.antiguedadSegundos());
        String fecha = tasa.fechaEfectiva() == null ? "—" : tasa.fechaEfectiva();
        lblTasaDetalle.setText(tasa.origen() + " · " + fecha + " (" + antiguedad + ")");
    }

    private static String formatearAntiguedad(Long segundos) {
        if (segundos == null || segundos < 0) return "sin registro";
        if (segundos < 60) return "hace " + segundos + " s";
        long minutos = segundos / 60;
        if (minutos < 60) return "hace " + minutos + " min";
        long horas = minutos / 60;
        if (horas < 48) return "hace " + horas + " h";
        return "hace " + (horas / 24) + " d";
    }

    private static String formatearCorte(String isoInstante) {
        if (isoInstante == null || isoInstante.isBlank()) {
            return CORTE_VISIBLE.format(Instant.now().atZone(RelojService.ZONA_OPERATIVA));
        }
        try {
            return CORTE_VISIBLE.format(Instant.parse(isoInstante).atZone(RelojService.ZONA_OPERATIVA));
        } catch (RuntimeException ignored) {
            try {
                return CORTE_VISIBLE.format(java.time.OffsetDateTime.parse(isoInstante)
                        .atZoneSameInstant(RelojService.ZONA_OPERATIVA));
            } catch (RuntimeException otro) {
                return isoInstante;
            }
        }
    }

    private void actualizarImportesPorMoneda(List<TotalMonedaResponse> importes, Label usd, Label nio) {
        if (usd != null) usd.setText(formatearImporte(importes, "USD"));
        if (nio != null) nio.setText(formatearImporte(importes, "NIO"));
    }

    private String formatearImporte(List<TotalMonedaResponse> importes, String moneda) {
        return importes.stream()
                .filter(total -> moneda.equals(total.moneda()))
                .findFirst()
                .map(total -> FormatoMoneda.formatear(total.moneda(), new BigDecimal(total.total())))
                .orElseGet(() -> FormatoMoneda.formatear(moneda, BigDecimal.ZERO));
    }

    @FXML
    public void handleGenerarReporte() {
        Window ventana = contentArea == null || contentArea.getScene() == null
                ? null
                : contentArea.getScene().getWindow();
        MetricasResponse snapshot = ultimasMetricas;
        VistaPreviaReporte.generarYMostrar(btnGenerarReporte, ventana, () -> {
            MetricasResponse metricas = snapshot != null
                    ? snapshot
                    : ClienteLatch.api().get("/api/v1/dashboard/metricas", MetricasResponse.class);
            return reporteService.generarDocumentoResumenDashboard(metricas);
        });
    }

    @FXML
    public void abrirDetalleUnidades() {
        navegarUnidades();
    }

    @FXML
    public void abrirDetalleContratosVigentes() {
        activarBoton(btnNavContratos);
        lblContexto.setText("Contratos · Vigente");
        cargarVista("contrato-view.fxml", controlador -> {
            if (controlador instanceof ContratoController c) {
                c.aplicarFiltroEstadoInicial("Vigente");
            }
        });
    }

    @FXML
    public void abrirDetalleContratosPorVencer() {
        activarBoton(btnNavContratos);
        lblContexto.setText("Contratos · Por vencer");
        cargarVista("contrato-view.fxml", controlador -> {
            if (controlador instanceof ContratoController c) {
                c.aplicarFiltroEstadoInicial("Por vencer");
            }
        });
    }

    @FXML
    public void abrirDetalleCargos() {
        String periodo = periodoSeleccionado();
        activarBoton(btnNavCargos);
        lblContexto.setText(periodo == null ? "Cargos" : "Cargos · " + periodo);
        cargarVista("cargo-view.fxml", controlador -> {
            if (controlador instanceof CargoController c && periodo != null) {
                c.aplicarFiltroInicial(periodo);
            }
        });
    }

    @FXML
    public void abrirDetalleMora() {
        activarBoton(btnNavCargos);
        lblContexto.setText("Cargos · Mora exigible");
        cargarVista("cargo-view.fxml", controlador -> {
            if (controlador instanceof CargoController c) {
                c.aplicarFiltroInicial("Mora");
            }
        });
    }

    @FXML
    public void abrirDetallePagos() {
        navegarPagos();
    }

    @FXML
    public void navegarDashboard() {
        activarBoton(btnNavDashboard);
        lblContexto.setText("Panel principal");
        if (!contentArea.getChildren().contains(homeView)) {
            if (auroraFondo != null) {
                contentArea.getChildren().setAll(auroraFondo, homeView);
            } else {
                contentArea.getChildren().setAll(homeView);
            }
            TransicionesUI.entrada(homeView);
        }
        actualizarMetricas();
    }

    @FXML
    public void navegarPropiedades() {
        activarBoton(btnNavPropiedades);
        lblContexto.setText("Propiedades");
        cargarVista("propiedad-view.fxml");
    }

    @FXML
    public void navegarUnidades() {
        activarBoton(btnNavUnidades);
        lblContexto.setText("Unidades");
        cargarVista("unidad-view.fxml");
    }

    @FXML
    public void navegarInquilinos() {
        activarBoton(btnNavInquilinos);
        lblContexto.setText("Inquilinos");
        cargarVista("inquilino-view.fxml");
    }

    @FXML
    public void navegarContratos() {
        activarBoton(btnNavContratos);
        lblContexto.setText("Contratos");
        cargarVista("contrato-view.fxml");
    }

    @FXML
    public void navegarCargos() {
        activarBoton(btnNavCargos);
        lblContexto.setText("Cargos");
        cargarVista("cargo-view.fxml");
    }

    @FXML
    public void navegarPagos() {
        activarBoton(btnNavPagos);
        lblContexto.setText("Pagos");
        cargarVista("pago-view.fxml");
    }

    @FXML
    public void navegarMantenimiento() {
        activarBoton(btnNavMantenimiento);
        lblContexto.setText("Mantenimiento");
        cargarVista("mantenimiento-view.fxml");
    }

    @FXML
    public void navegarTasas() {
        activarBoton(btnNavTasas);
        lblContexto.setText("Tasas de cambio");
        cargarVista("tasa-view.fxml");
    }

    @FXML
    public void navegarReportes() {
        activarBoton(btnNavReportes);
        lblContexto.setText("Reportes y exportación");
        cargarVista("reportes-view.fxml");
    }

    @FXML
    public void navegarAdministracion() {
        if (!SesionUsuario.esPropietario()) return;
        activarBoton(btnNavAdmin);
        lblContexto.setText("Administración y auditoría");
        cargarVista("admin-view.fxml");
    }

    @FXML
    public void handleCambiarPassword() {
        SesionUsuario identidad = SesionUsuario.getActual();
        if (identidad == null || !identidad.isVigente()) return;
        PasswordField txtActual = new PasswordField();
        PasswordField txtNueva = new PasswordField();
        PasswordField txtConfirmar = new PasswordField();
        txtActual.setPromptText("Contraseña actual");
        txtNueva.setPromptText("Nueva contraseña (12–128 caracteres)");
        txtConfirmar.setPromptText("Repite la nueva contraseña");
        Label lblErr = new Label();
        lblErr.getStyleClass().add("field-error");
        lblErr.setWrapText(true);
        lblErr.setVisible(false);
        lblErr.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Actual:"), txtActual);
        grid.addRow(1, new Label("Nueva:"), txtNueva);
        grid.addRow(2, new Label("Confirmar:"), txtConfirmar);
        grid.add(lblErr, 0, 3, 2, 1);

        Window propietario = contentArea != null && contentArea.getScene() != null
                ? contentArea.getScene().getWindow() : null;
        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Cambiar contraseña", grid, propietario, 460, 320, "Guardar contraseña");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            String actual = txtActual.getText();
            String nueva = txtNueva.getText();
            String confirmar = txtConfirmar.getText();
            if (nueva == null || !nueva.equals(confirmar)) {
                lblErr.setText("La nueva contraseña y su confirmación no coinciden.");
                lblErr.setVisible(true);
                lblErr.setManaged(true);
                return;
            }
            ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
            accion.setDisable(true);
            Task<Long> tarea = new Task<>() {
                @Override protected Long call() {
                    return comp.usuarios().cambiarPasswordPropia(
                            identidad,
                            actual == null ? new char[0] : actual.toCharArray(),
                            nueva.toCharArray());
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, nuevaVersion -> {
                txtActual.clear();
                txtNueva.clear();
                txtConfirmar.clear();
                if (identidad.isVigente()) {
                    SesionUsuario.establecer(SesionUsuario.paraSesionAutenticada(
                            identidad.getUsuarioId(), identidad.getNombre(), identidad.getRol(), nuevaVersion));
                }
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Contraseña actualizada", "Tu contraseña se actualizó correctamente.");
            }, error -> {
                accion.setDisable(false);
                String msg = (error instanceof IllegalArgumentException || error instanceof SecurityException)
                        ? error.getMessage() : "No se pudo cambiar la contraseña.";
                lblErr.setText(msg);
                lblErr.setVisible(true);
                lblErr.setManaged(true);
            });
        });
        dialogo.showAndWait();
    }

    private void cargarVista(String fxmlFile) {
        cargarVista(fxmlFile, null);
    }

    private void cargarVista(String fxmlFile, Consumer<Object> configurador) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource(fxmlFile));
            Parent view = loader.load();
            if (configurador != null) {
                configurador.accept(loader.getController());
            }
            if (auroraFondo != null) {
                contentArea.getChildren().setAll(auroraFondo, view);
                TransicionesUI.entrada(view);
            } else {
                TransicionesUI.reemplazar(contentArea, view);
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("No se pudo abrir la pantalla",
                    "La vista solicitada no está disponible. Inténtalo nuevamente.");
        }
    }

    private void configurarNavegacion() {
        boolean esPropietario = SesionUsuario.esPropietario();
        if (btnNavAdmin != null) {
            btnNavAdmin.setVisible(esPropietario);
            btnNavAdmin.setManaged(esPropietario);
        }
        Button[] botones = {btnNavDashboard, btnNavPropiedades, btnNavUnidades, btnNavInquilinos,
                btnNavContratos, btnNavCargos, btnNavPagos, btnNavMantenimiento, btnNavTasas, btnNavReportes, btnNavAdmin};
        Iconos.Nombre[] iconos = {Iconos.Nombre.PANEL, Iconos.Nombre.PROPIEDAD,
                Iconos.Nombre.UNIDAD, Iconos.Nombre.PERSONAS, Iconos.Nombre.CONTRATO,
                Iconos.Nombre.CARGO, Iconos.Nombre.PAGO, Iconos.Nombre.MANTENIMIENTO,
                Iconos.Nombre.TASA, Iconos.Nombre.REPORTE, Iconos.Nombre.EDITAR};
        for (int i = 0; i < botones.length; i++) {
            if (botones[i] == null) continue;
            String nombre = botones[i].getText();
            Iconos.asignar(botones[i], iconos[i], 18);
            botones[i].setAccessibleText(nombre);
            botones[i].setTooltip(new Tooltip(nombre));
        }
        Iconos.asignar(btnCerrarSesion, Iconos.Nombre.SALIR, 16);
        btnCerrarSesion.setAccessibleText("Cerrar sesión");
        btnCerrarSesion.setTooltip(new Tooltip("Cerrar sesión"));
        sidebar.sceneProperty().addListener((observable, anterior, escena) -> {
            if (escena != null) {
                escena.widthProperty().addListener((obs, anchoAnterior, anchoActual) ->
                        adaptarSidebar(anchoActual.doubleValue()));
                adaptarSidebar(escena.getWidth());
            }
        });
    }

    private void adaptarSidebar(double anchoVentana) {
        boolean compacta = anchoVentana < 1280;
        double ancho = compacta ? 72 : 224;
        sidebar.setPrefWidth(ancho);
        sidebar.setMinWidth(ancho);
        sidebar.setMaxWidth(ancho);
        sidebar.getChildren().stream().filter(nodo -> nodo.getStyleClass().contains("sidebar-brand-block"))
                .forEach(nodo -> { nodo.setVisible(!compacta); nodo.setManaged(!compacta); });
        userProfile.setVisible(!compacta);
        userProfile.setManaged(!compacta);
        sidebar.getChildren().stream().filter(nodo -> nodo.getStyleClass().contains("sidebar-separator"))
                .forEach(nodo -> { nodo.setVisible(!compacta); nodo.setManaged(!compacta); });
        Button[] botones = {btnNavDashboard, btnNavPropiedades, btnNavUnidades, btnNavInquilinos,
                btnNavContratos, btnNavCargos, btnNavPagos, btnNavMantenimiento, btnNavTasas, btnNavReportes, btnNavAdmin};
        for (Button boton : botones) {
            if (boton == null) continue;
            boton.setContentDisplay(compacta ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            boton.setAlignment(compacta ? javafx.geometry.Pos.CENTER : javafx.geometry.Pos.CENTER_LEFT);
            boton.setTooltip(compacta ? new Tooltip(boton.getAccessibleText()) : null);
        }
        btnCerrarSesion.setContentDisplay(compacta ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        btnCerrarSesion.setAlignment(compacta ? javafx.geometry.Pos.CENTER : javafx.geometry.Pos.CENTER_LEFT);
    }

    private void activarBoton(Button botonActivo) {
        Button[] botones = {btnNavDashboard, btnNavPropiedades, btnNavUnidades, btnNavInquilinos,
                btnNavContratos, btnNavCargos, btnNavPagos, btnNavMantenimiento, btnNavTasas, btnNavReportes, btnNavAdmin};
        for (Button b : botones) {
            if (b != null) {
                b.getStyleClass().remove("nav-btn-active");
            }
        }
        if (botonActivo != null && !botonActivo.getStyleClass().contains("nav-btn-active")) {
            botonActivo.getStyleClass().add("nav-btn-active");
        }
    }

    @FXML
    public void handleCerrarSesion(ActionEvent event) {
        boolean confirmar = AlertaUtil.mostrarConfirmacion(
                "Cerrar Sesión",
                "¿Quieres cerrar tu sesión de Latch?"
        );

        if (confirmar) {
            detenerMonitorInactividad();
            cerrarSesionRemota();
            SesionUsuario.cerrarSesion();
            TemaAplicacion.limpiarUsuarioActivo();
            mostrarLogin(event);
        }
    }

    private void cerrarSesionRemota() {
        final ClienteApi api;
        final String token;
        try {
            api = ClienteLatch.api();
            // Se captura y borra el token en el hilo de JavaFX, antes de
            // programar la revocación, para que ninguna petición posterior
            // lo reutilice.
            token = api.tomarToken();
        } catch (RuntimeException e) {
            return;
        }
        if (token == null || token.isBlank()) return;

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                try {
                    new ClienteAuth(api).logout(token);
                } catch (RuntimeException e) {
                    // La sesión local ya está cerrada; el token expira solo.
                }
                return null;
            }
        };
        try {
            LatchApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException e) {
            // El servicio local ya no está disponible.
        }
    }

    private void mostrarLogin(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        mostrarLoginEnStage(stage);
    }

    private void mostrarLoginEnStage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource("login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 460, 580);
            TemaAplicacion.aplicar(scene);

            stage.setTitle("Latch — Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMinWidth(460);
            stage.setMinHeight(580);
            stage.setWidth(460);
            stage.setHeight(580);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            AlertaUtil.mostrarError("No se pudo abrir el acceso",
                    "Inténtalo nuevamente para volver a iniciar sesión.");
        }
    }
}
