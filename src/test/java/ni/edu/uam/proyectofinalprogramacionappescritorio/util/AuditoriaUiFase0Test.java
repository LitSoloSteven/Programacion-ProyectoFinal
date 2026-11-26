package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Auditoría ejecutable de interfaz para el cierre de Fase 0:
 * valida renderizado JavaFX real en temas claro, oscuro y sin efectos,
 * dimensiones mínimas, accesibilidad básica y estructura de los 20 FXML.
 */
class AuditoriaUiFase0Test {
    private static final String FX_NS = "http://javafx.com/fxml/1";
    private static final Path VISTAS_DIR = Files.isDirectory(
            Path.of("src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio"))
            ? Path.of("src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio")
            : Path.of("latch-cliente/src/main/resources/ni/edu/uam/proyectofinalprogramacionappescritorio");
    private static final Pattern TEXTO_DESCRIPTIVO = Pattern.compile(".*[\\p{L}\\p{Nd}]{2,}.*");
    private static final Pattern CODIGOS_TECNICOS_PROHIBIDOS = Pattern.compile(
            "(?i)\\b(?:H[1-6]|M05|ERR-\\d+|REP-\\d+|RN-\\d+|MSG-\\d+)\\b");

    private record VistaAutocontenida(String fxml, double ancho, double alto, boolean verificarDesborde) { }

