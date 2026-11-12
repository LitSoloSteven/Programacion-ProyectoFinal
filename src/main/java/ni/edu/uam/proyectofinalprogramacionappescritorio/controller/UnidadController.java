package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de la vista de Unidades.
 * Consume datos reales de propiedades y unidades desde PostgreSQL mediante Services.
 */
public class UnidadController {

    @FXML
    private ComboBox<Propiedad> cmbPropiedad;

    @FXML
    private TextField txtCodigo;

    @FXML
    private TextField txtDescripcion;

    @FXML
    private ComboBox<String> cmbTipo;

    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private TextField txtCanon;

    @FXML
    private ComboBox<String> cmbMoneda;

    @FXML
    private Button btnEliminar;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableView<Unidad> tblUnidades;

    @FXML
    private TableColumn<Unidad, String> colPropiedad;

    @FXML
    private TableColumn<Unidad, String> colCodigo;

    @FXML
    private TableColumn<Unidad, String> colTipo;

    @FXML
    private TableColumn<Unidad, String> colEstado;

    @FXML
    private TableColumn<Unidad, String> colCanon;

    @FXML
    private TableColumn<Unidad, String> colDescripcion;

    private final UnidadService unidadService;
    private final PropiedadService propiedadService;

    private final ObservableList<Unidad> listaUnidades = FXCollections.observableArrayList();
    private FilteredList<Unidad> listaFiltrada;
    private Unidad unidadSeleccionada = null;

    public UnidadController() {
        this.unidadService = new UnidadService();
        this.propiedadService = new PropiedadService();
    }

    public UnidadController(UnidadService unidadService, PropiedadService propiedadService) {
        this.unidadService = unidadService;
        this.propiedadService = propiedadService;
    }

