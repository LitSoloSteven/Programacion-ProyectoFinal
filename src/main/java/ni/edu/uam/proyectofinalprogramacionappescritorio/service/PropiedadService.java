package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class PropiedadService {
    private final HibernateCrudService db;
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();

    public PropiedadService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public PropiedadService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
        this.db = new HibernateCrudService(transactions);
    }

    public List<Propiedad> listarTodas() throws SQLException {
        return db.listar(Propiedad.class,
                "from Propiedad p left join fetch p.propietario order by p.nombre");
    }

    public List<Propiedad> listarActivas() throws SQLException {
        return db.listar(Propiedad.class,
                "from Propiedad p left join fetch p.propietario where p.activa=true order by p.nombre");
    }

    public void guardar(Propiedad propiedad, String nombrePropietario,
                        String correoPropietario, String telefonoPropietario) throws SQLException {
        validarPropiedad(propiedad);
        validarDatosPropietario(nombrePropietario, correoPropietario, telefonoPropietario);
        SesionUsuario sesion = SesionUsuario.getActual();
        if (sesion == null) {
            throw new SecurityException("Inicie sesión para guardar una propiedad.");
        }

        transactions.ejecutar(session -> {
            Usuario usuario = permisos.exigir(session, sesion, Permiso.OPERAR_ALQUILERES);
            if (propiedad.getId() == null) {
                Propietario propietario = guardarPerfilPropietario(session, usuario,
                        nombrePropietario, correoPropietario, telefonoPropietario);
                propiedad.setPropietario(propietario);
                session.persist(propiedad);
                return null;
            }

            Propiedad existente = session.find(Propiedad.class, propiedad.getId());
            if (existente == null) {
                throw new IllegalArgumentException("La propiedad ya no existe.");
            }
            if (propiedad.getVersion() != existente.getVersion()) {
                throw new ConflictoDatosException(
                        "Otro usuario modificó el registro. Actualiza los datos antes de reintentar.");
            }

            cambiarPropietarioSiFueSolicitado(session, usuario, existente,
                    propiedad.getPropietario());
            if (esPropietarioActual(usuario, existente.getPropietario())) {
                guardarPerfilPropietario(session, usuario, nombrePropietario,
                        correoPropietario, telefonoPropietario);
            }

            existente.setNombre(propiedad.getNombre().strip());
            existente.setDireccion(propiedad.getDireccion().strip());
            existente.setActiva(propiedad.isActiva());
            return null;
        });
    }

    private static void validarPropiedad(Propiedad propiedad) {
        if (propiedad == null || propiedad.getNombre() == null || propiedad.getNombre().isBlank()
                || propiedad.getDireccion() == null || propiedad.getDireccion().isBlank()) {
            throw new IllegalArgumentException("Nombre y dirección son obligatorios.");
        }
    }

    private static void validarDatosPropietario(String nombre, String correo, String telefono) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100
                || correo == null || correo.strip().length() > 150
                || !correo.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || telefono != null && telefono.strip().length() > 20) {
            throw new IllegalArgumentException(
                    "Ingrese un nombre (máximo 100 caracteres), un correo válido "
                            + "(máximo 150) y un teléfono de hasta 20 caracteres.");
        }
    }

    private static Propietario guardarPerfilPropietario(Session session, Usuario usuario,
                                                        String nombre, String correo,
                                                        String telefono) {
        Propietario propietario = session.find(Propietario.class, usuario.getId());
        if (propietario == null) {
            propietario = new Propietario(usuario, nombre.strip(),
                    correo.strip().toLowerCase(Locale.ROOT), limpiarTelefono(telefono));
            session.persist(propietario);
        } else {
            propietario.setNombre(nombre.strip());
            propietario.setCorreo(correo.strip().toLowerCase(Locale.ROOT));
            propietario.setTelefono(limpiarTelefono(telefono));
        }
        return propietario;
    }

    private static void cambiarPropietarioSiFueSolicitado(Session session, Usuario usuario,
                                                            Propiedad existente,
                                                            Propietario solicitado) {
        if (solicitado == null) return;
        if (solicitado.getId() == null) {
            throw new IllegalArgumentException("El propietario seleccionado no es válido.");
        }
        Long propietarioActualId = existente.getPropietario() == null
                ? null : existente.getPropietario().getId();
        if (Objects.equals(propietarioActualId, solicitado.getId())) return;
        if (!Rol.PROPIETARIO.equals(usuario.getRol())) {
            throw new SecurityException("Solo el Propietario puede reasignar el dueño de una propiedad.");
        }

        Propietario nuevoPropietario = session.find(Propietario.class, solicitado.getId());
        if (nuevoPropietario == null || nuevoPropietario.getUsuario() == null
                || !nuevoPropietario.getUsuario().isActivo()
                || !Rol.PROPIETARIO.equals(nuevoPropietario.getUsuario().getRol())) {
            throw new IllegalArgumentException("El propietario seleccionado no está disponible.");
        }
        existente.setPropietario(nuevoPropietario);
    }

    private static boolean esPropietarioActual(Usuario usuario, Propietario propietario) {
        return Rol.PROPIETARIO.equals(usuario.getRol()) && propietario != null
                && Objects.equals(usuario.getId(), propietario.getId());
    }

    private static String limpiarTelefono(String telefono) {
        return telefono == null || telefono.isBlank() ? null : telefono.strip();
    }

    public void eliminar(UUID id) throws SQLException {
        db.eliminar(Propiedad.class, id, Permiso.GESTIONAR_USUARIOS);
    }

    public void cambiarEstado(UUID id, boolean activa) throws SQLException {
        db.cambiar(Propiedad.class, id, propiedad -> propiedad.setActiva(activa));
    }
}
