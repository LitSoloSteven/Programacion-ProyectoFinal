package ni.edu.uam.proyectofinalprogramacionappescritorio.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ni.edu.uam.proyectofinalprogramacionappescritorio.ComposicionAplicacion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.LatchApplication;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.AuditoriaRegistroDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoAdjuntoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.EstadoAlertaRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ParametroHistorialDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PerfilEmisorDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRestauracionDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoVerificacionRespaldoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.UsuarioResumenDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.MotorBaseRespaldo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RespaldoException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RespaldoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RestauracionException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.AlertaUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.DialogoFormulario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.Iconos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.TareasJavaFx;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Controlador de administración de cuentas, parámetros/emisor y auditoría (M01/M12). */
public class AdminController {
    private static final DateTimeFormatter FORMATO_INSTANTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private VBox rootView;
    @FXML private Label lblTitulo;

    @FXML private Button btnRefrescarUsuarios;
    @FXML private Button btnCrearUsuario;
    @FXML private Button btnCambiarRol;
    @FXML private Button btnActivarDesactivar;
    @FXML private Button btnRestablecerClave;
    @FXML private Label lblErrorUsuarios;
    @FXML private TableView<UsuarioResumenDTO> tblUsuarios;
    @FXML private TableColumn<UsuarioResumenDTO, String> colUsuarioNombre;
    @FXML private TableColumn<UsuarioResumenDTO, String> colUsuarioRol;
    @FXML private TableColumn<UsuarioResumenDTO, String> colUsuarioEstado;
    @FXML private TableColumn<UsuarioResumenDTO, String> colUsuarioCambioClave;

    @FXML private TextField txtEmisorNombre;
    @FXML private TextField txtEmisorIdentificacion;
    @FXML private TextField txtEmisorDireccion;
    @FXML private TextField txtEmisorTelefono;
    @FXML private TextField txtEmisorCorreo;
    @FXML private TextField txtEmisorMotivo;
    @FXML private Button btnGuardarEmisor;
    @FXML private Label lblEstadoEmisor;
    @FXML private Label lblLogotipoEstado;
    @FXML private Button btnAdjuntarLogotipo;
    @FXML private Button btnQuitarLogotipo;

    @FXML private TextField txtParamClave;
    @FXML private TextField txtParamValor;
    @FXML private TextField txtParamMotivo;
    @FXML private Button btnRegistrarParametro;
    @FXML private Button btnRefrescarParametros;
    @FXML private Label lblErrorParametro;
    @FXML private TableView<ParametroHistorialDTO> tblParametros;
    @FXML private TableColumn<ParametroHistorialDTO, String> colParamClave;
    @FXML private TableColumn<ParametroHistorialDTO, String> colParamValor;
    @FXML private TableColumn<ParametroHistorialDTO, String> colParamVigenteDesde;
    @FXML private TableColumn<ParametroHistorialDTO, String> colParamMotivo;
    @FXML private TableColumn<ParametroHistorialDTO, String> colParamCreadoPor;

    @FXML private TextField txtAuditoriaUsuario;
    @FXML private TextField txtAuditoriaAccion;
    @FXML private DatePicker dpAuditoriaDesde;
    @FXML private DatePicker dpAuditoriaHasta;
    @FXML private Button btnConsultarAuditoria;
    @FXML private Label lblErrorAuditoria;
    @FXML private TableView<AuditoriaRegistroDTO> tblAuditoria;
    @FXML private TableColumn<AuditoriaRegistroDTO, String> colAuditoriaInstante;
    @FXML private TableColumn<AuditoriaRegistroDTO, String> colAuditoriaActor;
    @FXML private TableColumn<AuditoriaRegistroDTO, String> colAuditoriaAccion;
    @FXML private TableColumn<AuditoriaRegistroDTO, String> colAuditoriaReferencia;
    @FXML private TableColumn<AuditoriaRegistroDTO, String> colAuditoriaCorrelacion;

