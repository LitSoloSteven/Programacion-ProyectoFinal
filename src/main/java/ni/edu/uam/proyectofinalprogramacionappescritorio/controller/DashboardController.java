package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.MetricasResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto.TotalMonedaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAuth;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;

import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Notificaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TransicionesUI;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador del Dashboard principal de Latch.
 * Coordina la navegación entre módulos y presenta las métricas del panel.
 */
public class DashboardController {
    private static final DateTimeFormatter HORA_VISIBLE = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private VBox sidebar;
    @FXML private VBox navContainer;
    @FXML private VBox userProfile;
    @FXML private Label lblContexto;
    @FXML private Label lblActualizado;
    @FXML private Button btnActualizar;
    @FXML private Button btnTema;
    @FXML private CheckBox chkReducirMovimiento;

    @FXML
    private Label lblUsuarioNombre;

    @FXML
    private Label lblUsuarioRol;

    @FXML private Label lblTituloKpiPropiedades;
    @FXML private Label lblTituloKpiUnidades;
    @FXML private Label lblTituloKpiInquilinos;
    @FXML private Label lblTituloKpiContratos;
    @FXML private Label lblTituloKpiCargos;
    @FXML private Label lblTituloKpiPagos;

    @FXML
    private Button btnNavDashboard;

    @FXML
    private Button btnNavPropiedades;

    @FXML
    private Button btnNavUnidades;

    @FXML
    private Button btnNavInquilinos;

    @FXML
    private Button btnNavContratos;

    @FXML
    private Button btnNavCargos;

    @FXML
    private Button btnNavPagos;

    @FXML
    private Button btnNavTasas;

    @FXML
    private Button btnCerrarSesion;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox homeView;

    @FXML
    private Label lblTotalPropiedades;

    @FXML
    private Label lblTotalUnidades;

    @FXML
    private Label lblTotalInquilinos;

    @FXML
    private Label lblTotalContratos;

    @FXML
    private Label lblCargosPendientes;

    @FXML private Label lblCargosMontoUsd;
    @FXML private Label lblCargosMontoNio;

    @FXML
    private Label lblPagosRegistrados;

    @FXML private Label lblPagosMontoUsd;
    @FXML private Label lblPagosMontoNio;

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

