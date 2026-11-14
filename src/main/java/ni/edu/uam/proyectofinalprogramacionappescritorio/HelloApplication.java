package ni.edu.uam.proyectofinalprogramacionappescritorio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.RutasCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.ServicioCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.RutasIndicadores;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.RutasPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.RutasValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasSegundoPlano;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;
import java.time.Duration;

public class HelloApplication extends Application {
    private final TareasSegundoPlano tareas = new TareasSegundoPlano();
    private static volatile HelloApplication instancia;
    private ServidorApi servidor;

    public TareasSegundoPlano getTareas() { return tareas; }

    public static HelloApplication instancia() {
        HelloApplication actual = instancia;
        if (actual == null) {
            throw new IllegalStateException("La aplicación Latch no está iniciada.");
        }
        return actual;
    }

    @Override
    public void stop() {
        ClienteLatch.cerrar();
        if (servidor != null) {
            servidor.detener();
            servidor = null;
        }
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
            HibernateTransactions transacciones = new HibernateTransactions(
                    HibernateUtil.getSessionFactory());
            Router router = new Router();
            new RutasTasas(new TipoCambioService(transacciones)).registrar(router);
            new RutasCatalogos(new ServicioCatalogos(transacciones)).registrar(router);
            new RutasValidaciones(transacciones).registrar(router);
            new RutasPagos(new ServicioPagos(transacciones)).registrar(router);
            new RutasIndicadores(new DashboardService(transacciones)).registrar(router);

            servidor = new ServidorApi(config,
                    new AutenticacionService(transacciones), new TokenService(), router);
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
    }
}