package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.geometry.Pos;
import javafx.util.Callback;

import java.util.List;
import java.util.function.Consumer;

/** Menú contextual compacto y accesible al final de una fila. */
public final class AccionesTabla {
    public record Accion<T>(String texto, Consumer<T> ejecutar, Iconos.Nombre icono) {
        public Accion(String texto, Consumer<T> ejecutar) {
            this(texto, ejecutar, null);
        }
    }

    private AccionesTabla() { }

    public static <T> void configurar(TableColumn<T, Void> columna, List<Accion<T>> acciones) {
        TableView<T> tabla = columna.getTableView();
        if (tabla != null) tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna.setCellFactory(new Callback<>() {
            @Override public TableCell<T, Void> call(TableColumn<T, Void> ignored) {
                return new TableCell<>() {
                    private final MenuButton menu = crearMenu();

                    {
                        getStyleClass().add("actions-cell");
                        setAlignment(Pos.CENTER);
                    }

                    private MenuButton crearMenu() {
                        MenuButton boton = new MenuButton();
                        boton.getStyleClass().add("row-actions");
                        Iconos.asignar(boton, Iconos.Nombre.ACCIONES, 14);
                        boton.setAccessibleText("Acciones de fila");
                        boton.setTooltip(new Tooltip("Acciones de fila"));
                        boton.setMinSize(36, 32);
                        boton.setPrefSize(36, 32);
                        boton.setOnShowing(evento -> {
                            T registro = getTableRow() == null ? null : getTableRow().getItem();
                            boton.getItems().clear();
                            if (registro == null) return;
                            for (Accion<T> accion : acciones) {
                                MenuItem item = new MenuItem(accion.texto());
                                if (accion.icono() != null) item.setGraphic(Iconos.crear(accion.icono(), 14));
                                item.setOnAction(accionEvento -> accion.ejecutar().accept(registro));
                                boton.getItems().add(item);
                            }
                        });
                        return boton;
                    }

                    @Override protected void updateItem(Void valor, boolean vacia) {
                        super.updateItem(valor, vacia);
                        setText(null);
                        setGraphic(vacia ? null : menu);
                    }
                };
            }
        });
        columna.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(null));
    }
}

