package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import com.google.gson.JsonParser;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto.LoginResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiIndicadoresSpringTest {
    @Test
    void dashboardEnSpringExigeBearerYSerializaLosTotalesVaciosPorMoneda() throws Exception {
        Properties propiedades = new Properties();
        propiedades.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        propiedades.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:indicadores_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        propiedades.setProperty("hibernate.connection.username", "sa");
        propiedades.setProperty("hibernate.hbm2ddl.auto", "create-drop");

        try (var factory = HibernateUtil.construir(propiedades);
             var servidor = new ServidorSpringPrueba(factory);
             var cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            String clave = "Clave-Sintetica-Indicadores-2026";
            String hash = new PasswordHasher().crear(clave.toCharArray());
            new HibernateTransactions(factory).ejecutar(sesion -> {
                sesion.persist(new Usuario("indicadores-asistente", hash, Rol.ASISTENTE));
                return null;
            });
            String url = servidor.urlBase() + "/api/v1";
            var cuerpo = Json.escribir(new LoginRequest("indicadores-asistente", clave));
            var login = HttpRequest.newBuilder(URI.create(url + "/auth/login"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
            var autenticada = cliente.send(login, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, autenticada.statusCode());
            String token = Json.leer(autenticada.body(), LoginResponse.class).token();

            var metricas = HttpRequest.newBuilder(URI.create(url + "/dashboard/metricas"))
                    .timeout(Duration.ofSeconds(10)).GET();
            assertEquals(401, cliente.send(metricas.build(), HttpResponse.BodyHandlers.ofString())
                    .statusCode());
            var respuesta = cliente.send(metricas.header("Authorization", "Bearer " + token).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, respuesta.statusCode());
            var datos = JsonParser.parseString(respuesta.body()).getAsJsonObject();
            assertEquals(0, datos.get("contratosVigentes").getAsInt());
            for (String campo : new String[]{"cargosPendientes", "pagosRegistrados"}) {
                var totales = datos.getAsJsonArray(campo);
                assertEquals(2, totales.size());
                assertEquals("USD", totales.get(0).getAsJsonObject().get("moneda").getAsString());
                assertEquals("NIO", totales.get(1).getAsJsonObject().get("moneda").getAsString());
                for (var total : totales) {
                    var monto = total.getAsJsonObject().get("total").getAsJsonPrimitive();
                    assertTrue(monto.isString());
                    assertEquals("0.00", monto.getAsString());
                }
            }
        }
    }
}