    @FXML
    public void initialize() {
        inicializarCombos();
        configurarTabla();
        configurarFiltro();
        aplicarControlPermisos();

        cargarPropiedadesReales();
        cargarDatos();

        tblUnidades.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarUnidad(newVal);
            }
        });
    }

    private void inicializarCombos() {
        cmbTipo.setItems(FXCollections.observableArrayList("Apartamento", "Casa", "Cuarto", "Local comercial"));
        cmbTipo.getSelectionModel().selectFirst();

        cmbEstado.setItems(FXCollections.observableArrayList("Disponible", "En mantenimiento", "Inactiva"));
        cmbEstado.getSelectionModel().selectFirst();

        cmbMoneda.setItems(FXCollections.observableArrayList("USD", "NIO"));
        cmbMoneda.getSelectionModel().selectFirst();
    }

    private void configurarTabla() {
        colPropiedad.setCellValueFactory(new PropertyValueFactory<>("propiedadNombre"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colCanon.setCellValueFactory(new PropertyValueFactory<>("canonFormateado"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        listaFiltrada = new FilteredList<>(listaUnidades, u -> true);
        tblUnidades.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            listaFiltrada.setPredicate(u -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filtro = newVal.toLowerCase().trim();
                boolean matchCod = u.getCodigo() != null && u.getCodigo().toLowerCase().contains(filtro);
                boolean matchProp = u.getPropiedadNombre() != null && u.getPropiedadNombre().toLowerCase().contains(filtro);
                boolean matchTipo = u.getTipo() != null && u.getTipo().toLowerCase().contains(filtro);
                boolean matchDescripcion = u.getDescripcion() != null && u.getDescripcion().toLowerCase().contains(filtro);
                return matchCod || matchProp || matchTipo || matchDescripcion;
            });
        });
    }

    private void aplicarControlPermisos() {
        boolean esAdmin = SesionUsuario.esPropietario();
        btnEliminar.setVisible(esAdmin);
        btnEliminar.setManaged(esAdmin);
    }

    public void cargarPropiedadesReales() {
        try {
            List<Propiedad> props = propiedadService.listarActivas();
            cmbPropiedad.setItems(FXCollections.observableArrayList(props));
            if (!props.isEmpty()) {
                cmbPropiedad.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al cargar propiedades", "No se pudieron obtener las propiedades reales de la base de datos.");
        }
    }

    public void cargarDatos() {
        try {
            List<Unidad> datos = unidadService.listarTodas();
            listaUnidades.setAll(datos);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al cargar unidades", "No fue posible consultar las unidades desde PostgreSQL.");
        }
    }

    @FXML
    public void refrescarTabla() {
        cargarPropiedadesReales();
        cargarDatos();
        limpiarFormulario();
        AlertaUtil.mostrarExito("Tabla actualizada", "El inventario de unidades ha sido sincronizado con PostgreSQL.");
    }

    @FXML
    public void handleGuardar() {
        Propiedad prop = cmbPropiedad.getValue();
        String codigo = txtCodigo.getText();
        String tipo = cmbTipo.getValue();
        String estado = cmbEstado.getValue();
        String strCanon = txtCanon.getText();
        String moneda = cmbMoneda.getValue();

        // Validaciones de formulario
        if (prop == null) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe seleccionar un inmueble para asociar la unidad.");
            cmbPropiedad.requestFocus();
            return;
        }

        if (codigo == null || codigo.trim().isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe ingresar el código o número de la unidad.");
            txtCodigo.requestFocus();
            return;
        }

        BigDecimal canon;
        try {
            canon = new BigDecimal(strCanon.trim());
            if (canon.compareTo(BigDecimal.ZERO) <= 0) {
                AlertaUtil.mostrarAdvertencia("Valor inválido", "El canon de referencia debe ser mayor a cero.");
                txtCanon.requestFocus();
                return;
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Formato numérico inválido", "Ingrese un valor decimal válido para el canon (ej. 450.00).");
            txtCanon.requestFocus();
            return;
        }

        try {
            if (unidadSeleccionada == null) {
                // Nueva unidad
                Unidad nueva = new Unidad();
                nueva.setPropiedadId(prop.getId());
                nueva.setCodigo(codigo.trim());
                nueva.setDescripcion(normalizarDescripcion(txtDescripcion.getText()));
                nueva.setTipo(tipo);
                nueva.setEstado(estado);
                nueva.setCanonReferencia(canon);
                nueva.setMoneda(moneda);

                unidadService.guardar(nueva);
                AlertaUtil.mostrarExito("Registro exitoso", "La unidad ha sido guardada satisfactoriamente en PostgreSQL.");
            } else {
                // Actualización
                unidadSeleccionada.setPropiedadId(prop.getId());
                unidadSeleccionada.setCodigo(codigo.trim());
                unidadSeleccionada.setDescripcion(normalizarDescripcion(txtDescripcion.getText()));
                unidadSeleccionada.setTipo(tipo);
                unidadSeleccionada.setEstado(estado);
                unidadSeleccionada.setCanonReferencia(canon);
                unidadSeleccionada.setMoneda(moneda);

                unidadService.guardar(unidadSeleccionada);
                AlertaUtil.mostrarExito("Actualización exitosa", "Los datos de la unidad fueron actualizados.");
            }

            cargarDatos();
            limpiarFormulario();
        } catch (IllegalArgumentException e) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de persistencia", "No fue posible guardar la unidad. Verifique que el código no esté duplicado en el mismo inmueble.");
        }
    }

    @FXML
    public void handleEliminar() {
        if (unidadSeleccionada == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione una unidad de la tabla para eliminar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de que desea eliminar la unidad '" + unidadSeleccionada.getCodigo() + "'?"
        );

        if (confirma) {
            try {
                unidadService.eliminar(unidadSeleccionada.getId());
                AlertaUtil.mostrarExito("Registro eliminado", "La unidad fue eliminada correctamente.");
                cargarDatos();
                limpiarFormulario();
            } catch (SecurityException e) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Restricción de integridad", "No es posible eliminar la unidad porque tiene contratos o cobros asociados.");
            }
        }
    }

    @FXML
    public void limpiarFormulario() {
        unidadSeleccionada = null;
        txtCodigo.clear();
        txtDescripcion.clear();
        txtCanon.clear();
        if (!cmbPropiedad.getItems().isEmpty()) {
            cmbPropiedad.getSelectionModel().selectFirst();
        } else {
            cmbPropiedad.getSelectionModel().clearSelection();
        }
        if (!cmbTipo.getItems().isEmpty()) cmbTipo.getSelectionModel().selectFirst();
        if (!cmbEstado.getItems().isEmpty()) cmbEstado.getSelectionModel().selectFirst();
        if (!cmbMoneda.getItems().isEmpty()) cmbMoneda.getSelectionModel().selectFirst();
        tblUnidades.getSelectionModel().clearSelection();
    }

    private void seleccionarUnidad(Unidad u) {
        this.unidadSeleccionada = u;
        txtCodigo.setText(u.getCodigo());
        txtDescripcion.setText(u.getDescripcion());
        txtCanon.setText(u.getCanonReferencia() != null ? u.getCanonReferencia().toPlainString() : "");
        cmbTipo.setValue(u.getTipo());
        cmbEstado.setValue(u.getEstado());
        cmbMoneda.setValue(u.getMoneda());

        // Seleccionar en ComboBox de Propiedad
        for (Propiedad p : cmbPropiedad.getItems()) {
            if (p.getId().equals(u.getPropiedadId())) {
                cmbPropiedad.setValue(p);
                break;
            }
        }
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) return null;
        String valor = descripcion.strip();
        if (valor.length() > 255) throw new IllegalArgumentException("La descripción no puede superar 255 caracteres.");
        return valor;
    }
}
