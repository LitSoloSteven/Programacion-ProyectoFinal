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
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoAdjuntoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.DocumentoAdjunto;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AdjuntoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Diálogos reutilizables para gestión y vista previa de archivos adjuntos (RN-25).
 * Todas las lecturas y escrituras en disco o base se ejecutan fuera del hilo de JavaFX.
 */
public final class DialogosAdjuntos {
    private DialogosAdjuntos() { }

    public static void mostrarFotoUnidad(Window owner, String tituloUnidad,
                                         AdjuntoService servicio, java.util.UUID unidadId) {
        SesionUsuario identidad = SesionUsuario.getActual();
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

        final DocumentoAdjuntoDTO[] actual = {null};

        Runnable recargar = () -> {
            btnSubir.setDisable(true);
            btnQuitar.setDisable(true);
            ocultarError(lblError);
            Task<FotoCargada> tarea = new Task<>() {
                @Override protected FotoCargada call() {
                    Optional<DocumentoAdjuntoDTO> meta = servicio.obtenerFotoUnidad(identidad, unidadId);
                    if (meta.isEmpty()) return new FotoCargada(null, null);
                    byte[] bytes = servicio.leerContenido(identidad, meta.get().id());
                    return new FotoCargada(meta.get(), bytes);
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, res -> {
                btnSubir.setDisable(false);
                actual[0] = res.meta();
                if (res.meta() == null) {
                    visor.setImage(null);
                    lblMeta.setText("Sin fotografía registrada para esta unidad.");
                    btnQuitar.setDisable(true);
                } else {
                    visor.setImage(new Image(new ByteArrayInputStream(res.bytes())));
                    lblMeta.setText(res.meta().nombreOriginal() + " · " + formatearBytes(res.meta().tamanoBytes())
                            + " · SHA-256: " + res.meta().sha256().substring(0, 12) + "…");
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
            Task<DocumentoAdjuntoDTO> tarea = new Task<>() {
                @Override protected DocumentoAdjuntoDTO call() {
                    return servicio.adjuntarFotoUnidad(identidad, unidadId, archivo.toPath());
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ok -> recargar.run(), err -> {
                btnSubir.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo adjuntar la fotografía."));
            });
        });

        btnQuitar.setOnAction(ev -> {
            if (actual[0] == null) return;
            if (!AlertaUtil.mostrarConfirmacion("Quitar fotografía",
                    "Se desactivará la fotografía actual de la unidad.", "Quitar")) return;
            btnQuitar.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    servicio.desactivar(identidad, actual[0].id());
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ok -> recargar.run(), err -> {
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
                                             AdjuntoService servicio,
                                             Supplier<List<DocumentoAdjuntoDTO>> cargador,
                                             Function<Path, DocumentoAdjuntoDTO> guardador) {
        SesionUsuario identidad = SesionUsuario.getActual();
        ObservableList<DocumentoAdjuntoDTO> filas = FXCollections.observableArrayList();
        TableView<DocumentoAdjuntoDTO> tabla = new TableView<>(filas);
        tabla.setPrefHeight(220);
        tabla.setPlaceholder(new Label("No hay documentos adjuntos registrados."));

        TableColumn<DocumentoAdjuntoDTO, String> colNombre = new TableColumn<>("Archivo");
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombreOriginal()));
        colNombre.setPrefWidth(210);
        TableColumn<DocumentoAdjuntoDTO, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().mime()));
        colTipo.setPrefWidth(120);
        TableColumn<DocumentoAdjuntoDTO, String> colTamano = new TableColumn<>("Tamaño");
        colTamano.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearBytes(c.getValue().tamanoBytes())));
        colTamano.setPrefWidth(90);
        TableColumn<DocumentoAdjuntoDTO, String> colSha = new TableColumn<>("Huella SHA-256");
        colSha.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().sha256().substring(0, 16) + "…"));
        colSha.setPrefWidth(150);
        tabla.getColumns().addAll(List.of(colNombre, colTipo, colTamano, colSha));

