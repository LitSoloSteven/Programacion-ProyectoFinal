package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

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
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

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
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.US);

    public DashboardController() {
        this.dashboardService = new DashboardService();
    }

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @FXML
    public void initialize() {
        Usuario usuario = SesionUsuario.getUsuarioActual();
        if (usuario != null) {
            lblUsuarioNombre.setText(usuario.getNombreVisible() != null ? usuario.getNombreVisible() : usuario.getNombreUsuario());
            lblUsuarioRol.setText(usuario.getRolNombre() != null ? usuario.getRolNombre() : "Usuario");
        } else {
            lblUsuarioNombre.setText("Usuario");
            lblUsuarioRol.setText("Invitado");
        }

        actualizarMetricas();
    }

    @FXML
    public void actualizarMetricas() {
        try {
            Map<String, Object> metricas = dashboardService.obtenerMetricas();
            lblTotalPropiedades.setText(String.valueOf(metricas.get("totalPropiedades")));
            lblTotalUnidades.setText(String.valueOf(metricas.get("totalUnidades")));
            lblTotalInquilinos.setText(String.valueOf(metricas.get("totalInquilinos")));
            lblTotalContratos.setText(String.valueOf(metricas.get("totalContratosVigentes")));

            int cantCargos = (int) metricas.get("cargosPendientesCant");
            BigDecimal montoCargos = (BigDecimal) metricas.get("cargosPendientesMonto");
            lblCargosPendientes.setText(String.valueOf(cantCargos));
            lblCargosMonto.setText("Total: " + formatoMoneda.format(montoCargos));

            int cantPagos = (int) metricas.get("pagosRegistradosCant");
            BigDecimal montoPagos = (BigDecimal) metricas.get("pagosRegistradosMonto");
            lblPagosRegistrados.setText(String.valueOf(cantPagos));
            lblPagosMonto.setText("Total: " + formatoMoneda.format(montoPagos));
        } catch (SQLException e) {
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
