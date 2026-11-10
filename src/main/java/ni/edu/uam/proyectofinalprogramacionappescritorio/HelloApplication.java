package ni.edu.uam.proyectofinalprogramacionappescritorio;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasSegundoPlano;

public class HelloApplication extends Application {
    private final TareasSegundoPlano tareas = new TareasSegundoPlano();

    public TareasSegundoPlano getTareas() { return tareas; }

    @Override
    public void stop() {
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
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
