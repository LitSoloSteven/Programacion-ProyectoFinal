package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GarantiaService {
    private final HibernateTransactions tx;
    private final HibernateCrudService db;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();

    public GarantiaService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    GarantiaService(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx);
        this.db = new HibernateCrudService(tx);
    }

    public List<Garantia> listarTodas() throws SQLException {
        return db.listar(Garantia.class, "from Garantia g join fetch g.contrato order by g.contrato.inicio desc");
    }

    public void ajustarSaldo(UUID id, BigDecimal saldoActual) throws SQLException {
        if (id == null || saldoActual == null || saldoActual.signum() < 0
                || saldoActual.stripTrailingZeros().scale() > 2
                || saldoActual.precision() > 19) {
            throw new IllegalArgumentException("El saldo debe ser positivo o cero y tener como máximo dos decimales.");
        }
        tx.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, SesionUsuario.getActual(),
                    Permiso.LIQUIDAR_GARANTIA);
            Garantia garantia = session.find(Garantia.class, id);
            if (garantia == null) throw new IllegalArgumentException("La garantía ya no existe.");
            garantia.ajustarSaldo(saldoActual.setScale(2));
            auditoria.registrar(session, actor, UUID.randomUUID(),
                    "GARANTIA_SALDO_AJUSTADO", "garantia:" + id
                            + ":saldo=" + saldoActual.toPlainString());
            return null;
        });
    }
}
