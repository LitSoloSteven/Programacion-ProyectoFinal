package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoMoneda;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Notificaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador del Dashboard principal de Latch.
 * Coordina navegación fluida entre módulos y visualización de métricas reales de PostgreSQL.
 */
public class DashboardController {
    private static final DateTimeFormatter HORA_VISIBLE = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private VBox sidebar;
    @FXML private VBox userProfile;
    @FXML private Label lblContexto;
    @FXML private Label lblActualizado;
    @FXML private Button btnActualizar;

    @FXML
    private Label lblUsuarioNombre;

    @FXML
    private Label lblUsuarioRol;

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

    @FXML
    private Label lblCargosMonto;

    @FXML
    private Label lblPagosRegistrados;

    @FXML
    private Label lblPagosMonto;

    private final DashboardService dashboardService;

    public DashboardController() {
        this(HelloApplication.instancia().getComposicion().dashboard());
    }

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

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
        Notificaciones.registrarAnfitrion(contentArea);
    }

    @FXML
    public void actualizarMetricas() {
        if (btnActualizar != null) {
            btnActualizar.setDisable(true);
            btnActualizar.setText("Actualizando…");
        }
        Task<MetricasDashboardDTO> tarea = new Task<>() {
            @Override
            protected MetricasDashboardDTO call() {
                return dashboardService.obtenerMetricas();
            }
        };
        TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, metricas -> {
            lblTotalPropiedades.setText(String.valueOf(metricas.propiedades()));
            lblTotalUnidades.setText(String.valueOf(metricas.unidades()));
            lblTotalInquilinos.setText(String.valueOf(metricas.inquilinos()));
            lblTotalContratos.setText(String.valueOf(metricas.contratosVigentes()));

            lblCargosPendientes.setText(String.valueOf(metricas.cantidadCargosPendientes()));
            lblCargosMonto.setText(String.join("\n", metricas.cargosPendientes().stream()
                    .map(total -> FormatoMoneda.formatear(total.moneda(), total.total())).toList()));

            lblPagosRegistrados.setText(String.valueOf(metricas.cantidadPagosRegistrados()));
            lblPagosMonto.setText(String.join("\n", metricas.pagosRegistrados().stream()
                    .map(total -> FormatoMoneda.formatear(total.moneda(), total.total())).toList()));
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
            if (error instanceof SecurityException permiso) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado",
                        "Esta cuenta no puede consultar estas métricas.");
            } else {
                AlertaUtil.mostrarError("No se pudo actualizar el panel",
                        "No fue posible cargar las métricas. Inténtalo nuevamente.");
            }
        });
    }

    @FXML
    public void navegarDashboard() {
        activarBoton(btnNavDashboard);
        lblContexto.setText("Panel principal");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(homeView);
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
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
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
