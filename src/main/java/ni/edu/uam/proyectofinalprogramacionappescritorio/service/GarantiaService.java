package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class GarantiaService {
    private final HibernateCrudService db = new HibernateCrudService();

    public List<Garantia> listarTodas() throws SQLException {
        return db.listar(Garantia.class, "from Garantia g join fetch g.contrato order by g.contrato.inicio desc");
    }

    public void ajustarSaldo(UUID id, BigDecimal saldoActual) throws SQLException {
        if (saldoActual == null || saldoActual.signum() < 0 || saldoActual.scale() > 2 || saldoActual.precision() > 19) {
            throw new IllegalArgumentException("El saldo debe ser positivo o cero y tener como máximo dos decimales.");
        }
        db.cambiar(Garantia.class, id, garantia -> garantia.ajustarSaldo(saldoActual));
    }
}
