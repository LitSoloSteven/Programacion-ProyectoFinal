package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.ApiValidacionesTest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import org.hibernate.SessionFactory;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

class ApiValidacionesSpringTest extends ApiValidacionesTest {
    @Override
    protected ServidorPrueba iniciarServidor(SessionFactory factory) {
        var servidor = (ServletWebServerApplicationContext)
                PilotoLoginApplication.iniciar(ApiConfig.paraPruebas(0), () -> factory);
        return new ServidorPrueba("http://" + ApiConfig.HOST_LOCAL + ":"
                + servidor.getWebServer().getPort(), servidor);
    }
}
