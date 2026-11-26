package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.adjuntos.dto.DocumentoAdjuntoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ClienteAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.ErrorClienteException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.cliente.SesionCliente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.contrato.ConstantesLatch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/** Diálogos reutilizables para gestión de fotografías y documentos adjuntos vía HTTP. */
public final class DialogosAdjuntos {

    private DialogosAdjuntos() { }

    public static void mostrarFotoUnidad(Window owner, String tituloUnidad,
                                         ClienteAdjuntos servicio, UUID unidadId) {
        Label lblMeta = new Label("Consultando fotografía...");
        lblMeta.setWrapText(true);
        Label lblAviso = new Label("La fotografía es opcional (PNG o JPEG, máximo 5 MiB).");
        lblAviso.getStyleClass().add("secondary-text");
        lblAviso.setWrapText(true);

        ImageView visor = new ImageView();
        visor.setFitWidth(420);
        visor.setFitHeight(260);
        visor.setPreserveRatio(true);

        Button btnSubir = new Button("Adjuntar foto (PNG/JPEG)");
        btnSubir.getStyleClass().add("button-primary");
        Button btnQuitar = new Button("Quitar foto");
        btnQuitar.getStyleClass().add("button-destructive");
        btnQuitar.setDisable(true);
        Label lblError = crearLabelError();

        final DocumentoAdjuntoResponse[] actual = {null};

        Runnable recargar = () -> {
            btnSubir.setDisable(true);
            btnQuitar.setDisable(true);
            ocultarError(lblError);
            Task<FotoCargada> tarea = new Task<>() {
                @Override protected FotoCargada call() {
                    Optional<DocumentoAdjuntoResponse> meta = servicio.obtenerFotoUnidad(unidadId);
                    if (meta.isEmpty()) return new FotoCargada(null, null);
                    byte[] bytes = servicio.leerContenido(UUID.fromString(meta.get().id()));
                    return new FotoCargada(meta.get(), bytes);
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
                btnSubir.setDisable(false);
                actual[0] = res.meta();
                if (res.meta() == null) {
                    visor.setImage(null);
                    lblMeta.setText("Sin fotografía registrada para esta unidad.");
                    btnQuitar.setDisable(true);
                } else {
                    visor.setImage(new Image(new ByteArrayInputStream(res.bytes())));
                    lblMeta.setText(res.meta().nombreOriginal() + " · " + formatearBytes(res.meta().tamanoBytes())
                            + " · SHA-256: " + res.meta().sha256().substring(0, Math.min(12, res.meta().sha256().length())) + "…");
                    btnQuitar.setDisable(false);
                }
            }, err -> {
                btnSubir.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo cargar la fotografía."));
            });
        };

        btnSubir.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar fotografía de unidad");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG o JPEG", "*.png", "*.jpg", "*.jpeg"));
            File archivo = fc.showOpenDialog(owner);
            if (archivo == null) return;
            btnSubir.setDisable(true);
            ocultarError(lblError);
            Task<DocumentoAdjuntoResponse> tarea = new Task<>() {
                @Override protected DocumentoAdjuntoResponse call() throws IOException {
                    return servicio.adjuntarFotoUnidad(unidadId, archivo.toPath());
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> recargar.run(), err -> {
                btnSubir.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo adjuntar la fotografía."));
            });
        });

        btnQuitar.setOnAction(ev -> {
            if (actual[0] == null) return;
            if (!AlertaUtil.mostrarConfirmacion("Quitar fotografía",
                    "Se desactivará la fotografía actual de la unidad.", "Quitar")) return;
            btnQuitar.setDisable(true);
            UUID adjuntoId = UUID.fromString(actual[0].id());
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    servicio.desactivar(adjuntoId);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> recargar.run(), err -> {
                btnQuitar.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo desactivar la fotografía."));
            });
        });

        HBox botones = new HBox(10, btnSubir, btnQuitar);
        botones.setAlignment(Pos.CENTER_LEFT);
        VBox contenido = new VBox(12,
                new Label("Unidad: " + tituloUnidad),
                lblAviso,
                visor,
                lblMeta,
                botones,
                lblError);
        contenido.setPadding(new Insets(12));

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Fotografía de unidad", contenido, owner, 540, 520, "Cerrar");
        dialogo.getDialogPane().getButtonTypes().remove(1);
        recargar.run();
        dialogo.showAndWait();
    }

    public static void mostrarGestorAdjuntos(Window owner,
                                             String tituloVentana,
                                             String subtituloFinalidad,
                                             boolean soloPdf,
                                             ClienteAdjuntos servicio,
                                             Supplier<List<DocumentoAdjuntoResponse>> cargador,
                                             ArchivoGuardador guardador) {
        boolean esPropietario = SesionCliente.getActual().esPropietario();
        ObservableList<DocumentoAdjuntoResponse> filas = FXCollections.observableArrayList();
        TableView<DocumentoAdjuntoResponse> tabla = new TableView<>(filas);
        tabla.setPrefHeight(220);
        tabla.setPlaceholder(new Label("No hay documentos adjuntos registrados."));

        TableColumn<DocumentoAdjuntoResponse, String> colNombre = new TableColumn<>("Archivo");
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombreOriginal()));
        colNombre.setPrefWidth(210);
        TableColumn<DocumentoAdjuntoResponse, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().mime()));
        colTipo.setPrefWidth(120);
        TableColumn<DocumentoAdjuntoResponse, String> colTamano = new TableColumn<>("Tamaño");
        colTamano.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearBytes(c.getValue().tamanoBytes())));
        colTamano.setPrefWidth(90);
        TableColumn<DocumentoAdjuntoResponse, String> colSha = new TableColumn<>("Huella SHA-256");
        colSha.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().sha256().substring(0, Math.min(16, c.getValue().sha256().length())) + "…"));
        colSha.setPrefWidth(140);
        tabla.getColumns().setAll(List.of(colNombre, colTipo, colTamano, colSha));

        Label lblFinalidad = new Label(subtituloFinalidad + " (límite 5 MiB; copia verificada con SHA-256).");
        lblFinalidad.getStyleClass().add("secondary-text");
        lblFinalidad.setWrapText(true);

        Button btnAdjuntar = new Button("Adjuntar archivo…");
        btnAdjuntar.getStyleClass().add("button-primary");
        Button btnGuardarCopia = new Button("Guardar copia…");
        btnGuardarCopia.getStyleClass().add("button-secondary");
        Button btnVistaPrevia = new Button("Ver imagen");
        btnVistaPrevia.getStyleClass().add("button-secondary");
        Button btnDesactivar = new Button("Quitar adjunto");
        btnDesactivar.getStyleClass().add("button-destructive");
        btnDesactivar.setVisible(esPropietario);
        btnDesactivar.setManaged(esPropietario);
        btnGuardarCopia.setDisable(true);
        btnVistaPrevia.setDisable(true);
        btnDesactivar.setDisable(true);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, ant, sel) -> {
            btnGuardarCopia.setDisable(sel == null);
            btnVistaPrevia.setDisable(sel == null || !ConstantesLatch.MIMES_IMAGEN.contains(sel.mime()));
            btnDesactivar.setDisable(sel == null || !esPropietario);
        });

        Label lblError = crearLabelError();

        Runnable recargar = () -> {
            ocultarError(lblError);
            Task<List<DocumentoAdjuntoResponse>> tarea = new Task<>() {
                @Override protected List<DocumentoAdjuntoResponse> call() {
                    return cargador.get();
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea,
                    filas::setAll,
                    err -> mostrarError(lblError, mensajeSeguro(err, "No se pudo consultar el historial de adjuntos.")));
        };

        btnAdjuntar.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar archivo adjunto");
            if (soloPdf) {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
            } else {
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                        "Documentos admitidos (PDF, PNG, JPEG)", "*.pdf", "*.png", "*.jpg", "*.jpeg"));
            }
            File archivo = fc.showOpenDialog(owner);
            if (archivo == null) return;
            btnAdjuntar.setDisable(true);
            ocultarError(lblError);
            Task<DocumentoAdjuntoResponse> tarea = new Task<>() {
                @Override protected DocumentoAdjuntoResponse call() throws IOException {
                    return guardador.guardar(archivo.toPath());
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                btnAdjuntar.setDisable(false);
                recargar.run();
            }, err -> {
                btnAdjuntar.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo adjuntar el archivo."));
            });
        });

        btnGuardarCopia.setOnAction(ev -> {
            DocumentoAdjuntoResponse sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar copia verificada");
            fc.setInitialFileName(sel.nombreOriginal());
            File destino = fc.showSaveDialog(owner);
            if (destino == null) return;
            btnGuardarCopia.setDisable(true);
            UUID adjuntoId = UUID.fromString(sel.id());
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() throws IOException {
                    byte[] bytes = servicio.leerContenido(adjuntoId);
                    Files.write(destino.toPath(), bytes);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                btnGuardarCopia.setDisable(false);
                AlertaUtil.mostrarExito("Copia guardada", "El archivo verificado se exportó correctamente.");
            }, err -> {
                btnGuardarCopia.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo exportar el archivo."));
            });
        });

        btnVistaPrevia.setOnAction(ev -> {
            DocumentoAdjuntoResponse sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            UUID adjuntoId = UUID.fromString(sel.id());
            Task<byte[]> tarea = new Task<>() {
                @Override protected byte[] call() {
                    return servicio.leerContenido(adjuntoId);
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, bytes -> {
                ImageView img = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                img.setFitWidth(520);
                img.setFitHeight(360);
                img.setPreserveRatio(true);
                Dialog<ButtonType> d = DialogoFormulario.crear(
                        sel.nombreOriginal(), new VBox(10, img), owner, 580, 460, "Cerrar");
                d.getDialogPane().getButtonTypes().remove(1);
                d.showAndWait();
            }, err -> mostrarError(lblError, mensajeSeguro(err, "No se pudo abrir la vista previa.")));
        });

        btnDesactivar.setOnAction(ev -> {
            DocumentoAdjuntoResponse sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!AlertaUtil.mostrarConfirmacion("Quitar adjunto",
                    "¿Deseas desactivar el adjunto '" + sel.nombreOriginal() + "'?", "Quitar")) return;
            btnDesactivar.setDisable(true);
            UUID adjuntoId = UUID.fromString(sel.id());
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    servicio.desactivar(adjuntoId);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> recargar.run(), err -> {
                btnDesactivar.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo quitar el adjunto."));
            });
        });

        HBox barra = new HBox(10, btnAdjuntar, btnGuardarCopia, btnVistaPrevia, btnDesactivar);
        barra.setAlignment(Pos.CENTER_LEFT);
        VBox contenido = new VBox(10, lblFinalidad, barra, tabla, lblError);
        contenido.setPadding(new Insets(12));

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(tituloVentana, contenido, owner, 680, 480, "Cerrar");
        dialogo.getDialogPane().getButtonTypes().remove(1);
        recargar.run();
        dialogo.showAndWait();
    }

    private static String formatearBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KiB", bytes / 1024.0);
        return String.format("%.2f MiB", bytes / (1024.0 * 1024.0));
    }

    private static Label crearLabelError() {
        Label lbl = new Label("");
        lbl.getStyleClass().add("field-error");
        lbl.setVisible(false);
        lbl.setManaged(false);
        lbl.setWrapText(true);
        return lbl;
    }

    private static void mostrarError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private static void ocultarError(Label lbl) {
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private static String mensajeSeguro(Throwable err, String respaldo) {
        return MensajesUi.traducir(err, respaldo);
    }

    @FunctionalInterface
    public interface ArchivoGuardador {
        DocumentoAdjuntoResponse guardar(Path archivo) throws IOException;
    }

    private record FotoCargada(DocumentoAdjuntoResponse meta, byte[] bytes) { }
}
