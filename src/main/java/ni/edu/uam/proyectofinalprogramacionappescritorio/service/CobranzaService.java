package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.CarteraItemDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.CarteraResumenDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.CarteraTramoMonedaDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ItemPrevisualizacionLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PrevisualizacionLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ProgresoLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoLoteDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResultadoRevisionH2DTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ParametroSistema;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PatronLike;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Procesos de cobranza M05: generación mensual por lotes (H1), revisión de
 * vencimientos y mora (H2), aplicación automática de saldo a favor (RN-06) y
 * consulta de cartera por contrato, período y antigüedad (RF-M05-10).
 */
public class CobranzaService {
    public static final String MSG_059 =
            "Se solicitó cancelar. El contrato en curso terminará antes de detener el lote.";
    public static final String TRAMO_AL_DIA = "Al día";
    public static final String TRAMO_1_30 = "1 a 30";
    public static final String TRAMO_31_60 = "31 a 60";
    public static final String TRAMO_61_90 = "61 a 90";
    public static final String TRAMO_MAS_90 = "Más de 90";

    private static final Set<String> ESTADOS_ELEGIBLES = Set.of("Vigente", "Por vencer", "Vencido");
    private static final List<String> MONEDAS = List.of("USD", "NIO");

    private final HibernateTransactions tx;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    private final RelojService reloj = new RelojService();

    public CobranzaService() {
        this(new HibernateTransactions(HibernateUtil.getSessionFactory()));
    }

