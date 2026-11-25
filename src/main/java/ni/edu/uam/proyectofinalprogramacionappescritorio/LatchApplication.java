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
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.PilotoLoginApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.ActualizacionTasaH3;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.CoordinadorCierreH6;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.MonitorInactividad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasSegundoPlano;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LatchApplication extends Application {
    @Getter
    private final TareasSegundoPlano tareas = new TareasSegundoPlano();
    private static volatile LatchApplication instancia;
    private ComposicionAplicacion composicion;
    private ServletWebServerApplicationContext servidor;
    private String problemaArranque;
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

    public ComposicionAplicacion getComposicion() {
        if (composicion == null) {
            throw new IllegalStateException("Los servicios de Latch todavía no están preparados.");
        }
        return composicion;
    }

    public void registrarMonitorInactividad(MonitorInactividad monitor) {
        this.monitorInactividad = monitor;
    }

    @Override
    public void init() {
        try {
            servidor = PilotoLoginApplication.iniciarLocal();
            composicion = servidor.getBean(ComposicionAplicacion.class);
            ClienteLatch.iniciar(servidor.getWebServer().getPort());
            ActualizacionTasaH3 h3 = servidor.getBeanProvider(ActualizacionTasaH3.class).getIfAvailable();
            coordinadorH6 = CoordinadorCierreH6.paraAplicacion(tareas, h3, this::cerrarRecursosServidor);
        } catch (RuntimeException error) {
            if (servidor != null) servidor.close();
            servidor = null;
            problemaArranque = PilotoLoginApplication.puertoOcupado(error)
                    ? "El puerto configurado está ocupado. Cierra la otra instancia o cambia LATCH_API_PORT."
                    : "Revisa LATCH_API_PORT, la conexión y el esquema de la base de datos.";
        }
    }

    private void cerrarRecursosServidor() {
        ClienteLatch.cerrar();
        instancia = null;
        if (servidor != null) {
            servidor.close();
            servidor = null;
        }
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
        cerrarRecursosServidor();
        tareas.cerrar();
    }

    @Override
    public void start(Stage stage) throws IOException {
        TemaAplicacion.inicializar();
        if (problemaArranque != null) {
            AlertaUtil.mostrarError("No se pudo iniciar Latch", problemaArranque);
            Platform.exit();
            return;
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
            SesionUsuario.cerrarSesion();
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
                : CoordinadorCierreH6.paraAplicacion(tareas, null, this::cerrarRecursosServidor);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.initOwner(stage);
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.setTitle("Cerrando Latch (H6)");

        ProgressIndicator indicador = new ProgressIndicator();
        indicador.setPrefSize(28, 28);
        Label lblEstado = new Label("Deteniendo tareas en curso (H1, H2, H3, H5) y liberando recursos…");
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
                    lblEstado.setText(resultado.codigoError() + " · " + resultado.mensaje()
                            + "\n\nTareas activas:\n• "
                            + String.join("\n• ", resultado.tareasPendientes()));
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
