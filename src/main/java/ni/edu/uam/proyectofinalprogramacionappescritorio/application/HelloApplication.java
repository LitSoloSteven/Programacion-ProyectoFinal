package ni.edu.uam.proyectofinalprogramacionappescritorio.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;

// 1. Importación corregida de HibernateUtil
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
                // Ahora esta línea compilará correctamente
                HibernateUtil.shutdown();
            }
        }, "latch-cierre");
        cierre.start();
    }

    @Override
    public void start(Stage stage) throws IOException {
        // 2. Preparar el cargador para la vista real (Caleb deberá crear Login.fxml en la carpeta resources adecuada)
        // Por ahora, dejamos hello-view.fxml si Caleb aún no crea el Login, pero ajustamos la ventana.
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/ni/edu/uam/proyectofinalprogramacionappescritorio/hello-view.fxml"));

        // Ajustamos la resolución a un tamaño de escritorio realista
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);

        stage.setTitle("Latch - Administración de Alquileres");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}