    public CobranzaService(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx, "Faltan las transacciones.");
    }

    public PrevisualizacionLoteDTO previsualizarGeneracionMes(YearMonth periodo) {
        Objects.requireNonNull(periodo, "El período es obligatorio.");
        LocalDate inicioMes = periodo.atDay(1);
        LocalDate finMes = periodo.atEndOfMonth();

        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            List<Contrato> elegibles = session.createQuery("""
                            select c from Contrato c
                            join fetch c.unidad u
                            join fetch u.propiedad
                            join fetch c.inquilino
                            where c.estado in :estados
                              and c.inicio <= :finMes
                              and c.fin >= :inicioMes
                            order by c.id
                            """, Contrato.class)
                    .setParameter("estados", ESTADOS_ELEGIBLES)
                    .setParameter("finMes", finMes)
                    .setParameter("inicioMes", inicioMes)
                    .getResultList();

            if (elegibles.isEmpty()) {
                return new PrevisualizacionLoteDTO(periodo.toString(), 0, 0, 0, List.of());
            }

            List<UUID> contratoIds = elegibles.stream().map(Contrato::getId).toList();
            Set<UUID> conRentaExistente = Set.copyOf(session.createQuery("""
                            select distinct c.contratoId from Cargo c
                            where c.contratoId in :ids
                              and c.periodo = :periodo
                              and c.tipo = 'Renta'
                            """, UUID.class)
                    .setParameter("ids", contratoIds)
                    .setParameter("periodo", inicioMes)
                    .getResultList());

            Map<UUID, BigDecimal> creditoPorContrato = calcularCreditoPorContrato(session, contratoIds);

            List<ItemPrevisualizacionLoteDTO> items = new ArrayList<>(elegibles.size());
            int porGenerar = 0;
            int omitidos = 0;
            for (Contrato contrato : elegibles) {
                boolean existe = conRentaExistente.contains(contrato.getId());
                if (existe) omitidos++;
                else porGenerar++;

                BigDecimal canon = nz(contrato.getCanon()).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal creditoDisponible = nz(creditoPorContrato.get(contrato.getId()))
                        .setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal creditoAplicable = existe
                        ? BigDecimal.ZERO.setScale(2)
                        : canon.min(creditoDisponible).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal saldoResultante = existe
                        ? BigDecimal.ZERO.setScale(2)
                        : canon.subtract(creditoAplicable).setScale(2, RoundingMode.HALF_EVEN);

                items.add(new ItemPrevisualizacionLoteDTO(
                        contrato.getId(),
                        contrato.toString(),
                        contrato.getMoneda(),
                        canon,
                        inicioMes,
                        ajustarDiaPago(periodo, contrato.getDiaPago()),
                        existe,
                        creditoDisponible,
                        creditoAplicable,
                        saldoResultante));
            }
            return new PrevisualizacionLoteDTO(
                    periodo.toString(), elegibles.size(), porGenerar, omitidos, List.copyOf(items));
        });
    }

    public ResultadoLoteDTO generarMes(YearMonth periodo) {
        return generarMes(periodo, null, null, null);
    }

    public ResultadoLoteDTO generarMes(YearMonth periodo,
                                       BooleanSupplier cancelacion,
                                       Consumer<ProgresoLoteDTO> progreso,
                                       Runnable ganchoEntreContratos) {
        Objects.requireNonNull(periodo, "El período es obligatorio.");
        LocalDate inicioMes = periodo.atDay(1);
        LocalDate finMes = periodo.atEndOfMonth();
        SesionUsuario identidad = SesionUsuario.getActual();

        List<UUID> elegibles = tx.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            reloj.comprobar(session);
            return session.createQuery("""
                            select c.id from Contrato c
                            where c.estado in :estados
                              and c.inicio <= :finMes
                              and c.fin >= :inicioMes
                            order by c.id
                            """, UUID.class)
                    .setParameter("estados", ESTADOS_ELEGIBLES)
                    .setParameter("finMes", finMes)
                    .setParameter("inicioMes", inicioMes)
                    .getResultList();
        });

        int total = elegibles.size();
        int procesados = 0;
        int confirmados = 0;
        int omitidos = 0;
        int fallidos = 0;
        boolean cancelado = false;
        String mensaje = "Generación completada.";
        List<UUID> generados = new ArrayList<>();

        for (UUID contratoId : elegibles) {
            if (cancelacion != null && cancelacion.getAsBoolean()) {
                cancelado = true;
                mensaje = MSG_059;
                break;
            }

            try {
                Optional<UUID> generado = tx.ejecutar(session -> {
                    Usuario actor = permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
                    RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
                    Contrato contrato = bloquearContrato(session, contratoId);
                    if (contrato == null || !ESTADOS_ELEGIBLES.contains(contrato.getEstado())
                            || contrato.getInicio().isAfter(finMes)
                            || contrato.getFin().isBefore(inicioMes)) {
                        return Optional.<UUID>empty();
                    }
                    Long existentes = session.createQuery("""
                                    select count(c) from Cargo c
                                    where c.contratoId = :contratoId
                                      and c.periodo = :periodo
                                      and c.tipo = 'Renta'
                                    """, Long.class)
                            .setParameter("contratoId", contratoId)
                            .setParameter("periodo", inicioMes)
                            .getSingleResult();
                    if (existentes > 0) {
                        return Optional.<UUID>empty();
                    }

                    Cargo cargo = new Cargo();
                    cargo.setContratoId(contratoId);
                    cargo.setTipo("Renta");
                    cargo.setPeriodo(inicioMes);
                    cargo.setFechaEmision(inicioMes);
                    cargo.setVenceEl(ajustarDiaPago(periodo, contrato.getDiaPago()));
                    cargo.setImporte(nz(contrato.getCanon()).setScale(2, RoundingMode.HALF_EVEN));
                    cargo.setConcepto("Renta " + periodo);
                    cargo.setEstado("Pendiente");
                    cargo.setCreadoPor(actor.getId());
                    session.persist(cargo);
                    session.flush();

                    aplicarCreditoDisponibleEnSesion(session, cargo, actor.getId(), tiempo);
                    auditoria.registrar(session, actor, UUID.randomUUID(),
                            "CARGO_RENTA_GENERADO",
                            "cargo:" + cargo.getId() + ";contrato:" + contratoId);
                    return Optional.of(cargo.getId());
                });
                if (generado.isPresent()) {
                    confirmados++;
                    generados.add(generado.get());
                } else {
                    omitidos++;
                }
            } catch (ConflictoDatosException e) {
                omitidos++;
            } catch (SecurityException | RelojDesfasadoException e) {
                throw e;
            } catch (RuntimeException e) {
                fallidos++;
            }

            procesados++;
            int pendientes = total - procesados;
            if (progreso != null) {
                progreso.accept(new ProgresoLoteDTO(
                        procesados, total, confirmados, omitidos, fallidos, pendientes,
                        false, "Procesando contrato " + procesados + " de " + total + "…"));
            }
            if (ganchoEntreContratos != null) {
                ganchoEntreContratos.run();
            }
        }

        int pendientes = total - procesados;
        if (cancelado && progreso != null) {
            progreso.accept(new ProgresoLoteDTO(
                    procesados, total, confirmados, omitidos, fallidos, pendientes,
                    true, MSG_059));
        }

        return new ResultadoLoteDTO(
                periodo.toString(), total, confirmados, omitidos, fallidos, pendientes,
                cancelado, mensaje, List.copyOf(generados));
    }

    /**
     * Aplica automáticamente el saldo a favor disponible del contrato sobre un
     * cargo recién creado (RF-M05-11 / RN-06) dentro de la misma transacción.
     */
    public static BigDecimal aplicarCreditoDisponibleEnSesion(Session session, Cargo cargo,
                                                              Long actorId,
                                                              RelojService.TiempoOperativo tiempo) {
        Objects.requireNonNull(session, "Falta la sesión.");
        Objects.requireNonNull(cargo, "Falta el cargo.");
        Objects.requireNonNull(tiempo, "Falta el instante operativo.");
        if ("Cancelado".equals(cargo.getEstadoPersistido()) || "Anulado".equals(cargo.getEstadoPersistido())) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal yaAplicadoCargo = session.createQuery("""
                        select sum(a.monto)
                        from AplicacionPago a left join Pago p on a.pagoId = p.id
                        where a.cargoId = :cargoId
                          and a.revertidaEn is null
                          and (a.pagoId is null or p.estado <> 'Anulado')
                        """, BigDecimal.class)
                .setParameter("cargoId", cargo.getId())
                .getSingleResult();
        BigDecimal saldoRestante = cargo.saldoPendiente(yaAplicadoCargo);
        if (saldoRestante.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        List<UUID> pagosCandidatos = session.createQuery("""
                        select p.id from Pago p
                        where p.contratoId = :contratoId
                          and p.estado = 'Registrado'
                        order by p.fechaPago asc, p.creadoEn asc, p.id asc
                        """, UUID.class)
                .setParameter("contratoId", cargo.getContratoId())
                .getResultList();

        BigDecimal totalAplicado = BigDecimal.ZERO.setScale(2);
        for (UUID pagoId : pagosCandidatos) {
            if (saldoRestante.signum() <= 0) break;
            Pago pago = bloquearPago(session, pagoId);
            if (pago == null || !"Registrado".equals(pago.getEstado())) continue;

            BigDecimal usadoPago = session.createQuery("""
                            select sum(a.monto) from AplicacionPago a
                            where a.pagoId = :pagoId and a.revertidaEn is null
                            """, BigDecimal.class)
                    .setParameter("pagoId", pagoId)
                    .getSingleResult();
            BigDecimal disponible = nz(pago.getMontoContrato()).subtract(nz(usadoPago))
                    .setScale(2, RoundingMode.HALF_EVEN);
            if (disponible.signum() <= 0) continue;

            BigDecimal abono = saldoRestante.min(disponible).setScale(2, RoundingMode.HALF_EVEN);
            if (abono.signum() <= 0) continue;

            session.persist(AplicacionPago.desdeCredito(
                    pagoId, cargo.getId(), abono, tiempo.fecha(), tiempo.instante(), actorId));
            totalAplicado = totalAplicado.add(abono);
            saldoRestante = saldoRestante.subtract(abono).setScale(2, RoundingMode.HALF_EVEN);
        }
        return totalAplicado;
    }

    public ResultadoRevisionH2DTO revisarMoraYVencimientos(LocalDate fechaOpcional) {
        return revisarMoraYVencimientos(fechaOpcional, null);
    }

    public ResultadoRevisionH2DTO revisarMoraYVencimientos(LocalDate fechaOpcional,
                                                          BooleanSupplier cancelacion) {
        SesionUsuario identidad = SesionUsuario.getActual();
        if (identidad == null) {
            throw new SecurityException("Debe iniciar sesión para ejecutar la revisión de cobranza.");
        }
        return ejecutarRevisionH2(fechaOpcional, identidad, true, cancelacion);
    }

    public ResultadoRevisionH2DTO revisarMoraYVencimientosSistema(LocalDate fechaOpcional) {
        return revisarMoraYVencimientosSistema(fechaOpcional, null);
    }

    public ResultadoRevisionH2DTO revisarMoraYVencimientosSistema(LocalDate fechaOpcional,
                                                                 BooleanSupplier cancelacion) {
        return ejecutarRevisionH2(fechaOpcional, null, false, cancelacion);
    }

    private ResultadoRevisionH2DTO ejecutarRevisionH2(LocalDate fechaOpcional,
                                                      SesionUsuario identidad,
                                                      boolean exigirUsuario,
                                                      BooleanSupplier cancelacion) {
        record ContextoH2(LocalDate fechaEvaluacion, PoliticaMora politica, int diasPorVencer,
                          List<UUID> contratos, List<UUID> rentasCandidatas) {}

        ContextoH2 contexto = tx.ejecutar(session -> {
            if (exigirUsuario) {
                permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES);
            }
            RelojService.TiempoOperativo tiempo = reloj.comprobar(session);
            LocalDate fecha = fechaOpcional != null ? fechaOpcional : tiempo.fecha();
            PoliticaMora politica = leerPoliticaMora(session, tiempo.instante());
            int diasPorVencer = leerEnteroParametro(session, "dias_por_vencer", tiempo.instante(), 30);

            List<UUID> contratos = session.createQuery("""
                            select c.id from Contrato c
                            where c.estado in ('Vigente', 'Por vencer')
                            order by c.id
                            """, UUID.class)
                    .getResultList();

            List<UUID> rentas = politica.valor().signum() <= 0 ? List.of() : session.createQuery("""
                            select c.id from Cargo c
                            where c.tipo = 'Renta'
                              and c.estado not in ('Cancelado', 'Anulado')
                              and not exists (
                                  select 1 from Cargo m
                                  where m.cargoOrigenId = c.id and m.tipo = 'Mora'
                              )
                            order by c.venceEl asc, c.id asc
                            """, UUID.class)
                    .getResultList();

            return new ContextoH2(fecha, politica, diasPorVencer, contratos, rentas);
        });

        int marcadosPorVencer = 0;
        int marcadosVencido = 0;
        int morasGeneradas = 0;
        int incidentes = 0;
        List<UUID> morasIds = new ArrayList<>();

        for (UUID contratoId : contexto.contratos()) {
            if (cancelacion != null && cancelacion.getAsBoolean()) {
                break;
            }
            try {
                String transicion = tx.ejecutar(session -> {
                    Usuario actor = exigirUsuario
                            ? permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES)
                            : null;
                    Contrato contrato = bloquearContrato(session, contratoId);
                    if (contrato == null) return null;

                    if (contexto.fechaEvaluacion().isAfter(contrato.getFin())
                            && ("Vigente".equals(contrato.getEstado())
                            || "Por vencer".equals(contrato.getEstado()))) {
                        contrato.transicionarA("Vencido", null);
                        registrarAuditoriaH2(session, actor, "REVISION_VENCIMIENTO",
                                "origen=H2;contrato=" + contratoId + ";estado=Vencido");
                        return "Vencido";
                    }

                    long diasRestantes = ChronoUnit.DAYS.between(
                            contexto.fechaEvaluacion(), contrato.getFin());
                    if (diasRestantes >= 0 && diasRestantes <= contexto.diasPorVencer()
                            && "Vigente".equals(contrato.getEstado())) {
                        contrato.transicionarA("Por vencer", null);
                        registrarAuditoriaH2(session, actor, "REVISION_VENCIMIENTO",
                                "origen=H2;contrato=" + contratoId + ";estado=Por vencer");
                        return "Por vencer";
                    }
                    return null;
                });
                if ("Vencido".equals(transicion)) marcadosVencido++;
                else if ("Por vencer".equals(transicion)) marcadosPorVencer++;
            } catch (RuntimeException e) {
                incidentes++;
                registrarIncidenteH2(contratoId, e);
            }
        }

        if (contexto.politica().valor().signum() > 0) {
            for (UUID rentaId : contexto.rentasCandidatas()) {
                if (cancelacion != null && cancelacion.getAsBoolean()) {
                    break;
                }
                try {
                    Optional<UUID> moraId = tx.ejecutar(session -> {
                        Usuario actor = exigirUsuario
                                ? permisos.exigir(session, identidad, Permiso.OPERAR_ALQUILERES)
                                : null;
                        Cargo renta = session.find(Cargo.class, rentaId);
                        if (renta == null || !"Renta".equals(renta.getTipo())
                                || "Cancelado".equals(renta.getEstadoPersistido())
                                || "Anulado".equals(renta.getEstadoPersistido())) {
                            return Optional.<UUID>empty();
                        }
                        LocalDate finGracia = renta.getVenceEl().plusDays(contexto.politica().diasGracia());
                        if (!contexto.fechaEvaluacion().isAfter(finGracia)) {
                            return Optional.<UUID>empty();
                        }

                        bloquearContrato(session, renta.getContratoId());
                        renta = bloquearCargo(session, rentaId);
                        if (renta == null) return Optional.<UUID>empty();

                        Long morasExistentes = session.createQuery("""
                                        select count(m) from Cargo m
                                        where m.cargoOrigenId = :origenId and m.tipo = 'Mora'
                                        """, Long.class)
                                .setParameter("origenId", rentaId)
                                .getSingleResult();
                        if (morasExistentes > 0) return Optional.<UUID>empty();

                        BigDecimal aplicado = session.createQuery("""
                                        select sum(a.monto)
                                        from AplicacionPago a left join Pago p on a.pagoId = p.id
                                        where a.cargoId = :cargoId
                                          and a.revertidaEn is null
                                          and (a.pagoId is null or p.estado <> 'Anulado')
                                        """, BigDecimal.class)
                                .setParameter("cargoId", rentaId)
                                .getSingleResult();
                        BigDecimal principalPendiente = renta.saldoPendiente(aplicado);
                        if (principalPendiente.signum() <= 0) {
                            return Optional.<UUID>empty();
                        }

                        BigDecimal importeMora = calcularImporteMora(principalPendiente, contexto.politica());
                        if (importeMora.signum() <= 0) {
                            return Optional.<UUID>empty();
                        }

                        Cargo mora = new Cargo();
                        mora.setContratoId(renta.getContratoId());
                        mora.setCargoOrigenId(renta.getId());
                        mora.setTipo("Mora");
                        mora.setPeriodo(renta.getPeriodo());
                        mora.setFechaEmision(contexto.fechaEvaluacion());
                        mora.setVenceEl(contexto.fechaEvaluacion());
                        mora.setImporte(importeMora);
                        mora.setConcepto("Mora por renta " + YearMonth.from(renta.getPeriodo()));
                        mora.setEstado("Pendiente");
                        if (actor != null) mora.setCreadoPor(actor.getId());
                        session.persist(mora);
                        session.flush();

                        String ref = String.format(Locale.ROOT,
                                "origen=H2;pol=%s;tipo=%s;val=%s;base=%s",
                                contexto.politica().politicaId(),
                                contexto.politica().tipo(),
                                contexto.politica().valor().toPlainString(),
                                principalPendiente.toPlainString());
                        if (ref.length() > 160) ref = ref.substring(0, 160);
                        registrarAuditoriaH2(session, actor, "MORA_AUTOMATICA", ref);
                        return Optional.of(mora.getId());
                    });
                    if (moraId.isPresent()) {
                        morasGeneradas++;
                        morasIds.add(moraId.get());
                    }
                } catch (ConflictoDatosException e) {
                    // Otra instancia H2 generó la mora concurrentemente; no es incidente.
                } catch (RuntimeException e) {
                    incidentes++;
                    registrarIncidenteH2(rentaId, e);
                }
            }
        }

        return new ResultadoRevisionH2DTO(
                contexto.fechaEvaluacion(),
                contexto.contratos().size(),
                marcadosPorVencer,
                marcadosVencido,
                morasGeneradas,
                incidentes,
                List.copyOf(morasIds));
    }

    public CarteraResumenDTO consultarCartera(LocalDate corte, UUID contratoId,
                                              YearMonth periodo, String moneda, String texto) {
        String monedaFiltro = moneda == null || moneda.isBlank()
                ? null : moneda.strip().toUpperCase(Locale.ROOT);
        if (monedaFiltro != null && !MONEDAS.contains(monedaFiltro)) {
            throw new IllegalArgumentException("La moneda de filtro debe ser USD o NIO.");
        }
        String criterio = texto == null || texto.isBlank()
                ? null : PatronLike.contieneLiteral(texto.strip().toLowerCase(Locale.ROOT));

        return tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            LocalDate fechaCorte = corte != null ? corte : reloj.leer(session).fecha();
            Instant finCorte = fechaCorte.plusDays(1)
                    .atStartOfDay(RelojService.ZONA_OPERATIVA).toInstant();

            StringBuilder hql = new StringBuilder("""
                    select c from Cargo c
                    join fetch c.contrato con
                    join fetch con.inquilino i
                    join fetch con.unidad u
                    join fetch u.propiedad prop
                    where c.fechaEmision <= :corte
                    """);
            if (contratoId != null) hql.append(" and c.contratoId = :contratoId");
            if (periodo != null) hql.append(" and c.periodo = :periodo");
            if (monedaFiltro != null) hql.append(" and con.moneda = :moneda");
            if (criterio != null) {
                hql.append("""
                         and (lower(c.concepto) like :texto escape '!'
                           or lower(c.tipo) like :texto escape '!'
                           or lower(i.nombre) like :texto escape '!'
                           or lower(prop.nombre) like :texto escape '!'
                           or lower(u.codigo) like :texto escape '!')
                        """);
            }
            hql.append(" order by c.venceEl asc, c.id asc");

            var consulta = session.createQuery(hql.toString(), Cargo.class)
                    .setParameter("corte", fechaCorte);
            if (contratoId != null) consulta.setParameter("contratoId", contratoId);
            if (periodo != null) consulta.setParameter("periodo", periodo.atDay(1));
            if (monedaFiltro != null) consulta.setParameter("moneda", monedaFiltro);
            if (criterio != null) consulta.setParameter("texto", criterio);

            List<Cargo> cargos = consulta.getResultList();
            if (cargos.isEmpty()) {
                return new CarteraResumenDTO(fechaCorte, List.of(), List.of());
            }

            List<UUID> cargoIds = cargos.stream().map(Cargo::getId).toList();
            Map<UUID, BigDecimal> aplicadoPorCargo = calcularAplicadoAlCorte(
                    session, cargoIds, fechaCorte, finCorte);

            List<CarteraItemDTO> items = new ArrayList<>();
            Map<String, AcumuladorCartera> porMoneda = new HashMap<>();
            for (Cargo cargo : cargos) {
                if (estaAnuladoAlCorte(cargo, finCorte)) continue;
                BigDecimal importe = nz(cargo.getImporte()).setScale(2, RoundingMode.HALF_EVEN);
                BigDecimal aplicado = nz(aplicadoPorCargo.get(cargo.getId()))
                        .setScale(2, RoundingMode.HALF_EVEN);
                cargo.establecerEstadoDerivado(aplicado, fechaCorte);
                BigDecimal saldo = cargo.saldoPendiente(aplicado);
                if (saldo.signum() <= 0) continue;

                long diasVencidos = Math.max(0L, ChronoUnit.DAYS.between(cargo.getVenceEl(), fechaCorte));
                String tramo = clasificarTramo(diasVencidos);
                String monedaCargo = cargo.getMonedaContrato();

                items.add(new CarteraItemDTO(
                        cargo.getId(),
                        cargo.getContratoId(),
                        cargo.getContratoInfo(),
                        monedaCargo,
                        cargo.getTipo(),
                        cargo.getPeriodo(),
                        cargo.getFechaEmision(),
                        cargo.getVenceEl(),
                        cargo.getConcepto(),
                        importe,
                        aplicado,
                        saldo,
                        cargo.getEstado(),
                        diasVencidos,
                        tramo));

                porMoneda.computeIfAbsent(monedaCargo, AcumuladorCartera::new)
                        .acumular(tramo, saldo);
            }

            List<CarteraTramoMonedaDTO> totales = MONEDAS.stream()
                    .map(porMoneda::get)
                    .filter(Objects::nonNull)
                    .map(AcumuladorCartera::aDto)
                    .toList();
            return new CarteraResumenDTO(fechaCorte, List.copyOf(items), totales);
        });
    }

    public static String clasificarTramo(long diasVencidos) {
        if (diasVencidos <= 0) return TRAMO_AL_DIA;
        if (diasVencidos <= 30) return TRAMO_1_30;
        if (diasVencidos <= 60) return TRAMO_31_60;
        if (diasVencidos <= 90) return TRAMO_61_90;
        return TRAMO_MAS_90;
    }

    private static LocalDate ajustarDiaPago(YearMonth periodo, int diaPago) {
        int dia = Math.min(Math.max(diaPago, 1), periodo.lengthOfMonth());
        return periodo.atDay(dia);
    }

    private static Map<UUID, BigDecimal> calcularCreditoPorContrato(Session session, List<UUID> contratoIds) {
        Map<UUID, BigDecimal> totalesPago = new HashMap<>();
        List<Pago> pagos = session.createQuery("""
                        from Pago p
                        where p.contratoId in :ids and p.estado = 'Registrado'
                        """, Pago.class)
                .setParameter("ids", contratoIds)
                .getResultList();
        if (pagos.isEmpty()) return Map.of();

        List<UUID> pagoIds = pagos.stream().map(Pago::getId).toList();
        Map<UUID, BigDecimal> usadoPorPago = new HashMap<>();
        for (Object[] fila : session.createQuery("""
                        select a.pagoId, sum(a.monto)
                        from AplicacionPago a
                        where a.pagoId in :pagoIds and a.revertidaEn is null
                        group by a.pagoId
                        """, Object[].class)
                .setParameter("pagoIds", pagoIds)
                .getResultList()) {
            usadoPorPago.put((UUID) fila[0], nz((BigDecimal) fila[1]));
        }

        for (Pago pago : pagos) {
            BigDecimal disponible = nz(pago.getMontoContrato())
                    .subtract(nz(usadoPorPago.get(pago.getId())));
            if (disponible.signum() > 0) {
                totalesPago.merge(pago.getContratoId(), disponible, BigDecimal::add);
            }
        }
        return totalesPago;
    }

    private static Map<UUID, BigDecimal> calcularAplicadoAlCorte(Session session, List<UUID> cargoIds,
                                                                 LocalDate fechaCorte, Instant finCorte) {
        Map<UUID, BigDecimal> aplicado = new HashMap<>();
        for (Object[] fila : session.createQuery("""
                        select a, p from AplicacionPago a left join Pago p on a.pagoId = p.id
                        where a.cargoId in :cargoIds
                        """, Object[].class)
                .setParameter("cargoIds", cargoIds)
                .getResultList()) {
            AplicacionPago app = (AplicacionPago) fila[0];
            Pago pago = (Pago) fila[1];
            if (app.getRevertidaEn() != null && app.getRevertidaEn().isBefore(finCorte)) continue;
            if (app.getPagoId() != null) {
                if (pago == null) continue;
                boolean pagoAnuladoAlCorte = "Anulado".equals(pago.getEstado())
                        && (pago.getAnuladoEn() == null || pago.getAnuladoEn().isBefore(finCorte));
                if (pagoAnuladoAlCorte) continue;
                if (pago.getFechaPago().isAfter(fechaCorte)) continue;
                if (app.getFechaAplicacion() != null && app.getFechaAplicacion().isAfter(fechaCorte)) continue;
                aplicado.merge(app.getCargoId(), app.getMonto(), BigDecimal::add);
            } else if (app.getMovimientoId() != null) {
                MovimientoGarantia mov = session.find(MovimientoGarantia.class, app.getMovimientoId());
                if (mov != null && !mov.getFechaMovimiento().isAfter(fechaCorte)) {
                    aplicado.merge(app.getCargoId(), app.getMonto(), BigDecimal::add);
                }
            }
        }
        return aplicado;
    }

    private static boolean estaAnuladoAlCorte(Cargo cargo, Instant finCorte) {
        String persistido = cargo.getEstadoPersistido();
        if (!"Cancelado".equals(persistido) && !"Anulado".equals(persistido)) return false;
        return cargo.getAnuladoEn() == null || cargo.getAnuladoEn().isBefore(finCorte);
    }

    private void registrarAuditoriaH2(Session session, Usuario actor, String accion, String referencia) {
        if (actor != null) {
            auditoria.registrar(session, actor, UUID.randomUUID(), accion, referencia);
        } else {
            auditoria.registrarSistema(session, UUID.randomUUID(), accion, referencia);
        }
    }

    private void registrarIncidenteH2(UUID entidadId, RuntimeException error) {
        try {
            tx.ejecutar(session -> {
                String detalle = error.getClass().getSimpleName();
                String ref = "origen=H2;entidad=" + entidadId + ";error=" + detalle;
                if (ref.length() > 160) ref = ref.substring(0, 160);
                auditoria.registrarSistema(session, UUID.randomUUID(), "INCIDENTE_H2", ref);
                return null;
            });
        } catch (RuntimeException ignored) {
            // No ocultar el conteo de incidentes si falla el registro secundario.
        }
    }

    private static PoliticaMora leerPoliticaMora(Session session, Instant ahora) {
        Optional<ParametroSistema> paramDias = ConfiguracionService.buscarParametroVigente(
                session, "dias_gracia", ahora);
        if (paramDias.isEmpty()) {
            paramDias = ConfiguracionService.buscarParametroVigente(session, "dias.gracia.mora", ahora);
        }
        int diasGracia = paramDias.map(p -> parsearEntero(p.getValor(), 3)).orElse(3);
        if (diasGracia < 0) diasGracia = 0;

        Optional<ParametroSistema> paramTipo = ConfiguracionService.buscarParametroVigente(
                session, "mora_tipo", ahora);
        String tipo = paramTipo.map(p -> limpiarValorJson(p.getValor()))
                .filter(v -> "Fijo".equalsIgnoreCase(v) || "Porcentaje".equalsIgnoreCase(v))
                .map(v -> "Fijo".equalsIgnoreCase(v) ? "Fijo" : "Porcentaje")
                .orElse("Porcentaje");

        Optional<ParametroSistema> paramValor = ConfiguracionService.buscarParametroVigente(
                session, "mora_valor", ahora);
        BigDecimal valor = paramValor.map(p -> parsearDecimal(p.getValor(), BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);
        if (valor.signum() < 0) valor = BigDecimal.ZERO;

        Optional<ParametroSistema> paramDiasFinal = paramDias;
        String politicaId = paramValor.map(p -> p.getId().toString())
                .or(() -> paramDiasFinal.map(p -> p.getId().toString()))
                .orElse("default");
        return new PoliticaMora(politicaId, diasGracia, tipo, valor);
    }

    private static int leerEnteroParametro(Session session, String clave, Instant ahora, int defecto) {
        return ConfiguracionService.buscarParametroVigente(session, clave, ahora)
                .map(p -> parsearEntero(p.getValor(), defecto))
                .filter(v -> v >= 0)
                .orElse(defecto);
    }

    private static BigDecimal calcularImporteMora(BigDecimal principalPendiente, PoliticaMora politica) {
        if ("Fijo".equalsIgnoreCase(politica.tipo())) {
            return politica.valor().setScale(2, RoundingMode.HALF_EVEN);
        }
        return principalPendiente.multiply(politica.valor())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
    }

    private static String limpiarValorJson(String raw) {
        if (raw == null) return "";
        String limpio = raw.strip();
        if (limpio.length() >= 2 && limpio.startsWith("\"") && limpio.endsWith("\"")) {
            limpio = limpio.substring(1, limpio.length() - 1);
        }
        return limpio.strip();
    }

    private static int parsearEntero(String raw, int defecto) {
        try {
            return Integer.parseInt(limpiarValorJson(raw));
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static BigDecimal parsearDecimal(String raw, BigDecimal defecto) {
        try {
            return new BigDecimal(limpiarValorJson(raw));
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static Contrato bloquearContrato(Session session, UUID contratoId) {
        Object fila = session.createNativeQuery("select id from contrato where id = :id for update")
                .setParameter("id", contratoId)
                .uniqueResult();
        return fila == null ? null : session.find(Contrato.class, contratoId);
    }

    private static Cargo bloquearCargo(Session session, UUID cargoId) {
        Object fila = session.createNativeQuery("select id from cargo where id = :id for update")
                .setParameter("id", cargoId)
                .uniqueResult();
        return fila == null ? null : session.find(Cargo.class, cargoId);
    }

    private static Pago bloquearPago(Session session, UUID pagoId) {
        Object fila = session.createNativeQuery("select id from pago where id = :id for update")
                .setParameter("id", pagoId)
                .uniqueResult();
        return fila == null ? null : session.find(Pago.class, pagoId);
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private record PoliticaMora(String politicaId, int diasGracia, String tipo, BigDecimal valor) { }

    private static final class AcumuladorCartera {
        private final String moneda;
        private long cantidad;
        private BigDecimal alDia = BigDecimal.ZERO.setScale(2);
        private BigDecimal tramo1a30 = BigDecimal.ZERO.setScale(2);
        private BigDecimal tramo31a60 = BigDecimal.ZERO.setScale(2);
        private BigDecimal tramo61a90 = BigDecimal.ZERO.setScale(2);
        private BigDecimal tramoMasDe90 = BigDecimal.ZERO.setScale(2);

        private AcumuladorCartera(String moneda) {
            this.moneda = moneda;
        }

        private void acumular(String tramo, BigDecimal saldo) {
            cantidad++;
            switch (tramo) {
                case TRAMO_AL_DIA -> alDia = alDia.add(saldo);
                case TRAMO_1_30 -> tramo1a30 = tramo1a30.add(saldo);
                case TRAMO_31_60 -> tramo31a60 = tramo31a60.add(saldo);
                case TRAMO_61_90 -> tramo61a90 = tramo61a90.add(saldo);
                default -> tramoMasDe90 = tramoMasDe90.add(saldo);
            }
        }

        private CarteraTramoMonedaDTO aDto() {
            BigDecimal total = alDia.add(tramo1a30).add(tramo31a60).add(tramo61a90).add(tramoMasDe90);
            return new CarteraTramoMonedaDTO(
                    moneda, cantidad, alDia, tramo1a30, tramo31a60, tramo61a90, tramoMasDe90, total);
        }
    }
}
