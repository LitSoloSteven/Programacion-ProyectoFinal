package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.Permiso;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PermisoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ValidadorEntrada;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Orquesta las lecturas y escrituras de la API 3. Las lecturas usan
 * HibernateTransactions con consultas paginadas y fetch joins; las escrituras
 * delegan en los servicios de dominio existentes para conservar sus reglas,
 * auditoría y control de versión.
 */
public final class ServicioCatalogos {
    public static final int TAMANO_PREDETERMINADO = 50;
    public static final int TAMANO_MAXIMO = 50;
    public static final int PAGINA_MAXIMA = 100_000;

    private static final Set<String> TIPOS_UNIDAD =
            Set.of("Apartamento", "Casa", "Cuarto", "Local comercial");
    private static final Set<String> ESTADOS_UNIDAD =
            Set.of("Disponible", "En mantenimiento", "Inactiva");
    private static final Set<String> ESTADOS_CONTRATO =
            Set.of("Vigente", "Borrador", "Por vencer", "Vencido", "Terminado", "Cancelado");
    private static final Set<String> MONEDAS = Set.of("USD", "NIO");

    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();
    private final PropiedadService propiedades;
    private final UnidadService unidades;
    private final InquilinoService inquilinos;
    private final ContratoService contratos;

    public ServicioCatalogos(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx, "Faltan las transacciones de la API.");
        this.propiedades = new PropiedadService(tx);
        this.unidades = new UnidadService(tx);
        this.inquilinos = new InquilinoService(tx);
        this.contratos = new ContratoService(tx);
    }

    // ------------------------------------------------------------------
    // Propiedades
    // ------------------------------------------------------------------

    public PaginaResponse<PropiedadResponse> listarPropiedades(
            int pagina, int tamano, String texto, Boolean activa) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (texto != null && !texto.isBlank()) {
            where.append("(lower(p.nombre) like :texto or lower(p.direccion) like :texto)");
            parametros.put("texto", "%" + texto.strip().toLowerCase(Locale.ROOT) + "%");
        }
        if (activa != null) {
            if (!where.isEmpty()) where.append(" and ");
            where.append("p.activa = :activa");
            parametros.put("activa", activa);
        }
        return paginar(Propiedad.class, "p",
                "Propiedad p left join fetch p.propietario", "Propiedad p",
                where.toString(), parametros, "p.nombre",
                pagina, tamano, ServicioCatalogos::aPropiedad);
    }

    public PropiedadResponse obtenerPropiedad(UUID id) {
        PropiedadResponse respuesta = tx.ejecutar(s -> {
            exigirLectura(s);
            Propiedad propiedad = s.createQuery(
                            "from Propiedad p left join fetch p.propietario where p.id = :id",
                            Propiedad.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return propiedad == null ? null : aPropiedad(propiedad);
        });
        if (respuesta == null) throw ExcepcionApi.noEncontrado();
        return respuesta;
    }

    public PropiedadResponse crearPropiedad(PropiedadRequest solicitud) {
        String nombre = textoRequerido(solicitud.nombre(), "nombre", 120);
        String direccion = textoRequerido(solicitud.direccion(), "direccion", 300);
        String[] propietario = datosPropietario(solicitud);

        Propiedad propiedad = new Propiedad();
        propiedad.setNombre(nombre);
        propiedad.setDireccion(direccion);
        propiedad.setActiva(solicitud.activa() == null || solicitud.activa());
        ejecutar(() -> propiedades.guardar(propiedad,
                propietario[0], propietario[1], propietario[2]));
        if (propiedad.getId() == null) throw ExcepcionApi.interno();
        return obtenerPropiedad(propiedad.getId());
    }

    public PropiedadResponse actualizarPropiedad(UUID id, long version,
                                                 PropiedadRequest solicitud) {
        obtenerPropiedad(id);
        String nombre = textoRequerido(solicitud.nombre(), "nombre", 120);
        String direccion = textoRequerido(solicitud.direccion(), "direccion", 300);
        String[] propietario = datosPropietarioActual(solicitud);

        Propiedad propiedad = new Propiedad();
        propiedad.setId(id);
        propiedad.setVersion(version);
        propiedad.setNombre(nombre);
        propiedad.setDireccion(direccion);
        propiedad.setActiva(solicitud.activa() == null || solicitud.activa());
        ejecutar(() -> propiedades.guardar(propiedad,
                propietario[0], propietario[1], propietario[2]));
        return obtenerPropiedad(id);
    }

    public PropiedadResponse cambiarEstadoPropiedad(UUID id, Boolean activa) {
        obtenerPropiedad(id);
        boolean nuevo = activa != null && activa;
        ejecutar(() -> propiedades.cambiarEstado(id, nuevo));
        return obtenerPropiedad(id);
    }

    // ------------------------------------------------------------------
    // Unidades
    // ------------------------------------------------------------------

    public PaginaResponse<UnidadResponse> listarUnidades(
            int pagina, int tamano, UUID propiedadId, String estado, String tipo, String texto) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (propiedadId != null) {
            where.append("u.propiedadId = :propiedadId");
            parametros.put("propiedadId", propiedadId);
        }
        if (estado != null && !estado.isBlank()) {
            separar(where);
            where.append("u.estado = :estado");
            parametros.put("estado", estado.strip());
        }
        if (tipo != null && !tipo.isBlank()) {
            separar(where);
            where.append("u.tipo = :tipo");
            parametros.put("tipo", tipo.strip());
        }
        if (texto != null && !texto.isBlank()) {
            separar(where);
            where.append("(lower(u.codigo) like :texto or lower(u.descripcion) like :texto)");
            parametros.put("texto", "%" + texto.strip().toLowerCase(Locale.ROOT) + "%");
        }
        return paginar(Unidad.class, "u",
                "Unidad u join fetch u.propiedad", "Unidad u",
                where.toString(), parametros, "u.codigo",
                pagina, tamano, ServicioCatalogos::aUnidad);
    }

    public UnidadResponse obtenerUnidad(UUID id) {
        UnidadResponse respuesta = tx.ejecutar(s -> {
            exigirLectura(s);
            Unidad unidad = s.createQuery(
                            "from Unidad u join fetch u.propiedad where u.id = :id",
                            Unidad.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return unidad == null ? null : aUnidad(unidad);
        });
        if (respuesta == null) throw ExcepcionApi.noEncontrado();
        return respuesta;
    }

    public UnidadResponse crearUnidad(UnidadRequest solicitud) {
        DatosUnidad datos = validarUnidad(solicitud);
        Unidad unidad = new Unidad();
        aplicar(unidad, datos);
        ejecutar(() -> unidades.guardar(unidad));
        if (unidad.getId() == null) throw ExcepcionApi.interno();
        return obtenerUnidad(unidad.getId());
    }

    public UnidadResponse actualizarUnidad(UUID id, long version, UnidadRequest solicitud) {
        obtenerUnidad(id);
        DatosUnidad datos = validarUnidad(solicitud);
        ejecutar(() -> unidades.actualizar(id, version, datos.propiedadId(), datos.codigo(),
                datos.descripcion(), datos.tipo(), datos.estado(), datos.canon(), datos.moneda()));
        return obtenerUnidad(id);
    }

    public UnidadResponse cambiarEstadoUnidad(UUID id, String estado) {
        obtenerUnidad(id);
        String nuevo = textoOpcional(estado, "estado", 24);
        if (nuevo == null) nuevo = "Inactiva";
        exigirEstadoUnidad(nuevo);
        String definitivo = nuevo;
        ejecutar(() -> unidades.cambiarEstado(id, definitivo));
        return obtenerUnidad(id);
    }

    // ------------------------------------------------------------------
    // Inquilinos
    // ------------------------------------------------------------------

    public PaginaResponse<InquilinoResponse> listarInquilinos(
            int pagina, int tamano, Boolean activo, String texto) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (activo != null) {
            where.append("i.activo = :activo");
            parametros.put("activo", activo);
        }
        if (texto != null && !texto.isBlank()) {
            separar(where);
            where.append("(lower(i.nombre) like :texto or lower(i.identificacion) like :texto")
                    .append(" or lower(i.correo) like :texto)");
            parametros.put("texto", "%" + texto.strip().toLowerCase(Locale.ROOT) + "%");
        }
        return paginar(Inquilino.class, "i",
                "Inquilino i", "Inquilino i",
                where.toString(), parametros, "i.nombre",
                pagina, tamano, ServicioCatalogos::aInquilino);
    }

    public InquilinoResponse obtenerInquilino(UUID id) {
        InquilinoResponse respuesta = tx.ejecutar(s -> {
            exigirLectura(s);
            Inquilino inquilino = s.find(Inquilino.class, id);
            return inquilino == null ? null : aInquilino(inquilino);
        });
        if (respuesta == null) throw ExcepcionApi.noEncontrado();
        return respuesta;
    }

    public InquilinoResponse crearInquilino(InquilinoRequest solicitud) {
        Inquilino inquilino = new Inquilino();
        aplicar(inquilino, validarInquilino(solicitud));
        ejecutar(() -> inquilinos.guardar(inquilino));
        if (inquilino.getId() == null) throw ExcepcionApi.interno();
        return obtenerInquilino(inquilino.getId());
    }

    public InquilinoResponse actualizarInquilino(UUID id, long version,
                                                 InquilinoRequest solicitud) {
        obtenerInquilino(id);
        DatosInquilino datos = validarInquilino(solicitud);
        ejecutar(() -> inquilinos.actualizar(id, version, datos.nombre(),
                datos.identificacion(), datos.telefono(), datos.correo(),
                datos.contactoEmergencia(), datos.activo()));
        return obtenerInquilino(id);
    }

    public InquilinoResponse cambiarEstadoInquilino(UUID id, Boolean activo) {
        obtenerInquilino(id);
        boolean nuevo = activo != null && activo;
        ejecutar(() -> inquilinos.cambiarEstado(id, nuevo));
        return obtenerInquilino(id);
    }

    // ------------------------------------------------------------------
    // Contratos
    // ------------------------------------------------------------------

    public PaginaResponse<ContratoResponse> listarContratos(
            int pagina, int tamano, String estado, UUID unidadId, UUID inquilinoId) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (estado != null && !estado.isBlank()) {
            where.append("c.estado = :estado");
            parametros.put("estado", estado.strip());
        }
        if (unidadId != null) {
            separar(where);
            where.append("c.unidadId = :unidadId");
            parametros.put("unidadId", unidadId);
        }
        if (inquilinoId != null) {
            separar(where);
            where.append("c.inquilinoId = :inquilinoId");
            parametros.put("inquilinoId", inquilinoId);
        }
        return paginar(Contrato.class, "c",
                "Contrato c join fetch c.unidad join fetch c.inquilino"
                        + " left join fetch c.garantiaSeguimiento",
                "Contrato c",
                where.toString(), parametros, "c.inicio desc",
                pagina, tamano, ServicioCatalogos::aContrato);
    }

    public ContratoResponse obtenerContrato(UUID id) {
        ContratoResponse respuesta = tx.ejecutar(s -> {
            exigirLectura(s);
            Contrato contrato = s.createQuery(
                            "from Contrato c join fetch c.unidad join fetch c.inquilino"
                                    + " left join fetch c.garantiaSeguimiento where c.id = :id",
                            Contrato.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return contrato == null ? null : aContrato(contrato);
        });
        if (respuesta == null) throw ExcepcionApi.noEncontrado();
        return respuesta;
    }

    public ContratoResponse crearContrato(ContratoRequest solicitud) {
        DatosContrato datos = validarContrato(solicitud);
        Contrato contrato = new Contrato();
        aplicar(contrato, datos);
        ejecutar(() -> contratos.guardar(contrato));
        if (contrato.getId() == null) throw ExcepcionApi.interno();
        return obtenerContrato(contrato.getId());
    }

    public ContratoResponse actualizarContrato(UUID id, long version, ContratoRequest solicitud) {
        obtenerContrato(id);
        DatosContrato datos = validarContrato(solicitud);
        ejecutar(() -> contratos.actualizar(id, version, datos.unidadId(), datos.inquilinoId(),
                datos.inicio(), datos.fin(), datos.canon(), datos.moneda(), datos.diaPago(),
                datos.depositoPactado(), datos.estado()));
        return obtenerContrato(id);
    }

    public ContratoResponse cambiarEstadoContrato(UUID id, String estado) {
        obtenerContrato(id);
        String nuevo = textoOpcional(estado, "estado", 24);
        if (nuevo == null) nuevo = "Cancelado";
        exigirEstadoContrato(nuevo);
        String definitivo = nuevo;
        ejecutar(() -> contratos.cambiarEstado(id, definitivo));
        return obtenerContrato(id);
    }

    // ------------------------------------------------------------------
    // Lectura paginada
    // ------------------------------------------------------------------

    private <E, R> PaginaResponse<R> paginar(Class<E> tipo, String alias,
                                             String desdeDatos, String desdeCuenta,
                                             String where, Map<String, Object> parametros,
                                             String orden, int pagina, int tamano,
                                             Function<E, R> mapeo) {
        String filtro = where == null || where.isBlank() ? "" : " where " + where;
        return tx.ejecutar(s -> {
            exigirLectura(s);
            Query<Long> conteo = s.createQuery(
                    "select count(" + alias + ") from " + desdeCuenta + filtro, Long.class);
            aplicarParametros(conteo, parametros);
            long total = conteo.uniqueResult();

            Query<E> consulta = s.createQuery(
                    "select " + alias + " from " + desdeDatos + filtro + " order by " + orden,
                    tipo);
            aplicarParametros(consulta, parametros);
            consulta.setFirstResult((pagina - 1) * tamano);
            consulta.setMaxResults(tamano);
            List<R> elementos = consulta.getResultList().stream().map(mapeo).toList();
            return new PaginaResponse<>(pagina, tamano, total, elementos);
        });
    }

    private static void aplicarParametros(Query<?> consulta, Map<String, Object> parametros) {
        parametros.forEach(consulta::setParameter);
    }

    private void exigirLectura(Session session) {
        permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
    }

    private static void separar(StringBuilder where) {
        if (!where.isEmpty()) where.append(" and ");
    }

    // ------------------------------------------------------------------
    // Mapeo a DTO
    // ------------------------------------------------------------------

    private static PropiedadResponse aPropiedad(Propiedad p) {
        Propietario propietario = p.getPropietario();
        return new PropiedadResponse(
                p.getId().toString(),
                p.getNombre(),
                p.getDireccion(),
                p.isActiva(),
                propietario == null ? null : propietario.getNombre(),
                propietario == null ? null : propietario.getCorreo(),
                propietario == null ? null : propietario.getTelefono(),
                instante(p.getCreadoEn()),
                p.getVersion());
    }

    private static UnidadResponse aUnidad(Unidad u) {
        return new UnidadResponse(
                u.getId().toString(),
                u.getPropiedadId() == null ? null : u.getPropiedadId().toString(),
                u.getPropiedadNombre(),
                u.getCodigo(),
                u.getDescripcion(),
                u.getTipo(),
                u.getEstado(),
                decimal(u.getCanonReferencia()),
                u.getMoneda(),
                u.getVersion());
    }

    private static InquilinoResponse aInquilino(Inquilino i) {
        return new InquilinoResponse(
                i.getId().toString(),
                i.getNombre(),
                i.getIdentificacion(),
                i.getTelefono(),
                i.getCorreo(),
                i.getContactoEmergencia(),
                i.isActivo(),
                instante(i.getCreadoEn()),
                i.getVersion());
    }

    private static ContratoResponse aContrato(Contrato c) {
        Garantia garantia = c.getGarantiaSeguimiento();
        return new ContratoResponse(
                c.getId().toString(),
                c.getUnidadId() == null ? null : c.getUnidadId().toString(),
                c.getUnidadInfo(),
                c.getInquilinoId() == null ? null : c.getInquilinoId().toString(),
                c.getInquilinoNombre(),
                c.getInicio().toString(),
                c.getFin().toString(),
                decimal(c.getCanon()),
                c.getMoneda(),
                c.getDiaPago(),
                decimal(c.getDepositoPactado()),
                garantia == null ? null : decimal(garantia.getSaldoActual()),
                c.getEstado(),
                c.getVersion());
    }

    private static String instante(Timestamp valor) {
        return valor == null ? null : valor.toInstant().toString();
    }

    private static String decimal(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    // ------------------------------------------------------------------
    // Validación de entradas
    // ------------------------------------------------------------------

    private String[] datosPropietario(PropiedadRequest solicitud) {
        String nombre = textoOpcional(solicitud.propietarioNombre(), "propietarioNombre", 100);
        String correo = textoOpcional(solicitud.propietarioCorreo(), "propietarioCorreo", 150);
        String telefono = textoOpcional(solicitud.propietarioTelefono(), "propietarioTelefono", 20);
        if (nombre == null || correo == null) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El nombre y el correo del propietario son obligatorios.");
        }
        exigirCorreo(correo, "propietarioCorreo");
        return new String[]{nombre, correo.toLowerCase(Locale.ROOT), telefono};
    }

    private String[] datosPropietarioActual(PropiedadRequest solicitud) {
        String nombre = textoOpcional(solicitud.propietarioNombre(), "propietarioNombre", 100);
        String correo = textoOpcional(solicitud.propietarioCorreo(), "propietarioCorreo", 150);
        String telefono = textoOpcional(solicitud.propietarioTelefono(), "propietarioTelefono", 20);
        if (nombre == null && correo == null) return propietarioActual();
        if (nombre == null || correo == null) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El nombre y el correo del propietario son obligatorios.");
        }
        exigirCorreo(correo, "propietarioCorreo");
        return new String[]{nombre, correo.toLowerCase(Locale.ROOT), telefono};
    }

    private String[] propietarioActual() {
        SesionUsuario sesion = SesionUsuario.getActual();
        if (sesion == null) throw ExcepcionApi.noAutorizado();
        String[] datos = tx.ejecutar(s -> {
            exigirLectura(s);
            Propietario propietario = s.find(Propietario.class, sesion.getUsuarioId());
            return propietario == null ? null : new String[]{
                    propietario.getNombre(),
                    propietario.getCorreo(),
                    propietario.getTelefono()};
        });
        if (datos == null) {
            throw ExcepcionApi.entidadNoProcesable(
                    "Registre el nombre y el correo del propietario.");
        }
        return datos;
    }

    private DatosUnidad validarUnidad(UnidadRequest solicitud) {
        UUID propiedadId = uuid(solicitud.propiedadId(), "propiedadId");
        exigirPropiedad(propiedadId);
        String codigo = textoRequerido(solicitud.codigo(), "codigo", 30);
        String descripcion = textoOpcional(solicitud.descripcion(), "descripcion", 255);
        String tipo = textoOpcional(solicitud.tipo(), "tipo", 24);
        if (tipo == null) tipo = "Apartamento";
        if (!TIPOS_UNIDAD.contains(tipo)) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El tipo debe ser Apartamento, Casa, Cuarto o Local comercial.");
        }
        String estado = textoOpcional(solicitud.estado(), "estado", 24);
        if (estado == null) estado = "Disponible";
        exigirEstadoUnidad(estado);
        BigDecimal canon = monto(solicitud.canonReferencia(), "canonReferencia", false, false);
        String moneda = moneda(solicitud.moneda());
        return new DatosUnidad(propiedadId, codigo, descripcion, tipo, estado, canon, moneda);
    }

    private void exigirPropiedad(UUID propiedadId) {
        boolean existe = tx.ejecutar(s -> {
            exigirLectura(s);
            return s.find(Propiedad.class, propiedadId) != null;
        });
        if (!existe) {
            throw ExcepcionApi.entidadNoProcesable("La propiedad indicada no existe.");
        }
    }

    private DatosInquilino validarInquilino(InquilinoRequest solicitud) {
        String nombre = textoRequerido(solicitud.nombre(), "nombre", 160);
        String identificacion = textoOpcional(solicitud.identificacion(), "identificacion", 40);
        String telefono = textoOpcional(solicitud.telefono(), "telefono", 30);
        if (telefono != null) exigirTelefono(telefono);
        String correo = textoOpcional(solicitud.correo(), "correo", 254);
        if (correo != null) {
            exigirCorreo(correo, "correo");
            correo = correo.toLowerCase(Locale.ROOT);
        }
        String contacto = textoOpcional(solicitud.contactoEmergencia(),
                "contactoEmergencia", 200);
        boolean activo = solicitud.activo() == null || solicitud.activo();
        return new DatosInquilino(nombre, identificacion, telefono, correo, contacto, activo);
    }

    private DatosContrato validarContrato(ContratoRequest solicitud) {
        UUID unidadId = uuid(solicitud.unidadId(), "unidadId");
        UUID inquilinoId = uuid(solicitud.inquilinoId(), "inquilinoId");
        exigirUnidad(unidadId);
        exigirInquilino(inquilinoId);
        LocalDate inicio = fecha(solicitud.inicio(), "inicio");
        LocalDate fin = fecha(solicitud.fin(), "fin");
        try {
            ValidadorEntrada.validarRangoFechas(inicio, fin, 0);
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
        BigDecimal canon = monto(solicitud.canon(), "canon", true, true);
        String moneda = moneda(solicitud.moneda());
        int diaPago = solicitud.diaPago() == null ? 5 : solicitud.diaPago();
        if (diaPago < 1 || diaPago > 31) {
            throw ExcepcionApi.entidadNoProcesable("El día de pago debe estar entre 1 y 31.");
        }
        BigDecimal deposito = monto(solicitud.depositoPactado(),
                "depositoPactado", false, false);
        String estado = textoOpcional(solicitud.estado(), "estado", 24);
        if (estado == null) estado = "Vigente";
        exigirEstadoContrato(estado);
        return new DatosContrato(unidadId, inquilinoId, inicio, fin, canon, moneda,
                diaPago, deposito, estado);
    }

    private void exigirUnidad(UUID unidadId) {
        boolean existe = tx.ejecutar(s -> {
            exigirLectura(s);
            return s.find(Unidad.class, unidadId) != null;
        });
        if (!existe) {
            throw ExcepcionApi.entidadNoProcesable("La unidad indicada no existe.");
        }
    }

    private void exigirInquilino(UUID inquilinoId) {
        boolean existe = tx.ejecutar(s -> {
            exigirLectura(s);
            return s.find(Inquilino.class, inquilinoId) != null;
        });
        if (!existe) {
            throw ExcepcionApi.entidadNoProcesable("El inquilino indicado no existe.");
        }
    }

    private static void exigirEstadoUnidad(String estado) {
        if (!ESTADOS_UNIDAD.contains(estado)) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El estado debe ser Disponible, En mantenimiento o Inactiva.");
        }
    }

    private static void exigirEstadoContrato(String estado) {
        if (!ESTADOS_CONTRATO.contains(estado)) {
            throw ExcepcionApi.entidadNoProcesable("El estado del contrato no es válido.");
        }
    }

    private static void exigirCorreo(String correo, String campo) {
        if (!ValidadorEntrada.esCorreoValido(correo)) {
            throw ExcepcionApi.entidadNoProcesable("El campo " + campo + " no es un correo válido.");
        }
    }

    private static void exigirTelefono(String telefono) {
        if (!ValidadorEntrada.esTelefonoValido(telefono)) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El teléfono solo admite números, espacios, guiones y el signo +.");
        }
    }

    private static String moneda(String valor) {
        String moneda = textoOpcional(valor, "moneda", 3);
        if (moneda == null) return "USD";
        String normalizada = moneda.toUpperCase(Locale.ROOT);
        if (!MONEDAS.contains(normalizada)) {
            throw ExcepcionApi.entidadNoProcesable("La moneda debe ser USD o NIO.");
        }
        return normalizada;
    }

    private static BigDecimal monto(String valor, String campo,
                                    boolean obligatorio, boolean positivo) {
        if (valor == null || valor.isBlank()) {
            if (obligatorio) {
                throw ExcepcionApi.entidadNoProcesable("El campo " + campo + " es obligatorio.");
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        BigDecimal numero;
        try {
            numero = new BigDecimal(valor.strip());
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("El campo " + campo + " debe ser un número decimal.");
        }
        if (numero.stripTrailingZeros().scale() > 2) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " admite como máximo 2 decimales.");
        }
        if (positivo && numero.compareTo(BigDecimal.ZERO) <= 0) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " debe ser mayor que cero.");
        }
        if (!positivo && numero.compareTo(BigDecimal.ZERO) < 0) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " no puede ser negativo.");
        }
        return numero.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static UUID uuid(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.entidadNoProcesable("El campo " + campo + " es obligatorio.");
        }
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El campo " + campo + " no es un identificador válido.");
        }
    }

    private static LocalDate fecha(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw ExcepcionApi.entidadNoProcesable("El campo " + campo + " es obligatorio.");
        }
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El campo " + campo + " debe tener el formato AAAA-MM-DD.");
        }
    }

    private static String textoRequerido(String valor, String campo, int maximo) {
        String limpio = textoOpcional(valor, campo, maximo);
        if (limpio == null) {
            throw ExcepcionApi.entidadNoProcesable("El campo " + campo + " es obligatorio.");
        }
        return limpio;
    }

    private static String textoOpcional(String valor, String campo, int maximo) {
        if (valor == null) return null;
        String limpio = valor.strip();
        if (limpio.isEmpty()) return null;
        if (limpio.length() > maximo) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }

    private static void aplicar(Unidad unidad, DatosUnidad datos) {
        unidad.setPropiedadId(datos.propiedadId());
        unidad.setCodigo(datos.codigo());
        unidad.setDescripcion(datos.descripcion());
        unidad.setTipo(datos.tipo());
        unidad.setEstado(datos.estado());
        unidad.setCanonReferencia(datos.canon());
        unidad.setMoneda(datos.moneda());
    }

    private static void aplicar(Inquilino inquilino, DatosInquilino datos) {
        inquilino.setNombre(datos.nombre());
        inquilino.setIdentificacion(datos.identificacion());
        inquilino.setTelefono(datos.telefono());
        inquilino.setCorreo(datos.correo());
        inquilino.setContactoEmergencia(datos.contactoEmergencia());
        inquilino.setActivo(datos.activo());
    }

    private static void aplicar(Contrato contrato, DatosContrato datos) {
        contrato.setUnidadId(datos.unidadId());
        contrato.setInquilinoId(datos.inquilinoId());
        contrato.setInicio(datos.inicio());
        contrato.setFin(datos.fin());
        contrato.setCanon(datos.canon());
        contrato.setMoneda(datos.moneda());
        contrato.setDiaPago(datos.diaPago());
        contrato.setDepositoPactado(datos.depositoPactado());
        contrato.setEstado(datos.estado());
    }

    /**
     * Ejecuta una escritura de dominio traduciendo sus excepciones a la
     * respuesta HTTP correspondiente sin exponer detalles internos.
     */
    private static void ejecutar(Operacion operacion) {
        try {
            operacion.ejecutar();
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        } catch (SQLException e) {
            throw ExcepcionApi.interno();
        }
    }

    @FunctionalInterface
    private interface Operacion {
        void ejecutar() throws SQLException;
    }

    private record DatosUnidad(UUID propiedadId, String codigo, String descripcion,
                               String tipo, String estado, BigDecimal canon, String moneda) {
    }

    private record DatosInquilino(String nombre, String identificacion, String telefono,
                                  String correo, String contactoEmergencia, boolean activo) {
    }

    private record DatosContrato(UUID unidadId, UUID inquilinoId, LocalDate inicio,
                                 LocalDate fin, BigDecimal canon, String moneda, int diaPago,
                                 BigDecimal depositoPactado, String estado) {
    }
}