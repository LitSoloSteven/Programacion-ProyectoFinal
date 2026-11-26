package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecargadorVistaTest {
    private record Fila(String id, String nombre) { }

    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException yaIniciado) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS), "JavaFX debe iniciar");
        enFx(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    @Test
    void descartaRespuestaTardiaYNoSobrescribeFiltroMasReciente() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch primeraIniciada = new CountDownLatch(1);
        CountDownLatch liberarPrimera = new CountDownLatch(1);
        CountDownLatch segundaCompletada = new CountDownLatch(1);
        CountDownLatch primeraTerminada = new CountDownLatch(1);
        List<String> aplicados = new ArrayList<>();

        RecargadorVista recargador = new RecargadorVista(
                tareas, () -> true, () -> true, () -> 1L);
        try {
            enFx(() -> {
                recargador.recargar(() -> {
                    primeraIniciada.countDown();
                    try {
                        liberarPrimera.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        primeraTerminada.countDown();
                    }
                    return "filtro-1";
                }, aplicados::add, err -> { });
                return null;
            });

            assertTrue(primeraIniciada.await(5, TimeUnit.SECONDS), "la primera consulta debe iniciar");

            enFx(() -> {
                recargador.recargar(
                        () -> "filtro-2",
                        valor -> {
                            aplicados.add(valor);
                            segundaCompletada.countDown();
                        },
                        err -> { });
                return null;
            });

            assertTrue(segundaCompletada.await(5, TimeUnit.SECONDS), "la segunda consulta debe completar");
            liberarPrimera.countDown();
            assertTrue(primeraTerminada.await(5, TimeUnit.SECONDS), "la primera consulta debe terminar");
            enFx(() -> null);

            assertEquals(List.of("filtro-2"), aplicados);
            assertFalse(enFx(recargador::isCargando));
        } finally {
            liberarPrimera.countDown();
            enFx(recargador::desactivar);
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void agrupaSolicitudesRepetidasEnUnaSolaConsulta() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        AtomicInteger invocaciones = new AtomicInteger();
        AtomicReference<String> filtroAplicado = new AtomicReference<>();
        CountDownLatch completada = new CountDownLatch(1);

        RecargadorVista recargador = new RecargadorVista(
                tareas, () -> true, () -> true, () -> 1L);
        try {
            enFx(() -> {
                for (int i = 1; i <= 5; i++) {
                    String filtro = "texto-" + i;
                    recargador.recargarAgrupado(
                            Duration.millis(80),
                            () -> {
                                invocaciones.incrementAndGet();
                                return filtro;
                            },
                            valor -> {
                                filtroAplicado.set(valor);
                                completada.countDown();
                            },
                            err -> { });
                }
                return null;
            });

            assertTrue(completada.await(5, TimeUnit.SECONDS), "la consulta agrupada debe ejecutarse");
            Thread.sleep(150);
            assertEquals(1, invocaciones.get());
            assertEquals("texto-5", filtroAplicado.get());
        } finally {
            enFx(recargador::desactivar);
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void descartaRespuestaCuandoLaSesionSeCierraOCambia() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch workerFinalizado = new CountDownLatch(1);
        AtomicBoolean sesionActiva = new AtomicBoolean(true);
        AtomicLong generacion = new AtomicLong(1L);
        AtomicInteger exitoInvocado = new AtomicInteger();
        AtomicInteger falloInvocado = new AtomicInteger();

        RecargadorVista recargador = new RecargadorVista(
                tareas, () -> true, sesionActiva::get, generacion::get);
        try {
            enFx(() -> {
                recargador.recargar(() -> {
                    iniciada.countDown();
                    try {
                        liberar.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        workerFinalizado.countDown();
                    }
                    return "datos-viejos";
                }, res -> exitoInvocado.incrementAndGet(), err -> falloInvocado.incrementAndGet());
                return null;
            });

            assertTrue(iniciada.await(5, TimeUnit.SECONDS));
            sesionActiva.set(false);
            generacion.incrementAndGet();
            liberar.countDown();
            assertTrue(workerFinalizado.await(5, TimeUnit.SECONDS));
            enFx(() -> null);

            assertEquals(0, exitoInvocado.get());
            assertEquals(0, falloInvocado.get());
        } finally {
            liberar.countDown();
            enFx(recargador::desactivar);
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void descartaRespuestaAlSalirDelPanelYDetieneConsultaPeriodica() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch workerFinalizado = new CountDownLatch(1);
        AtomicBoolean vistaActiva = new AtomicBoolean(true);
        AtomicInteger exitoInvocado = new AtomicInteger();
        AtomicInteger pulsos = new AtomicInteger();

        RecargadorVista recargador = new RecargadorVista(
                tareas, vistaActiva::get, () -> true, () -> 1L);
        try {
            enFx(() -> {
                recargador.activarConsultaPeriodica(Duration.millis(40), pulsos::incrementAndGet);
                recargador.recargar(() -> {
                    iniciada.countDown();
                    try {
                        liberar.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        workerFinalizado.countDown();
                    }
                    return "datos";
                }, res -> exitoInvocado.incrementAndGet(), err -> { });
                return null;
            });

            assertTrue(iniciada.await(5, TimeUnit.SECONDS));
            enFx(() -> {
                vistaActiva.set(false);
                recargador.desactivar();
                return null;
            });
            int pulsosAlDesactivar = pulsos.get();
            liberar.countDown();
            assertTrue(workerFinalizado.await(5, TimeUnit.SECONDS));
            Thread.sleep(150);
            enFx(() -> null);

            assertEquals(0, exitoInvocado.get());
            assertEquals(pulsosAlDesactivar, pulsos.get());
        } finally {
            liberar.countDown();
            enFx(recargador::desactivar);
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void consultaPeriodicaNoDuplicaSiHayRecargaEnCurso() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch iniciada = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch completada = new CountDownLatch(1);
        AtomicInteger consultas = new AtomicInteger();

        RecargadorVista recargador = new RecargadorVista(
                tareas, () -> true, () -> true, () -> 1L);
        try {
            enFx(() -> {
                recargador.recargar(() -> {
                    consultas.incrementAndGet();
                    iniciada.countDown();
                    try {
                        liberar.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "ok";
                }, res -> completada.countDown(), err -> { });
                return null;
            });

            assertTrue(iniciada.await(5, TimeUnit.SECONDS));
            enFx(() -> {
                recargador.recargarSiInactivo(() -> {
                    consultas.incrementAndGet();
                    return "duplicado";
                }, res -> { }, err -> { });
                return null;
            });

            liberar.countDown();
            assertTrue(completada.await(5, TimeUnit.SECONDS));
            assertEquals(1, consultas.get());
        } finally {
            liberar.countDown();
            enFx(recargador::desactivar);
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void conservaSeleccionPorIdTrasRecargaYLaLimpiaSiDesaparece() throws Exception {
        enFx(() -> {
            TableView<Fila> tabla = new TableView<>();
            Fila f1 = new Fila("id-1", "Uno");
            Fila f2 = new Fila("id-2", "Dos");
            tabla.getItems().setAll(f1, f2);
            tabla.getSelectionModel().select(f2);

            Fila f1Nueva = new Fila("id-1", "Uno v2");
            Fila f2Nueva = new Fila("id-2", "Dos v2");
            RecargadorVista.conservarSeleccionPorId(tabla, Fila::id,
                    () -> tabla.getItems().setAll(f1Nueva, f2Nueva));

            assertNotNull(tabla.getSelectionModel().getSelectedItem());
            assertEquals("Dos v2", tabla.getSelectionModel().getSelectedItem().nombre());

            RecargadorVista.conservarSeleccionPorId(tabla, Fila::id,
                    () -> tabla.getItems().setAll(f1Nueva));

            assertNull(tabla.getSelectionModel().getSelectedItem());
            return null;
        });
    }

    @Test
    void hiloJavaFxSigueRespondiendoMientrasLaApiEsLentaOFalla() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch lentaIniciada = new CountDownLatch(1);
        CountDownLatch liberarLenta = new CountDownLatch(1);
        CountDownLatch errorMostrado = new CountDownLatch(1);

        RecargadorVista recargador = new RecargadorVista(
                tareas, () -> true, () -> true, () -> 1L);
        try {
            AtomicReference<BarraErrorContextual> barraRef = new AtomicReference<>();
            AtomicReference<Label> lblRef = new AtomicReference<>();
            AtomicReference<VBox> contenedorRef = new AtomicReference<>();

            enFx(() -> {
                Label lbl = new Label();
                VBox padre = new VBox(lbl);
                BarraErrorContextual barra = BarraErrorContextual.envolver(lbl);
                lblRef.set(lbl);
                contenedorRef.set(padre);
                barraRef.set(barra);

                recargador.recargar(() -> {
                    lentaIniciada.countDown();
                    try {
                        liberarLenta.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new IllegalStateException("API lenta o caída");
                }, res -> { }, err -> {
                    assertTrue(Platform.isFxApplicationThread());
                    barra.mostrar("Fallo temporal", () -> { });
                    errorMostrado.countDown();
                });
                return null;
            });

            assertTrue(lentaIniciada.await(5, TimeUnit.SECONDS));

            long inicioNanos = System.nanoTime();
            Boolean fxResponde = enFx(() -> true);
            long transcurridoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
            assertTrue(fxResponde);
            assertTrue(transcurridoMs < 200, "El hilo JavaFX debe responder en <200 ms, tardó " + transcurridoMs + " ms");

            liberarLenta.countDown();
            assertTrue(errorMostrado.await(5, TimeUnit.SECONDS));

            enFx(() -> {
                assertTrue(lblRef.get().isVisible());
                assertEquals("Fallo temporal", lblRef.get().getText());
                HBox barraHbox = (HBox) contenedorRef.get().getChildren().getFirst();
                Button reintentar = (Button) barraHbox.getChildren().get(1);
                assertTrue(reintentar.isVisible());
                assertEquals("Reintentar", reintentar.getText());
                barraRef.get().ocultar();
                assertFalse(lblRef.get().isVisible());
                return null;
            });
        } finally {
            liberarLenta.countDown();
            enFx(recargador::desactivar);
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    @Test
    void permiteCargaInicialAntesDeMontarEnEscenaYDesactivaAlDesmontar() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch inicialCompletada = new CountDownLatch(1);
        CountDownLatch lentaIniciada = new CountDownLatch(1);
        CountDownLatch liberarLenta = new CountDownLatch(1);
        CountDownLatch lentaTerminada = new CountDownLatch(1);
        AtomicReference<String> datoInicial = new AtomicReference<>();
        AtomicInteger exitoTardio = new AtomicInteger();
        AtomicInteger falloTardio = new AtomicInteger();

        SesionCliente.iniciar(99L, "test-fx", "PROPIETARIO");
        AtomicReference<RecargadorVista> recargadorRef = new AtomicReference<>();
        try {
            AtomicReference<Scene> escenaRef = new AtomicReference<>();
            enFx(() -> {
                VBox raiz = new VBox();
                RecargadorVista recargador = RecargadorVista.paraNodo(raiz, tareas);
                recargadorRef.set(recargador);

                recargador.recargar(
                        () -> "carga-inicial",
                        valor -> {
                            datoInicial.set(valor);
                            inicialCompletada.countDown();
                        },
                        err -> { });

                escenaRef.set(new Scene(raiz, 400, 300));
                return null;
            });

            assertTrue(inicialCompletada.await(5, TimeUnit.SECONDS),
                    "La carga inicial disparada en initialize() antes de montar Scene debe completarse");
            assertEquals("carga-inicial", datoInicial.get());

            enFx(() -> {
                recargadorRef.get().recargar(() -> {
                    lentaIniciada.countDown();
                    try {
                        liberarLenta.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        lentaTerminada.countDown();
                    }
                    return "tardia";
                }, valor -> exitoTardio.incrementAndGet(), err -> falloTardio.incrementAndGet());
                return null;
            });

            assertTrue(lentaIniciada.await(5, TimeUnit.SECONDS));

            enFx(() -> {
                escenaRef.get().setRoot(new VBox());
                return null;
            });

            liberarLenta.countDown();
            assertTrue(lentaTerminada.await(5, TimeUnit.SECONDS));
            enFx(() -> null);

            assertEquals(0, exitoTardio.get());
            assertEquals(0, falloTardio.get());
        } finally {
            liberarLenta.countDown();
            if (recargadorRef.get() != null) {
                enFx(recargadorRef.get()::desactivar);
            }
            SesionCliente.cerrarSesion();
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    private record RespuestaRecurso(String valor) { }

    @Test
    void httpLentoReordenamientoYExpiracion401DescartanRespuestasYNotificanUnaVez() throws Exception {
        TareasSegundoPlano tareas = new TareasSegundoPlano();
        CountDownLatch lentaIniciada = new CountDownLatch(1);
        CountDownLatch liberarLenta = new CountDownLatch(1);
        CountDownLatch lentaTerminada = new CountDownLatch(1);
        CountDownLatch rapidaCompletada = new CountDownLatch(1);

        CountDownLatch lenta2Iniciada = new CountDownLatch(1);
        CountDownLatch liberarLenta2 = new CountDownLatch(1);
        CountDownLatch lenta2Terminada = new CountDownLatch(1);

        ExecutorService poolServidor = Executors.newCachedThreadPool();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(poolServidor);
        servidor.createContext("/api/v1/auth/login", ex -> responderJson(ex, 200,
                "{\"token\":\"tok-f5\",\"expiraEn\":\"2026-11-26T00:00:00Z\",\"rol\":\"PROPIETARIO\","
                        + "\"usuarioId\":1,\"nombre\":\"prop-f5\"}"));
        servidor.createContext("/api/v1/recurso", ex -> {
            String query = ex.getRequestURI().getQuery();
            if ("consulta=lenta".equals(query)) {
                lentaIniciada.countDown();
                try {
                    liberarLenta.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lentaTerminada.countDown();
                }
                responderJson(ex, 200, "{\"valor\":\"lenta\"}");
            } else if ("consulta=lenta2".equals(query)) {
                lenta2Iniciada.countDown();
                try {
                    liberarLenta2.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lenta2Terminada.countDown();
                }
                responderJson(ex, 200, "{\"valor\":\"lenta2\"}");
            } else if ("consulta=rapida".equals(query)) {
                responderJson(ex, 200, "{\"valor\":\"rapida\"}");
            } else if ("consulta=expirada".equals(query)) {
                responderJson(ex, 401,
                        "{\"error\":{\"codigo\":\"NO_AUTORIZADO\",\"mensaje\":\"Sesión expirada\"}}");
            } else {
                responderJson(ex, 404, "{\"error\":{\"codigo\":\"NO_ENCONTRADO\",\"mensaje\":\"No existe\"}}");
            }
        });
        servidor.start();

        AtomicReference<RecargadorVista> recargadorRef = new AtomicReference<>();
        try {
            ClienteLatch.iniciar("http://127.0.0.1:" + servidor.getAddress().getPort());
            ClienteLatch.auth().login("prop-f5", "ClaveSintetica-2026");

            List<String> aplicados = new ArrayList<>();
            AtomicInteger fallosTramoA = new AtomicInteger();
            enFx(() -> {
                VBox raiz = new VBox();
                new Scene(raiz, 400, 300);
                RecargadorVista recargador = RecargadorVista.paraNodo(raiz, tareas);
                recargadorRef.set(recargador);
                recargador.recargar(
                        () -> ClienteLatch.api().get("/api/v1/recurso?consulta=lenta", RespuestaRecurso.class).valor(),
                        aplicados::add,
                        err -> fallosTramoA.incrementAndGet());
                return null;
            });

            assertTrue(lentaIniciada.await(5, TimeUnit.SECONDS), "La petición HTTP lenta debe iniciar");
            long inicioNanos = System.nanoTime();
            assertTrue(enFx(() -> true));
            long transcurridoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
            assertTrue(transcurridoMs < 200, "El hilo JavaFX debe responder en <200 ms, tardó " + transcurridoMs + " ms");

            enFx(() -> {
                recargadorRef.get().recargar(
                        () -> ClienteLatch.api().get("/api/v1/recurso?consulta=rapida", RespuestaRecurso.class).valor(),
                        valor -> {
                            aplicados.add(valor);
                            rapidaCompletada.countDown();
                        },
                        err -> fallosTramoA.incrementAndGet());
                return null;
            });

            assertTrue(rapidaCompletada.await(5, TimeUnit.SECONDS), "La petición HTTP rápida debe completar");
            liberarLenta.countDown();
            assertTrue(lentaTerminada.await(5, TimeUnit.SECONDS), "El handler HTTP lento debe terminar");
            enFx(() -> null);

            assertEquals(List.of("rapida"), aplicados);
            assertEquals(0, fallosTramoA.get());

            // Tramo B: expiración 401 real sobre HTTP descarta petición en vuelo y notifica una vez
            AtomicInteger expiraciones = new AtomicInteger();
            ClienteLatch.alExpirar(expiraciones::incrementAndGet);
            AtomicInteger exitoTramoB = new AtomicInteger();
            AtomicInteger falloTramoB = new AtomicInteger();

            enFx(() -> {
                recargadorRef.get().recargar(
                        () -> ClienteLatch.api().get("/api/v1/recurso?consulta=lenta2", RespuestaRecurso.class).valor(),
                        valor -> exitoTramoB.incrementAndGet(),
                        err -> falloTramoB.incrementAndGet());
                return null;
            });

            assertTrue(lenta2Iniciada.await(5, TimeUnit.SECONDS), "La segunda petición lenta debe iniciar");

            ErrorClienteException error401 = assertThrows(ErrorClienteException.class,
                    () -> ClienteLatch.api().get("/api/v1/recurso?consulta=expirada", RespuestaRecurso.class));
            assertEquals(401, error401.getEstado());
            assertFalse(SesionCliente.haySesionActiva());
            assertEquals(1, expiraciones.get());

            liberarLenta2.countDown();
            assertTrue(lenta2Terminada.await(5, TimeUnit.SECONDS));
            enFx(() -> null);

            assertEquals(0, exitoTramoB.get());
            assertEquals(0, falloTramoB.get());
        } finally {
            liberarLenta.countDown();
            liberarLenta2.countDown();
            if (recargadorRef.get() != null) {
                enFx(recargadorRef.get()::desactivar);
            }
            ClienteLatch.cerrar();
            servidor.stop(0);
            poolServidor.shutdownNow();
            tareas.cerrar();
            tareas.esperarCierre(java.time.Duration.ofSeconds(1));
        }
    }

    private static void responderJson(HttpExchange ex, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(estado, bytes.length);
        try (var salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static void enFx(Runnable accion) throws Exception {
        enFx(() -> {
            accion.run();
            return null;
        });
    }

    private static <T> T enFx(Callable<T> tarea) throws Exception {
        FutureTask<T> futuro = new FutureTask<>(tarea);
        Platform.runLater(futuro);
        return futuro.get(10, TimeUnit.SECONDS);
    }
}
