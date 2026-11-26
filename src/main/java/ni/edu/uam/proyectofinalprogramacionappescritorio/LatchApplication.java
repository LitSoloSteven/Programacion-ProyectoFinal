package ni.edu.uam.proyectofinalprogramacionappescritorio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.CoordinadorCierreH6;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MensajesUi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MonitorInactividad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasSegundoPlano;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LatchApplication extends Application {
    @Getter
    private final TareasSegundoPlano tareas = new TareasSegundoPlano();
    private static volatile LatchApplication instancia;
    private String urlBaseConfigurada;
    private String problemaConexion;
    @Getter
    private final ClientePagos pagos = new ClientePagos();
    private volatile MonitorInactividad monitorInactividad;
    private volatile CoordinadorCierreH6 coordinadorH6;
    private volatile boolean cierreH6Completado;
    private final AtomicBoolean cierreEnCurso = new AtomicBoolean(false);

    public static LatchApplication instancia() {
        LatchApplication actual = instancia;
        if (actual == null) {
            throw new IllegalStateException("La aplicación Latch no está iniciada.");
        }
        return actual;
    }

    public void registrarMonitorInactividad(MonitorInactividad monitor) {
        this.monitorInactividad = monitor;
    }

    @Override
    public void init() {
        try {
            urlBaseConfigurada = ClienteConfig.resolverUrlBase();
            ClienteLatch.iniciar(urlBaseConfigurada);
            coordinadorH6 = CoordinadorCierreH6.paraAplicacion(tareas, this::cerrarRecursosCliente);
            ClienteLatch.api().getSalud();
            problemaConexion = null;
        } catch (RuntimeException error) {
            problemaConexion = error.getMessage() == null || error.getMessage().isBlank()
                    ? "No se pudo conectar con el servidor Latch en " + urlBaseConfigurada + "."
                    : error.getMessage();
        }
    }

    private void cerrarRecursosCliente() {
        ClienteLatch.cerrar();
        instancia = null;
    }

    @Override
    public void stop() {
        if (monitorInactividad != null) {
            monitorInactividad.detener();
            monitorInactividad = null;
        }
        if (cierreH6Completado) {
            return;
        }
        if (coordinadorH6 != null) {
            var resultado = coordinadorH6.ejecutarCierre(CoordinadorCierreH6.TIEMPO_LIMITE_DEFECTO);
            if (resultado.completado()) {
                cierreH6Completado = true;
                return;
            }
            System.err.println(resultado.codigoError() + ": " + resultado.mensaje()
                    + " Pendientes: " + resultado.tareasPendientes());
        }
        cerrarRecursosCliente();
        tareas.cerrar();
    }

    @Override
    public void start(Stage stage) throws IOException {
        TemaAplicacion.inicializar();
        while (problemaConexion != null) {
            if (!mostrarDialogoSinServidor(stage)) {
                cerrarRecursosCliente();
                Platform.exit();
                return;
            }
            try {
                ClienteLatch.iniciar(urlBaseConfigurada);
                ClienteLatch.api().getSalud();
                problemaConexion = null;
            } catch (RuntimeException error) {
                problemaConexion = error.getMessage() == null || error.getMessage().isBlank()
                        ? "No se pudo conectar con el servidor Latch en " + urlBaseConfigurada + "."
                        : error.getMessage();
            }
        }

        instancia = this;
        stage.setOnCloseRequest(evento -> {
            evento.consume();
            solicitarCierreControlado(stage);
        });
        ClienteLatch.alExpirar(() -> Platform.runLater(() -> {
            if (instancia != this || ClienteLatch.api().sesionActiva()) return;
            if (monitorInactividad != null) {
                monitorInactividad.detener();
                monitorInactividad = null;
            }
            SesionCliente.cerrarSesion();
            TemaAplicacion.limpiarUsuarioActivo();
            for (Window ventana : List.copyOf(Window.getWindows()).reversed()) {
                if (perteneceA(ventana, stage)) ventana.hide();
            }
            try {
                mostrarLogin(stage);
            } catch (IOException error) {
                AlertaUtil.mostrarError("Sesión expirada", "Vuelve a abrir Latch para iniciar sesión.");
                Platform.exit();
                return;
            }
            AlertaUtil.mostrarError("Sesión expirada", "Vuelve a iniciar sesión para continuar.");
        }));
        try {
            mostrarLogin(stage);
        } catch (IOException | RuntimeException error) {
            stop();
            throw error;
        }
    }

    private boolean mostrarDialogoSinServidor(Stage stage) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Servidor Latch no disponible");
        dialogo.initModality(Modality.APPLICATION_MODAL);
        if (stage != null && stage.isShowing()) {
            dialogo.initOwner(stage);
        }
        Label mensaje = new Label(problemaConexion
                + "\n\nDirección configurada: " + (urlBaseConfigurada == null ? ClienteConfig.URL_DEFECTO : urlBaseConfigurada)
                + "\nAsegúrate de que el servidor Tomcat 11 de Latch esté en ejecución o ajusta LATCH_API_BASE_URL.");
        mensaje.setWrapText(true);
        VBox contenido = new VBox(10, mensaje);
        contenido.setPadding(new Insets(16));
        contenido.setPrefWidth(480);

        DialogPane panel = dialogo.getDialogPane();
        panel.setContent(contenido);
        ButtonType btnReintentar = new ButtonType("Reintentar conexión", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnSalir = new ButtonType("Salir", ButtonBar.ButtonData.CANCEL_CLOSE);
        panel.getButtonTypes().setAll(btnReintentar, btnSalir);
        TemaAplicacion.aplicar(panel);
        return dialogo.showAndWait().orElse(btnSalir) == btnReintentar;
    }

    private void solicitarCierreControlado(Stage stage) {
        if (cierreH6Completado) {
            Platform.exit();
            return;
        }
        if (!cierreEnCurso.compareAndSet(false, true)) {
            return;
        }
        if (monitorInactividad != null) monitorInactividad.pausarEvaluacion();
        CoordinadorCierreH6 coordinador = coordinadorH6 != null
                ? coordinadorH6
                : CoordinadorCierreH6.paraAplicacion(tareas, this::cerrarRecursosCliente);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(stage);
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.setTitle("Cerrando Latch");

        ProgressIndicator indicador = new ProgressIndicator();
        indicador.setPrefSize(28, 28);
        Label lblEstado = new Label("Finalizando tareas en curso y cerrando sesión…");
        lblEstado.setWrapText(true);
        HBox cabecera = new HBox(12, indicador, lblEstado);
        VBox contenido = new VBox(10, cabecera);
        contenido.setPadding(new Insets(16));
        contenido.setPrefWidth(480);

        DialogPane panel = dialogo.getDialogPane();
        panel.setContent(contenido);
        ButtonType btnReintentar = new ButtonType("Reintentar cierre", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnSeguir = new ButtonType("Continuar en la aplicación", ButtonBar.ButtonData.CANCEL_CLOSE);
        panel.getButtonTypes().setAll(btnSeguir);
        panel.lookupButton(btnSeguir).setDisable(true);
        TemaAplicacion.aplicar(panel);

        dialogo.setOnCloseRequest(evento -> {
            if (cierreEnCurso.get()) {
                evento.consume();
            }
        });

        Thread hiloCierre = new Thread(() -> {
            var resultado = coordinador.ejecutarCierre(CoordinadorCierreH6.TIEMPO_LIMITE_DEFECTO);
            Platform.runLater(() -> {
                cierreEnCurso.set(false);
                if (resultado.completado()) {
                    cierreH6Completado = true;
                    if (monitorInactividad != null) {
                        monitorInactividad.detener();
                        monitorInactividad = null;
                    }
                    dialogo.setOnCloseRequest(null);
                    dialogo.close();
                    Platform.exit();
                } else {
                    indicador.setVisible(false);
                    indicador.setManaged(false);
                    List<String> tareasLimpias = resultado.tareasPendientes().stream()
                            .map(MensajesUi::limpiarParaUsuario)
                            .filter(s -> !s.isBlank())
                            .toList();
                    lblEstado.setText(MensajesUi.limpiarParaUsuario(resultado.mensaje())
                            + (tareasLimpias.isEmpty() ? "" : "\n\nTareas activas:\n• " + String.join("\n• ", tareasLimpias)));
                    panel.getButtonTypes().setAll(btnReintentar, btnSeguir);
                    panel.lookupButton(btnSeguir).setDisable(false);
                }
            });
        }, "latch-h6");
        hiloCierre.setDaemon(false);
        hiloCierre.start();

        dialogo.showAndWait().ifPresent(opcion -> {
            if (opcion == btnReintentar) {
                solicitarCierreControlado(stage);
            }
        });
        if (!cierreH6Completado && monitorInactividad != null) monitorInactividad.reanudarEvaluacion();
    }

    private static boolean perteneceA(Window ventana, Stage principal) {
        while (ventana instanceof Stage dialogo && dialogo.getOwner() != null) {
            ventana = dialogo.getOwner();
            if (ventana == principal) return true;
        }
        return false;
    }

    private void mostrarLogin(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(LatchApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 460, 580);
        TemaAplicacion.aplicar(scene);
        stage.setTitle("Latch — Iniciar sesión");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMinWidth(460);
        stage.setMinHeight(580);
        stage.setWidth(460);
        stage.setHeight(580);
        stage.centerOnScreen();
        stage.show();
    }
}