        actualizarMetricas();
        configurarNavegacion();
        Iconos.asignar(btnActualizar, Iconos.Nombre.ACTUALIZAR, 16);
        asignarIconosKpi();
        Iconos.asignar(btnTema, Iconos.Nombre.TEMA, 16);
        btnTema.setAccessibleText("Cambiar tema de color");
        actualizarControlTema();
        Notificaciones.registrarAnfitrion(contentArea);
    }

    private void asignarIconosKpi() {
        Label[] titulos = {lblTituloKpiPropiedades, lblTituloKpiUnidades, lblTituloKpiInquilinos,
                lblTituloKpiContratos, lblTituloKpiCargos, lblTituloKpiPagos};
        Iconos.Nombre[] iconos = {Iconos.Nombre.PROPIEDAD, Iconos.Nombre.UNIDAD, Iconos.Nombre.PERSONAS,
                Iconos.Nombre.CONTRATO, Iconos.Nombre.CARGO, Iconos.Nombre.PAGO};
        for (int i = 0; i < titulos.length; i++) {
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
        TemaAplicacion.establecerMovimientoReducido(chkReducirMovimiento.isSelected());
    }

    private void actualizarControlTema() {
        btnTema.setText(TemaAplicacion.getTema() == TemaAplicacion.Tema.OSCURO ? "Tema claro" : "Tema oscuro");
        btnTema.setTooltip(new Tooltip("Cambiar a tema "
                + (TemaAplicacion.getTema() == TemaAplicacion.Tema.OSCURO ? "claro" : "oscuro")));
        chkReducirMovimiento.setSelected(TemaAplicacion.isMovimientoReducido());
    }

    @FXML
    public void actualizarMetricas() {
        if (btnActualizar != null) {
            btnActualizar.setDisable(true);
            btnActualizar.setText("Actualizando…");
        }
        Task<MetricasResponse> tarea = new Task<>() {
            @Override
            protected MetricasResponse call() {
                return ClienteLatch.api().get("/api/v1/dashboard/metricas", MetricasResponse.class);
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, metricas -> {
            lblTotalPropiedades.setText(String.valueOf(metricas.propiedades()));
            lblTotalUnidades.setText(String.valueOf(metricas.unidades()));
            lblTotalInquilinos.setText(String.valueOf(metricas.inquilinos()));
            lblTotalContratos.setText(String.valueOf(metricas.contratosVigentes()));

            lblCargosPendientes.setText(String.valueOf(metricas.cargosPendientes().stream().mapToLong(TotalMonedaResponse::cantidad).sum()));
            actualizarImportesPorMoneda(metricas.cargosPendientes(), lblCargosMontoUsd, lblCargosMontoNio);

            lblPagosRegistrados.setText(String.valueOf(metricas.pagosRegistrados().stream().mapToLong(TotalMonedaResponse::cantidad).sum()));
            actualizarImportesPorMoneda(metricas.pagosRegistrados(), lblPagosMontoUsd, lblPagosMontoNio);
            lblActualizado.setText("Actualizado a las "
                    + HORA_VISIBLE.format(LocalTime.now(RelojService.ZONA_OPERATIVA)));
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

    private void actualizarImportesPorMoneda(List<TotalMonedaResponse> importes, Label usd, Label nio) {
        usd.setText(formatearImporte(importes, "USD"));
        nio.setText(formatearImporte(importes, "NIO"));
    }

    private String formatearImporte(List<TotalMonedaResponse> importes, String moneda) {
        return importes.stream()
                .filter(total -> moneda.equals(total.moneda()))
                .findFirst()
                .map(total -> FormatoMoneda.formatear(total.moneda(), new BigDecimal(total.total())))
                .orElseGet(() -> FormatoMoneda.formatear(moneda, BigDecimal.ZERO));
    }

    @FXML
    public void navegarDashboard() {
        activarBoton(btnNavDashboard);
        lblContexto.setText("Panel principal");
        TransicionesUI.reemplazar(contentArea, homeView);
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
    public void navegarTasas() {
        activarBoton(btnNavTasas);
        lblContexto.setText("Tasas de cambio");
        cargarVista("tasa-view.fxml");
    }

    private void cargarVista(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
            Parent view = loader.load();
            TransicionesUI.reemplazar(contentArea, view);
        } catch (IOException e) {
            AlertaUtil.mostrarError("No se pudo abrir la pantalla",
                    "La vista solicitada no está disponible. Inténtalo nuevamente.");
        }
    }

    private void configurarNavegacion() {
        Button[] botones = {btnNavDashboard, btnNavPropiedades, btnNavUnidades, btnNavInquilinos,
                btnNavContratos, btnNavCargos, btnNavPagos, btnNavTasas};
        Iconos.Nombre[] iconos = {Iconos.Nombre.PANEL, Iconos.Nombre.PROPIEDAD,
                Iconos.Nombre.UNIDAD, Iconos.Nombre.PERSONAS, Iconos.Nombre.CONTRATO,
                Iconos.Nombre.CARGO, Iconos.Nombre.PAGO, Iconos.Nombre.TASA};
        for (int i = 0; i < botones.length; i++) {
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
                btnNavContratos, btnNavCargos, btnNavPagos, btnNavTasas};
        for (Button boton : botones) {
            boton.setContentDisplay(compacta ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
            boton.setAlignment(compacta ? javafx.geometry.Pos.CENTER : javafx.geometry.Pos.CENTER_LEFT);
            boton.setTooltip(compacta ? new Tooltip(boton.getAccessibleText()) : null);
        }
        btnCerrarSesion.setContentDisplay(compacta ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        btnCerrarSesion.setAlignment(compacta ? javafx.geometry.Pos.CENTER : javafx.geometry.Pos.CENTER_LEFT);
    }

    private void activarBoton(Button botonActivo) {
        Button[] botones = {btnNavDashboard, btnNavPropiedades, btnNavUnidades, btnNavInquilinos, btnNavContratos, btnNavCargos, btnNavPagos, btnNavTasas};
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
            cerrarSesionRemota();
            SesionUsuario.cerrarSesion();
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
            HelloApplication.instancia().getTareas().ejecutar(tarea);
        } catch (RuntimeException e) {
            // El servicio local ya no está disponible.
        }
    }

    private void mostrarLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 440, 520);
            TemaAplicacion.aplicar(scene);

            stage.setTitle("Latch — Iniciar Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMinWidth(440);
            stage.setMinHeight(520);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            AlertaUtil.mostrarError("No se pudo abrir el acceso",
                    "Inténtalo nuevamente para volver a iniciar sesión.");
        }
    }
}