    @FXML private Label lblEstadoRespaldo;
    @FXML private Button btnRefrescarRespaldo;
    @FXML private TextField txtCarpetaRespaldo;
    @FXML private Button btnElegirCarpetaRespaldo;
    @FXML private PasswordField pwdClaveRespaldo;
    @FXML private PasswordField pwdConfirmarClaveRespaldo;
    @FXML private Button btnCrearRespaldo;
    @FXML private TextField txtArchivoRespaldo;
    @FXML private Button btnElegirArchivoRespaldo;
    @FXML private PasswordField pwdClaveRestaurar;
    @FXML private Button btnVerificarRespaldo;
    @FXML private TextField txtDestinoJdbcRestaurar;
    @FXML private TextField txtDestinoUsuarioRestaurar;
    @FXML private PasswordField pwdDestinoClaveRestaurar;
    @FXML private TextField txtDestinoAdjuntosRestaurar;
    @FXML private Button btnElegirDestinoAdjuntos;
    @FXML private CheckBox chkModoMantenimiento;
    @FXML private Button btnRestaurarAislado;
    @FXML private Label lblErrorRespaldo;
    @FXML private Label lblResultadoRespaldo;

    private final ObservableList<UsuarioResumenDTO> usuarios = FXCollections.observableArrayList();
    private final ObservableList<ParametroHistorialDTO> parametros = FXCollections.observableArrayList();
    private final ObservableList<AuditoriaRegistroDTO> auditorias = FXCollections.observableArrayList();

    private DocumentoAdjuntoDTO logotipoVigente;

    private record DatosParametros(Optional<PerfilEmisorDTO> emisor,
                                  Optional<DocumentoAdjuntoDTO> logotipo,
                                  List<ParametroHistorialDTO> historial) { }

