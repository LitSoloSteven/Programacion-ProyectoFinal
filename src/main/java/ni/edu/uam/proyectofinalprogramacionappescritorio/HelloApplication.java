package ni.edu.uam.proyectofinalprogramacionappescritorio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasSegundoPlano;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;
import java.time.Duration;

public class HelloApplication extends Application {
    private final TareasSegundoPlano tareas = new TareasSegundoPlano();
    private static volatile HelloApplication instancia;
    private ComposicionAplicacion composicion;
    private ServidorApi servidor;

    public TareasSegundoPlano getTareas() { return tareas; }

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
    public void stop() {
        ClienteLatch.cerrar();
        if (servidor != null) {
            servidor.detener();
            servidor = null;
        }
        composicion = null;
        instancia = null;
        tareas.cerrar();
        Thread cierre = new Thread(() -> {
            try {
                if (!tareas.esperarCierre(Duration.ofSeconds(10))) {
                    System.err.println("Hay tareas que no finalizaron a tiempo. Revisa su cancelación cooperativa.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                HibernateUtil.shutdown();
            }
        }, "latch-cierre");
        cierre.start();
    }

    @Override
    public void start(Stage stage) throws IOException {
        instancia = this;
        String problema = iniciarServicioLocal();
        if (problema != null) {
            AlertaUtil.mostrarError("No se pudo iniciar Latch", problema);
            Platform.exit();
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 850, 620);
        var css = HelloApplication.class.getResource("estilos.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("Latch — Iniciar sesión");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Levanta la API antes de mostrar el login: la interfaz ya no accede
     * directamente a los servicios de negocio para autenticarse.
     */
    private String iniciarServicioLocal() {
        try {
            ApiConfig config = ApiConfig.desdeArchivo(Path.of(".env"), System.getenv());
            composicion = new ComposicionAplicacion(HibernateUtil.getSessionFactory());
            servidor = composicion.crearServidorApi(config);
            servidor.iniciar();
            ClienteLatch.iniciar(servidor.puerto());
            return null;
        } catch (BindException e) {
            return "El puerto configurado ya está en uso. Cierre la otra instancia "
                    + "o cambie LATCH_API_PORT en el archivo .env.";
        } catch (IOException e) {
            return "No se pudo abrir el puerto local de la API. Revisa LATCH_API_PORT.";
        } catch (RuntimeException e) {
            detenerServicioLocal();
            return e.getMessage() == null
                    ? "No se pudo iniciar el servicio local de Latch."
                    : e.getMessage();
        }
    }

    private void detenerServicioLocal() {
        ClienteLatch.cerrar();
        if (servidor != null) {
            servidor.detener();
            servidor = null;
        }
        composicion = null;
    }
}
