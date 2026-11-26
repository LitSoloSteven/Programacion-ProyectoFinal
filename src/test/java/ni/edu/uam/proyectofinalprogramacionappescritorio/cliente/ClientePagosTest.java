package ni.edu.uam.proyectofinalprogramacionappescritorio.cliente;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import com.sun.net.httpserver.HttpServer;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientePagosTest {

    @Test
    void registrarEnviaLaClaveUuidComoCabecera() throws Exception {
        AtomicReference<String> claveRecibida = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/pagos", intercambio -> {
            claveRecibida.set(intercambio.getRequestHeaders().getFirst("Idempotency-Key"));
            byte[] respuesta = ("{\"pagoId\":\"pago-1\",\"numeroRecibo\":17,"
                    + "\"montoContrato\":\"1000.00\",\"credito\":\"0.00\"}")
                    .getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(201, respuesta.length);
            try (var cuerpo = intercambio.getResponseBody()) {
                cuerpo.write(respuesta);
            }
        });
        servidor.start();

        try {
            UUID clave = UUID.randomUUID();
            ClientePagos pagos = new ClientePagos(
                    new ClienteApi("http://127.0.0.1:" + servidor.getAddress().getPort()));
            PagoRequest solicitud = new PagoRequest(UUID.randomUUID().toString(),
                    "2026-11-14", "USD", "1000.00", "Transferencia", null,
                    List.of(), null);

            PagoRegistroResponse respuesta = pagos.registrar(solicitud, clave);

            assertEquals(clave.toString(), claveRecibida.get());
            assertEquals("pago-1", respuesta.pagoId());
            assertEquals(17, respuesta.numeroRecibo());
        } finally {
            servidor.stop(0);
        }
    }
    @Test
    void elReintentoConservaLaClaveHastaRecibirElRecibo() throws Exception {
        var claves = new CopyOnWriteArrayList<String>();
        var intentos = new AtomicInteger();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/pagos", intercambio -> {
            try (intercambio) {
                claves.add(intercambio.getRequestHeaders().getFirst("Idempotency-Key"));
                if (intentos.getAndIncrement() == 0) {
                    intercambio.sendResponseHeaders(503, -1);
                    return;
                }
                byte[] recibo = "{\"pagoId\":\"pago-1\",\"numeroRecibo\":17}".getBytes(StandardCharsets.UTF_8);
                intercambio.sendResponseHeaders(201, recibo.length);
                intercambio.getResponseBody().write(recibo);
            }
        });
        servidor.start();

        try {
            var pagos = new ClientePagos(new ClienteApi(
                    "http://127.0.0.1:" + servidor.getAddress().getPort()));
            var solicitud = new PagoRequest(UUID.randomUUID().toString(), "2026-11-14",
                    "USD", "1000.00", "Efectivo", null, List.of(), null);
            assertThrows(ErrorClienteException.class, () -> pagos.registrarReintentable(1, solicitud));
            assertEquals(17, pagos.registrarReintentable(1, solicitud).numeroRecibo());
            pagos.registrarReintentable(1, solicitud);

            assertEquals(claves.get(0), claves.get(1));
            assertNotEquals(claves.get(1), claves.get(2));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void estadoCuentaConsultaEndpointConYSinFechaDeCorte() throws Exception {
        var uris = new CopyOnWriteArrayList<String>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/contratos/", intercambio -> {
            try (intercambio) {
                uris.add(intercambio.getRequestURI().toString());
                String json = """
                        {
                          "contratoId":"c-10",
                          "corte":"2026-11-24",
                          "moneda":"USD",
                          "canon":"450.00",
                          "cargos":[],
                          "pagos":[],
                          "totales":{"cargos":"900.00","pagado":"100.00","saldo":"800.00","credito":"0.00"},
                          "garantia":{"pactada":"450.00","saldoActual":"450.00"}
                        }
                        """;
                byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().set("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, cuerpo.length);
                intercambio.getResponseBody().write(cuerpo);
            }
        });
        servidor.start();

        try {
            ClientePagos pagos = new ClientePagos(
                    new ClienteApi("http://127.0.0.1:" + servidor.getAddress().getPort()));

            EstadoCuentaResponse conCorte = pagos.estadoCuenta("c-10", LocalDate.of(2026, 11, 24));
            EstadoCuentaResponse sinCorte = pagos.estadoCuenta("c-10", null);

            assertEquals("/api/v1/contratos/c-10/estado-cuenta?corte=2026-11-24", uris.get(0));
            assertEquals("/api/v1/contratos/c-10/estado-cuenta", uris.get(1));
            assertEquals("USD", conCorte.moneda());
            assertEquals("800.00", conCorte.totales().saldo());
            assertEquals("450.00", sinCorte.garantia().saldoActual());
        } finally {
            servidor.stop(0);
        }
    }
}
