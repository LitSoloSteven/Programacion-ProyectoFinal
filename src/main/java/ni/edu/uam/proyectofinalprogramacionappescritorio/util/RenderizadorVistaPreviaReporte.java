package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Objects;

/** Renderiza páginas de JasperPrint sobre lienzo blanco sin depender de javafx-swing. */
public final class RenderizadorVistaPreviaReporte {

    public static final float ZOOM_MIN = 0.50f;
    public static final float ZOOM_MAX = 2.00f;
    public static final float ZOOM_PASO = 0.25f;
    public static final float ZOOM_INICIAL = 1.00f;

    private RenderizadorVistaPreviaReporte() { }

    public static int totalPaginas(JasperPrint jasperPrint) {
        if (jasperPrint == null || jasperPrint.getPages() == null) {
            return 0;
        }
        return jasperPrint.getPages().size();
    }

    public static float acotarZoom(float escala) {
        if (Float.isNaN(escala) || Float.isInfinite(escala)) {
            return ZOOM_INICIAL;
        }
        return Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, escala));
    }

    public static float calcularZoomAjustarAncho(JasperPrint jasperPrint, double anchoDisponiblePx) {
        if (jasperPrint == null) {
            return ZOOM_INICIAL;
        }
        double anchoUtil = Math.max(200.0, anchoDisponiblePx - 48.0);
        int anchoPagina = Math.max(1, jasperPrint.getPageWidth());
        return acotarZoom((float) (anchoUtil / anchoPagina));
    }

    public static BufferedImage renderizarPaginaAwt(JasperPrint jasperPrint, int indicePagina, float escala) {
        Objects.requireNonNull(jasperPrint, "Falta el documento de reporte.");
        int total = totalPaginas(jasperPrint);
        if (total == 0) {
            throw new IllegalArgumentException("El reporte no contiene páginas para previsualizar.");
        }
        if (indicePagina < 0 || indicePagina >= total) {
            throw new IndexOutOfBoundsException("Índice de página fuera de rango: " + indicePagina);
        }

        float escalaAcotada = acotarZoom(escala);
        Image imagenRenderizada;
        try {
            imagenRenderizada = JasperPrintManager.printPageToImage(jasperPrint, indicePagina, escalaAcotada);
        } catch (JRException error) {
            throw new IllegalStateException("No se pudo renderizar la página del reporte.", error);
        }
        if (imagenRenderizada == null) {
            throw new IllegalStateException("JasperReports devolvió una imagen nula para la página.");
        }

        int ancho = Math.max(1, imagenRenderizada.getWidth(null));
        int alto = Math.max(1, imagenRenderizada.getHeight(null));

        BufferedImage lienzo = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = lienzo.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ancho, alto);
            g.drawImage(imagenRenderizada, 0, 0, null);
        } finally {
            g.dispose();
        }
        return lienzo;
    }

    public static WritableImage convertirAImagenJavaFx(BufferedImage imagen) {
        Objects.requireNonNull(imagen, "Falta la imagen a convertir.");
        BufferedImage origen = imagen;
        if (imagen.getType() != BufferedImage.TYPE_INT_ARGB) {
            origen = new BufferedImage(imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = origen.createGraphics();
            try {
                g.drawImage(imagen, 0, 0, null);
            } finally {
                g.dispose();
            }
        }

        int ancho = origen.getWidth();
        int alto = origen.getHeight();
        int[] pixeles = new int[ancho * alto];
        origen.getRGB(0, 0, ancho, alto, pixeles, 0, ancho);

        WritableImage resultado = new WritableImage(ancho, alto);
        resultado.getPixelWriter().setPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        return resultado;
    }

    public static WritableImage renderizarPagina(JasperPrint jasperPrint, int indicePagina, float escala) {
        return convertirAImagenJavaFx(renderizarPaginaAwt(jasperPrint, indicePagina, escala));
    }
}
