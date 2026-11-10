package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de la vista de Inquilinos.
 * Gestiona el ciclo de vida de los arrendatarios con persistencia en PostgreSQL 18.
 */
public class InquilinoController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtIdentificacion;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextField txtCorreo;

    @FXML
    private TextField txtContactoEmergencia;

    @FXML
    private CheckBox chkActivo;

    @FXML
    private Button btnDarBaja;

    @FXML
    private Button btnEliminar;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TableView<Inquilino> tblInquilinos;

    @FXML
    private TableColumn<Inquilino, String> colNombre;

    @FXML
    private TableColumn<Inquilino, String> colIdentificacion;

    @FXML
    private TableColumn<Inquilino, String> colTelefono;

    @FXML
    private TableColumn<Inquilino, String> colCorreo;

    @FXML
    private TableColumn<Inquilino, String> colEmergencia;

    @FXML
    private TableColumn<Inquilino, String> colEstado;

    private final InquilinoService inquilinoService;
    private final ObservableList<Inquilino> listaInquilinos = FXCollections.observableArrayList();
    private FilteredList<Inquilino> listaFiltrada;
    private Inquilino inquilinoSeleccionado = null;

    public InquilinoController() {
        this.inquilinoService = new InquilinoService();
    }

    public InquilinoController(InquilinoService inquilinoService) {
        this.inquilinoService = inquilinoService;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltro();
        aplicarControlPermisos();
        cargarDatos();

        tblInquilinos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                seleccionarInquilino(newVal);
            }
        });
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colIdentificacion.setCellValueFactory(new PropertyValueFactory<>("identificacion"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colEmergencia.setCellValueFactory(new PropertyValueFactory<>("contactoEmergencia"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoTexto"));

        listaFiltrada = new FilteredList<>(listaInquilinos, i -> true);
        tblInquilinos.setItems(listaFiltrada);
    }

    private void configurarFiltro() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            listaFiltrada.setPredicate(inq -> {
                if (newVal == null || newVal.isBlank()) return true;
                String filtro = newVal.toLowerCase().trim();
                boolean matchNom = inq.getNombre() != null && inq.getNombre().toLowerCase().contains(filtro);
                boolean matchId = inq.getIdentificacion() != null && inq.getIdentificacion().toLowerCase().contains(filtro);
                boolean matchTel = inq.getTelefono() != null && inq.getTelefono().toLowerCase().contains(filtro);
                return matchNom || matchId || matchTel;
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
            List<Inquilino> datos = inquilinoService.listarTodos();
            listaInquilinos.setAll(datos);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al cargar inquilinos", "No fue posible consultar los inquilinos desde PostgreSQL.");
        }
    }

    @FXML
    public void refrescarTabla() {
        cargarDatos();
        limpiarFormulario();
        AlertaUtil.mostrarExito("Tabla actualizada", "El listado de inquilinos ha sido sincronizado con PostgreSQL.");
    }

    @FXML
    public void handleGuardar() {
        String nombre = txtNombre.getText();
        String identificacion = txtIdentificacion.getText();
        String telefono = txtTelefono.getText();
        String correo = txtCorreo.getText();
        String emergencia = txtContactoEmergencia.getText();
        boolean activo = chkActivo.isSelected();

        // Validaciones previas
        if (nombre == null || nombre.trim().isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo obligatorio", "Debe ingresar el nombre completo del inquilino.");
            txtNombre.requestFocus();
            return;
        }

        try {
            if (inquilinoSeleccionado == null) {
                // Nuevo inquilino
                Inquilino nuevo = new Inquilino();
                nuevo.setNombre(nombre.trim());
                nuevo.setIdentificacion(identificacion != null && !identificacion.isBlank() ? identificacion.trim() : null);
                nuevo.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : null);
                nuevo.setCorreo(correo != null && !correo.isBlank() ? correo.trim() : null);
                nuevo.setContactoEmergencia(emergencia != null && !emergencia.isBlank() ? emergencia.trim() : null);
                nuevo.setActivo(activo);

                inquilinoService.guardar(nuevo);
                AlertaUtil.mostrarExito("Registro exitoso", "El inquilino ha sido registrado correctamente en PostgreSQL.");
            } else {
                // Edición
                inquilinoSeleccionado.setNombre(nombre.trim());
                inquilinoSeleccionado.setIdentificacion(identificacion != null && !identificacion.isBlank() ? identificacion.trim() : null);
                inquilinoSeleccionado.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : null);
                inquilinoSeleccionado.setCorreo(correo != null && !correo.isBlank() ? correo.trim() : null);
                inquilinoSeleccionado.setContactoEmergencia(emergencia != null && !emergencia.isBlank() ? emergencia.trim() : null);
                inquilinoSeleccionado.setActivo(activo);

                inquilinoService.guardar(inquilinoSeleccionado);
                AlertaUtil.mostrarExito("Actualización exitosa", "Los datos del inquilino fueron actualizados.");
            }

            cargarDatos();
            limpiarFormulario();
        } catch (IllegalArgumentException e) {
            AlertaUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de persistencia", "No fue posible guardar el registro del inquilino en PostgreSQL.");
        }
    }

    @FXML
    public void handleDarDeBaja() {
        if (inquilinoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un inquilino de la tabla para deshabilitar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar baja lógica",
                "¿Desea deshabilitar (marcar inactivo) al inquilino '" + inquilinoSeleccionado.getNombre() + "'?"
        );

        if (confirma) {
            try {
                inquilinoService.darDeBaja(inquilinoSeleccionado.getId());
                AlertaUtil.mostrarExito("Inquilino deshabilitado", "El inquilino ha quedado marcado como inactivo.");
                cargarDatos();
                limpiarFormulario();
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error", "No se pudo actualizar el estado del inquilino en la base de datos.");
            }
        }
    }

    @FXML
    public void handleEliminar() {
        if (inquilinoSeleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un inquilino de la tabla para eliminar.");
            return;
        }

        boolean confirma = AlertaUtil.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de que desea eliminar permanentemente al inquilino '" + inquilinoSeleccionado.getNombre() + "'?"
        );

        if (confirma) {
            try {
                inquilinoService.eliminar(inquilinoSeleccionado.getId());
                AlertaUtil.mostrarExito("Registro eliminado", "El inquilino fue eliminado físicamente de PostgreSQL.");
                cargarDatos();
                limpiarFormulario();
            } catch (SecurityException e) {
                AlertaUtil.mostrarAdvertencia("Permiso denegado", e.getMessage());
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Restricción de integridad", "No es posible eliminar el inquilino porque tiene contratos o cobros históricos asociados. Utilice 'Deshabilitar' en su lugar.");
            }
        }
    }

    @FXML
    public void limpiarFormulario() {
        inquilinoSeleccionado = null;
        txtNombre.clear();
        txtIdentificacion.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        txtContactoEmergencia.clear();
        chkActivo.setSelected(true);
        tblInquilinos.getSelectionModel().clearSelection();
    }

    private void seleccionarInquilino(Inquilino inq) {
        this.inquilinoSeleccionado = inq;
        txtNombre.setText(inq.getNombre());
        txtIdentificacion.setText(inq.getIdentificacion() != null ? inq.getIdentificacion() : "");
        txtTelefono.setText(inq.getTelefono() != null ? inq.getTelefono() : "");
        txtCorreo.setText(inq.getCorreo() != null ? inq.getCorreo() : "");
        txtContactoEmergencia.setText(inq.getContactoEmergencia() != null ? inq.getContactoEmergencia() : "");
        chkActivo.setSelected(inq.isActivo());
    }
}
