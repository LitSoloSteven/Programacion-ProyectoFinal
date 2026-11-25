package ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoCancelarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRenovarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.ContratoTerminarRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaLiquidacionRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.GarantiaResumenResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.InquilinoResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PaginaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.PropiedadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto.UnidadResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenGarantiaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.GarantiaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.Permiso;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PermisoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RelojService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ParserMonto;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PatronLike;
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
import java.util.function.Supplier;

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
    private final GarantiaService garantias;

    public ServicioCatalogos(HibernateTransactions tx) {
        this(tx, new PropiedadService(tx), new UnidadService(tx),
                new InquilinoService(tx), new ContratoService(tx), new GarantiaService(tx));
    }

    public ServicioCatalogos(HibernateTransactions tx, PropiedadService propiedades,
                             UnidadService unidades, InquilinoService inquilinos,
                             ContratoService contratos) {
        this(tx, propiedades, unidades, inquilinos, contratos, new GarantiaService(tx));
    }

    public ServicioCatalogos(HibernateTransactions tx, PropiedadService propiedades,
                             UnidadService unidades, InquilinoService inquilinos,
                             ContratoService contratos, GarantiaService garantias) {
        this.tx = Objects.requireNonNull(tx, "Faltan las transacciones de la API.");
        this.propiedades = Objects.requireNonNull(propiedades, "Falta el servicio de propiedades.");
        this.unidades = Objects.requireNonNull(unidades, "Falta el servicio de unidades.");
        this.inquilinos = Objects.requireNonNull(inquilinos, "Falta el servicio de inquilinos.");
        this.contratos = Objects.requireNonNull(contratos, "Falta el servicio de contratos.");
        this.garantias = Objects.requireNonNull(garantias, "Falta el servicio de garantías.");
    }

    public <T> T enTransaccion(Supplier<T> operacion) {
        Objects.requireNonNull(operacion, "Falta la operación de la API.");
        return tx.ejecutar(session -> operacion.get());
    }

    public PropiedadService propiedadService() { return propiedades; }
    public UnidadService unidadService() { return unidades; }
    public InquilinoService inquilinoService() { return inquilinos; }
    public ContratoService contratoService() { return contratos; }
    public GarantiaService garantiaService() { return garantias; }

    // ------------------------------------------------------------------
    // Propiedades
    // ------------------------------------------------------------------

    public PaginaResponse<PropiedadResponse> listarPropiedades(
            int pagina, int tamano, String texto, Boolean activa) {
        StringBuilder where = new StringBuilder();
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (texto != null && !texto.isBlank()) {
            where.append("(lower(p.nombre) like :texto escape '!' or lower(p.direccion) like :texto escape '!')");
            parametros.put("texto", PatronLike.contieneLiteral(
                    texto.strip().toLowerCase(Locale.ROOT)));
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
        exigirPermiso(Permiso.GESTIONAR_PROPIEDADES);
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
        exigirPermiso(Permiso.GESTIONAR_PROPIEDADES);
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
        exigirPermiso(Permiso.GESTIONAR_PROPIEDADES);
        obtenerPropiedad(id);
        if (activa == null) {
            throw ExcepcionApi.entidadNoProcesable("El campo activa es obligatorio.");
        }
        boolean nuevo = activa;
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
            where.append("(lower(u.codigo) like :texto escape '!' or lower(u.descripcion) like :texto escape '!')");
            parametros.put("texto", PatronLike.contieneLiteral(
                    texto.strip().toLowerCase(Locale.ROOT)));
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
        exigirPermiso(Permiso.GESTIONAR_UNIDADES);
        DatosUnidad datos = validarUnidad(solicitud);
        Unidad unidad = new Unidad();
        aplicar(unidad, datos);
        tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.GESTIONAR_UNIDADES);
            Propiedad propiedad = session.find(Propiedad.class, datos.propiedadId());
            if (propiedad == null) throw ExcepcionApi.noEncontrado();
            unidad.setPropiedad(propiedad);
            ejecutar(() -> unidades.guardar(unidad));
            return null;
        });
        if (unidad.getId() == null) throw ExcepcionApi.interno();
        return obtenerUnidad(unidad.getId());
    }

    public UnidadResponse actualizarUnidad(UUID id, long version, UnidadRequest solicitud) {
        obtenerUnidad(id);
        DatosUnidad datos = validarUnidad(solicitud);
        tx.ejecutar(session -> {
            ejecutar(() -> unidades.actualizar(id, version, datos.propiedadId(), datos.codigo(),
                    datos.descripcion(), datos.tipo(), datos.estado(), datos.canon(), datos.moneda()));
            Unidad unidad = session.find(Unidad.class, id);
            unidad.setPropiedad(session.getReference(Propiedad.class, datos.propiedadId()));
            return null;
        });
        return obtenerUnidad(id);
    }

    public UnidadResponse cambiarEstadoUnidad(UUID id, String estado) {
        exigirPermiso(Permiso.GESTIONAR_UNIDADES);
        obtenerUnidad(id);
        String nuevo = textoRequerido(estado, "estado", 24);
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
            where.append("(lower(i.nombre) like :texto escape '!' or lower(i.identificacion) like :texto escape '!'")
                    .append(" or lower(i.correo) like :texto escape '!')");
            parametros.put("texto", PatronLike.contieneLiteral(
                    texto.strip().toLowerCase(Locale.ROOT)));
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
        exigirPermiso(Permiso.DESACTIVAR_INQUILINO);
        obtenerInquilino(id);
        if (activo == null) {
            throw ExcepcionApi.entidadNoProcesable("El campo activo es obligatorio.");
        }
        boolean nuevo = activo;
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
        String filtro = where.isEmpty() ? "" : " where " + where;
        return tx.ejecutar(s -> {
            exigirLectura(s);
            Query<Long> conteo = s.createQuery(
                    "select count(c) from Contrato c" + filtro, Long.class);
            aplicarParametros(conteo, parametros);
            long total = conteo.uniqueResult();

            Query<Contrato> consulta = s.createQuery(
                    "select c from Contrato c join fetch c.unidad join fetch c.inquilino"
                            + " left join fetch c.garantiaSeguimiento" + filtro
                            + " order by c.inicio desc",
                    Contrato.class);
            aplicarParametros(consulta, parametros);
            consulta.setFirstResult((pagina - 1) * tamano);
            consulta.setMaxResults(tamano);
            List<Contrato> lista = consulta.getResultList();
            Map<UUID, BigDecimal> devuelto = devueltoPorGarantia(s, lista);
            List<ContratoResponse> elementos = lista.stream()
                    .map(c -> aContrato(c, devuelto))
                    .toList();
            return new PaginaResponse<>(pagina, tamano, total, elementos);
        });
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
            if (contrato == null) return null;
            return aContrato(contrato, devueltoPorGarantia(s, List.of(contrato)));
        });
        if (respuesta == null) throw ExcepcionApi.noEncontrado();
        return respuesta;
    }

    public ContratoResponse crearContrato(ContratoRequest solicitud) {
        DatosContrato datos = validarContrato(solicitud);
        Contrato contrato = new Contrato();
        aplicar(contrato, datos);
        tx.ejecutar(session -> {
            contrato.setUnidad(session.getReference(Unidad.class, datos.unidadId()));
            contrato.setInquilino(session.getReference(Inquilino.class, datos.inquilinoId()));
            ejecutar(() -> contratos.guardar(contrato));
            return null;
        });
        if (contrato.getId() == null) throw ExcepcionApi.interno();
        return obtenerContrato(contrato.getId());
    }

    public ContratoResponse actualizarContrato(UUID id, long version, ContratoRequest solicitud) {
        obtenerContrato(id);
        DatosContrato datos = validarContrato(solicitud);
        tx.ejecutar(session -> {
            ejecutar(() -> contratos.actualizar(id, version, datos.unidadId(), datos.inquilinoId(),
                    datos.inicio(), datos.fin(), datos.canon(), datos.moneda(), datos.diaPago(),
                    datos.depositoPactado(), datos.estado()));
            Contrato contrato = session.find(Contrato.class, id);
            contrato.setUnidad(session.getReference(Unidad.class, datos.unidadId()));
            contrato.setInquilino(session.getReference(Inquilino.class, datos.inquilinoId()));
            return null;
        });
        return obtenerContrato(id);
    }

    public ContratoResponse cambiarEstadoContrato(UUID id, String estado) {
        obtenerContrato(id);
        String nuevo = textoRequerido(estado, "estado", 24);
        exigirEstadoContrato(nuevo);
        String definitivo = nuevo;
        ejecutar(() -> contratos.cambiarEstado(id, definitivo));
        return obtenerContrato(id);
    }

    public ContratoResponse activarContrato(UUID id, long version) {
        obtenerContrato(id);
        ejecutar(() -> contratos.activar(id, version));
        return obtenerContrato(id);
    }

    public ContratoResponse cancelarBorradorContrato(UUID id, long version,
                                                     ContratoCancelarRequest solicitud) {
        obtenerContrato(id);
        if (solicitud == null) {
            throw ExcepcionApi.entidadNoProcesable("El cuerpo de la cancelación es obligatorio.");
        }
        String motivo = textoRequerido(solicitud.motivo(), "motivo", 500);
        ejecutar(() -> contratos.cancelarBorrador(id, version, motivo));
        return obtenerContrato(id);
    }

    public ContratoResponse renovarContrato(UUID id, ContratoRenovarRequest solicitud) {
        obtenerContrato(id);
        if (solicitud == null) {
            throw ExcepcionApi.entidadNoProcesable("Las condiciones de renovación son obligatorias.");
        }
        LocalDate inicio = fecha(solicitud.inicio(), "inicio");
        LocalDate fin = fecha(solicitud.fin(), "fin");
        try {
            ValidadorEntrada.validarRangoFechas(inicio, fin, 1);
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
        BigDecimal canon = monto(solicitud.canon(), "canon", true, true);
        String moneda = moneda(solicitud.moneda());
        int diaPago = solicitud.diaPago() == null ? 5 : solicitud.diaPago();
        if (diaPago < 1 || diaPago > 31) {
            throw ExcepcionApi.entidadNoProcesable("El día de pago debe estar entre 1 y 31.");
        }
        BigDecimal deposito = monto(solicitud.depositoPactado(), "depositoPactado", false, false);
        var comando = new ContratoService.SolicitudRenovacion(
                inicio, fin, canon, moneda, diaPago, deposito);
        UUID sucesorId = ejecutarResultado(() -> contratos.renovar(id, comando));
        return obtenerContrato(sucesorId);
    }

    public ContratoResponse terminarContratoConCierre(UUID id, long version,
                                                      ContratoTerminarRequest solicitud) {
        obtenerContrato(id);
        if (solicitud == null) {
            throw ExcepcionApi.entidadNoProcesable("Los datos de terminación son obligatorios.");
        }
        LocalDate fechaTerminacion = fecha(solicitud.fechaTerminacion(), "fechaTerminacion");
        String motivo = textoRequerido(solicitud.motivo(), "motivo", 500);
        UUID sucesorId = uuidOpcional(solicitud.activarSucesorId(), "activarSucesorId");
        GarantiaService.SolicitudLiquidacion liquidacion = null;
        if (solicitud.liquidacion() != null
                && solicitud.liquidacion().movimientos() != null
                && !solicitud.liquidacion().movimientos().isEmpty()) {
            liquidacion = validarLiquidacion(id, null, solicitud.liquidacion());
        }
        var comando = new ContratoService.SolicitudTerminacion(
                fechaTerminacion, motivo, sucesorId, liquidacion);
        ejecutar(() -> contratos.terminar(id, version, comando));
        return obtenerContrato(id);
    }

    public GarantiaResumenResponse obtenerGarantiaContrato(UUID id) {
        obtenerContrato(id);
        ResumenGarantiaDTO resumen = ejecutarResultado(() -> garantias.consultarPorContrato(id));
        return aGarantia(resumen);
    }

    public GarantiaResumenResponse liquidarGarantiaContrato(UUID id, String idempotencyKey,
                                                            GarantiaLiquidacionRequest solicitud) {
        obtenerContrato(id);
        GarantiaService.SolicitudLiquidacion comando = validarLiquidacion(id, idempotencyKey, solicitud);
        ResumenGarantiaDTO resumen = ejecutarResultado(() -> garantias.liquidar(comando));
        return aGarantia(resumen);
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

    private void exigirPermiso(String permiso) {
        tx.ejecutar(s -> {
            permisos.exigir(s, SesionUsuario.getActual(), permiso);
            return null;
        });
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

    private static ContratoResponse aContrato(Contrato c, Map<UUID, BigDecimal> devueltoPorGarantia) {
        Garantia garantia = c.getGarantiaSeguimiento();
        String garantiaEstado = garantia == null ? null : Garantia.derivarEstado(
                garantia.getMontoInicial(),
                garantia.getSaldoActual(),
                devueltoPorGarantia.getOrDefault(garantia.getId(), BigDecimal.ZERO));
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
                c.getVersion(),
                c.getPredecesorId() == null ? null : c.getPredecesorId().toString(),
                c.getTerminadoEl() == null ? null : c.getTerminadoEl().toString(),
                c.getMotivoCierre(),
                garantiaEstado);
    }

    private static Map<UUID, BigDecimal> devueltoPorGarantia(Session session, List<Contrato> contratos) {
        List<UUID> garantiaIds = contratos.stream()
                .map(Contrato::getGarantiaSeguimiento)
                .filter(Objects::nonNull)
                .map(Garantia::getId)
                .distinct()
                .toList();
        if (garantiaIds.isEmpty()) return Map.of();
        Map<UUID, BigDecimal> totales = new LinkedHashMap<>();
        for (Object[] fila : session.createQuery("""
                        select m.garantiaId, sum(m.importe)
                        from MovimientoGarantia m
                        where m.garantiaId in :ids and m.tipo = 'Devolucion'
                        group by m.garantiaId
                        """, Object[].class)
                .setParameter("ids", garantiaIds)
                .getResultList()) {
            totales.put((UUID) fila[0], (BigDecimal) fila[1]);
        }
        return totales;
    }

    private static GarantiaResumenResponse aGarantia(ResumenGarantiaDTO dto) {
        List<GarantiaResumenResponse.MovimientoGarantiaResponse> movimientos = dto.movimientos().stream()
                .map(m -> new GarantiaResumenResponse.MovimientoGarantiaResponse(
                        m.id().toString(),
                        m.cargoId() == null ? null : m.cargoId().toString(),
                        m.cargoResumen(),
                        m.liquidacionId().toString(),
                        m.orden(),
                        m.tipo(),
                        decimal(m.importe()),
                        m.fechaMovimiento().toString(),
                        m.metodo(),
                        m.concepto(),
                        RelojService.presentar(m.creadoEn()),
                        m.creadoPor()))
                .toList();
        return new GarantiaResumenResponse(
                dto.garantiaId().toString(),
                dto.contratoId().toString(),
                dto.moneda(),
                decimal(dto.montoInicial()),
                decimal(dto.totalAplicado()),
                decimal(dto.totalDeducido()),
                decimal(dto.totalDevuelto()),
                decimal(dto.saldoActual()),
                dto.estadoDerivado(),
                dto.conciliado(),
                movimientos);
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
            ValidadorEntrada.validarRangoFechas(inicio, fin, 1);
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
            numero = ParserMonto.parsear(valor, campo);
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida("El campo " + campo + " debe ser un número decimal.");
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        }
        if (positivo && numero.compareTo(BigDecimal.ZERO) <= 0) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " debe ser mayor que cero.");
        }
        if (!positivo && numero.compareTo(BigDecimal.ZERO) < 0) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " no puede ser negativo.");
        }
        return numero;
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

    private static UUID uuidOpcional(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        return uuid(valor, campo);
    }

    private GarantiaService.SolicitudLiquidacion validarLiquidacion(UUID contratoId,
                                                                    String idempotencyKey,
                                                                    GarantiaLiquidacionRequest solicitud) {
        if (solicitud == null) {
            throw ExcepcionApi.entidadNoProcesable("El cuerpo de la liquidación es obligatorio.");
        }
        UUID claveCabecera = uuidOpcional(idempotencyKey, "Idempotency-Key");
        UUID claveCuerpo = uuidOpcional(solicitud.liquidacionId(), "liquidacionId");
        if (claveCabecera != null && claveCuerpo != null && !claveCabecera.equals(claveCuerpo)) {
            throw ExcepcionApi.solicitudInvalida(
                    "La cabecera Idempotency-Key no coincide con liquidacionId.");
        }
        UUID liquidacionId = claveCabecera != null ? claveCabecera : claveCuerpo;
        if (liquidacionId == null) {
            throw ExcepcionApi.entidadNoProcesable(
                    "La cabecera Idempotency-Key o liquidacionId es obligatoria.");
        }
        if (solicitud.movimientos() == null || solicitud.movimientos().isEmpty()) {
            throw ExcepcionApi.entidadNoProcesable("Debe incluir al menos un movimiento de garantía.");
        }
        List<GarantiaService.ItemMovimiento> items = new java.util.ArrayList<>(solicitud.movimientos().size());
        for (var fila : solicitud.movimientos()) {
            if (fila == null) {
                throw ExcepcionApi.entidadNoProcesable("La liquidación contiene un movimiento vacío.");
            }
            String tipo = textoRequerido(fila.tipo(), "tipo", 24);
            UUID cargoId = uuidOpcional(fila.cargoId(), "cargoId");
            BigDecimal importe = monto(fila.importe(), "importe", true, true);
            LocalDate fechaMov = fila.fechaMovimiento() == null || fila.fechaMovimiento().isBlank()
                    ? null : fecha(fila.fechaMovimiento(), "fechaMovimiento");
            String metodo = textoOpcional(fila.metodo(), "metodo", 24);
            String concepto = textoRequerido(fila.concepto(), "concepto", 300);
            items.add(new GarantiaService.ItemMovimiento(
                    tipo, cargoId, importe, fechaMov, metodo, concepto));
        }
        return new GarantiaService.SolicitudLiquidacion(contratoId, liquidacionId, List.copyOf(items));
    }

    /**
     * Ejecuta una escritura de dominio traduciendo sus excepciones a la
     * respuesta HTTP correspondiente sin exponer detalles internos.
     */
    private static void ejecutar(Operacion operacion) {
        ejecutarResultado(() -> {
            operacion.ejecutar();
            return null;
        });
    }

    private static <T> T ejecutarResultado(OperacionResultado<T> operacion) {
        try {
            return operacion.ejecutar();
        } catch (ConflictoDatosException e) {
            throw ExcepcionApi.conflicto(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.entidadNoProcesable(e.getMessage());
        } catch (IllegalStateException e) {
            Throwable causa = e.getCause();
            String mensaje = causa instanceof IllegalStateException
                    && causa.getMessage() != null && !causa.getMessage().isBlank()
                    ? causa.getMessage() : e.getMessage();
            throw ExcepcionApi.entidadNoProcesable(mensaje);
        } catch (SQLException e) {
            throw ExcepcionApi.interno();
        }
    }

    @FunctionalInterface
    private interface Operacion {
        void ejecutar() throws SQLException;
    }

    @FunctionalInterface
    private interface OperacionResultado<T> {
        T ejecutar() throws SQLException;
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
