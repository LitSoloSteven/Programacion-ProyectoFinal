package ni.edu.uam.proyectofinalprogramacionappescritorio;

import lombok.Getter;

import javafx.stage.Window;
import java.util.List;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClientePagos;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.PilotoLoginApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TemaAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasSegundoPlano;

import java.io.IOException;
import java.time.Duration;

public class HelloApplication extends Application {
    @Getter
    private final TareasSegundoPlano tareas = new TareasSegundoPlano();
    private static volatile HelloApplication instancia;
    private ComposicionAplicacion composicion;
    private ServletWebServerApplicationContext servidor;
    private String problemaArranque;
    @Getter
    private final ClientePagos pagos = new ClientePagos();

    public static HelloApplication instancia() {
        HelloApplication actual = instancia;
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

    @Override
    public void init() {
        try {
            servidor = PilotoLoginApplication.iniciarLocal();
            composicion = servidor.getBean(ComposicionAplicacion.class);
            ClienteLatch.iniciar(servidor.getWebServer().getPort());
        } catch (RuntimeException error) {
            if (servidor != null) servidor.close();
            servidor = null;
            problemaArranque = PilotoLoginApplication.puertoOcupado(error)
                    ? "El puerto configurado está ocupado. Cierra la otra instancia o cambia LATCH_API_PORT."
                    : "Revisa LATCH_API_PORT, la conexión y el esquema de la base de datos.";
        }
    }

    @Override
    public void stop() {
        ClienteLatch.cerrar();
        instancia = null;
        tareas.cerrar();
        Thread cierre = new Thread(() -> {
            try {
                if (!tareas.esperarCierre(Duration.ofSeconds(10))) {
                    System.err.println("Hay tareas de Latch que no finalizaron a tiempo.");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } finally {
                if (servidor != null) servidor.close();
            }
        }, "latch-cierre");
        cierre.start();
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
        ClienteLatch.alExpirar(() -> Platform.runLater(() -> {
            if (instancia != this || ClienteLatch.api().sesionActiva()) return;
            SesionUsuario.cerrarSesion();
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

    private static boolean perteneceA(Window ventana, Stage principal) {
        while (ventana instanceof Stage dialogo && dialogo.getOwner() != null) {
            ventana = dialogo.getOwner();
            if (ventana == principal) return true;
        }
        return false;
    }

    private void mostrarLogin(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 440, 520);
        TemaAplicacion.aplicar(scene);
        stage.setTitle("Latch — Iniciar sesión");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMinWidth(440);
        stage.setMinHeight(520);
        stage.setWidth(440);
        stage.setHeight(520);
        stage.centerOnScreen();
        stage.show();
    }
}