    @FXML
    public void initialize() {
        lblTitulo.setGraphic(Iconos.crear(Iconos.Nombre.PANEL, 20));
        Iconos.asignar(btnRefrescarUsuarios, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnCrearUsuario, Iconos.Nombre.MAS, 16);
        Iconos.asignar(btnCambiarRol, Iconos.Nombre.EDITAR, 16);
        Iconos.asignar(btnRefrescarParametros, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnConsultarAuditoria, Iconos.Nombre.BUSCAR, 16);
        Iconos.asignar(btnRefrescarRespaldo, Iconos.Nombre.ACTUALIZAR, 16);
        Iconos.asignar(btnCrearRespaldo, Iconos.Nombre.DESCARGAR, 16);
        Iconos.asignar(btnVerificarRespaldo, Iconos.Nombre.CONFIRMACION, 16);

        tblUsuarios.setItems(usuarios);
        colUsuarioNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colUsuarioRol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().rol()));
        colUsuarioEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activo() ? "Activo" : "Inactivo"));
        colUsuarioCambioClave.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().debeCambiarPassword() ? "Pendiente" : "Vigente"));
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, ant, actual) -> {
            boolean haySeleccion = actual != null;
            btnCambiarRol.setDisable(!haySeleccion);
            btnActivarDesactivar.setDisable(!haySeleccion);
            btnRestablecerClave.setDisable(!haySeleccion);
            if (haySeleccion) {
                btnActivarDesactivar.setText(actual.activo() ? "Desactivar" : "Activar");
            }
        });

        tblParametros.setItems(parametros);
        colParamClave.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().clave()));
        colParamValor.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().valor()));
        colParamVigenteDesde.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearInstante(c.getValue().vigenteDesde())));
        colParamMotivo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().motivo()));
        colParamCreadoPor.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().creadoPor()));

        tblAuditoria.setItems(auditorias);
        colAuditoriaInstante.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatearInstante(c.getValue().instante())));
        colAuditoriaActor.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().actor() == null ? "Sistema / Anónimo" : c.getValue().actor()));
        colAuditoriaAccion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().accion()));
        colAuditoriaReferencia.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().referencia()));
        colAuditoriaCorrelacion.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().correlacion() == null ? "" : c.getValue().correlacion().toString()));

        refrescarUsuarios();
        refrescarParametros();
        consultarAuditoria();
        refrescarEstadoRespaldo();
    }

    @FXML
    public void refrescarUsuarios() {
        ocultarError(lblErrorUsuarios);
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnRefrescarUsuarios.setDisable(true);
        Task<List<UsuarioResumenDTO>> tarea = new Task<>() {
            @Override protected List<UsuarioResumenDTO> call() {
                return comp.usuarios().listar(identidad);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, lista -> {
            usuarios.setAll(lista);
            btnRefrescarUsuarios.setDisable(false);
        }, error -> {
            btnRefrescarUsuarios.setDisable(false);
            mostrarError(lblErrorUsuarios, mensajeSeguro(error, "No se pudieron consultar los usuarios."));
        });
    }

    @FXML
    public void handleCrearUsuario() {
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("usuario.nuevo");
        PasswordField txtClave = new PasswordField();
        txtClave.setPromptText("Mínimo 12 caracteres");
        ComboBox<String> cmbRol = new ComboBox<>(FXCollections.observableArrayList(Rol.ASISTENTE, Rol.PROPIETARIO));
        cmbRol.getSelectionModel().selectFirst();
        Label lblErr = new Label();
        lblErr.getStyleClass().add("field-error");
        lblErr.setWrapText(true);
        lblErr.setVisible(false);
        lblErr.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Usuario:"), txtNombre);
        grid.addRow(1, new Label("Contraseña:"), txtClave);
        grid.addRow(2, new Label("Rol:"), cmbRol);
        grid.add(lblErr, 0, 3, 2, 1);

        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Nuevo usuario", grid, ventanaActual(), 460, 360, "Crear usuario");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            String nombre = txtNombre.getText();
            String clave = txtClave.getText();
            String rol = cmbRol.getValue();
            SesionUsuario identidad = SesionUsuario.getActual();
            ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
            accion.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    comp.usuarios().crear(identidad, nombre, clave == null ? new char[0] : clave.toCharArray(), rol);
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                txtClave.clear();
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Usuario creado", "La cuenta quedó registrada.");
                refrescarUsuarios();
            }, error -> {
                accion.setDisable(false);
                mostrarError(lblErr, mensajeSeguro(error, "No se pudo crear la cuenta."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void handleCambiarRol() {
        UsuarioResumenDTO sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String nuevoRol = Rol.PROPIETARIO.equals(sel.rol()) ? Rol.ASISTENTE : Rol.PROPIETARIO;
        if (!AlertaUtil.mostrarConfirmacion("Cambiar rol",
                "¿Cambiar el rol de '" + sel.nombre() + "' a " + nuevoRol + "? Las sesiones activas de esa cuenta serán revocadas.")) {
            return;
        }
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                comp.usuarios().cambiarRol(identidad, sel.id(), nuevoRol);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            AlertaUtil.mostrarExito("Rol actualizado", "El rol cambió a " + nuevoRol + ".");
            refrescarUsuarios();
        }, error -> AlertaUtil.mostrarError("No se pudo cambiar el rol",
                mensajeSeguro(error, "No fue posible cambiar el rol.")));
    }

    @FXML
    public void handleAlternarActivo() {
        UsuarioResumenDTO sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean activar = !sel.activo();
        String titulo = activar ? "Activar cuenta" : "Desactivar cuenta";
        if (!AlertaUtil.mostrarConfirmacion(titulo,
                "¿Confirmas " + (activar ? "activar" : "desactivar") + " la cuenta '" + sel.nombre() + "'?")) {
            return;
        }
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                if (activar) comp.usuarios().activar(identidad, sel.id());
                else comp.usuarios().desactivar(identidad, sel.id());
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            AlertaUtil.mostrarExito("Cuenta actualizada", activar ? "La cuenta quedó activa." : "La cuenta quedó desactivada.");
            refrescarUsuarios();
        }, error -> AlertaUtil.mostrarError("No se pudo actualizar la cuenta",
                mensajeSeguro(error, "No fue posible actualizar la cuenta.")));
    }

    @FXML
    public void handleRestablecerPassword() {
        UsuarioResumenDTO sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        PasswordField txtTemporal = new PasswordField();
        txtTemporal.setPromptText("Contraseña temporal (12–128 caracteres)");
        Label lblErr = new Label();
        lblErr.getStyleClass().add("field-error");
        lblErr.setWrapText(true);
        lblErr.setVisible(false);
        lblErr.setManaged(false);

        VBox box = new VBox(8, new Label("Cuenta: " + sel.nombre()), txtTemporal, lblErr);
        box.setPadding(new Insets(8));
        Dialog<ButtonType> dialogo = DialogoFormulario.crear(
                "Restablecer contraseña", box, ventanaActual(), 440, 260, "Restablecer");
        Button accion = (Button) dialogo.getDialogPane().lookupButton(dialogo.getDialogPane().getButtonTypes().getFirst());
        accion.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume();
            String temporal = txtTemporal.getText();
            SesionUsuario identidad = SesionUsuario.getActual();
            ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
            accion.setDisable(true);
            Task<Void> tarea = new Task<>() {
                @Override protected Void call() {
                    comp.usuarios().restablecerPassword(identidad, sel.id(),
                            temporal == null ? new char[0] : temporal.toCharArray());
                    return null;
                }
            };
            TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
                txtTemporal.clear();
                DialogoFormulario.cerrar(dialogo);
                AlertaUtil.mostrarExito("Contraseña restablecida", "Se marcó cambio de contraseña pendiente.");
                refrescarUsuarios();
            }, error -> {
                accion.setDisable(false);
                mostrarError(lblErr, mensajeSeguro(error, "No se pudo restablecer la contraseña."));
            });
        });
        dialogo.showAndWait();
    }

    @FXML
    public void refrescarParametros() {
        ocultarError(lblErrorParametro);
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnRefrescarParametros.setDisable(true);
        Task<DatosParametros> tarea = new Task<>() {
            @Override protected DatosParametros call() {
                return new DatosParametros(
                        comp.configuracion().consultarPerfilEmisorVigente(identidad),
                        comp.adjuntos().obtenerLogotipoVigente(identidad),
                        comp.configuracion().listarHistorialParametros(identidad, null));
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, datos -> {
            btnRefrescarParametros.setDisable(false);
            parametros.setAll(datos.historial());
            logotipoVigente = datos.logotipo().orElse(null);
            if (logotipoVigente != null) {
                lblLogotipoEstado.setText("Logotipo: " + logotipoVigente.nombreOriginal());
                btnQuitarLogotipo.setDisable(false);
            } else {
                lblLogotipoEstado.setText("Sin logotipo configurado");
                btnQuitarLogotipo.setDisable(true);
            }
            datos.emisor().ifPresentOrElse(emisor -> {
                txtEmisorNombre.setText(emisor.nombre());
                txtEmisorIdentificacion.setText(emisor.identificacion());
                txtEmisorDireccion.setText(emisor.direccion());
                txtEmisorTelefono.setText(emisor.telefono());
                txtEmisorCorreo.setText(emisor.correo());
                lblEstadoEmisor.setText("Emisor vigente para nuevos recibos: " + emisor.resumenVisible());
            }, () -> lblEstadoEmisor.setText("Sin perfil emisor configurado; los recibos mostrarán el usuario emisor."));
        }, error -> {
            btnRefrescarParametros.setDisable(false);
            mostrarError(lblErrorParametro, mensajeSeguro(error, "No se pudo cargar la configuración."));
        });
    }

    @FXML
    public void handleAdjuntarLogotipo() {
        ocultarError(lblErrorParametro);
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar logotipo del emisor");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG o JPEG", "*.png", "*.jpg", "*.jpeg"));
        java.io.File archivo = fc.showOpenDialog(ventanaActual());
        if (archivo == null) return;
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnAdjuntarLogotipo.setDisable(true);
        Task<DocumentoAdjuntoDTO> tarea = new Task<>() {
            @Override protected DocumentoAdjuntoDTO call() {
                return comp.adjuntos().adjuntarLogotipoEmisor(identidad, archivo.toPath());
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            btnAdjuntarLogotipo.setDisable(false);
            AlertaUtil.mostrarExito("Logotipo guardado", "El logotipo quedó vinculado al perfil emisor vigente.");
            refrescarParametros();
        }, error -> {
            btnAdjuntarLogotipo.setDisable(false);
            mostrarError(lblErrorParametro, mensajeSeguro(error, "No se pudo adjuntar el logotipo."));
        });
    }

    @FXML
    public void handleQuitarLogotipo() {
        if (logotipoVigente == null) return;
        ocultarError(lblErrorParametro);
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        java.util.UUID adjuntoId = logotipoVigente.id();
        btnQuitarLogotipo.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                comp.adjuntos().desactivar(identidad, adjuntoId);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            AlertaUtil.mostrarExito("Logotipo quitado", "Se desactivó el logotipo del emisor.");
            refrescarParametros();
        }, error -> {
            btnQuitarLogotipo.setDisable(false);
            mostrarError(lblErrorParametro, mensajeSeguro(error, "No se pudo quitar el logotipo."));
        });
    }

    @FXML
    public void handleGuardarEmisor() {
        ocultarError(lblErrorParametro);
        PerfilEmisorDTO perfil = new PerfilEmisorDTO(
                txtEmisorNombre.getText(), txtEmisorIdentificacion.getText(),
                txtEmisorDireccion.getText(), txtEmisorTelefono.getText(), txtEmisorCorreo.getText());
        String motivo = txtEmisorMotivo.getText();
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnGuardarEmisor.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                comp.configuracion().guardarPerfilEmisor(identidad, perfil, motivo);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            btnGuardarEmisor.setDisable(false);
            txtEmisorMotivo.clear();
            AlertaUtil.mostrarExito("Perfil emisor guardado", "Los nuevos recibos usarán este perfil emisor.");
            refrescarParametros();
        }, error -> {
            btnGuardarEmisor.setDisable(false);
            mostrarError(lblErrorParametro, mensajeSeguro(error, "No se pudo guardar el perfil emisor."));
        });
    }

    @FXML
    public void handleRegistrarParametro() {
        ocultarError(lblErrorParametro);
        String clave = txtParamClave.getText();
        String valor = txtParamValor.getText();
        String motivo = txtParamMotivo.getText();
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnRegistrarParametro.setDisable(true);
        Task<Void> tarea = new Task<>() {
            @Override protected Void call() {
                comp.configuracion().registrarParametro(identidad, clave, valor, null, motivo);
                return null;
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ok -> {
            btnRegistrarParametro.setDisable(false);
            txtParamClave.clear();
            txtParamValor.clear();
            txtParamMotivo.clear();
            AlertaUtil.mostrarExito("Parámetro registrado", "Se guardó la nueva vigencia en el historial.");
            refrescarParametros();
        }, error -> {
            btnRegistrarParametro.setDisable(false);
            mostrarError(lblErrorParametro, mensajeSeguro(error, "No se pudo registrar el parámetro."));
        });
    }

    @FXML
    public void consultarAuditoria() {
        ocultarError(lblErrorAuditoria);
        String usuarioFiltro = txtAuditoriaUsuario.getText();
        String accionFiltro = txtAuditoriaAccion.getText();
        LocalDate dDesde = dpAuditoriaDesde.getValue();
        LocalDate dHasta = dpAuditoriaHasta.getValue();
        Instant desde = dDesde == null ? null : dDesde.atStartOfDay(RelojService.ZONA_OPERATIVA).toInstant();
        Instant hasta = dHasta == null ? null
                : dHasta.plusDays(1).atStartOfDay(RelojService.ZONA_OPERATIVA).minusNanos(1).toInstant();
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnConsultarAuditoria.setDisable(true);
        Task<List<AuditoriaRegistroDTO>> tarea = new Task<>() {
            @Override protected List<AuditoriaRegistroDTO> call() {
                return comp.auditoria().consultar(identidad, usuarioFiltro, accionFiltro, desde, hasta, 200);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, lista -> {
            btnConsultarAuditoria.setDisable(false);
            auditorias.setAll(lista);
        }, error -> {
            btnConsultarAuditoria.setDisable(false);
            mostrarError(lblErrorAuditoria, mensajeSeguro(error, "No se pudo consultar la auditoría."));
        });
    }

    @FXML
    public void refrescarEstadoRespaldo() {
        ocultarError(lblErrorRespaldo);
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnRefrescarRespaldo.setDisable(true);
        Task<EstadoAlertaRespaldoDTO> tarea = new Task<>() {
            @Override protected EstadoAlertaRespaldoDTO call() {
                return comp.respaldos().consultarEstadoAlerta(identidad);
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, estado -> {
            btnRefrescarRespaldo.setDisable(false);
            lblEstadoRespaldo.setText(estado.mensaje());
        }, error -> {
            btnRefrescarRespaldo.setDisable(false);
            lblEstadoRespaldo.setText("No se pudo consultar el estado del último respaldo.");
            mostrarError(lblErrorRespaldo, mensajeSeguro(error, "No se pudo consultar el estado del respaldo."));
        });
    }

    @FXML
    public void handleElegirCarpetaRespaldo() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta de destino del respaldo");
        File carpeta = dc.showDialog(ventanaActual());
        if (carpeta != null) {
            txtCarpetaRespaldo.setText(carpeta.getAbsolutePath());
        }
    }

    @FXML
    public void handleElegirArchivoRespaldo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar paquete de respaldo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Paquete de respaldo Latch (*" + RespaldoService.EXTENSION_RESPALDO + ")",
                "*" + RespaldoService.EXTENSION_RESPALDO));
        File archivo = fc.showOpenDialog(ventanaActual());
        if (archivo != null) {
            txtArchivoRespaldo.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    public void handleElegirDestinoAdjuntos() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Seleccionar carpeta destino aislada para adjuntos");
        File carpeta = dc.showDialog(ventanaActual());
        if (carpeta != null) {
            txtDestinoAdjuntosRestaurar.setText(carpeta.getAbsolutePath());
        }
    }

    @FXML
    public void handleCrearRespaldo() {
        ocultarError(lblErrorRespaldo);
        ocultarError(lblResultadoRespaldo);
        String rutaDestino = txtCarpetaRespaldo.getText();
        if (rutaDestino == null || rutaDestino.isBlank()) {
            mostrarError(lblErrorRespaldo, "Selecciona la carpeta de destino del respaldo.");
            return;
        }
        String claveTexto = pwdClaveRespaldo.getText();
        String confirmacionTexto = pwdConfirmarClaveRespaldo.getText();
        if (claveTexto == null || !claveTexto.equals(confirmacionTexto)) {
            mostrarError(lblErrorRespaldo, "La contraseña de respaldo y su confirmación no coinciden.");
            return;
        }
        char[] clave = claveTexto.toCharArray();
        pwdClaveRespaldo.clear();
        pwdConfirmarClaveRespaldo.clear();

        Path destino = Path.of(rutaDestino.strip());
        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnCrearRespaldo.setDisable(true);
        Task<ResultadoRespaldoDTO> tarea = new Task<>() {
            @Override protected ResultadoRespaldoDTO call() {
                try {
                    return comp.respaldos().crearRespaldo(identidad, destino, clave);
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, resultado -> {
            btnCrearRespaldo.setDisable(false);
            txtArchivoRespaldo.setText(resultado.archivo().toString());
            mostrarError(lblResultadoRespaldo,
                    "Respaldo generado y verificado: " + resultado.archivo().getFileName()
                            + " (" + resultado.cantidadAdjuntos() + " adjuntos, SHA-256: "
                            + resultado.sha256Paquete().substring(0, 16) + "…, " + resultado.duracionMs() + " ms).");
            refrescarEstadoRespaldo();
        }, error -> {
            btnCrearRespaldo.setDisable(false);
            mostrarError(lblErrorRespaldo, mensajeSeguro(error, "No se pudo generar el respaldo."));
        });
    }

    @FXML
    public void handleVerificarRespaldo() {
        ocultarError(lblErrorRespaldo);
        ocultarError(lblResultadoRespaldo);
        String rutaArchivo = txtArchivoRespaldo.getText();
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            mostrarError(lblErrorRespaldo, "Selecciona el archivo de respaldo a verificar.");
            return;
        }
        String claveTexto = pwdClaveRestaurar.getText();
        char[] clave = claveTexto == null ? new char[0] : claveTexto.toCharArray();
        pwdClaveRestaurar.clear();

        Path archivo = Path.of(rutaArchivo.strip());
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnVerificarRespaldo.setDisable(true);
        Task<ResultadoVerificacionRespaldoDTO> tarea = new Task<>() {
            @Override protected ResultadoVerificacionRespaldoDTO call() {
                try {
                    return comp.respaldos().verificarPaquete(archivo, clave);
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, ver -> {
            btnVerificarRespaldo.setDisable(false);
            mostrarError(lblResultadoRespaldo,
                    "Paquete auténtico e íntegro: corte " + ver.manifiesto().corteLocal()
                            + ", esquema " + ver.manifiesto().esquemaVersion()
                            + ", " + ver.adjuntosVerificados() + " adjuntos verificados, último recibo #"
                            + ver.manifiesto().ultimoNumeroRecibo() + ".");
        }, error -> {
            btnVerificarRespaldo.setDisable(false);
            mostrarError(lblErrorRespaldo, mensajeSeguro(error, "No se pudo verificar el respaldo."));
        });
    }

    @FXML
    public void handleRestaurarAislado() {
        ocultarError(lblErrorRespaldo);
        ocultarError(lblResultadoRespaldo);
        if (!chkModoMantenimiento.isSelected()) {
            mostrarError(lblErrorRespaldo,
                    "ERR-058: Activa la casilla de modo mantenimiento sin sesiones operativas antes de restaurar.");
            return;
        }
        String rutaArchivo = txtArchivoRespaldo.getText();
        String jdbcUrl = txtDestinoJdbcRestaurar.getText();
        String usuarioDestino = txtDestinoUsuarioRestaurar.getText();
        String rutaAdjuntos = txtDestinoAdjuntosRestaurar.getText();
        if (rutaArchivo == null || rutaArchivo.isBlank()
                || jdbcUrl == null || jdbcUrl.isBlank()
                || usuarioDestino == null || usuarioDestino.isBlank()
                || rutaAdjuntos == null || rutaAdjuntos.isBlank()) {
            mostrarError(lblErrorRespaldo,
                    "Completa el paquete de respaldo, la URL JDBC aislada, el usuario destino y la carpeta de adjuntos.");
            return;
        }
        if (!AlertaUtil.mostrarConfirmacion("Restaurar en entorno aislado",
                "Se creará una copia previa antes de restaurar el paquete en el destino aislado indicado. ¿Deseas continuar?")) {
            return;
        }

        String claveRespaldoTexto = pwdClaveRestaurar.getText();
        String claveDestinoTexto = pwdDestinoClaveRestaurar.getText();
        char[] claveRespaldo = claveRespaldoTexto == null ? new char[0] : claveRespaldoTexto.toCharArray();
        char[] claveDestino = claveDestinoTexto == null ? new char[0] : claveDestinoTexto.toCharArray();
        pwdClaveRestaurar.clear();
        pwdDestinoClaveRestaurar.clear();

        Path paquete = Path.of(rutaArchivo.strip());
        Path carpetaAdjuntos = Path.of(rutaAdjuntos.strip());
        Path carpetaCopiaPrevia = paquete.toAbsolutePath().normalize().getParent();
        MotorBaseRespaldo.ConexionJdbc destinoJdbc = new MotorBaseRespaldo.ConexionJdbc(
                jdbcUrl.strip(), usuarioDestino.strip(), claveDestino);
        Arrays.fill(claveDestino, '\0');

        SesionUsuario identidad = SesionUsuario.getActual();
        ComposicionAplicacion comp = LatchApplication.instancia().getComposicion();
        btnRestaurarAislado.setDisable(true);
        Task<ResultadoRestauracionDTO> tarea = new Task<>() {
            @Override protected ResultadoRestauracionDTO call() {
                try {
                    return comp.respaldos().restaurarEnEntornoAislado(new RespaldoService.SolicitudRestauracion(
                            identidad, paquete, claveRespaldo, destinoJdbc, carpetaAdjuntos,
                            carpetaCopiaPrevia, true, false, false));
                } finally {
                    Arrays.fill(claveRespaldo, '\0');
                }
            }
        };
        TareasJavaFx.ejecutar(LatchApplication.instancia().getTareas(), tarea, res -> {
            btnRestaurarAislado.setDisable(false);
            mostrarError(lblResultadoRespaldo,
                    "Restauración aislada completada: " + res.adjuntosVerificados()
                            + " adjuntos verificados, " + res.hashesDiscordantes()
                            + " hashes discordantes (" + res.duracionMs() + " ms). Registro: "
                            + res.registroExterno().getFileName());
        }, error -> {
            btnRestaurarAislado.setDisable(false);
            mostrarError(lblErrorRespaldo, mensajeSeguro(error, "No se pudo restaurar el respaldo en destino aislado."));
        });
    }

    private Window ventanaActual() {
        return rootView != null && rootView.getScene() != null ? rootView.getScene().getWindow() : null;
    }

    private static String formatearInstante(Instant instante) {
        if (instante == null) return "—";
        return FORMATO_INSTANTE.format(instante.atZone(RelojService.ZONA_OPERATIVA));
    }

    private static void mostrarError(Label label, String mensaje) {
        label.setText(mensaje);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void ocultarError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private static String mensajeSeguro(Throwable error, String respaldo) {
        if (error instanceof IllegalArgumentException || error instanceof SecurityException
                || error instanceof IllegalStateException || error instanceof ConflictoDatosException
                || error instanceof RespaldoException || error instanceof RestauracionException) {
            return error.getMessage();
        }
        return respaldo;
    }
}
