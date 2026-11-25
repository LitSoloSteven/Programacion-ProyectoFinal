package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.controller.VistaPreviaReporteController;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Helper central para generar, previsualizar y exportar reportes PDF de forma atómica. */
public final class VistaPreviaReporte {

    private static final String FXML_VISTA_PREVIA =
            "/ni/edu/uam/proyectofinalprogramacionappescritorio/vista-previa-reporte.fxml";

    private VistaPreviaReporte() { }

    public static void generarYMostrar(Button botonDisparador,
                                       Window owner,
                                       Supplier<DocumentoReporte> generador) {
        Objects.requireNonNull(generador, "Falta el generador del reporte.");
        String textoOriginal = botonDisparador != null ? botonDisparador.getText() : null;
        if (botonDisparador != null) {
            botonDisparador.setDisable(true);
            botonDisparador.setText("Generando…");
        }

        Task<DocumentoReporte> tarea = new Task<>() {
            @Override
            protected DocumentoReporte call() {
                return generador.get();
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tarea,
                documento -> {
                    restaurarBoton(botonDisparador, textoOriginal);
                    if (documento == null) {
                        AlertaUtil.mostrarError("Error de reporte",
                                "No se obtuvo ningún documento para previsualizar.");
                        return;
                    }
                    mostrar(owner, documento);
                },
                error -> {
                    restaurarBoton(botonDisparador, textoOriginal);
                    AlertaUtil.mostrarError("Error al generar reporte", sanitizarError(error));
                }
        );
    }

    public static void mostrar(Window owner, DocumentoReporte documento) {
        Objects.requireNonNull(documento, "Falta el documento del reporte.");
        Runnable abrir = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(VistaPreviaReporte.class.getResource(FXML_VISTA_PREVIA));
                Parent raiz = loader.load();
                VistaPreviaReporteController controller = loader.getController();

                Scene scene = new Scene(raiz);
                TemaAplicacion.aplicar(scene);

                Stage stage = new Stage();
                stage.setTitle(documento.titulo());
                stage.setScene(scene);
                stage.setMinWidth(820);
                stage.setMinHeight(600);
                if (owner != null) {
                    stage.initOwner(owner);
                    stage.initModality(Modality.WINDOW_MODAL);
                }
                stage.setOnHidden(evento -> controller.liberarRecursos());
                controller.inicializarDocumento(documento);
                stage.show();
            } catch (IOException | RuntimeException error) {
                AlertaUtil.mostrarError("Error de vista previa",
                        "No se pudo abrir la ventana de vista previa del reporte.");
            }
        };
        if (Platform.isFxApplicationThread()) {
            abrir.run();
        } else {
            Platform.runLater(abrir);
        }
    }

    public static void exportarPdfAtomico(JasperPrint jasperPrint, Path destino) throws IOException {
        Objects.requireNonNull(jasperPrint, "Falta el documento JasperPrint.");
        Objects.requireNonNull(destino, "Falta la ruta de destino.");
        exportarAtomico(destino, out -> JasperExportManager.exportReportToPdfStream(jasperPrint, out));
    }

    static void exportarAtomico(Path destino, EscritorPdf escritor) throws IOException {
        Objects.requireNonNull(destino, "Falta la ruta de destino.");
        Objects.requireNonNull(escritor, "Falta el escritor del PDF.");

        Path destinoAbsoluto = destino.toAbsolutePath().normalize();
        Path directorio = destinoAbsoluto.getParent();
        if (directorio == null) {
            throw new IOException("La ruta de destino no tiene un directorio padre válido.");
        }
        Files.createDirectories(directorio);

        Path temporal = Files.createTempFile(directorio, ".latch-rep-", ".tmp");
        boolean movido = false;
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                escritor.escribir(salida);
            }
            try {
                Files.move(temporal, destinoAbsoluto,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temporal, destinoAbsoluto, StandardCopyOption.REPLACE_EXISTING);
            }
            movido = true;
        } catch (IOException error) {
            throw error;
        } catch (Exception error) {
            throw new IOException("Falló la exportación del PDF.", error);
        } finally {
            if (!movido) {
                Files.deleteIfExists(temporal);
            }
        }
    }

    private static void restaurarBoton(Button boton, String textoOriginal) {
        if (boton != null) {
            if (textoOriginal != null) {
                boton.setText(textoOriginal);
            }
            boton.setDisable(false);
        }
    }

    private static String sanitizarError(Throwable error) {
        Throwable causa = error instanceof ExecutionException && error.getCause() != null
                ? error.getCause()
                : error;
        if (causa instanceof IllegalArgumentException || causa instanceof SecurityException) {
            String mensaje = causa.getMessage();
            if (mensaje != null && !mensaje.isBlank()) {
                return mensaje;
            }
        }
        return "No se pudo generar el reporte. Intenta nuevamente.";
    }

    @FunctionalInterface
    interface EscritorPdf {
        void escribir(OutputStream out) throws Exception;
    }
}
