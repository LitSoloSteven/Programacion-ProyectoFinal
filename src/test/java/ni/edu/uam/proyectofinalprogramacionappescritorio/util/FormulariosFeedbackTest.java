package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
    void todosLosFormulariosAnuncianCamposVacios() throws Exception {
        enFx(() -> {
            for (String nombre : List.of("propiedad", "unidad", "inquilino", "contrato", "cargo", "pago", "tasa")) {
                FXMLLoader loader = cargar(nombre);
                Parent raiz = loader.getRoot();
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
            Parent raiz = loader.getRoot();
            var controlador = (ni.edu.uam.proyectofinalprogramacionappescritorio.controller.InquilinoFormController) loader.getController();
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
    void alertaVisiblePerteneceAlFormularioYConservaLosDatos() throws Exception {
        enFx(() -> {
            FXMLLoader loader = cargar("inquilino");
            Parent raiz = loader.getRoot();
            Stage ventana = new Stage();
            ventana.setScene(new Scene(raiz, 650, 600));
            ventana.show();
            FutureTask<Void> comprobar = new FutureTask<>(() -> {
                Window alerta = Window.getWindows().stream().filter(w -> w != ventana && w.isShowing())
                        .findFirst().orElseThrow();
                try {
                    assertSame(ventana, ((Stage) alerta).getOwner());
                    DialogPane pane = (DialogPane) alerta.getScene().getRoot();
                    assertTrue(pane.getContentText().contains("nombre completo"));
                    ((Button) pane.lookupButton(ButtonType.OK)).fire();
                } finally { alerta.hide(); }
                return null;
            });
            Platform.runLater(comprobar);
            try {
                assertEquals(false, loader.getController().getClass().getMethod("validar").invoke(loader.getController()));
                comprobar.get(5, TimeUnit.SECONDS);
                assertTrue(((Label) loader.getNamespace().get("lblErrorNombre")).isVisible());
            } finally { ventana.hide(); }
            return null;
        });
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