    private static final List<VistaAutocontenida> VISTAS_AUTOCONTENIDAS = List.of(
            new VistaAutocontenida("login-view.fxml", 460, 580, true),
            new VistaAutocontenida("vista-previa-reporte.fxml", 1060, 740, true),
            new VistaAutocontenida("vista-previa-reporte.fxml", 860, 620, true),
            new VistaAutocontenida("propiedad-form.fxml", 620, 600, false),
            new VistaAutocontenida("unidad-form.fxml", 680, 640, false),
            new VistaAutocontenida("inquilino-form.fxml", 620, 620, false),
            new VistaAutocontenida("contrato-form.fxml", 740, 680, false),
            new VistaAutocontenida("cargo-form.fxml", 720, 680, false),
            new VistaAutocontenida("pago-form.fxml", 740, 680, false),
            new VistaAutocontenida("tasa-form.fxml", 540, 460, false)
    );

    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException existente) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(5, TimeUnit.SECONDS), "JavaFX debe iniciar para auditar la UI");
        enFx(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    @Test
    void todasLasVistasCargablesRenderizanEnTemasClaroOscuroYSinEfectos() throws Exception {
        Map<String, String> almacenMemoria = new ConcurrentHashMap<>();
        AlmacenPreferencias almacenTest = new AlmacenPreferencias() {
            @Override public Optional<String> leer(String clave) {
                return Optional.ofNullable(almacenMemoria.get(clave));
            }
            @Override public void guardar(String clave, String valor) {
                almacenMemoria.put(clave, valor);
            }
        };

        enFx(() -> {
            TemaAplicacion.configurarAlmacen(almacenTest);
            List<String> hallazgos = new ArrayList<>();
            try {
                List<Runnable> modos = List.of(
                        () -> {
                            TemaAplicacion.establecerTema(TemaAplicacion.Tema.CLARO);
                            TemaAplicacion.establecerSinEfectos(false);
                        },
                        () -> {
                            TemaAplicacion.establecerTema(TemaAplicacion.Tema.OSCURO);
                            TemaAplicacion.establecerSinEfectos(false);
                        },
                        () -> {
                            TemaAplicacion.establecerTema(TemaAplicacion.Tema.OSCURO);
                            TemaAplicacion.establecerSinEfectos(true);
                        }
                );
                List<String> nombresModo = List.of("CLARO", "OSCURO", "OSCURO_SIN_EFECTOS");

                for (int i = 0; i < modos.size(); i++) {
                    modos.get(i).run();
                    String modo = nombresModo.get(i);
                    for (VistaAutocontenida vista : VISTAS_AUTOCONTENIDAS) {
                        FXMLLoader loader = new FXMLLoader(LatchApplication.class.getResource(vista.fxml()));
                        Parent root = loader.load();
                        Scene scene = new Scene(root, vista.ancho(), vista.alto());
                        TemaAplicacion.aplicar(scene);
                        root.applyCss();
                        root.resize(vista.ancho(), vista.alto());
                        root.layout();
                        root.applyCss();
                        root.layout();

                        List<Node> nodos = new ArrayList<>();
                        recolectarNodos(root, nodos);
                        for (Node nodo : nodos) {
                            if (!nodo.isVisible() || !nodo.isManaged()) continue;

                            if (nodo instanceof Button boton) {
                                String ctx = modo + " " + vista.fxml() + " (" + (int) vista.ancho() + "x"
                                        + (int) vista.alto() + ") botón #" + boton.getId() + " [" + boton.getText() + "]";
                                if (boton.getWidth() <= 0 || boton.getHeight() <= 0) {
                                    hallazgos.add(ctx + ": tamaño inválido " + boton.getWidth() + "x" + boton.getHeight());
                                }
                                if (!tieneNombreDescriptivo(boton.getText()) && !tieneTexto(boton.getAccessibleText())) {
                                    hallazgos.add(ctx + ": sin texto descriptivo ni accessibleText");
                                }
                                if (vista.verificarDesborde()) {
                                    Bounds limites = boton.localToScene(boton.getBoundsInLocal());
                                    if (limites != null && limites.getMaxX() > scene.getWidth() + 1.0) {
                                        hallazgos.add(ctx + ": desborda horizontalmente (maxX="
                                                + String.format("%.1f", limites.getMaxX())
                                                + " > ancho=" + (int) scene.getWidth() + ")");
                                    }
                                }
                            }

                            if (esControlInteractivo(nodo) && !nodo.isDisabled() && !nodo.isFocusTraversable()) {
                                hallazgos.add(modo + " " + vista.fxml() + ": control " + nodo.getId()
                                        + " (" + nodo.getClass().getSimpleName() + ") no es focusTraversable");
                            }
                        }
                    }
                }
            } finally {
                TemaAplicacion.restaurarAlmacenPorDefecto();
            }
            assertTrue(hallazgos.isEmpty(), String.join(System.lineSeparator(), hallazgos));
            return null;
        });
    }

    @Test
    void todosLosVeinteFxmlCumplenReglasBaseDeAccesibilidadYNoUsanTextoVacioEnBotones() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        List<Path> fxmls;
        try (var stream = Files.walk(VISTAS_DIR)) {
            fxmls = stream.filter(p -> p.toString().endsWith(".fxml")).sorted().toList();
        }
        assertEquals(20, fxmls.size(), "Se esperan exactamente 20 archivos FXML en la aplicación");

        List<String> errores = new ArrayList<>();
        for (Path archivo : fxmls) {
            var doc = factory.newDocumentBuilder().parse(archivo.toFile());
            NodeList botones = doc.getElementsByTagName("Button");
            for (int i = 0; i < botones.getLength(); i++) {
                if (!(botones.item(i) instanceof Element boton)) continue;
                String id = boton.getAttributeNS(FX_NS, "id");
                String texto = boton.getAttribute("text");
                String accesible = boton.getAttribute("accessibleText");
                if (!tieneNombreDescriptivo(texto) && !tieneTexto(accesible)) {
                    errores.add(archivo.getFileName() + ": Button fx:id=" + id
                            + " con text=\"" + texto + "\" requiere accessibleText");
                }
            }
            NodeList todos = doc.getElementsByTagName("*");
            for (int i = 0; i < todos.getLength(); i++) {
                if (!(todos.item(i) instanceof Element elemento)) continue;
                for (String attr : List.of("text", "promptText", "accessibleText")) {
                    String valor = elemento.getAttribute(attr);
                    if (valor != null && !valor.isBlank() && CODIGOS_TECNICOS_PROHIBIDOS.matcher(valor).find()) {
                        errores.add(archivo.getFileName() + ": " + attr + "=\"" + valor
                                + "\" expone código técnico interno");
                    }
                }
            }
        }
        assertTrue(errores.isEmpty(), String.join(System.lineSeparator(), errores));
    }

    @Test
    void todosLosFormulariosDeclaranResumenYEtiquetasDeErrorPorCampo() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        List<String> formularios = List.of(
                "propiedad-form.fxml", "unidad-form.fxml", "inquilino-form.fxml",
                "contrato-form.fxml", "cargo-form.fxml", "pago-form.fxml", "tasa-form.fxml");
        List<String> errores = new ArrayList<>();

        for (String nombre : formularios) {
            Path ruta = VISTAS_DIR.resolve(nombre);
            var doc = factory.newDocumentBuilder().parse(ruta.toFile());
            NodeList labels = doc.getElementsByTagName("Label");
            boolean tieneResumen = false;
            int erroresCampo = 0;
            for (int i = 0; i < labels.getLength(); i++) {
                if (!(labels.item(i) instanceof Element label)) continue;
                String id = label.getAttributeNS(FX_NS, "id");
                if ("lblResumenError".equals(id)) tieneResumen = true;
                else if (id.startsWith("lblError")) erroresCampo++;
            }
            if (!tieneResumen) errores.add(nombre + ": falta lblResumenError");
            if (erroresCampo == 0) errores.add(nombre + ": no declara etiquetas lblError* por campo");
        }
        assertTrue(errores.isEmpty(), String.join(System.lineSeparator(), errores));
    }

    private static void recolectarNodos(Node actual, List<Node> salida) {
        salida.add(actual);
        if (actual instanceof Parent parent) {
            for (Node hijo : parent.getChildrenUnmodifiable()) {
                recolectarNodos(hijo, salida);
            }
        }
    }

    private static boolean esControlInteractivo(Node nodo) {
        if (nodo instanceof TextField && nodo.getParent() instanceof DatePicker) {
            return false;
        }
        return nodo instanceof Button
                || nodo instanceof TextField
                || nodo instanceof PasswordField
                || nodo instanceof ComboBox<?>
                || nodo instanceof DatePicker
                || nodo instanceof CheckBox;
    }

    private static boolean tieneNombreDescriptivo(String texto) {
        return texto != null && TEXTO_DESCRIPTIVO.matcher(texto.strip()).matches();
    }

    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static <T> T enFx(Callable<T> tarea) throws Exception {
        FutureTask<T> futuro = new FutureTask<>(tarea);
        Platform.runLater(futuro);
        return futuro.get(20, TimeUnit.SECONDS);
    }
}
