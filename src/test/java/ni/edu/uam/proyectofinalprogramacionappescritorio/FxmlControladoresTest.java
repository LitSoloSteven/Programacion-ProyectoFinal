package ni.edu.uam.proyectofinalprogramacionappescritorio;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FxmlControladoresTest {
    private static final String FX_NS = "http://javafx.com/fxml/1";
    private static final Path VISTAS = Path.of(
            "src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio");

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

