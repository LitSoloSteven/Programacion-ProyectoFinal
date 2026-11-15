package ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas;

import ni.edu.uam.proyectofinalprogramacionappescritorio.ComposicionAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Arranque independiente de la API para pruebas locales con curl.
 * La aplicación JavaFX la integrará en la Fase 2.
 */
public final class IniciarApi {
    private IniciarApi() { }

    public static void main(String[] args) throws Exception {
        ApiConfig config = ApiConfig.desdeArchivo(Path.of(".env"), System.getenv());
        ComposicionAplicacion composicion =
                new ComposicionAplicacion(HibernateUtil.getSessionFactory());
        ServidorApi servidor = composicion.crearServidorApi(config);

        CountDownLatch cierre = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.detener();
            HibernateUtil.shutdown();
            cierre.countDown();
        }, "latch-api-cierre"));

        servidor.iniciar();
        System.out.println("API Latch escuchando en " + servidor.urlBase());
        cierre.await();
    }
}
