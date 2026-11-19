package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ni.edu.uam.proyectofinalprogramacionappescritorio.ComposicionAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.ServicioLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.RutasCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.RutasIndicadores;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.RutasPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ControlIntentosLogin;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.DatabaseConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.ServicioValidaciones;
import org.hibernate.SessionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;

import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

import java.net.BindException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.function.Supplier;

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import({LoginController.class, CatalogosController.class, TasasController.class,
        IndicadoresController.class, CobranzaController.class, RespuestasApi.class})
public class PilotoLoginApplication {
    public static ServletWebServerApplicationContext iniciarLocal() {
        ApiConfig config = ApiConfig.desdeArchivo(Path.of(".env"), System.getenv());
        return (ServletWebServerApplicationContext) iniciar(config, () -> HibernateUtil.construir(
                DatabaseConfig.desdeArchivo(Path.of(".env"), System.getenv())));
    }

    public static boolean puertoOcupado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof PortInUseException || causa instanceof BindException) return true;
        }
        return false;
    }

    static ConfigurableApplicationContext iniciar(ApiConfig config, Supplier<SessionFactory> factory) {
        SpringApplication app = new SpringApplication(PilotoLoginApplication.class);
        app.setRegisterShutdownHook(false);
        app.setDefaultProperties(Map.of("spring.web.resources.add-mappings", "false",
                "spring.mvc.problemdetails.enabled", "false",
                "spring.main.banner-mode", "off",
                "logging.level.org.apache.coyote.http11.Http11Processor", "WARN"));
        app.addInitializers((GenericApplicationContext contexto) -> {
            contexto.registerBean(ApiConfig.class, () -> config);
            contexto.registerBean(SessionFactory.class, factory,
                    definicion -> definicion.setDestroyMethodName("close"));
        });
        // El personalizador de Spring Boot debe conservar el límite de ProtocoloApi.
        return app.run("--server.http2.enabled=false", "--server.forward-headers-strategy=none",
                "--server.tomcat.max-keep-alive-requests=1");
    }

    @Bean
    TomcatServletWebServerFactory servidor(ApiConfig config) throws UnknownHostException {
        InetAddress loopback = InetAddress.getByName(ApiConfig.HOST_LOCAL);
        TomcatServletWebServerFactory servidor = new TomcatServletWebServerFactory(config.puerto());
        servidor.setProtocol(ProtocoloApi.class.getName());
        servidor.addConnectorCustomizers(conector -> {
            var protocolo = (ProtocoloApi) conector.getProtocolHandler();
            conector.setPort(config.puerto());
            protocolo.setAddress(loopback);
            protocolo.configurar(conector);
        });
        return servidor;
    }

    @Bean
    ComposicionAplicacion composicion(SessionFactory factory) {
        return new ComposicionAplicacion(factory);
    }

    @Bean
    TokenService tokens() {
        return new TokenService();
    }

    @Bean
    ServicioLogin servicioLogin(ComposicionAplicacion composicion, TokenService tokens) {
        return new ServicioLogin(composicion.autenticacion(), tokens, new ControlIntentosLogin());
    }

    @Bean
    ServicioValidaciones servicioValidaciones(ComposicionAplicacion composicion) {
        return new ServicioValidaciones(composicion.transacciones());
    }

    @Bean
    RutasCatalogos rutasCatalogos(ComposicionAplicacion composicion) {
        return new RutasCatalogos(composicion.catalogosApi());
    }

    @Bean
    RutasTasas rutasTasas(ComposicionAplicacion composicion) {
        return new RutasTasas(composicion.tasas());
    }

    @Bean
    RutasIndicadores rutasIndicadores(ComposicionAplicacion composicion) {
        return new RutasIndicadores(composicion.dashboard());
    }

    @Bean
    RutasPagos rutasPagos(ComposicionAplicacion composicion) {
        return new RutasPagos(composicion.pagosApi());
    }

    @Bean
    FiltroApi filtroApi(
            @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> rutasMvc,
            ComposicionAplicacion composicion, TokenService tokens) {
        return new FiltroApi(rutasMvc, composicion.autenticacion(), tokens);
    }
}
