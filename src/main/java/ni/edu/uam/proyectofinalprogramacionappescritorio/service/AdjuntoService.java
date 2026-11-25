package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoAdjuntoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.DocumentoAdjunto;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ParametroSistema;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.PuertaMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Servicio transaccional de documentos e imágenes adjuntas (M02, M03, M04, M12 / RN-25).
 */
public final class AdjuntoService {
    private static final Set<String> MIME_SOLO_PDF = Set.of(DocumentoAdjunto.MIME_PDF);

    private final HibernateTransactions transactions;
    private final AlmacenAdjuntos almacen;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public AdjuntoService(HibernateTransactions transactions) {
        this(transactions, AlmacenAdjuntos.porDefecto());
    }

    public AdjuntoService(HibernateTransactions transactions, AlmacenAdjuntos almacen) {
        this.transactions = Objects.requireNonNull(transactions, "Faltan las transacciones.");
        this.almacen = Objects.requireNonNull(almacen, "Falta el almacén de adjuntos.");
    }

    public DocumentoAdjuntoDTO adjuntarFotoUnidad(SesionUsuario identidad, UUID unidadId, Path archivo) {
        Objects.requireNonNull(unidadId, "Falta la unidad.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(archivo, DocumentoAdjunto.MIMES_IMAGEN);
        return persistirUnidad(identidad, unidadId, guardado);
    }

    public DocumentoAdjuntoDTO adjuntarFotoUnidad(SesionUsuario identidad, UUID unidadId,
                                                  String nombreOriginal, byte[] contenido) {
        Objects.requireNonNull(unidadId, "Falta la unidad.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(
                nombreOriginal, contenido, DocumentoAdjunto.MIMES_IMAGEN);
        return persistirUnidad(identidad, unidadId, guardado);
    }

    private DocumentoAdjuntoDTO persistirUnidad(SesionUsuario identidad, UUID unidadId,
                                                AlmacenAdjuntos.ArchivoGuardado guardado) {
        return ejecutarConRollbackEnDisco(guardado, session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            bloquearPuertaSiExiste(session);
            if (session.find(Unidad.class, unidadId) == null) {
                throw new IllegalArgumentException("La unidad seleccionada no existe.");
            }
            desactivarActivosPorColumna(session, "unidadId", unidadId);
            Instant ahora = reloj.leer(session).instante();
            DocumentoAdjunto adjunto = DocumentoAdjunto.paraUnidad(
                    unidadId, guardado.nombreOriginal(), guardado.rutaInterna(),
                    guardado.mime(), guardado.tamanoBytes(), guardado.sha256(), ahora, actor);
            session.persist(adjunto);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "ADJUNTAR_FOTO_UNIDAD", "unidad:" + unidadId + ";adjunto:" + adjunto.getId());
            return aDto(adjunto);
        });
    }

    public DocumentoAdjuntoDTO adjuntarDocumentoInquilino(SesionUsuario identidad, UUID inquilinoId, Path archivo) {
        Objects.requireNonNull(inquilinoId, "Falta el inquilino.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(archivo, DocumentoAdjunto.MIMES_PERMITIDOS);
        return persistirInquilino(identidad, inquilinoId, guardado);
    }

    public DocumentoAdjuntoDTO adjuntarDocumentoInquilino(SesionUsuario identidad, UUID inquilinoId,
                                                          String nombreOriginal, byte[] contenido) {
        Objects.requireNonNull(inquilinoId, "Falta el inquilino.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(
                nombreOriginal, contenido, DocumentoAdjunto.MIMES_PERMITIDOS);
        return persistirInquilino(identidad, inquilinoId, guardado);
    }

    private DocumentoAdjuntoDTO persistirInquilino(SesionUsuario identidad, UUID inquilinoId,
                                                   AlmacenAdjuntos.ArchivoGuardado guardado) {
        return ejecutarConRollbackEnDisco(guardado, session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            bloquearPuertaSiExiste(session);
            if (session.find(Inquilino.class, inquilinoId) == null) {
                throw new IllegalArgumentException("El inquilino seleccionado no existe.");
            }
            Instant ahora = reloj.leer(session).instante();
            DocumentoAdjunto adjunto = DocumentoAdjunto.paraInquilino(
                    inquilinoId, guardado.nombreOriginal(), guardado.rutaInterna(),
                    guardado.mime(), guardado.tamanoBytes(), guardado.sha256(), ahora, actor);
            session.persist(adjunto);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "ADJUNTAR_DOCUMENTO_INQUILINO", "inquilino:" + inquilinoId + ";adjunto:" + adjunto.getId());
            return aDto(adjunto);
        });
    }

    public DocumentoAdjuntoDTO adjuntarDocumentoContrato(SesionUsuario identidad, UUID contratoId, Path archivo) {
        Objects.requireNonNull(contratoId, "Falta el contrato.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(archivo, MIME_SOLO_PDF);
        return persistirContrato(identidad, contratoId, guardado);
    }

    public DocumentoAdjuntoDTO adjuntarDocumentoContrato(SesionUsuario identidad, UUID contratoId,
                                                         String nombreOriginal, byte[] contenido) {
        Objects.requireNonNull(contratoId, "Falta el contrato.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(
                nombreOriginal, contenido, MIME_SOLO_PDF);
        return persistirContrato(identidad, contratoId, guardado);
    }

    private DocumentoAdjuntoDTO persistirContrato(SesionUsuario identidad, UUID contratoId,
                                                  AlmacenAdjuntos.ArchivoGuardado guardado) {
        return ejecutarConRollbackEnDisco(guardado, session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            bloquearPuertaSiExiste(session);
            if (session.find(Contrato.class, contratoId) == null) {
                throw new IllegalArgumentException("El contrato seleccionado no existe.");
            }
            Instant ahora = reloj.leer(session).instante();
            DocumentoAdjunto adjunto = DocumentoAdjunto.paraContrato(
                    contratoId, guardado.nombreOriginal(), guardado.rutaInterna(),
                    guardado.mime(), guardado.tamanoBytes(), guardado.sha256(), ahora, actor);
            session.persist(adjunto);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "ADJUNTAR_DOCUMENTO_CONTRATO", "contrato:" + contratoId + ";adjunto:" + adjunto.getId());
            return aDto(adjunto);
        });
    }

    public DocumentoAdjuntoDTO adjuntarDocumentoPropiedad(SesionUsuario identidad, UUID propiedadId, Path archivo) {
        Objects.requireNonNull(propiedadId, "Falta la propiedad.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(archivo, DocumentoAdjunto.MIMES_PERMITIDOS);
        return persistirPropiedad(identidad, propiedadId, guardado);
    }

    public DocumentoAdjuntoDTO adjuntarDocumentoPropiedad(SesionUsuario identidad, UUID propiedadId,
                                                          String nombreOriginal, byte[] contenido) {
        Objects.requireNonNull(propiedadId, "Falta la propiedad.");
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(
                nombreOriginal, contenido, DocumentoAdjunto.MIMES_PERMITIDOS);
        return persistirPropiedad(identidad, propiedadId, guardado);
    }

    private DocumentoAdjuntoDTO persistirPropiedad(SesionUsuario identidad, UUID propiedadId,
                                                   AlmacenAdjuntos.ArchivoGuardado guardado) {
        return ejecutarConRollbackEnDisco(guardado, session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            bloquearPuertaSiExiste(session);
            if (session.find(Propiedad.class, propiedadId) == null) {
                throw new IllegalArgumentException("La propiedad seleccionada no existe.");
            }
            Instant ahora = reloj.leer(session).instante();
            DocumentoAdjunto adjunto = DocumentoAdjunto.paraPropiedad(
                    propiedadId, guardado.nombreOriginal(), guardado.rutaInterna(),
                    guardado.mime(), guardado.tamanoBytes(), guardado.sha256(), ahora, actor);
            session.persist(adjunto);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "ADJUNTAR_DOCUMENTO_PROPIEDAD", "propiedad:" + propiedadId + ";adjunto:" + adjunto.getId());
            return aDto(adjunto);
        });
    }

    public DocumentoAdjuntoDTO adjuntarLogotipoEmisor(SesionUsuario identidad, Path archivo) {
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(archivo, DocumentoAdjunto.MIMES_IMAGEN);
        return persistirLogotipo(identidad, guardado);
    }

    public DocumentoAdjuntoDTO adjuntarLogotipoEmisor(SesionUsuario identidad,
                                                      String nombreOriginal, byte[] contenido) {
        AlmacenAdjuntos.ArchivoGuardado guardado = almacen.guardar(
                nombreOriginal, contenido, DocumentoAdjunto.MIMES_IMAGEN);
        return persistirLogotipo(identidad, guardado);
    }

    private DocumentoAdjuntoDTO persistirLogotipo(SesionUsuario identidad,
                                                  AlmacenAdjuntos.ArchivoGuardado guardado) {
        return ejecutarConRollbackEnDisco(guardado, session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            bloquearPuertaSiExiste(session);
            Instant ahora = reloj.leer(session).instante();
            ParametroSistema emisor = ConfiguracionService.buscarParametroVigente(
                    session, ConfiguracionService.CLAVE_EMISOR, ahora)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Guarda primero el perfil del emisor antes de adjuntar el logotipo."));
            List<DocumentoAdjunto> previos = session.createQuery("""
                            select d from DocumentoAdjunto d
                            join ParametroSistema p on d.parametroId = p.id
                            where p.clave = :clave and d.activo = true
                            """, DocumentoAdjunto.class)
                    .setParameter("clave", ConfiguracionService.CLAVE_EMISOR)
                    .getResultList();
            for (DocumentoAdjunto previo : previos) {
                previo.desactivar();
            }
            DocumentoAdjunto adjunto = DocumentoAdjunto.paraParametro(
                    emisor.getId(), guardado.nombreOriginal(), guardado.rutaInterna(),
                    guardado.mime(), guardado.tamanoBytes(), guardado.sha256(), ahora, actor);
            session.persist(adjunto);
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "ADJUNTAR_LOGOTIPO_EMISOR", "parametro:" + emisor.getId() + ";adjunto:" + adjunto.getId());
            return aDto(adjunto);
        });
    }

    public Optional<DocumentoAdjuntoDTO> obtenerFotoUnidad(SesionUsuario identidad, UUID unidadId) {
        Objects.requireNonNull(unidadId, "Falta la unidad.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            return session.createQuery("""
                            select d from DocumentoAdjunto d
                            join fetch d.creadoPor
                            where d.unidadId = :unidadId and d.activo = true
                            order by d.creadoEn desc
                            """, DocumentoAdjunto.class)
                    .setParameter("unidadId", unidadId)
                    .setMaxResults(1)
                    .uniqueResultOptional()
                    .map(AdjuntoService::aDto);
        });
    }

    public List<DocumentoAdjuntoDTO> listarPorInquilino(SesionUsuario identidad, UUID inquilinoId) {
        return listarActivosPorColumna(identidad, "inquilinoId", inquilinoId);
    }

    public List<DocumentoAdjuntoDTO> listarPorContrato(SesionUsuario identidad, UUID contratoId) {
        return listarActivosPorColumna(identidad, "contratoId", contratoId);
    }

    public List<DocumentoAdjuntoDTO> listarPorPropiedad(SesionUsuario identidad, UUID propiedadId) {
        return listarActivosPorColumna(identidad, "propiedadId", propiedadId);
    }

    public Optional<DocumentoAdjuntoDTO> obtenerLogotipoVigente(SesionUsuario identidad) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CONSULTAR_REPORTES);
            return session.createQuery("""
                            select d from DocumentoAdjunto d
                            join fetch d.creadoPor
                            join ParametroSistema p on d.parametroId = p.id
                            where p.clave = :clave and d.activo = true
                            order by d.creadoEn desc
                            """, DocumentoAdjunto.class)
                    .setParameter("clave", ConfiguracionService.CLAVE_EMISOR)
                    .setMaxResults(1)
                    .uniqueResultOptional()
                    .map(AdjuntoService::aDto);
        });
    }

    public byte[] leerContenido(SesionUsuario identidad, UUID adjuntoId) {
        Objects.requireNonNull(adjuntoId, "Falta el identificador del adjunto.");
        DocumentoAdjunto adjunto = transactions.ejecutar(session -> {
            DocumentoAdjunto encontrado = session.find(DocumentoAdjunto.class, adjuntoId);
            if (encontrado == null || !encontrado.isActivo()) {
                throw new IllegalArgumentException("El documento adjunto no existe o está inactivo.");
            }
            String permisoRequerido = encontrado.getParametroId() != null
                    ? Permiso.CONSULTAR_REPORTES : Permiso.OPERAR_ALQUILERES;
            permisos.exigir(session, identidad, permisoRequerido);
            return encontrado;
        });
        return almacen.leerYVerificar(adjunto.getRutaInterna(), adjunto.getSha256());
    }

    public void desactivar(SesionUsuario identidad, UUID adjuntoId) {
        Objects.requireNonNull(adjuntoId, "Falta el identificador del adjunto.");
        transactions.ejecutar(session -> {
            DocumentoAdjunto adjunto = session.find(DocumentoAdjunto.class, adjuntoId, LockModeType.PESSIMISTIC_WRITE);
            if (adjunto == null) {
                throw new IllegalArgumentException("El documento adjunto no existe.");
            }
            String permisoRequerido = adjunto.getParametroId() != null
                    ? Permiso.CAMBIAR_CONFIGURACION : Permiso.OPERAR_ALQUILERES;
            Usuario actor = permisos.exigir(session, identidad, permisoRequerido);
            if (!adjunto.isActivo()) return null;
            adjunto.desactivar();
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "DESACTIVAR_DOCUMENTO", "adjunto:" + adjunto.getId());
            return null;
        });
    }

    private List<DocumentoAdjuntoDTO> listarActivosPorColumna(SesionUsuario identidad, String campo, UUID valor) {
        Objects.requireNonNull(valor, "Falta el identificador de la entidad.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            return session.createQuery(
                            "select d from DocumentoAdjunto d join fetch d.creadoPor where d."
                                    + campo + " = :valor and d.activo = true order by d.creadoEn desc",
                            DocumentoAdjunto.class)
                    .setParameter("valor", valor)
                    .getResultList().stream().map(AdjuntoService::aDto).toList();
        });
    }

    private void desactivarActivosPorColumna(Session session, String campo, UUID valor) {
        List<DocumentoAdjunto> activos = session.createQuery(
                        "from DocumentoAdjunto d where d." + campo + " = :valor and d.activo = true",
                        DocumentoAdjunto.class)
                .setParameter("valor", valor)
                .getResultList();
        for (DocumentoAdjunto existente : activos) {
            existente.desactivar();
        }
    }

    private <T> T ejecutarConRollbackEnDisco(AlmacenAdjuntos.ArchivoGuardado guardado,
                                             java.util.function.Function<Session, T> accion) {
        try {
            return transactions.ejecutar(accion);
        } catch (RuntimeException error) {
            almacen.eliminarSilenciosamente(guardado.rutaInterna());
            throw error;
        }
    }

    private static void bloquearPuertaSiExiste(Session session) {
        PuertaMantenimiento puerta = session.find(
                PuertaMantenimiento.class, PuertaMantenimiento.ID, LockModeType.PESSIMISTIC_READ);
        if (puerta == null) {
            // En entornos H2 create-drop sin V16 semilla, la operación continúa sin fallo.
        }
    }

    private static DocumentoAdjuntoDTO aDto(DocumentoAdjunto d) {
        return new DocumentoAdjuntoDTO(
                d.getId(), d.entidadTipo(), d.entidadId(), d.getNombreOriginal(),
                d.getMime(), d.getTamanoBytes(), d.getSha256(), d.isActivo(),
                d.getCreadoEn(), d.getCreadoPor().getNombre(), d.getVersion());
    }
}
