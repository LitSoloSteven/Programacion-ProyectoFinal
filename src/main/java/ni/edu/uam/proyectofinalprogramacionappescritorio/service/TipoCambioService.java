package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.ParserMonto;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro y autorización de tasas NIO por USD. Las tasas son inmutables:
 * autorizar una tasa informativa crea una nueva instantánea autorizada.
 */
public final class TipoCambioService {
    public static final int LIMITE_MAXIMO = 200;
    public static final String MENSAJE_TASA_DUPLICADA =
            "Ya existe una tasa autorizada para esa fecha.";
    public static final String MENSAJE_FUERA_DE_BANDA =
            "La tasa está fuera de la banda 1–100 NIO/USD; el Propietario debe confirmar la revisión.";

    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();

    public TipoCambioService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
    }

    public List<TipoCambio> listar(LocalDate fecha, Boolean autorizada, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException(
                    "El límite debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            StringBuilder hql = new StringBuilder(
                    "select t from TipoCambio t left join fetch t.autorizadoPor where 1 = 1");
            if (fecha != null) hql.append(" and t.fecha = :fecha");
            if (autorizada != null) {
                hql.append(autorizada
                        ? " and t.autorizadoPor is not null"
                        : " and t.autorizadoPor is null");
            }
            hql.append(" order by t.fecha desc, t.registradoEn desc");
            var consulta = session.createQuery(hql.toString(), TipoCambio.class)
                    .setMaxResults(limite);
            if (fecha != null) consulta.setParameter("fecha", fecha);
            return consulta.getResultList();
        });
    }

    public Optional<TipoCambio> obtener(UUID id) {
        Objects.requireNonNull(id, "Falta el identificador de la tasa.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return Optional.ofNullable(session.find(TipoCambio.class, id));
        });
    }

    public Optional<TipoCambio> vigente(LocalDate fecha) {
        Objects.requireNonNull(fecha, "Falta la fecha de la tasa.");
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            return session.createQuery("""
                            select t from TipoCambio t
                            left join fetch t.autorizadoPor
                            where t.fecha = :fecha and t.autorizadoPor is not null
                            order by t.registradoEn desc
                            """, TipoCambio.class)
                    .setParameter("fecha", fecha)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        });
    }

    public TipoCambio registrar(LocalDate fecha, BigDecimal tasa, String fuente) {
        if (fecha == null) throw new IllegalArgumentException("La fecha de la tasa es obligatoria.");
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la tasa no puede ser futura.");
        }
        if (tasa == null) {
            throw new IllegalArgumentException("La tasa es obligatoria.");
        }
        ParserMonto.validarTasa(tasa, "tasa");
        if (tasa.signum() <= 0) {
            throw new IllegalArgumentException("La tasa debe ser un número positivo.");
        }
        if (fuente == null || fuente.isBlank()) {
            throw new IllegalArgumentException("La fuente de la tasa es obligatoria.");
        }
        return transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.OPERAR_ALQUILERES);
            TipoCambio registro = TipoCambio.informativa(fecha, tasa, fuente, Instant.now());
            session.persist(registro);
            auditoria.registrar(session, actor, UUID.randomUUID(), "TASA_REGISTRADA",
                    "tasa:" + registro.getId());
            return registro;
        });
    }

    public Optional<TipoCambio> autorizar(UUID id, String motivo) {
        return autorizar(id, motivo, false);
    }

    /**
     * Autoriza una tasa informativa creando una nueva instantánea. Devuelve
     * vacío si el identificador no existe; lanza conflicto si la fecha ya
     * tiene una tasa autorizada. Fuera de la banda 1–100 NIO/USD exige
     * confirmación explícita del Propietario, sin restricción en la BD.
     */
    public Optional<TipoCambio> autorizar(UUID id, String motivo,
                                          boolean confirmarFueraDeBanda) {
        Objects.requireNonNull(id, "Falta el identificador de la tasa.");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de autorización es obligatorio.");
        }
        try {
            return transactions.ejecutar((Session session) -> {
                Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                        Permiso.AUTORIZAR_TASA);
                TipoCambio origen = session.find(TipoCambio.class, id);
                if (origen == null) return Optional.<TipoCambio>empty();
                if (origen.isAutorizada()) {
                    throw new ConflictoDatosException(MENSAJE_TASA_DUPLICADA);
                }
                if (origen.estaFueraDeBanda() && !confirmarFueraDeBanda) {
                    throw new IllegalArgumentException(MENSAJE_FUERA_DE_BANDA);
                }
                Long autorizadas = session.createQuery("""
                                select count(t) from TipoCambio t
                                where t.fecha = :fecha and t.autorizadoPor is not null
                                """, Long.class)
                        .setParameter("fecha", origen.getFecha())
                        .getSingleResult();
                if (autorizadas > 0) {
                    throw new ConflictoDatosException(MENSAJE_TASA_DUPLICADA);
                }
                TipoCambio autorizada = TipoCambio.autorizada(origen.getFecha(),
                        origen.getNioPorUsd(), origen.getFuente(), Instant.now(), actor, motivo);
                session.persist(autorizada);
                auditoria.registrar(session, actor, UUID.randomUUID(), "TASA_AUTORIZADA",
                        "tasa:" + autorizada.getId());
                return Optional.of(autorizada);
            });
        } catch (ConflictoDatosException e) {
            // También normaliza la violación del índice único ante concurrencia.
            throw new ConflictoDatosException(MENSAJE_TASA_DUPLICADA);
        }
    }
}
