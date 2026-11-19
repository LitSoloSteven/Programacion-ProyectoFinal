package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import org.hibernate.SessionFactory;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

public final class ServidorSpringPrueba implements AutoCloseable {
    private final ServletWebServerApplicationContext contexto;

    public ServidorSpringPrueba(SessionFactory factory) {
        contexto = (ServletWebServerApplicationContext)
                PilotoLoginApplication.iniciar(ApiConfig.paraPruebas(0), () -> factory);
    }

    public int plazoConexionMillis() {
        var servidor = (TomcatWebServer) contexto.getWebServer();
        return ((ProtocoloApi) servidor.getTomcat().getConnector().getProtocolHandler()).getConnectionTimeout();
    }

    public int puerto() {
        return contexto.getWebServer().getPort();
    }

    public boolean tieneRuta(RequestMethod metodo, String ruta) {
        return mapeos().getHandlerMethods().keySet().stream().anyMatch(info ->
                info.getMethodsCondition().getMethods().contains(metodo)
                        && info.getPatternValues().contains(ruta));
    }

    public long operacionesApi() {
        return mapeos().getHandlerMethods().keySet().stream().filter(info ->
                info.getPatternValues().stream().anyMatch(ruta -> ruta.startsWith("/api/v1/"))).count();
    }

    private RequestMappingHandlerMapping mapeos() {
        return contexto.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
    }

    public String urlBase() {
        return "http://" + ApiConfig.HOST_LOCAL + ":" + contexto.getWebServer().getPort();
    }

    @Override
    public void close() {
        contexto.close();
    }
}
