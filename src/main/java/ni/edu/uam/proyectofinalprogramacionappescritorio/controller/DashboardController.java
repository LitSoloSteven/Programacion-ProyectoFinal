package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.FormatoMoneda;

import java.io.IOException;

/**
 * Controlador del Dashboard principal de Latch.
 * Coordina navegación fluida entre módulos y visualización de métricas reales de PostgreSQL.
 */
public class DashboardController {

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
        this.dashboardService = new DashboardService();
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
    }

    @FXML
    public void actualizarMetricas() {
        try {
            MetricasDashboardDTO metricas = dashboardService.obtenerMetricas();
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
        } catch (SecurityException e) {
            AlertaUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
        } catch (RuntimeException e) {
            AlertaUtil.mostrarError("Error de Base de Datos", "No fue posible actualizar las métricas desde PostgreSQL.");
        }
    }

    @FXML
    public void navegarDashboard() {
        activarBoton(btnNavDashboard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(homeView);
        actualizarMetricas();
    }

    @FXML
    public void navegarPropiedades() {
        activarBoton(btnNavPropiedades);
        cargarVista("propiedad-view.fxml");
    }

    @FXML
    public void navegarUnidades() {
        activarBoton(btnNavUnidades);
        cargarVista("unidad-view.fxml");
    }

    @FXML
    public void navegarInquilinos() {
        activarBoton(btnNavInquilinos);
        cargarVista("inquilino-view.fxml");
    }

    @FXML
    public void navegarContratos() {
        activarBoton(btnNavContratos);
        cargarVista("contrato-view.fxml");
    }

    @FXML
    public void navegarCargos() {
        activarBoton(btnNavCargos);
        cargarVista("cargo-view.fxml");
    }

    @FXML
    public void navegarPagos() {
        activarBoton(btnNavPagos);
        cargarVista("pago-view.fxml");
    }

    private void cargarVista(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error al cargar pantalla", "No se pudo cargar la vista solicitada: " + fxmlFile);
        }
    }

    private void activarBoton(Button botonActivo) {
        Button[] botones = {btnNavDashboard, btnNavPropiedades, btnNavUnidades, btnNavInquilinos, btnNavContratos, btnNavCargos, btnNavPagos};
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
                "¿Está seguro de que desea salir del sistema Latch?"
        );

        if (confirmar) {
            SesionUsuario.cerrarSesion();
            try {
                FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 850, 620);

                var css = HelloApplication.class.getResource("estilos.css");
                if (css != null) {
                    scene.getStylesheets().add(css.toExternalForm());
                }

                stage.setTitle("Latch — Iniciar Sesión");
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();
            } catch (IOException e) {
                AlertaUtil.mostrarError("Error", "No se pudo volver a la pantalla de inicio de sesión.");
            }
        }
    }
}
