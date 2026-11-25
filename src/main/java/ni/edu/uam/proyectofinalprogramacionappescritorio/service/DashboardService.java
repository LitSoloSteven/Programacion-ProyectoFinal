package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.MetricasDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenTasaDashboardDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.TotalMonedaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ReferenciaTipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
        return obtenerMetricas(null);
    }

    public MetricasDashboardDTO obtenerMetricas(YearMonth periodo) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            RelojService.TiempoOperativo tiempo = reloj.leer(session);
            LocalDate hoy = tiempo.fecha();
            String instanteGeneracion = RelojService.presentar(tiempo.instante());

            int propiedades = session.createQuery("select count(p) from Propiedad p where p.activa = true", Long.class)
                    .getSingleResult().intValue();
            int unidades = session.createQuery("select count(u) from Unidad u", Long.class)
                    .getSingleResult().intValue();
            int unidadesActivas = session.createQuery("select count(u) from Unidad u where u.estado <> 'Inactiva'", Long.class)
                    .getSingleResult().intValue();
            int unidadesOcupadas = session.createQuery("""
                            select count(distinct c.unidadId) from Contrato c join c.unidad u
                            where c.estado in ('Vigente', 'Por vencer', 'Vencido')
                              and u.estado <> 'Inactiva'
                            """, Long.class)
                    .getSingleResult().intValue();
            BigDecimal porcentajeOcupacion = unidadesActivas <= 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                    : BigDecimal.valueOf(unidadesOcupadas)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(unidadesActivas), 2, RoundingMode.HALF_EVEN);

            int inquilinos = session.createQuery("select count(i) from Inquilino i where i.activo = true", Long.class)
                    .getSingleResult().intValue();
            int contratosVigentes = session.createQuery("select count(c) from Contrato c where c.estado = 'Vigente'", Long.class)
                    .getSingleResult().intValue();
            int contratosPorVencer = session.createQuery("select count(c) from Contrato c where c.estado = 'Por vencer'", Long.class)
                    .getSingleResult().intValue();
            int contratosVencidos = session.createQuery("select count(c) from Contrato c where c.estado = 'Vencido'", Long.class)
                    .getSingleResult().intValue();

            ResumenCargos resumenCargos = resumirCargosPendientes(session, hoy, periodo);
            List<TotalMonedaDTO> pagosRegistrados = resumirPagosRegistrados(session, periodo);
            ResumenTasaDashboardDTO tasaDisponible = resumirTasaDisponible(session, tiempo.instante(), hoy);

            return new MetricasDashboardDTO(
                    propiedades,
                    unidades,
                    unidadesActivas,
                    unidadesOcupadas,
                    porcentajeOcupacion,
                    instanteGeneracion,
                    periodo == null ? null : periodo.toString(),
                    inquilinos,
                    contratosVigentes,
                    contratosPorVencer,
                    contratosVencidos,
                    resumenCargos.todos(),
                    resumenCargos.principal(),
                    resumenCargos.mora(),
                    pagosRegistrados,
                    tasaDisponible);
        });
    }

    private List<TotalMonedaDTO> resumirPagosRegistrados(Session session, YearMonth periodo) {
        Map<String, TotalMonedaDTO> totales = mapaInicialMonedas();
        String hql = periodo == null
                ? "select p.monedaContrato, count(p), sum(p.montoContrato) from Pago p where p.estado = 'Registrado' group by p.monedaContrato order by p.monedaContrato"
                : "select p.monedaContrato, count(p), sum(p.montoContrato) from Pago p where p.estado = 'Registrado' and p.fechaPago >= :inicioMes and p.fechaPago <= :finMes group by p.monedaContrato order by p.monedaContrato";
        var query = session.createQuery(hql, Object[].class);
        if (periodo != null) {
            query.setParameter("inicioMes", periodo.atDay(1));
            query.setParameter("finMes", periodo.atEndOfMonth());
        }
        for (Object[] fila : query.getResultList()) {
            String moneda = (String) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            BigDecimal total = ((BigDecimal) fila[2]).setScale(2, RoundingMode.HALF_EVEN);
            totales.put(moneda, new TotalMonedaDTO(moneda, cantidad, total));
        }
        return MONEDAS.stream().map(totales::get).toList();
    }

    private record ResumenCargos(
            List<TotalMonedaDTO> todos,
            List<TotalMonedaDTO> principal,
            List<TotalMonedaDTO> mora) { }

    private ResumenCargos resumirCargosPendientes(Session session, LocalDate hoy, YearMonth periodo) {
        Map<String, TotalMonedaDTO> todos = mapaInicialMonedas();
        Map<String, TotalMonedaDTO> principal = mapaInicialMonedas();
        Map<String, TotalMonedaDTO> mora = mapaInicialMonedas();

        String filtroPeriodo = periodo == null ? "" : " where c.periodo = :inicioMes ";
        var query = session.createQuery("""
                        select c, ct.moneda,
                          (select sum(a.monto) from AplicacionPago a left join Pago p on a.pagoId = p.id
                           where a.cargoId = c.id
                             and a.revertidaEn is null
                             and (a.pagoId is null or p.estado <> :anulado))
                        from Cargo c join c.contrato ct
                        """ + filtroPeriodo, Object[].class)
                .setParameter("anulado", "Anulado");
        if (periodo != null) {
            query.setParameter("inicioMes", periodo.atDay(1));
        }

        for (Object[] fila : query.getResultList()) {
            Cargo cargo = (Cargo) fila[0];
            String moneda = (String) fila[1];
            BigDecimal aplicado = (BigDecimal) fila[2];
            cargo.establecerEstadoDerivado(aplicado, hoy);
            String estado = cargo.getEstado();
            if (!"Pendiente".equals(estado) && !"Parcial".equals(estado) && !"Vencido".equals(estado)) {
                continue;
            }
            BigDecimal saldo = cargo.saldoPendiente(aplicado).setScale(2, RoundingMode.HALF_EVEN);
            acumular(todos, moneda, saldo);
            if ("Mora".equalsIgnoreCase(cargo.getTipo())) {
                acumular(mora, moneda, saldo);
            } else {
                acumular(principal, moneda, saldo);
            }
        }
        return new ResumenCargos(
                MONEDAS.stream().map(todos::get).toList(),
                MONEDAS.stream().map(principal::get).toList(),
                MONEDAS.stream().map(mora::get).toList());
    }

    private static ResumenTasaDashboardDTO resumirTasaDisponible(Session session, Instant ahora, LocalDate hoy) {
        TipoCambio autorizada = session.createQuery("""
                        from TipoCambio t
                        where t.autorizadoPor is not null
                        order by t.fecha desc, t.registradoEn desc
                        """, TipoCambio.class)
                .setMaxResults(1)
                .uniqueResult();
        if (autorizada != null) {
            long antiguedad = autorizada.getRegistradoEn() == null
                    ? 0L
                    : Math.max(0L, Duration.between(autorizada.getRegistradoEn(), ahora).toSeconds());
            return new ResumenTasaDashboardDTO(
                    true,
                    "AUTORIZADA",
                    autorizada.getNioPorUsd().setScale(8, RoundingMode.HALF_EVEN),
                    "Autorizada · " + autorizada.getFuente(),
                    autorizada.getFecha(),
                    antiguedad,
                    true);
        }

        ReferenciaTipoCambio referencia = session.createQuery("""
                        from ReferenciaTipoCambio r
                        order by r.consultadoEn desc, r.id desc
                        """, ReferenciaTipoCambio.class)
                .setMaxResults(1)
                .uniqueResult();
        if (referencia != null) {
            LocalDate efectiva = RelojService.fechaOperativa(referencia.getActualizadoEn());
            boolean historica = !efectiva.equals(hoy) || !ahora.isBefore(referencia.getProximoEn());
            long antiguedad = Math.max(0L, Duration.between(referencia.getConsultadoEn(), ahora).toSeconds());
            return new ResumenTasaDashboardDTO(
                    true,
                    historica ? "HISTORICA" : "INFORMATIVA",
                    referencia.getValor().setScale(8, RoundingMode.HALF_EVEN),
                    "Referencia H3 · " + referencia.getProveedor(),
                    efectiva,
                    antiguedad,
                    false);
        }

        return ResumenTasaDashboardDTO.sinTasa();
    }

    private static Map<String, TotalMonedaDTO> mapaInicialMonedas() {
        Map<String, TotalMonedaDTO> totales = new HashMap<>();
        for (String moneda : MONEDAS) {
            totales.put(moneda, new TotalMonedaDTO(moneda, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)));
        }
        return totales;
    }

    private static void acumular(Map<String, TotalMonedaDTO> mapa, String moneda, BigDecimal monto) {
        TotalMonedaDTO actual = mapa.get(moneda);
        if (actual == null) {
            mapa.put(moneda, new TotalMonedaDTO(moneda, 1L, monto));
        } else {
            mapa.put(moneda, new TotalMonedaDTO(moneda, actual.cantidad() + 1L, actual.total().add(monto)));
        }
    }
}
