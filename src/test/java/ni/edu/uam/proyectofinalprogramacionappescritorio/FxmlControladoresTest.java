package ni.edu.uam.proyectofinalprogramacionappescritorio;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FxmlControladoresTest {
    private static final String FX_NS = "http://javafx.com/fxml/1";
    private static final Path VISTAS = Files.isDirectory(
            Path.of("src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio"))
            ? Path.of("src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio")
            : Path.of("latch-cliente/src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio");

    @Test
    void cadaVistaResuelveLosCamposYAccionesDeSuControladorSinJavaFxRuntime() throws Exception {
        var constructor = DocumentBuilderFactory.newInstance();
        constructor.setNamespaceAware(true);
        constructor.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        constructor.setExpandEntityReferences(false);

        List<String> errores = new ArrayList<>();
        try (var archivos = Files.walk(VISTAS)) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                var documento = constructor.newDocumentBuilder().parse(archivo.toFile());
                Element raiz = documento.getDocumentElement();
                String nombreControlador = raiz.getAttributeNS(FX_NS, "controller");
                assertFalse(nombreControlador.isBlank(), archivo + " declara su controlador");
                Class<?> controlador = Class.forName(nombreControlador);

                var nodos = documento.getElementsByTagName("*");
                for (int i = 0; i < nodos.getLength(); i++) {
                    Node nodo = nodos.item(i);
                    if (!(nodo instanceof Element elemento)) continue;
                    for (String medida : List.of("minWidth", "minHeight", "prefWidth", "prefHeight",
                            "maxWidth", "maxHeight")) {
                        String valor = elemento.getAttribute(medida);
                        if ("USE_PREF_SIZE".equals(valor) || "USE_COMPUTED_SIZE".equals(valor)) {
                            errores.add(archivo.getFileName() + ": " + medida + "=" + valor
                                    + " debe resolverse como double en FXMLLoader");
                        }
                    }
                    String id = elemento.getAttributeNS(FX_NS, "id");
                    if (!id.isBlank() && !tieneCampo(controlador, id)) {
                        errores.add(archivo.getFileName() + ": fx:id=" + id
                                + " no existe en " + nombreControlador);
                    }
                    String accion = elemento.getAttribute("onAction");
                    if (accion.startsWith("#") && !tieneMetodo(controlador, accion.substring(1))) {
                        errores.add(archivo.getFileName() + ": onAction=" + accion
                                + " no existe en " + nombreControlador);
                    }
                }
            }
        }
        assertTrue(errores.isEmpty(), String.join(System.lineSeparator(), errores));
    }

    @Test
    void ningunaVistaDeclaraBotonesDeActualizarManualYConservaAccionesDeNegocio() throws Exception {
        Set<String> idsProhibidos = Set.of(
                "btnActualizar", "btnRefrescar", "btnRefrescarCatalogos",
                "btnRefrescarUsuarios", "btnRefrescarParametros", "btnRefrescarRespaldo");
        Set<String> accionesProhibidas = Set.of(
                "#actualizarMetricas", "#refrescarTabla", "#refrescarDatos",
                "#cargarCatalogos", "#refrescarUsuarios", "#refrescarParametros", "#refrescarEstadoRespaldo");
        Set<String> accionesNegocioEsperadas = new HashSet<>(Set.of(
                "btnAutorizar", "btnRevisarMora", "btnGenerarRentas",
                "btnCrearRespaldo", "btnConsultarAuditoria", "btnConsultarGastos"));

        var constructor = DocumentBuilderFactory.newInstance();
        constructor.setNamespaceAware(true);
        constructor.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        constructor.setExpandEntityReferences(false);

        List<String> hallazgos = new ArrayList<>();
        try (var archivos = Files.walk(VISTAS)) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                var documento = constructor.newDocumentBuilder().parse(archivo.toFile());
                var nodos = documento.getElementsByTagName("*");
                for (int i = 0; i < nodos.getLength(); i++) {
                    if (!(nodos.item(i) instanceof Element elemento)) continue;
                    String id = elemento.getAttributeNS(FX_NS, "id");
                    if (idsProhibidos.contains(id)) {
                        hallazgos.add(archivo.getFileName() + ": botón prohibido fx:id=" + id);
                    }
                    accionesNegocioEsperadas.remove(id);
                    String accion = elemento.getAttribute("onAction");
                    if (accionesProhibidas.contains(accion)) {
                        hallazgos.add(archivo.getFileName() + ": acción prohibida onAction=" + accion);
                    }
                }
            }
        }
        assertTrue(hallazgos.isEmpty(), String.join(System.lineSeparator(), hallazgos));
        assertTrue(accionesNegocioEsperadas.isEmpty(),
                "Faltan acciones de negocio esperadas: " + accionesNegocioEsperadas);
    }

    @Test
    void ningunaVistaFueraDeReportesDeclaraGeneracionDeDocumentosYReportesExponeCentroSinCodigosTecnicos() throws Exception {
        Set<String> idsProhibidosFueraDeReportes = Set.of(
                "btnGenerarReporte", "btnGenerarReportePropiedades",
                "btnReporteGarantias", "btnEstadoCuenta", "btnVerRecibo");
        Set<String> accionesProhibidasFueraDeReportes = Set.of(
                "#handleGenerarReporte", "#handleGenerarReportePropiedades",
                "#handleReporteGarantias", "#handleEstadoCuenta", "#handleVerRecibo");
        Set<String> idsEsperadosEnReportes = new HashSet<>(Set.of(
                "cmbCategoriaReporte", "cmbTipoReporte", "lblFormatosDisponibles",
                "tblRecibos", "txtNumeroRecibo", "txtBuscarRecibo", "txtBuscarContrato",
                "btnVistaPrevia", "btnExportarPdf", "btnExportarXlsx", "btnCancelarH5"));

        var constructor = DocumentBuilderFactory.newInstance();
        constructor.setNamespaceAware(true);
        constructor.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        constructor.setExpandEntityReferences(false);

        List<String> hallazgos = new ArrayList<>();
        try (var archivos = Files.walk(VISTAS)) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                String nombreArchivo = archivo.getFileName().toString();
                var documento = constructor.newDocumentBuilder().parse(archivo.toFile());
                var nodos = documento.getElementsByTagName("*");
                for (int i = 0; i < nodos.getLength(); i++) {
                    if (!(nodos.item(i) instanceof Element elemento)) continue;
                    String id = elemento.getAttributeNS(FX_NS, "id");
                    String accion = elemento.getAttribute("onAction");
                    String texto = elemento.getAttribute("text");

                    if ("reportes-view.fxml".equals(nombreArchivo)) {
                        idsEsperadosEnReportes.remove(id);
                        if (texto != null && (texto.contains("REP-") || texto.contains("(H5)"))) {
                            hallazgos.add("reportes-view.fxml expone código técnico en text=\"" + texto + "\"");
                        }
                    } else if (!"vista-previa-reporte.fxml".equals(nombreArchivo)) {
                        if (idsProhibidosFueraDeReportes.contains(id)) {
                            hallazgos.add(nombreArchivo + ": botón de reporte fuera del Centro fx:id=" + id);
                        }
                        if (accionesProhibidasFueraDeReportes.contains(accion)) {
                            hallazgos.add(nombreArchivo + ": acción de reporte fuera del Centro onAction=" + accion);
                        }
                    }
                }
            }
        }
        assertTrue(hallazgos.isEmpty(), String.join(System.lineSeparator(), hallazgos));
        assertTrue(idsEsperadosEnReportes.isEmpty(),
                "Faltan controles esperados en reportes-view.fxml: " + idsEsperadosEnReportes);
    }

    private static boolean tieneCampo(Class<?> tipo, String nombre) {
        for (Class<?> actual = tipo; actual != null; actual = actual.getSuperclass()) {
            try {
                actual.getDeclaredField(nombre);
                return true;
            } catch (NoSuchFieldException ignorado) {
                // Busca en la jerarquía del controlador.
            }
        }
        return false;
    }

    private static boolean tieneMetodo(Class<?> tipo, String nombre) {
        for (Class<?> actual = tipo; actual != null; actual = actual.getSuperclass()) {
            for (var metodo : actual.getDeclaredMethods()) {
                if (metodo.getName().equals(nombre)) return true;
            }
        }
        return false;
    }
}