        Button btnAdjuntar = new Button(soloPdf ? "Adjuntar PDF" : "Adjuntar archivo");
        btnAdjuntar.getStyleClass().add("button-primary");
        Button btnGuardarCopia = new Button("Guardar copia");
        btnGuardarCopia.getStyleClass().add("button-secondary");
        btnGuardarCopia.setDisable(true);
        Button btnVistaPrevia = new Button("Vista previa");
        btnVistaPrevia.getStyleClass().add("button-secondary");
        btnVistaPrevia.setDisable(true);
        Button btnDesactivar = new Button("Quitar adjunto");
        btnDesactivar.getStyleClass().add("button-destructive");
        btnDesactivar.setDisable(true);

        Label lblError = crearLabelError();
        Label lblFinalidad = new Label(subtituloFinalidad);
        lblFinalidad.getStyleClass().add("secondary-text");
        lblFinalidad.setWrapText(true);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, ant, sel) -> {
            boolean hay = sel != null;
            btnGuardarCopia.setDisable(!hay);
            btnDesactivar.setDisable(!hay);
            btnVistaPrevia.setDisable(!hay || !DocumentoAdjunto.MIMES_IMAGEN.contains(sel.mime()));
        });

        Runnable recargar = () -> {
            ocultarError(lblError);
            Task<List<DocumentoAdjuntoDTO>> tarea = new Task<>() {
                @Override protected List<DocumentoAdjuntoDTO> call() {
                    return cargador.get();
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea,
                    filas::setAll,
                    err -> mostrarError(lblError, mensajeSeguro(err, "No se pudieron consultar los adjuntos.")));
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
            Task<DocumentoAdjuntoDTO> tarea = new Task<>() {
                @Override protected DocumentoAdjuntoDTO call() {
                    return guardador.apply(archivo.toPath());
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ok -> {
                btnAdjuntar.setDisable(false);
                recargar.run();
            }, err -> {
                btnAdjuntar.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo adjuntar el archivo."));
            });
        });

        btnGuardarCopia.setOnAction(ev -> {
            DocumentoAdjuntoDTO sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar copia verificada");
            fc.setInitialFileName(sel.nombreOriginal());
            File destino = fc.showSaveDialog(owner);
            if (destino == null) return;
            btnGuardarCopia.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() throws IOException {
                    byte[] bytes = servicio.leerContenido(identidad, sel.id());
                    Files.write(destino.toPath(), bytes);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ok -> {
                btnGuardarCopia.setDisable(false);
                AlertaUtil.mostrarExito("Copia guardada", "El archivo verificado se exportó correctamente.");
            }, err -> {
                btnGuardarCopia.setDisable(false);
                mostrarError(lblError, mensajeSeguro(err, "No se pudo exportar el archivo."));
            });
        });

        btnVistaPrevia.setOnAction(ev -> {
            DocumentoAdjuntoDTO sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Task<byte[]> tarea = new Task<>() {
                @Override protected byte[] call() {
                    return servicio.leerContenido(identidad, sel.id());
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, bytes -> {
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
            DocumentoAdjuntoDTO sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!AlertaUtil.mostrarConfirmacion("Quitar adjunto",
                    "¿Deseas desactivar el adjunto '" + sel.nombreOriginal() + "'?", "Quitar")) return;
            btnDesactivar.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    servicio.desactivar(identidad, sel.id());
                    return null;
                }
            };
            TareasJavaFx.ejecutar(HelloApplication.instancia().getTareas(), tarea, ok -> recargar.run(), err -> {
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
        if (err instanceof IllegalArgumentException || err instanceof SecurityException
                || err instanceof IllegalStateException) {
            return err.getMessage();
        }
        return respaldo;
    }

    private record FotoCargada(DocumentoAdjuntoDTO meta, byte[] bytes) { }
}
