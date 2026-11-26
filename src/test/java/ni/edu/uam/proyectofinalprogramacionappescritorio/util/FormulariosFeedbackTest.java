package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.CargoFormController;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.ContratoFormController;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.InquilinoFormController;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.PropiedadFormController;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.TasaFormController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FormulariosFeedbackTest {
    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        try { Platform.startup(iniciado::countDown); }
        catch (IllegalStateException existente) { iniciado.countDown(); }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS));
        enFx(() -> { Platform.setImplicitExit(false); return null; });
    }

    @Test
    void ningunFormularioMarcaErroresAntesDeInteractuar() throws Exception {
        enFx(() -> {
            for (String nombre : List.of("propiedad", "unidad", "inquilino", "contrato", "cargo", "pago", "tasa")) {
                FXMLLoader loader = cargar(nombre);
                Parent raiz = loader.getRoot();
                Label resumen = (Label) loader.getNamespace().get("lblResumenError");
                assertFalse(resumen.isVisible(), nombre + " no debe mostrar resumen antes de interactuar");
                for (var entry : loader.getNamespace().entrySet()) {
                    if (entry.getKey().startsWith("lblError") && entry.getValue() instanceof Label label) {
                        assertFalse(label.isVisible(), nombre + " " + entry.getKey() + " debe iniciar oculto");
                    }
                    if (entry.getValue() instanceof Node nodo) {
                        assertFalse(nodo.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO),
                                nombre + " " + entry.getKey() + " no debe tener clase inválida al iniciar");
                    }
                }
                assertNotNull(raiz);
            }
            return null;
        });
    }

    @Test
    void todosLosFormulariosAnuncianCamposVacios() throws Exception {
        enFx(() -> {
            for (String nombre : List.of("propiedad", "unidad", "inquilino", "contrato", "cargo", "pago", "tasa")) {
                FXMLLoader loader = cargar(nombre);
                Object controlador = loader.getController();
                String metodo = nombre.equals("tasa") ? "validarRegistro" : "validar";
                assertEquals(false, controlador.getClass().getMethod(metodo).invoke(controlador), nombre);
                Label resumen = (Label) loader.getNamespace().get("lblResumenError");
                assertTrue(resumen.isVisible(), nombre);
                assertFalse(resumen.getText().isBlank(), nombre);
                assertTrue(resumen.isFocusTraversable(), nombre);
            }
            return null;
        });
    }

    @Test
    void unSoloErrorSeAnunciaYSeLimpiaAlCorregirlo() throws Exception {
        enFx(() -> {
            FXMLLoader loader = cargar("inquilino");
            var controlador = (InquilinoFormController) loader.getController();
            assertFalse(controlador.validar());
            Label resumen = (Label) loader.getNamespace().get("lblResumenError");
            assertEquals("Escribe el nombre completo.", resumen.getText());
            ((TextField) loader.getNamespace().get("txtNombre")).setText("Persona de prueba");
            assertTrue(controlador.validar(), "Los campos opcionales pueden quedar vacíos");
            assertFalse(resumen.isVisible());
            ((TextField) loader.getNamespace().get("txtTelefono")).setText("incorrecto");
            assertFalse(controlador.validar());
            assertTrue(resumen.getText().contains("teléfono válido"));
            ((TextField) loader.getNamespace().get("txtTelefono")).setText("+505 8888-1234");
            assertTrue(controlador.validar());
            return null;
        });
    }

    @Test
    void validacionFallidaEnfocaPrimerCampoInvalidoSinAlertaModalYConservaDatos() throws Exception {
        enFx(() -> {
            FXMLLoader loader = cargar("inquilino");
            Parent raiz = loader.getRoot();
            TextField txtNombre = (TextField) loader.getNamespace().get("txtNombre");
            TextField txtCorreo = (TextField) loader.getNamespace().get("txtCorreo");
            Label lblErrorNombre = (Label) loader.getNamespace().get("lblErrorNombre");
            Label lblResumen = (Label) loader.getNamespace().get("lblResumenError");
            txtCorreo.setText("inquilino@ejemplo.com");

            Stage ventana = new Stage();
            ventana.setScene(new Scene(raiz, 650, 600));
            ventana.show();
            try {
                int ventanasAntes = (int) Window.getWindows().stream().filter(Window::isShowing).count();
                var controlador = (InquilinoFormController) loader.getController();
                assertFalse(controlador.validar());
                int ventanasDespues = (int) Window.getWindows().stream().filter(Window::isShowing).count();
                assertEquals(ventanasAntes, ventanasDespues,
                        "No debe abrirse un Alert modal redundante al fallar la validación");
                assertTrue(lblErrorNombre.isVisible());
                assertNotNull(lblErrorNombre.getGraphic(), "El error debe acompañarse de icono");
                assertTrue(lblResumen.isVisible());
                assertTrue(txtNombre.isFocused(), "Debe enfocar el primer campo con error");
                assertTrue(txtNombre.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO));
                assertEquals("inquilino@ejemplo.com", txtCorreo.getText(),
                        "Los valores válidos ingresados deben conservarse");
            } finally {
                ventana.hide();
            }
            return null;
        });
    }

    @Test
    void validacionEnVivoAlEscribirYPegarYLimpiezaAlCorregir() throws Exception {
        enFx(() -> {
            // 1. PropiedadFormController: correo inválido en vivo y limpieza inmediata al corregir
            FXMLLoader propLoader = cargar("propiedad");
            TextField txtCorreo = (TextField) propLoader.getNamespace().get("txtCorreoPropietario");
            Label lblErrorCorreo = (Label) propLoader.getNamespace().get("lblErrorCorreo");
            txtCorreo.setText("correo-invalido");
            assertTrue(lblErrorCorreo.isVisible());
            assertTrue(txtCorreo.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO));
            txtCorreo.setText("dueno@latch.ni");
            assertFalse(lblErrorCorreo.isVisible());
            assertFalse(txtCorreo.getStyleClass().contains(ValidacionFormulario.CLASE_CAMPO_INVALIDO));

            // 2. ContratoFormController: cruce de fechas en vivo (vigencia < 1 mes)
            FXMLLoader contratoLoader = cargar("contrato");
            var contratoCtrl = (ContratoFormController) contratoLoader.getController();
            contratoCtrl.establecerCatalogos(
                    List.of(new UnidadResponse("u1", "p1", "Propiedad", "A-1", "Desc", "Apartamento", "Disponible", "350.00", "USD", 0L)),
                    List.of(new InquilinoResponse("i1", "Ana Pérez", "001", "88887777", "ana@correo.com", null, true, null, 0L)));
            @SuppressWarnings("unchecked")
            ComboBox<InquilinoResponse> cmbInq = (ComboBox<InquilinoResponse>) contratoLoader.getNamespace().get("cmbInquilino");
            cmbInq.getSelectionModel().selectFirst();
            DatePicker dpInicio = (DatePicker) contratoLoader.getNamespace().get("dpInicio");
            DatePicker dpFin = (DatePicker) contratoLoader.getNamespace().get("dpFin");
            Label lblErrorFechas = (Label) contratoLoader.getNamespace().get("lblErrorFechas");
            LocalDate hoy = LocalDate.now(ConstantesLatch.ZONA_OPERATIVA);
            dpInicio.setValue(hoy);
            dpFin.setValue(hoy.plusDays(10));
            assertTrue(lblErrorFechas.isVisible());
            assertTrue(lblErrorFechas.getText().contains("un mes"));
            dpFin.setValue(hoy.plusMonths(2));
            assertFalse(lblErrorFechas.isVisible());

            // 3. CargoFormController: cruce emisión/vencimiento en vivo
            FXMLLoader cargoLoader = cargar("cargo");
            var cargoCtrl = (CargoFormController) cargoLoader.getController();
            cargoCtrl.establecerContratos(List.of(new ContratoResponse(
                    "c1", "u1", "Propiedad - A-1", "i1", "Ana Pérez",
                    hoy.toString(), hoy.plusMonths(6).toString(), "350.00", "USD", 5, "350.00", "350.00", "Vigente", 0L, null, null, null, null)));
            DatePicker dpEmision = (DatePicker) cargoLoader.getNamespace().get("dpFechaEmision");
            DatePicker dpVence = (DatePicker) cargoLoader.getNamespace().get("dpVenceEl");
            Label lblErrorCargoFechas = (Label) cargoLoader.getNamespace().get("lblErrorFechas");
            dpEmision.setValue(hoy);
            dpVence.setValue(hoy.minusDays(1));
            assertTrue(lblErrorCargoFechas.isVisible());
            dpVence.setValue(hoy.plusDays(5));
            assertFalse(lblErrorCargoFechas.isVisible());

            // 4. TasaFormController: fecha futura y tasa negativa
            FXMLLoader tasaLoader = cargar("tasa");
            DatePicker dpFechaTasa = (DatePicker) tasaLoader.getNamespace().get("dpFecha");
            TextField txtTasa = (TextField) tasaLoader.getNamespace().get("txtTasa");
            Label lblErrorFechaTasa = (Label) tasaLoader.getNamespace().get("lblErrorFecha");
            Label lblErrorTasa = (Label) tasaLoader.getNamespace().get("lblErrorTasa");
            dpFechaTasa.setValue(hoy.plusDays(2));
            assertTrue(lblErrorFechaTasa.isVisible());
            dpFechaTasa.setValue(hoy);
            assertFalse(lblErrorFechaTasa.isVisible());
            txtTasa.setText("-5");
            assertTrue(lblErrorTasa.isVisible());
            txtTasa.setText("36.75000000");
            assertFalse(lblErrorTasa.isVisible());
            assertInstanceOf(PropiedadFormController.class, propLoader.getController());
            assertInstanceOf(TasaFormController.class, tasaLoader.getController());
            return null;
        });
    }

    @Test
    void validadorAsincronoEvitaDuplicadosYDescartaRespuestasObsoletas() throws Exception {
        CountDownLatch primeraIniciada = new CountDownLatch(1);
        CountDownLatch liberarPrimera = new CountDownLatch(1);
        CountDownLatch segundaAplicada = new CountDownLatch(1);
        AtomicInteger llamadas = new AtomicInteger();
        List<String> aplicadas = new ArrayList<>();

        ValidadorAsincrono validador = new ValidadorAsincrono(Runnable::run, Duration.ZERO);
        // En FX, la primera respuesta queda bloqueada en un hilo aparte y llega tarde.
        ValidadorAsincrono asincrono = new ValidadorAsincrono(
                runnable -> new Thread(runnable, "test-validador").start(),
                Duration.ZERO);

        enFx(() -> {
            asincrono.solicitar("a@b.com", () -> true, () -> {
                llamadas.incrementAndGet();
                primeraIniciada.countDown();
                try { liberarPrimera.await(2, TimeUnit.SECONDS); }
                catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                return "respuesta-obsoleta";
            }, aplicadas::add, err -> fail(err));
            return null;
        });
        assertTrue(primeraIniciada.await(2, TimeUnit.SECONDS));

        enFx(() -> {
            // Misma clave -> deduplicada
            asincrono.solicitar("a@b.com", () -> true, () -> {
                llamadas.incrementAndGet();
                return "duplicada";
            }, aplicadas::add, err -> fail(err));
            // Nueva clave -> invalida la anterior
            asincrono.solicitar("nuevo@b.com", () -> true, () -> {
                llamadas.incrementAndGet();
                return "respuesta-vigente";
            }, res -> {
                aplicadas.add(res);
                segundaAplicada.countDown();
            }, err -> fail(err));
            return null;
        });

        assertTrue(segundaAplicada.await(3, TimeUnit.SECONDS));
        liberarPrimera.countDown();
        Thread.sleep(80);
        enFx(() -> null);
        assertEquals(2, llamadas.get(), "La clave duplicada no debe disparar una tercera solicitud");
        assertEquals(List.of("respuesta-vigente"), aplicadas, "La respuesta obsoleta debe descartarse");
        assertNotNull(validador);
    }

    @Test
    void mensajesUiLimpiaCodigosTecnicosYTraduceErroresPreservandoRecibos() {
        assertEquals("Activa el modo mantenimiento antes de restaurar.",
                MensajesUi.limpiarParaUsuario("ERR-058: Activa el modo mantenimiento antes de restaurar."));
        assertEquals("Generación mensual de rentas",
                MensajesUi.limpiarParaUsuario("Generación mensual de rentas (H1)"));
        assertEquals("Recibo N.º 42 por 450.00 USD registrado.",
                MensajesUi.limpiarParaUsuario("Recibo N.º 42 por 450.00 USD registrado."));
        assertEquals("",
                MensajesUi.limpiarParaUsuario("SQLSTATE 23505 en tabla pago"));

        assertTrue(MensajesUi.traducir(new ErrorClienteException(401, "NO_AUTENTICADO", "Token"), "x")
                .contains("sesión expiró"));
        assertTrue(MensajesUi.traducir(new ErrorClienteException(503, "ERR-057", "Reloj"), "x")
                .contains("reloj del sistema"));
        assertTrue(MensajesUi.traducir(new ErrorClienteException(409, "CONFLICTO", "Otro usuario modificó el registro."), "x")
                .contains("Recarga"));
        assertTrue(MensajesUi.traducir(new ErrorClienteException(0, "SIN_CONEXION", "Fallo"), "x")
                .contains("conectar con el servidor"));
    }

    @Test
    void notificacionMantieneAlturaCompactaYSePuedeCerrar() throws Exception {
        enFx(() -> {
            StackPane root = new StackPane();
            Scene scene = new Scene(root, 1400, 900);
            TemaAplicacion.aplicar(scene);
            Notificaciones.registrarAnfitrion(root);
            assertTrue(Notificaciones.mostrarExito("La unidad quedó registrada."));
            root.applyCss();
            root.resize(1400, 900);
            root.layout();
            HBox toast = (HBox) root.getChildren().getFirst();
            assertTrue(toast.getHeight() > 0 && toast.getHeight() < 150, "El aviso no debe ocupar toda la altura");
            assertTrue(toast.getWidth() <= 440);
            assertTrue(toast.getLayoutY() < 30);
            ((Button) toast.getChildren().getLast()).fire();
            assertTrue(root.getChildren().isEmpty());
            return null;
        });
    }

    private static FXMLLoader cargar(String nombre) throws Exception {
        FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource(nombre + "-form.fxml"));
        loader.load();
        return loader;
    }

    private static <T> T enFx(java.util.concurrent.Callable<T> tarea) throws Exception {
        FutureTask<T> futuro = new FutureTask<>(tarea);
        Platform.runLater(futuro);
        return futuro.get(15, TimeUnit.SECONDS);
    }
}
