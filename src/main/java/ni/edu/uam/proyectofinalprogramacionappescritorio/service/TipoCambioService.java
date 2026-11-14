package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
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
        if (tasa == null || tasa.signum() <= 0) {
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

    /**
     * Autoriza una tasa informativa creando una nueva instantánea. Devuelve
     * vacío si el identificador no existe; lanza conflicto si la fecha ya
     * tiene una tasa autorizada.
     */
    public Optional<TipoCambio> autorizar(UUID id, String motivo) {
        Objects.requireNonNull(id, "Falta el identificador de la tasa.");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de autorización es obligatorio.");
        }
        return transactions.ejecutar((Session session) -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.AUTORIZAR_TASA);
            TipoCambio origen = session.find(TipoCambio.class, id);
            if (origen == null) return Optional.<TipoCambio>empty();
            if (origen.isAutorizada()) {
                throw new ConflictoDatosException("La tasa ya está autorizada.");
            }
            Long autorizadas = session.createQuery("""
                            select count(t) from TipoCambio t
                            where t.fecha = :fecha and t.autorizadoPor is not null
                            """, Long.class)
                    .setParameter("fecha", origen.getFecha())
                    .getSingleResult();
            if (autorizadas > 0) {
                throw new ConflictoDatosException(
                        "Ya existe una tasa autorizada para esa fecha.");
            }
            TipoCambio autorizada = TipoCambio.autorizada(origen.getFecha(),
                    origen.getNioPorUsd(), origen.getFuente(), Instant.now(), actor, motivo);
            session.persist(autorizada);
            auditoria.registrar(session, actor, UUID.randomUUID(), "TASA_AUTORIZADA",
                    "tasa:" + autorizada.getId());
            return Optional.of(autorizada);
        });
    }
}