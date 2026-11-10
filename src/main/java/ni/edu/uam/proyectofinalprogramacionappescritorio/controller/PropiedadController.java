package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Controlador de la vista de Propiedad.
 * Conecta la interfaz con PropiedadService, valida entradas, gestiona confirmaciones
 * y actualiza la TableView con datos reales de PostgreSQL.
 */
public class PropiedadController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtDireccion;

    @FXML
    private CheckBox chkActiva;

    @FXML
    private Button btnEliminar;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableView<Propiedad> tblPropiedades;

    @FXML
    private TableColumn<Propiedad, String> colNombre;

    @FXML
    private TableColumn<Propiedad, String> colDireccion;

    @FXML
    private TableColumn<Propiedad, String> colEstado;

    @FXML
    private TableColumn<Propiedad, Timestamp> colFecha;

    private final PropiedadService propiedadService;
    private final ObservableList<Propiedad> listaPropiedades = FXCollections.observableArrayList();
    private FilteredList<Propiedad> listaFiltrada;

    private Propiedad propiedadSeleccionada = null;

    public PropiedadController() {
        this.propiedadService = new PropiedadService();
    }

    public PropiedadController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltro();
        aplicarControlPermisos();
        cargarDatos();

        tblPropiedades.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarPropiedad(newVal);
            }
        });
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoTexto"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("creadoEn"));

        listaFiltrada = new FilteredList<>(listaPropiedades, p -> true);
        tblPropiedades.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            listaFiltrada.setPredicate(prop -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filtro = newVal.toLowerCase().trim();
                boolean coincideNombre = prop.getNombre() != null && prop.getNombre().toLowerCase().contains(filtro);
                boolean coincideDireccion = prop.getDireccion() != null && prop.getDireccion().toLowerCase().contains(filtro);
                return coincideNombre || coincideDireccion;
            });
        });
    }

    private void aplicarControlPermisos() {
        boolean esAdmin = SesionUsuario.esPropietario();
        btnEliminar.setVisible(esAdmin);
        btnEliminar.setManaged(esAdmin);
    }

    public void cargarDatos() {
        try {
            List<Propiedad> datos = propiedadService.listarTodas();
            listaPropiedades.setAll(datos);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al cargar propiedades", "No fue posible consultar los inmuebles desde PostgreSQL.");
        }
    }

    @FXML
    public void refrescarTabla() {
        cargarDatos();
        limpiarFormulario();
        AlertaUtil.mostrarExito("Tabla actualizada", "Los datos de propiedades han sido sincronizados con PostgreSQL.");
    }

    @FXML
    public void handleGuardar() {
        String nombre = txtNombre.getText();
        String direccion = txtDireccion.getText();
        boolean activa = chkActiva.isSelected();

        // Validaciones previas
        if (nombre == null || nombre.trim().isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe ingresar el nombre del inmueble.");
            txtNombre.requestFocus();
            return;
        }

        if (direccion == null || direccion.trim().isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe ingresar la dirección completa de la propiedad.");
            txtDireccion.requestFocus();
            return;
        }

        try {
            if (propiedadSeleccionada == null) {
                // Registro nuevo
                Propiedad nueva = new Propiedad();
                nueva.setNombre(nombre.trim());
                nueva.setDireccion(direccion.trim());
                nueva.setActiva(activa);
                propiedadService.guardar(nueva);
                AlertaUtil.mostrarExito("Registro exitoso", "La propiedad ha sido registrada correctamente en PostgreSQL.");
            } else {
                // Edición
                propiedadSeleccionada.setNombre(nombre.trim());
                propiedadSeleccionada.setDireccion(direccion.trim());
                propiedadSeleccionada.setActiva(activa);
                propiedadService.guardar(propiedadSeleccionada);
                AlertaUtil.mostrarExito("Actualización exitosa", "Los datos de la propiedad han sido actualizados en PostgreSQL.");
            }

            cargarDatos();
            limpiarFormulario();
        } catch (IllegalArgumentException e) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de persistencia", "No fue posible guardar la propiedad en la base de datos.");
        }
    }

    @FXML
    public void handleEliminar() {
        if (propiedadSeleccionada == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione una propiedad de la tabla para eliminar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de que desea eliminar la propiedad '" + propiedadSeleccionada.getNombre() + "'? Esta acción afectará las unidades vinculadas."
        );

        if (confirma) {
            try {
                propiedadService.eliminar(propiedadSeleccionada.getId());
                AlertaUtil.mostrarExito("Registro eliminado", "La propiedad fue eliminada correctamente.");
                cargarDatos();
                limpiarFormulario();
            } catch (SecurityException e) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Restricción de integridad", "No es posible eliminar la propiedad porque tiene unidades o contratos asociados.");
            }
        }
    }

    @FXML
    public void limpiarFormulario() {
        propiedadSeleccionada = null;
        txtNombre.clear();
        txtDireccion.clear();
        chkActiva.setSelected(true);
        tblPropiedades.getSelectionModel().clearSelection();
    }

    private void seleccionarPropiedad(Propiedad p) {
        this.propiedadSeleccionada = p;
        txtNombre.setText(p.getNombre());
        txtDireccion.setText(p.getDireccion());
        chkActiva.setSelected(p.isActiva());
    }
}
