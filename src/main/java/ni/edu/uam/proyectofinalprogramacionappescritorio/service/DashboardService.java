package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TotalMonedaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DashboardService {
    private static final List<String> MONEDAS = List.of("USD", "NIO");

    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final RelojService reloj = new RelojService();

    public DashboardService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public DashboardService(HibernateTransactions transactions) {
        this.transactions = Objects.requireNonNull(transactions);
    }

    public MetricasDashboardDTO obtenerMetricas() {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            int propiedades = session.createQuery("select count(p) from Propiedad p where p.activa = true", Long.class)
                    .getSingleResult().intValue();
            int unidades = session.createQuery("select count(u) from Unidad u", Long.class)
                    .getSingleResult().intValue();
            int inquilinos = session.createQuery("select count(i) from Inquilino i where i.activo = true", Long.class)
                    .getSingleResult().intValue();
            int contratosVigentes = session.createQuery("select count(c) from Contrato c where c.estado = 'Vigente'", Long.class)
                    .getSingleResult().intValue();

            List<TotalMonedaDTO> cargosPendientes = cargosPendientes(session);
            List<TotalMonedaDTO> pagosRegistrados = totalesPorMoneda(session,
                    "select p.monedaContrato, count(p), sum(p.montoContrato) from Pago p where p.estado = 'Registrado' group by p.monedaContrato order by p.monedaContrato");

            return new MetricasDashboardDTO(propiedades, unidades, inquilinos, contratosVigentes,
                    cargosPendientes, pagosRegistrados);
        });
    }

    private List<TotalMonedaDTO> totalesPorMoneda(Session session, String consulta) {
        Map<String, TotalMonedaDTO> totales = new HashMap<>();
        for (String moneda : MONEDAS) {
            totales.put(moneda, new TotalMonedaDTO(moneda, 0, BigDecimal.ZERO));
        }

        for (Object[] fila : session.createQuery(consulta, Object[].class).getResultList()) {
            String moneda = (String) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            BigDecimal total = (BigDecimal) fila[2];
            totales.put(moneda, new TotalMonedaDTO(moneda, cantidad, total));
        }
        return MONEDAS.stream().map(totales::get).toList();
    }

    private List<TotalMonedaDTO> cargosPendientes(Session session) {
        Map<String, TotalMonedaDTO> totales = new HashMap<>();
        LocalDate hoy = reloj.leer(session).fecha();
        for (String moneda : MONEDAS) {
            totales.put(moneda, new TotalMonedaDTO(moneda, 0, BigDecimal.ZERO));
        }

        for (Object[] fila : session.createQuery("""
                        select c, ct.moneda,
                          (select sum(a.monto) from AplicacionPago a, Pago p
                           where a.pagoId = p.id and a.cargoId = c.id
                             and p.estado <> :anulado)
                        from Cargo c join c.contrato ct
                        """, Object[].class)
                .setParameter("anulado", "Anulado")
                .getResultList()) {
            Cargo cargo = (Cargo) fila[0];
            String moneda = (String) fila[1];
            BigDecimal aplicado = (BigDecimal) fila[2];
            cargo.establecerEstadoDerivado(aplicado, hoy);
            String estado = cargo.getEstado();
            if (!"Pendiente".equals(estado) && !"Parcial".equals(estado)
                    && !"Vencido".equals(estado)) {
                continue;
            }

            TotalMonedaDTO actual = totales.get(moneda);
            totales.put(moneda, new TotalMonedaDTO(moneda,
                    actual.cantidad() + 1,
                    actual.total().add(cargo.saldoPendiente(aplicado))));
        }
        return MONEDAS.stream().map(totales::get).toList();
    }
}
