package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.AuditoriaRegistroDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PatronLike;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AuditoriaService {
    private final RelojService reloj = new RelojService();
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();

    public AuditoriaService() {
        this.transactions = null;
    }

    public AuditoriaService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions, "Faltan las transacciones de auditoría.");
    }

    // Participa en la transacción de negocio: si ésta falla, tampoco queda una auditoría de éxito.
    public void registrar(Session session, Usuario actor, UUID correlacion, String accion, String referencia) {
        session.persist(new Auditoria(actor, reloj.leer(session).instante(), correlacion,
                accion, referencia));
    }

    public void registrarSistema(Session session, UUID correlacion, String accion, String referencia) {
        session.persist(Auditoria.eventoSistema(reloj.leer(session).instante(), correlacion,
                accion, referencia));
    }

    public void registrarIntentoLoginFallido(Session session, UUID correlacion,
                                             String usuario, String origen) {
        String identificador = usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
        String origenSeguro = origen == null ? "desconocido" : origen.strip().toLowerCase(Locale.ROOT);
        if (!origenSeguro.matches("[a-z0-9:.%_-]{1,64}")) origenSeguro = "desconocido";
        String referencia = "usuario_sha256:" + sha256(identificador) + ";origen:" + origenSeguro;
        session.persist(Auditoria.intentoLoginFallido(reloj.leer(session).instante(),
                correlacion, referencia));
    }

    public List<AuditoriaRegistroDTO> consultar(SesionUsuario identidad, String usuarioFiltro,
                                                String accionFiltro, Instant desde, Instant hasta, int limite) {
        if (transactions == null) {
            throw new IllegalStateException("El servicio de auditoría requiere transacciones para consultar.");
        }
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        }
        int limiteSeguro = limite <= 0 ? 100 : Math.min(limite, 500);
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CONSULTAR_AUDITORIA);
            StringBuilder hql = new StringBuilder(
                    "select a from Auditoria a left join fetch a.actor actor");
            StringBuilder where = new StringBuilder();
            Map<String, Object> params = new LinkedHashMap<>();
            if (usuarioFiltro != null && !usuarioFiltro.isBlank()) {
                where.append("lower(actor.nombre) like :usuario escape '!'");
                params.put("usuario", PatronLike.contieneLiteral(usuarioFiltro.strip().toLowerCase(Locale.ROOT)));
            }
            if (accionFiltro != null && !accionFiltro.isBlank()) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("lower(a.accion) like :accion escape '!'");
                params.put("accion", PatronLike.contieneLiteral(accionFiltro.strip().toLowerCase(Locale.ROOT)));
            }
            if (desde != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("a.instante >= :desde");
                params.put("desde", desde);
            }
            if (hasta != null) {
                if (!where.isEmpty()) where.append(" and ");
                where.append("a.instante <= :hasta");
                params.put("hasta", hasta);
            }
            if (!where.isEmpty()) hql.append(" where ").append(where);
            hql.append(" order by a.instante desc, a.id desc");

            Query<Auditoria> query = session.createQuery(hql.toString(), Auditoria.class);
            params.forEach(query::setParameter);
            query.setMaxResults(limiteSeguro);
            return query.getResultList().stream()
                    .map(a -> new AuditoriaRegistroDTO(
                            a.getId(),
                            a.getActor() == null ? null : a.getActor().getNombre(),
                            a.getInstante(),
                            a.getCorrelacion(),
                            a.getAccion(),
                            a.getReferencia()))
                    .toList();
        });
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256 para registrar el intento.", e);
        }
    }
}
