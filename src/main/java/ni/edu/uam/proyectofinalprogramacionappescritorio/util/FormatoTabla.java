package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Factories compartidas para fechas, cifras e insignias en TableView. */
public final class FormatoTabla {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatoTabla() { }

    public static <S> Callback<TableColumn<S, LocalDate>, TableCell<S, LocalDate>> fecha() {
        return columna -> new TableCell<>() {
            @Override protected void updateItem(LocalDate valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setText(vacia || valor == null ? null : FECHA.format(valor));
                setGraphic(null);
            }
        };
    }

    public static <S> Callback<TableColumn<S, Timestamp>, TableCell<S, Timestamp>> fechaHora() {
        return columna -> new TableCell<>() {
            @Override protected void updateItem(Timestamp valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setText(vacia || valor == null ? null : FECHA.format(valor.toLocalDateTime().toLocalDate()));
                setGraphic(null);
            }
        };
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> numerica() {
        return columna -> new TableCell<>() {
            {
                if (!getStyleClass().contains("col-numeric")) getStyleClass().add("col-numeric");
            }
            @Override protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setText(vacia ? null : valor);
                setGraphic(null);
            }
        };
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> estado() {
        return columna -> new TableCell<>() {
            @Override protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setText(null);
                setGraphic(vacia || valor == null ? null : InsigniaEstado.crear(valor));
            }
        };
    }
}

