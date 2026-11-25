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
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.OperacionCanceladaException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.GestorH5Reportes;
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
import java.util.concurrent.ExecutionException;

/** Controlador del diálogo reutilizable de vista previa de reportes (PAN-16 / H5). */
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
    @FXML private Button btnCancelarExportacion;
    @FXML private Button btnExportarXlsx;
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
    private GestorH5Reportes.EjecucionH5 ejecucionExportacion;

    @FXML
    public void initialize() {
        if (btnExportarPdf != null) {
            Iconos.asignar(btnExportarPdf, Iconos.Nombre.DESCARGAR, 16);
            btnExportarPdf.setTooltip(new Tooltip("Guardar este reporte como archivo PDF"));
        }
        if (btnExportarXlsx != null) {
            Iconos.asignar(btnExportarXlsx, Iconos.Nombre.DESCARGAR, 16);
            btnExportarXlsx.setTooltip(new Tooltip("Guardar este reporte como libro tabular Excel (.xlsx)"));
        }
        if (btnCancelarExportacion != null) {
            Iconos.asignar(btnCancelarExportacion, Iconos.Nombre.CERRAR, 16);
            btnCancelarExportacion.setTooltip(new Tooltip("Cancelar la exportación en curso (H5)"));
        }
        if (btnCerrar != null) {
            Iconos.asignar(btnCerrar, Iconos.Nombre.CERRAR, 16);
            btnCerrar.setTooltip(new Tooltip("Cerrar la vista previa"));
        }
        if (btnAnterior != null) btnAnterior.setTooltip(new Tooltip("Página anterior"));
        if (btnSiguiente != null) btnSiguiente.setTooltip(new Tooltip("Siguiente página"));
        if (btnZoomMenos != null) btnZoomMenos.setTooltip(new Tooltip("Reducir acercamiento"));
        if (btnZoomMas != null) btnZoomMas.setTooltip(new Tooltip("Aumentar acercamiento"));
        if (btnZoomCien != null) btnZoomCien.setTooltip(new Tooltip("Restablecer zoom al 100%"));
        if (btnAjustarAncho != null) btnAjustarAncho.setTooltip(new Tooltip("Ajustar el ancho de la hoja a la ventana"));
    }

    public void inicializarDocumento(DocumentoReporte documento) {
        this.documento = Objects.requireNonNull(documento, "Falta el documento del reporte.");
        cachePaginas.clear();
        solicitudRenderId++;
        exportando = false;

        totalPaginas = RenderizadorVistaPreviaReporte.totalPaginas(documento.jasperPrint());
        paginaActual = 0;
        escalaActual = RenderizadorVistaPreviaReporte.ZOOM_INICIAL;

        if (btnExportarXlsx != null) {
            boolean admiteXlsx = documento.admiteXlsx();
            btnExportarXlsx.setVisible(admiteXlsx);
            btnExportarXlsx.setManaged(admiteXlsx);
        }

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
        if (totalPaginas <= 0 || paginaActual <= 0) return;
        paginaActual--;
        actualizarControles();
        cargarPaginaActual();
    }

    @FXML
    public void handleSiguiente() {
        if (totalPaginas <= 0 || paginaActual >= totalPaginas - 1) return;
        paginaActual++;
        actualizarControles();
        cargarPaginaActual();
    }

    @FXML public void handleZoomMenos() { aplicarZoom(escalaActual - RenderizadorVistaPreviaReporte.ZOOM_PASO); }
    @FXML public void handleZoomMas() { aplicarZoom(escalaActual + RenderizadorVistaPreviaReporte.ZOOM_PASO); }
    @FXML public void handleZoomCien() { aplicarZoom(RenderizadorVistaPreviaReporte.ZOOM_INICIAL); }

    @FXML
    public void handleAjustarAncho() {
        if (documento == null || totalPaginas <= 0) return;
        double anchoDisponible = spHoja != null && spHoja.getViewportBounds().getWidth() > 0
                ? spHoja.getViewportBounds().getWidth()
                : (spHoja != null ? spHoja.getWidth() : 960.0);
        aplicarZoom(RenderizadorVistaPreviaReporte.calcularZoomAjustarAncho(
                documento.jasperPrint(), anchoDisponible));
    }

    @FXML
    public void handleExportarPdf() {
        iniciarExportacion(false);
    }

    @FXML
    public void handleExportarXlsx() {
        if (documento == null || !documento.admiteXlsx()) {
            AlertaUtil.mostrarAdvertencia("Formato no disponible",
                    "Este documento (recibo) solo admite exportación en PDF.");
            return;
        }
        iniciarExportacion(true);
    }

    @FXML
    public void handleCancelarExportacion() {
        if (ejecucionExportacion != null) {
            ejecucionExportacion.solicitarCancelacion();
            if (btnCancelarExportacion != null) {
                btnCancelarExportacion.setDisable(true);
                btnCancelarExportacion.setText("Cancelando…");
            }
        }
    }

    private void iniciarExportacion(boolean comoXlsx) {
        if (documento == null || (!comoXlsx && totalPaginas <= 0) || exportando) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(comoXlsx ? "Exportar reporte a XLSX" : "Exportar reporte a PDF");
        fileChooser.setInitialFileName(comoXlsx
                ? documento.nombreArchivoXlsxSugerido()
                : documento.nombreArchivoSugerido());
        fileChooser.getExtensionFilters().add(comoXlsx
                ? new FileChooser.ExtensionFilter("Libro Excel (*.xlsx)", "*.xlsx")
                : new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));

        File archivoDestino = fileChooser.showSaveDialog(obtenerVentana());
        if (archivoDestino == null) {
            return;
        }

        final GestorH5Reportes.EjecucionH5 ejecucion;
        try {
            ejecucion = GestorH5Reportes.iniciar(comoXlsx ? "Exportación XLSX" : "Exportación PDF");
        } catch (IllegalStateException ocupada) {
            AlertaUtil.mostrarAdvertencia("Exportación en curso", ocupada.getMessage());
            return;
        }

        Path rutaDestino = archivoDestino.toPath();
        DocumentoReporte documentoActual = documento;
        ejecucionExportacion = ejecucion;
        exportando = true;
        actualizarEstadoExportacion(true, comoXlsx);

        Task<VistaPreviaReporte.ResultadoPublicacion> tareaExportar = new Task<>() {
            @Override
            protected VistaPreviaReporte.ResultadoPublicacion call() throws Exception {
                try {
                    return comoXlsx
                            ? VistaPreviaReporte.exportarXlsxAtomico(
                                    documentoActual, rutaDestino, ejecucion::estaCancelada)
                            : VistaPreviaReporte.exportarPdfAtomico(
                                    documentoActual.jasperPrint(), rutaDestino, ejecucion::estaCancelada);
                } finally {
                    ejecucion.close();
                }
            }
        };

        TareasJavaFx.ejecutar(
                HelloApplication.instancia().getTareas(),
                tareaExportar,
                resultado -> {
                    ejecucion.close();
                    ejecucionExportacion = null;
                    exportando = false;
                    actualizarEstadoExportacion(false, comoXlsx);
                    String titulo = comoXlsx ? "XLSX exportado" : "PDF exportado";
                    String mensaje = resultado.publicadoAntesDeCancelar()
                            ? "La exportación concluyó antes de aplicar la cancelación y se publicó en "
                            + resultado.rutaFinal().getFileName() + "."
                            : "El reporte se guardó correctamente en " + resultado.rutaFinal().getFileName() + ".";
                    AlertaUtil.mostrarExito(titulo, mensaje);
                    abrirSiEsPosible(resultado.rutaFinal());
                },
                error -> {
                    ejecucion.close();
                    ejecucionExportacion = null;
                    exportando = false;
                    actualizarEstadoExportacion(false, comoXlsx);
                    Throwable causa = extraerCausa(error);
                    if (causa instanceof OperacionCanceladaException) {
                        AlertaUtil.mostrarExito("Exportación cancelada", OperacionCanceladaException.MSG_089);
                    } else if (causa instanceof IllegalArgumentException) {
                        AlertaUtil.mostrarAdvertencia("Exportación no disponible", causa.getMessage());
                    } else {
                        AlertaUtil.mostrarError("Error al exportar",
                                "No se pudo guardar el archivo en disco. El archivo previo (si existía) se conservó intacto.");
                    }
                }
        );
    }

    private void actualizarEstadoExportacion(boolean enCurso, boolean comoXlsx) {
        if (btnExportarPdf != null) {
            btnExportarPdf.setDisable(enCurso || totalPaginas <= 0);
            btnExportarPdf.setText(enCurso && !comoXlsx ? "Exportando…" : "Exportar PDF…");
        }
        if (btnExportarXlsx != null) {
            boolean admite = documento != null && documento.admiteXlsx();
            btnExportarXlsx.setDisable(enCurso || !admite || totalPaginas <= 0);
            btnExportarXlsx.setText(enCurso && comoXlsx ? "Exportando…" : "Exportar XLSX…");
        }
        if (btnCancelarExportacion != null) {
            btnCancelarExportacion.setVisible(enCurso);
            btnCancelarExportacion.setManaged(enCurso);
            btnCancelarExportacion.setDisable(!enCurso);
            btnCancelarExportacion.setText("Cancelar");
        }
    }

    @FXML
    public void handleCerrar() {
        if (ejecucionExportacion != null) {
            ejecucionExportacion.solicitarCancelacion();
        }
        Window ventana = obtenerVentana();
        if (ventana instanceof Stage stage) {
            stage.close();
        }
    }

    public void liberarRecursos() {
        if (ejecucionExportacion != null) {
            ejecucionExportacion.solicitarCancelacion();
        }
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
        if (totalPaginas <= 0) return;
        float acotada = RenderizadorVistaPreviaReporte.acotarZoom(nuevaEscala);
        if (Math.round(acotada * 100) == Math.round(escalaActual * 100)) return;
        escalaActual = acotada;
        actualizarControles();
        cargarPaginaActual();
    }

    private void cargarPaginaActual() {
        if (documento == null || totalPaginas <= 0) return;
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
                    if (idSolicitud != solicitudRenderId || documento == null) return;
                    cachePaginas.put(clave, imagen);
                    mostrarImagen(imagen);
                },
                error -> {
                    if (idSolicitud != solicitudRenderId) return;
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
        if (btnAnterior != null) btnAnterior.setDisable(!hayPaginas || paginaActual <= 0);
        if (btnSiguiente != null) btnSiguiente.setDisable(!hayPaginas || paginaActual >= totalPaginas - 1);
        if (btnZoomMenos != null) {
            btnZoomMenos.setDisable(!hayPaginas || escalaActual <= RenderizadorVistaPreviaReporte.ZOOM_MIN + 0.001f);
        }
        if (btnZoomMas != null) {
            btnZoomMas.setDisable(!hayPaginas || escalaActual >= RenderizadorVistaPreviaReporte.ZOOM_MAX - 0.001f);
        }
        if (btnZoomCien != null) btnZoomCien.setDisable(!hayPaginas);
        if (btnAjustarAncho != null) btnAjustarAncho.setDisable(!hayPaginas);
        if (btnExportarPdf != null && !exportando) btnExportarPdf.setDisable(!hayPaginas);
        if (btnExportarXlsx != null && !exportando) {
            btnExportarXlsx.setDisable(!hayPaginas || documento == null || !documento.admiteXlsx());
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
        if (btnCerrar != null && btnCerrar.getScene() != null) return btnCerrar.getScene().getWindow();
        if (spHoja != null && spHoja.getScene() != null) return spHoja.getScene().getWindow();
        return null;
    }

    private static Throwable extraerCausa(Throwable error) {
        Throwable actual = error;
        while (actual instanceof ExecutionException && actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual;
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
