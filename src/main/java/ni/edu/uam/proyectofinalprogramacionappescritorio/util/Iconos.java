package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.shape.SVGPath;

/** Catálogo local de iconos vectoriales monocromos. */
public final class Iconos {
    public enum Nombre {
        PANEL, PROPIEDAD, UNIDAD, PERSONAS, CONTRATO, CARGO, PAGO, TASA,
        BUSCAR, ACTUALIZAR, ADVERTENCIA, CONFIRMACION, CERRAR, RELOJ,
        MAS, ELIMINAR, EDITAR, ACCIONES, SALIR, TEMA, PUNTO
    }

    private static final String[] TRAZOS = {
            "M3 3h8v8H3z M13 3h8v5h-8z M13 10h8v11h-8z M3 13h8v8H3z",
            "M3 21V4h18v17 M7 8h3 M14 8h3 M7 12h3 M14 12h3 M7 16h3 M14 16h3",
            "M4 21V3h16v18 M8 7h3 M14 7h3 M8 11h3 M14 11h3 M10 21v-5h4v5",
            "M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M20 8v6 M17 11h6",
            "M6 3h9l4 4v14H6z M14 3v5h5 M9 13h7 M9 17h7",
            "M4 4h16v16H4z M8 8h8 M8 12h8 M8 16h5",
            "M3 5h18v14H3z M7 9h10 M7 13h6 M7 16h4",
            "M12 2v20 M17 5H9a3 3 0 0 0 0 6h6a3 3 0 0 1 0 6H6",
            "M20 20l-5-5 M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0",
            "M20 6v5h-5 M20 11a8 8 0 1 0 2 5",
            "M12 3 2.8 20h18.4z M12 9v5 M12 17h.01",
            "M5 12l4 4L19 6",
            "M6 6l12 12 M18 6 6 18",
            "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 7v5l3 2",
            "M12 5v14 M5 12h14",
            "M4 7h16 M10 11v6 M14 11v6 M6 7l1 14h10l1-14 M9 7V4h6v3",
            "M4 20l4-.8L19 8a2.1 2.1 0 0 0-3-3L5 16z M14 7l3 3",
            "M5 7h14 M5 12h14 M5 17h14",
            "M10 4H4v16h16v-6 M14 3h7v7 M21 3l-9 9",
            "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 7v5l3 2",
            "M12 12h.01"
    };

    private Iconos() { }

    public static Node crear(Nombre nombre, double tamano) {
        int indice = nombre.ordinal();
        SVGPath path = new SVGPath();
        path.setContent(TRAZOS[indice]);
        path.getStyleClass().add("icon-glyph");
        path.setAccessibleText(nombre.name().toLowerCase().replace('_', ' '));
        path.setMouseTransparent(true);
        Group grupo = new Group(path);
        double escala = tamano / 24.0;
        grupo.setScaleX(escala);
        grupo.setScaleY(escala);
        grupo.setManaged(false);
        return grupo;
    }

    public static void asignar(Button boton, Nombre nombre, double tamano) {
        boton.setGraphic(crear(nombre, tamano));
    }
}

