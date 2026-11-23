package ni.edu.uam.proyectofinalprogramacionappescritorio.herramientas;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto.PilotoLoginApplication;

import java.util.concurrent.CountDownLatch;

public final class IniciarApi {
    private IniciarApi() { }

    public static void main(String[] args) throws InterruptedException {
        try (var servidor = PilotoLoginApplication.iniciarLocal()) {
            var cierre = new CountDownLatch(1);
            var hook = new Thread(() -> {
                try {
                    servidor.close();
                } finally {
                    cierre.countDown();
                }
            }, "latch-api-cierre");
            Runtime.getRuntime().addShutdownHook(hook);
            System.out.println("API Latch (Spring Boot) en http://127.0.0.1:" + servidor.getWebServer().getPort());
            try {
                cierre.await();
            } finally {
                if (cierre.getCount() != 0) Runtime.getRuntime().removeShutdownHook(hook);
            }
        }
    }
}
