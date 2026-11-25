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
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TablaReporteXlsx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.OperacionCanceladaException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Helper central para generar, previsualizar y exportar reportes PDF/XLSX de forma atómica bajo H5. */
public final class VistaPreviaReporte {

    private static final String FXML_VISTA_PREVIA =
            "/ni/edu/uam/proyectofinalprogramacionappescritorio/vista-previa-reporte.fxml";

    private VistaPreviaReporte() { }

    public record ResultadoPublicacion(Path rutaFinal, boolean publicadoAntesDeCancelar) { }

    public static void generarYMostrar(Button botonDisparador,
                                       Window owner,
                                       Supplier<DocumentoReporte> generador) {
        Objects.requireNonNull(generador, "Falta el generador del reporte.");
        final GestorH5Reportes.EjecucionH5 ejecucion;
        try {
            ejecucion = GestorH5Reportes.iniciar("Generación de reporte");
        } catch (IllegalStateException ocupada) {
            AlertaUtil.mostrarAdvertencia("Reporte en curso", ocupada.getMessage());
            return;
        }

        String textoOriginal = botonDisparador != null ? botonDisparador.getText() : null;
        if (botonDisparador != null) {
            botonDisparador.setDisable(true);
            botonDisparador.setText("Generando…");
        }

        Task<DocumentoReporte> tarea = new Task<>() {
            @Override
            protected DocumentoReporte call() {
                try {
                    if (ejecucion.estaCancelada()) {
                        throw new OperacionCanceladaException();
                    }
                    DocumentoReporte doc = generador.get();
                    if (ejecucion.estaCancelada()) {
                        throw new OperacionCanceladaException();
                    }
                    return doc;
                } finally {
                    ejecucion.close();
                }
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tarea,
                documento -> {
                    ejecucion.close();
                    restaurarBoton(botonDisparador, textoOriginal);
                    if (documento == null) {
                        AlertaUtil.mostrarError("Error de reporte",
                                "No se obtuvo ningún documento para previsualizar.");
                        return;
                    }
                    mostrar(owner, documento);
                },
                error -> {
                    ejecucion.close();
                    restaurarBoton(botonDisparador, textoOriginal);
                    Throwable causa = extraerCausa(error);
                    if (causa instanceof OperacionCanceladaException) {
                        AlertaUtil.mostrarExito("Reporte cancelado", OperacionCanceladaException.MSG_089);
                    } else {
                        AlertaUtil.mostrarError("Error al generar reporte", sanitizarError(error));
                    }
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
                stage.setMinWidth(860);
                stage.setMinHeight(620);
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
        exportarPdfAtomico(jasperPrint, destino, null);
    }

    public static ResultadoPublicacion exportarPdfAtomico(JasperPrint jasperPrint,
                                                          Path destino,
                                                          BooleanSupplier cancelado) throws IOException {
        Objects.requireNonNull(jasperPrint, "Falta el documento JasperPrint.");
        Objects.requireNonNull(destino, "Falta la ruta de destino.");
        return exportarAtomicoConCancelacion(destino, cancelado, out -> {
            if (cancelado != null && cancelado.getAsBoolean()) {
                throw new OperacionCanceladaException();
            }
            JasperExportManager.exportReportToPdfStream(jasperPrint, out);
        });
    }

    public static ResultadoPublicacion exportarXlsxAtomico(DocumentoReporte documento,
                                                           Path destino,
                                                           BooleanSupplier cancelado) throws IOException {
        Objects.requireNonNull(documento, "Falta el documento del reporte.");
        if (!documento.admiteXlsx()) {
            throw new IllegalArgumentException("Este reporte solo admite exportación a PDF.");
        }
        return exportarXlsxAtomico(documento.datosXlsx(), destino, cancelado);
    }

    public static ResultadoPublicacion exportarXlsxAtomico(TablaReporteXlsx tabla,
                                                           Path destino,
                                                           BooleanSupplier cancelado) throws IOException {
        Objects.requireNonNull(tabla, "Faltan los datos tabulares del reporte XLSX.");
        Objects.requireNonNull(destino, "Falta la ruta de destino.");
        return exportarAtomicoConCancelacion(destino, cancelado,
                out -> ExportadorXlsx.escribir(tabla, out, cancelado));
    }

    static void exportarAtomico(Path destino, EscritorPdf escritor) throws IOException {
        exportarAtomicoConCancelacion(destino, null, escritor);
    }

    static ResultadoPublicacion exportarAtomicoConCancelacion(Path destino,
                                                              BooleanSupplier cancelado,
                                                              EscritorPdf escritor) throws IOException {
        Objects.requireNonNull(destino, "Falta la ruta de destino.");
        Objects.requireNonNull(escritor, "Falta el escritor del reporte.");

        if (cancelado != null && cancelado.getAsBoolean()) {
            throw new OperacionCanceladaException();
        }

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
            if (cancelado != null && cancelado.getAsBoolean()) {
                throw new OperacionCanceladaException();
            }
            publicarConVerificacion(temporal, destinoAbsoluto);
            movido = true;
            boolean cancelacionTardia = cancelado != null && cancelado.getAsBoolean();
            return new ResultadoPublicacion(destinoAbsoluto, cancelacionTardia);
        } catch (OperacionCanceladaException cancelacion) {
            throw cancelacion;
        } catch (IOException error) {
            throw error;
        } catch (Exception error) {
            throw new IOException("Falló la exportación del reporte.", error);
        } finally {
            if (!movido) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignored) {
                    // Mejor esfuerzo al limpiar temporal incompleto.
                }
            }
        }
    }

    private static void publicarConVerificacion(Path temporal, Path destinoAbsoluto) throws IOException {
        try {
            Files.move(temporal, destinoAbsoluto,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException fallback) {
            long tamanoEsperado = Files.size(temporal);
            byte[] hashEsperado = sha256(temporal);
            Path preparacion = Files.createTempFile(destinoAbsoluto.getParent(), ".latch-pub-", ".tmp");
            try {
                Files.copy(temporal, preparacion, StandardCopyOption.REPLACE_EXISTING);
                if (Files.size(preparacion) != tamanoEsperado
                        || !Arrays.equals(hashEsperado, sha256(preparacion))) {
                    throw new IOException("La verificación de integridad de la copia falló antes de publicar.");
                }
                Files.move(preparacion, destinoAbsoluto, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(temporal);
            } finally {
                Files.deleteIfExists(preparacion);
            }
        }
    }

    private static byte[] sha256(Path archivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(archivo);
                 DigestInputStream dis = new DigestInputStream(in, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // Lectura secuencial para huella
                }
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este runtime.", e);
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

    private static Throwable extraerCausa(Throwable error) {
        Throwable actual = error;
        while (actual instanceof ExecutionException && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
    }

    private static String sanitizarError(Throwable error) {
        Throwable causa = extraerCausa(error);
        if (causa instanceof IllegalArgumentException
                || causa instanceof SecurityException
                || causa instanceof OperacionCanceladaException) {
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
