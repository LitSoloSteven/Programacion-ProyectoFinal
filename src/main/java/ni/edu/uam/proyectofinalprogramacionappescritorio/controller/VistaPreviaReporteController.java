package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.HelloApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.RenderizadorVistaPreviaReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.VistaPreviaReporte;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Controlador del diálogo reutilizable de vista previa de reportes. */
public class VistaPreviaReporteController {

    private static final int MAX_PAGINAS_CACHE = 8;

    @FXML private Button btnAnterior;
    @FXML private Label lblPaginacion;
    @FXML private Button btnSiguiente;
    @FXML private Button btnZoomMenos;
    @FXML private Label lblZoom;
    @FXML private Button btnZoomMas;
    @FXML private Button btnZoomCien;
    @FXML private Button btnAjustarAncho;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnCerrar;
    @FXML private ScrollPane spHoja;
    @FXML private ImageView imgPagina;
    @FXML private Label lblEstado;

    private final Map<ClavePagina, WritableImage> cachePaginas = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ClavePagina, WritableImage> eldest) {
            return size() > MAX_PAGINAS_CACHE;
        }
    };

    private DocumentoReporte documento;
    private int totalPaginas;
    private int paginaActual;
    private float escalaActual = RenderizadorVistaPreviaReporte.ZOOM_INICIAL;
    private long solicitudRenderId;
    private boolean exportando;

    @FXML
    public void initialize() {
        if (btnExportarPdf != null) {
            Iconos.asignar(btnExportarPdf, Iconos.Nombre.DESCARGAR, 16);
            btnExportarPdf.setTooltip(new Tooltip("Guardar este reporte como archivo PDF"));
        }
        if (btnCerrar != null) {
            Iconos.asignar(btnCerrar, Iconos.Nombre.CERRAR, 16);
            btnCerrar.setTooltip(new Tooltip("Cerrar la vista previa"));
        }
        if (btnAnterior != null) {
            btnAnterior.setTooltip(new Tooltip("Página anterior"));
        }
        if (btnSiguiente != null) {
            btnSiguiente.setTooltip(new Tooltip("Siguiente página"));
        }
        if (btnZoomMenos != null) {
            btnZoomMenos.setTooltip(new Tooltip("Reducir acercamiento"));
        }
        if (btnZoomMas != null) {
            btnZoomMas.setTooltip(new Tooltip("Aumentar acercamiento"));
        }
        if (btnZoomCien != null) {
            btnZoomCien.setTooltip(new Tooltip("Restablecer zoom al 100%"));
        }
        if (btnAjustarAncho != null) {
            btnAjustarAncho.setTooltip(new Tooltip("Ajustar el ancho de la hoja a la ventana"));
        }
    }

    public void inicializarDocumento(DocumentoReporte documento) {
        this.documento = Objects.requireNonNull(documento, "Falta el documento del reporte.");
        cachePaginas.clear();
        solicitudRenderId++;
        exportando = false;

        totalPaginas = RenderizadorVistaPreviaReporte.totalPaginas(documento.jasperPrint());
        paginaActual = 0;
        escalaActual = RenderizadorVistaPreviaReporte.ZOOM_INICIAL;

        if (totalPaginas == 0) {
            if (imgPagina != null) {
                imgPagina.setImage(null);
                imgPagina.setVisible(false);
            }
            mostrarEstado("El reporte no contiene páginas para mostrar.");
            actualizarControles();
            return;
        }

        ocultarEstado();
        actualizarControles();
        cargarPaginaActual();
    }

    @FXML
    public void handleAnterior() {
        if (totalPaginas <= 0 || paginaActual <= 0) {
            return;
        }
        paginaActual--;
        actualizarControles();
        cargarPaginaActual();
    }

    @FXML
    public void handleSiguiente() {
        if (totalPaginas <= 0 || paginaActual >= totalPaginas - 1) {
            return;
        }
        paginaActual++;
        actualizarControles();
        cargarPaginaActual();
    }

    @FXML
    public void handleZoomMenos() {
        aplicarZoom(escalaActual - RenderizadorVistaPreviaReporte.ZOOM_PASO);
    }

    @FXML
    public void handleZoomMas() {
        aplicarZoom(escalaActual + RenderizadorVistaPreviaReporte.ZOOM_PASO);
    }

    @FXML
    public void handleZoomCien() {
        aplicarZoom(RenderizadorVistaPreviaReporte.ZOOM_INICIAL);
    }

    @FXML
    public void handleAjustarAncho() {
        if (documento == null || totalPaginas <= 0) {
            return;
        }
        double anchoDisponible = spHoja != null && spHoja.getViewportBounds().getWidth() > 0
                ? spHoja.getViewportBounds().getWidth()
                : (spHoja != null ? spHoja.getWidth() : 960.0);
        aplicarZoom(RenderizadorVistaPreviaReporte.calcularZoomAjustarAncho(
                documento.jasperPrint(), anchoDisponible));
    }

    @FXML
    public void handleExportarPdf() {
        if (documento == null || totalPaginas <= 0 || exportando) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar reporte a PDF");
        fileChooser.setInitialFileName(documento.nombreArchivoSugerido());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf")
        );

        Window ventana = obtenerVentana();
        File archivoDestino = fileChooser.showSaveDialog(ventana);
        if (archivoDestino == null) {
            return;
        }

        Path rutaDestino = archivoDestino.toPath();
        DocumentoReporte documentoActual = documento;
        String textoOriginal = btnExportarPdf != null ? btnExportarPdf.getText() : "Exportar PDF…";
        exportando = true;
        if (btnExportarPdf != null) {
            btnExportarPdf.setDisable(true);
            btnExportarPdf.setText("Exportando…");
        }

        Task<Path> tareaExportar = new Task<>() {
            @Override
            protected Path call() throws Exception {
                VistaPreviaReporte.exportarPdfAtomico(documentoActual.jasperPrint(), rutaDestino);
                return rutaDestino;
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tareaExportar,
                destinoFinal -> {
                    exportando = false;
                    restaurarBotonExportar(textoOriginal);
                    AlertaUtil.mostrarExito("PDF exportado",
                            "El reporte se guardó correctamente en " + destinoFinal.getFileName() + ".");
                    abrirSiEsPosible(destinoFinal);
                },
                error -> {
                    exportando = false;
                    restaurarBotonExportar(textoOriginal);
                    AlertaUtil.mostrarError("Error al exportar",
                            "No se pudo guardar el archivo PDF en disco.");
                }
        );
    }

    @FXML
    public void handleCerrar() {
        Window ventana = obtenerVentana();
        if (ventana instanceof Stage stage) {
            stage.close();
        }
    }

    public void liberarRecursos() {
        solicitudRenderId++;
        cachePaginas.clear();
        if (imgPagina != null) {
            imgPagina.setImage(null);
        }
        documento = null;
        totalPaginas = 0;
        paginaActual = 0;
    }

    private void aplicarZoom(float nuevaEscala) {
        if (totalPaginas <= 0) {
            return;
        }
        float acotada = RenderizadorVistaPreviaReporte.acotarZoom(nuevaEscala);
        if (Math.round(acotada * 100) == Math.round(escalaActual * 100)) {
            return;
        }
        escalaActual = acotada;
        actualizarControles();
        cargarPaginaActual();
    }

    private void cargarPaginaActual() {
        if (documento == null || totalPaginas <= 0) {
            return;
        }
        int porcentaje = Math.round(escalaActual * 100);
        ClavePagina clave = new ClavePagina(paginaActual, porcentaje);
        WritableImage enCache = cachePaginas.get(clave);
        if (enCache != null) {
            mostrarImagen(enCache);
            return;
        }

        long idSolicitud = ++solicitudRenderId;
        int indice = paginaActual;
        float escala = escalaActual;
        DocumentoReporte doc = documento;
        mostrarEstado("Renderizando página " + (indice + 1) + " de " + totalPaginas + "…");

        Task<WritableImage> tareaRender = new Task<>() {
            @Override
            protected WritableImage call() {
                return RenderizadorVistaPreviaReporte.renderizarPagina(doc.jasperPrint(), indice, escala);
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tareaRender,
                imagen -> {
                    if (idSolicitud != solicitudRenderId || documento == null) {
                        return;
                    }
                    cachePaginas.put(clave, imagen);
                    mostrarImagen(imagen);
                },
                error -> {
                    if (idSolicitud != solicitudRenderId) {
                        return;
                    }
                    if (imgPagina != null) {
                        imgPagina.setImage(null);
                        imgPagina.setVisible(false);
                    }
                    mostrarEstado("No se pudo renderizar la página seleccionada.");
                }
        );
    }

    private void mostrarImagen(WritableImage imagen) {
        ocultarEstado();
        if (imgPagina != null) {
            imgPagina.setImage(imagen);
            imgPagina.setFitWidth(imagen.getWidth());
            imgPagina.setFitHeight(imagen.getHeight());
            imgPagina.setVisible(true);
        }
    }

    private void actualizarControles() {
        boolean hayPaginas = totalPaginas > 0;
        if (lblPaginacion != null) {
            lblPaginacion.setText(hayPaginas
                    ? "Página " + (paginaActual + 1) + " de " + totalPaginas
                    : "Página 0 de 0");
        }
        if (lblZoom != null) {
            lblZoom.setText(Math.round(escalaActual * 100) + "%");
        }
        if (btnAnterior != null) {
            btnAnterior.setDisable(!hayPaginas || paginaActual <= 0);
        }
        if (btnSiguiente != null) {
            btnSiguiente.setDisable(!hayPaginas || paginaActual >= totalPaginas - 1);
        }
        if (btnZoomMenos != null) {
            btnZoomMenos.setDisable(!hayPaginas || escalaActual <= RenderizadorVistaPreviaReporte.ZOOM_MIN + 0.001f);
        }
        if (btnZoomMas != null) {
            btnZoomMas.setDisable(!hayPaginas || escalaActual >= RenderizadorVistaPreviaReporte.ZOOM_MAX - 0.001f);
        }
        if (btnZoomCien != null) {
            btnZoomCien.setDisable(!hayPaginas);
        }
        if (btnAjustarAncho != null) {
            btnAjustarAncho.setDisable(!hayPaginas);
        }
        if (btnExportarPdf != null && !exportando) {
            btnExportarPdf.setDisable(!hayPaginas);
        }
    }

    private void restaurarBotonExportar(String textoOriginal) {
        if (btnExportarPdf != null) {
            btnExportarPdf.setText(textoOriginal);
            btnExportarPdf.setDisable(totalPaginas <= 0);
        }
    }

    private void mostrarEstado(String mensaje) {
        if (lblEstado != null) {
            lblEstado.setText(mensaje);
            lblEstado.setVisible(true);
            lblEstado.setManaged(true);
        }
    }

    private void ocultarEstado() {
        if (lblEstado != null) {
            lblEstado.setVisible(false);
            lblEstado.setManaged(false);
        }
    }

    private Window obtenerVentana() {
        if (btnCerrar != null && btnCerrar.getScene() != null) {
            return btnCerrar.getScene().getWindow();
        }
        if (spHoja != null && spHoja.getScene() != null) {
            return spHoja.getScene().getWindow();
        }
        return null;
    }

    private static void abrirSiEsPosible(Path destino) {
        if (destino == null || !Files.isRegularFile(destino) || !Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(destino.toFile());
            }
        } catch (Exception ignored) {
            // La vista previa sigue operativa aunque el visor externo no esté disponible.
        }
    }

    private record ClavePagina(int indicePagina, int porcentajeZoom) { }
}
