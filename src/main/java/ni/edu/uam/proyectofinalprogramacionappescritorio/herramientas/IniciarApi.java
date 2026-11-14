package ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.RutasCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.ServicioCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.RutasIndicadores;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.RutasPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.RutasValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;

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
        HibernateTransactions transacciones = new HibernateTransactions(
                HibernateUtil.getSessionFactory());
        Router router = new Router();
        new RutasTasas(new TipoCambioService(transacciones)).registrar(router);
        new RutasCatalogos(new ServicioCatalogos(transacciones)).registrar(router);
        new RutasValidaciones(transacciones).registrar(router);
        new RutasPagos(new ServicioPagos(transacciones)).registrar(router);
        new RutasIndicadores(new DashboardService(transacciones)).registrar(router);
        ServidorApi servidor = new ServidorApi(config,
                new AutenticacionService(transacciones), new TokenService(), router);

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