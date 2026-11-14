package ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Json;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.AplicacionResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.EstadoCuentaResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoDetalleResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRegistroResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.PagoRequest;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto.ReciboResponse;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ReciboDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudIdempotente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PagoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.Permiso;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PermisoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * API 4: cobranza y pagos. Las escrituras usan una Idempotency-Key persistente
 * y comparten transacción con el pago para que no existan cobros duplicados.
 */
public final class ServicioPagos {
    private static final String RECURSO = "PAGOS";
    private static final Set<String> MONEDAS = Set.of("USD", "NIO");
    private static final int MAXIMO_DISTRIBUCIONES = 100;

    private final HibernateTransactions tx;
    private final PagoService pagos;
    private final PermisoService permisos = new PermisoService();

    public ServicioPagos(HibernateTransactions tx) {
        this.tx = Objects.requireNonNull(tx, "Faltan las transacciones de la API.");
        this.pagos = new PagoService(tx);
    }

    public PagoRegistroResponse registrar(String clave, PagoRequest solicitud) {
        String idempotencyKey = claveUuid(clave);
        DatosPago datos = validar(solicitud);
        String huella = huella(datos);
        try {
            return tx.ejecutar(session ->
                    registrarEnSesion(session, idempotencyKey, huella, datos));
        } catch (ConflictoDatosException e) {
            // Otra petición pudo insertar la misma clave en paralelo. Si ya
            // terminó, se repite su respuesta; si no coincide, se conserva 409.
            PagoRegistroResponse repetida = buscarRepeticion(idempotencyKey, huella);
            if (repetida != null) return repetida;
            throw e;
        }
    }

    public PagoDetalleResponse obtenerPago(UUID id) {
        Pago pago = pagos.obtener(id).orElseThrow(ExcepcionApi::noEncontrado);
        Recibo recibo = pagos.obtenerReciboDePago(id).orElse(null);
        List<AplicacionPago> aplicaciones = pagos.listarAplicaciones(id);
        BigDecimal aplicado = aplicaciones.stream()
                .map(AplicacionPago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credito = nz(pago.getMontoContrato()).subtract(aplicado)
                .setScale(2, RoundingMode.HALF_EVEN);
        return aDetalle(pago, recibo, aplicaciones, credito);
    }

    public ReciboResponse obtenerRecibo(long numero) {
        if (numero <= 0) {
            throw ExcepcionApi.solicitudInvalida(
                    "El número de recibo debe ser un entero positivo.");
        }
        Recibo recibo = pagos.obtenerRecibo(numero).orElseThrow(ExcepcionApi::noEncontrado);
        ReciboDTO dto = recibo.presentar();
        return new ReciboResponse(
                dto.getNumero(),
                dto.getPagoId().toString(),
                dto.getContratoId().toString(),
                dto.getFechaPago().toString(),
                dto.getContratoInfo(),
                decimal(dto.getMontoOriginal()),
                dto.getMonedaOriginal(),
                decimal(dto.getMontoContrato()),
                dto.getMonedaContrato(),
                dto.getMetodo(),
                dto.getReferencia(),
                dto.isAnulado());
    }

    public PagoDetalleResponse anular(UUID id, String motivo) {
        String limpio = textoRequerido(motivo, "motivo de anulación", 500);
        Pago pago = pagos.anular(id, limpio).orElseThrow(ExcepcionApi::noEncontrado);
        return obtenerPago(pago.getId());
    }

    public EstadoCuentaResponse estadoCuenta(UUID contratoId, LocalDate corte) {
        Objects.requireNonNull(contratoId, "Falta el contrato.");
        LocalDate fechaCorte = corte == null ? LocalDate.now() : corte;
        EstadoCuentaResponse respuesta = tx.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.OPERAR_ALQUILERES);
            Contrato contrato = session.createQuery("""
                            from Contrato c
                            join fetch c.unidad
                            join fetch c.inquilino
                            left join fetch c.garantiaSeguimiento
                            where c.id = :id
                            """, Contrato.class)
                    .setParameter("id", contratoId)
                    .uniqueResult();
            if (contrato == null) return null;

            List<Cargo> cargos = session.createQuery("""
                            from Cargo c
                            where c.contratoId = :contratoId and c.fechaEmision <= :corte
                            order by c.fechaEmision, c.periodo, c.tipo
                            """, Cargo.class)
                    .setParameter("contratoId", contratoId)
                    .setParameter("corte", fechaCorte)
                    .getResultList();
            List<Pago> listaPagos = session.createQuery("""
                            from Pago p
                            where p.contratoId = :contratoId and p.fechaPago <= :corte
                            order by p.fechaPago, p.id
                            """, Pago.class)
                    .setParameter("contratoId", contratoId)
                    .setParameter("corte", fechaCorte)
                    .getResultList();

            Map<UUID, Pago> pagosPorId = new HashMap<>();
            for (Pago pago : listaPagos) pagosPorId.put(pago.getId(), pago);

            Set<UUID> cargosVigentes = new HashSet<>();
            for (Cargo cargo : cargos) {
                if (!"Cancelado".equals(cargo.getEstado())) cargosVigentes.add(cargo.getId());
            }

            Map<UUID, BigDecimal> aplicadoPorCargo = new HashMap<>();
            Map<UUID, BigDecimal> aplicadoIncluidoPorPago = new HashMap<>();
            if (!listaPagos.isEmpty()) {
                List<AplicacionPago> aplicaciones = session.createQuery("""
                                from AplicacionPago a where a.pagoId in :pagos
                                """, AplicacionPago.class)
                        .setParameter("pagos", pagosPorId.keySet())
                        .getResultList();
                for (AplicacionPago aplicacion : aplicaciones) {
                    Pago pago = pagosPorId.get(aplicacion.getPagoId());
                    if (pago == null || "Anulado".equals(pago.getEstado())) continue;
                    aplicadoPorCargo.merge(aplicacion.getCargoId(),
                            aplicacion.getMonto(), BigDecimal::add);
                    if (cargosVigentes.contains(aplicacion.getCargoId())) {
                        aplicadoIncluidoPorPago.merge(aplicacion.getPagoId(),
                                aplicacion.getMonto(), BigDecimal::add);
                    }
                }
            }

            Map<UUID, Long> recibos = new HashMap<>();
            if (!listaPagos.isEmpty()) {
                for (Object[] fila : session.createQuery("""
                                select r.numero, p.id from Recibo r join r.pago p
                                where p.id in :pagos
                                """, Object[].class)
                        .setParameter("pagos", pagosPorId.keySet())
                        .getResultList()) {
                    recibos.put((UUID) fila[1], ((Number) fila[0]).longValue());
                }
            }

            List<EstadoCuentaResponse.CargoEstadoCuenta> cargosDto = new ArrayList<>();
            BigDecimal totalCargos = BigDecimal.ZERO;
            BigDecimal totalPagado = BigDecimal.ZERO;
            for (Cargo cargo : cargos) {
                BigDecimal importe = nz(cargo.getImporte());
                BigDecimal aplicado = nz(aplicadoPorCargo.get(cargo.getId()));
                BigDecimal saldo = importe.subtract(aplicado);
                boolean cancelado = "Cancelado".equals(cargo.getEstado());
                String estado = cancelado ? "Cancelado"
                        : aplicado.compareTo(importe) >= 0 ? "Pagado"
                        : aplicado.signum() > 0 ? "Parcial" : cargo.getEstado();
                if (!cancelado) {
                    totalCargos = totalCargos.add(importe);
                    totalPagado = totalPagado.add(aplicado);
                }
                cargosDto.add(new EstadoCuentaResponse.CargoEstadoCuenta(
                        cargo.getId().toString(),
                        cargo.getTipo(),
                        cargo.getPeriodo().toString(),
                        cargo.getFechaEmision().toString(),
                        cargo.getVenceEl().toString(),
                        cargo.getConcepto(),
                        decimal(importe),
                        decimal(aplicado),
                        decimal(saldo),
                        estado));
            }

            List<EstadoCuentaResponse.PagoEstadoCuenta> pagosDto = new ArrayList<>();
            BigDecimal totalCredito = BigDecimal.ZERO;
            for (Pago pago : listaPagos) {
                boolean anulado = "Anulado".equals(pago.getEstado());
                BigDecimal aplicado = anulado ? BigDecimal.ZERO
                        : nz(aplicadoIncluidoPorPago.get(pago.getId()));
                BigDecimal credito = anulado ? BigDecimal.ZERO
                        : nz(pago.getMontoContrato()).subtract(aplicado);
                if (!anulado) totalCredito = totalCredito.add(credito);
                pagosDto.add(new EstadoCuentaResponse.PagoEstadoCuenta(
                        pago.getId().toString(),
                        recibos.get(pago.getId()),
                        pago.getFechaPago().toString(),
                        decimal(pago.getMontoOriginal()),
                        pago.getMonedaOriginal(),
                        decimal(pago.getMontoContrato()),
                        pago.getMetodo(),
                        pago.getReferencia(),
                        pago.getEstado(),
                        decimal(aplicado),
                        decimal(credito)));
            }

            Garantia garantia = contrato.getGarantiaSeguimiento();
            EstadoCuentaResponse.GarantiaEstadoCuenta garantiaDto = garantia == null ? null
                    : new EstadoCuentaResponse.GarantiaEstadoCuenta(
                    decimal(contrato.getDepositoPactado()),
                    decimal(garantia.getSaldoActual()));

            return new EstadoCuentaResponse(
                    contrato.getId().toString(),
                    fechaCorte.toString(),
                    contrato.getMoneda(),
                    decimal(contrato.getCanon()),
                    cargosDto,
                    pagosDto,
                    new EstadoCuentaResponse.TotalesEstadoCuenta(
                            decimal(totalCargos),
                            decimal(totalPagado),
                            decimal(totalCargos.subtract(totalPagado)),
                            decimal(totalCredito)),
                    garantiaDto);
        });
        if (respuesta == null) throw ExcepcionApi.noEncontrado();
        return respuesta;
    }

    private PagoRegistroResponse registrarEnSesion(Session session, String clave,
                                                   String huella, DatosPago datos) {
        SesionUsuario sesion = SesionUsuario.getActual();
        if (sesion == null) throw new SecurityException("No hay una sesión activa.");

        SolicitudIdempotente existente = buscarPorClave(session, clave);
        if (existente != null) {
            return repetir(existente, huella, sesion);
        }

        SolicitudIdempotente registro = new SolicitudIdempotente(clave,
                sesion.getUsuarioId(), RECURSO, huella, Instant.now());
        session.persist(registro);
        session.flush();

        Pago pago = new Pago();
        pago.setContratoId(datos.contratoId());
        pago.setFechaPago(datos.fechaPago());
        pago.setMonedaOriginal(datos.moneda());
        pago.setMontoOriginal(datos.monto());
        pago.setMetodo(datos.metodo());
        pago.setReferencia(datos.referencia());
        pago.setNotas(datos.notas());
        pago.setEstado("Registrado");

        List<AplicacionPago> aplicaciones = datos.distribucion().stream()
                .map(d -> new AplicacionPago(null, d.cargoId(), d.monto()))
                .toList();
        PagoService.RegistroPago resultado = pagos.registrarPagoEnSesion(
                session, pago, aplicaciones);

        PagoRegistroResponse respuesta = new PagoRegistroResponse(
                resultado.pago().getId().toString(),
                resultado.recibo().getNumero(),
                decimal(resultado.pago().getMontoContrato()),
                decimal(resultado.credito()));
        registro.completar(201, Json.escribir(respuesta), Instant.now());
        return respuesta;
    }

    private PagoRegistroResponse buscarRepeticion(String clave, String huella) {
        return tx.ejecutar(session -> {
            SesionUsuario sesion = SesionUsuario.getActual();
            if (sesion == null) return null;
            SolicitudIdempotente existente = buscarPorClave(session, clave);
            return existente == null ? null : repetir(existente, huella, sesion);
        });
    }

    private static PagoRegistroResponse repetir(SolicitudIdempotente existente, String huella,
                                                SesionUsuario sesion) {
        if (!existente.getUsuarioId().equals(sesion.getUsuarioId())
                || !RECURSO.equals(existente.getRecurso())
                || !existente.getHashPeticion().equals(huella)) {
            throw new ConflictoDatosException(
                    "La Idempotency-Key ya fue utilizada con un contenido diferente.");
        }
        if (!existente.isCompletada()) {
            throw new ConflictoDatosException(
                    "La solicitud con esa Idempotency-Key todavía se está procesando.");
        }
        return Json.gson().fromJson(existente.getRespuestaJson(),
                PagoRegistroResponse.class);
    }

    private static SolicitudIdempotente buscarPorClave(Session session, String clave) {
        return session.createQuery(
                        "from SolicitudIdempotente s where s.clave = :clave",
                        SolicitudIdempotente.class)
                .setParameter("clave", clave)
                .uniqueResult();
    }

    private static PagoDetalleResponse aDetalle(Pago pago, Recibo recibo,
                                                List<AplicacionPago> aplicaciones,
                                                BigDecimal credito) {
        List<AplicacionResponse> aplicacionesDto = aplicaciones.stream()
                .map(a -> new AplicacionResponse(a.getCargoId().toString(),
                        decimal(a.getMonto())))
                .toList();
        return new PagoDetalleResponse(
                pago.getId().toString(),
                pago.getContratoId().toString(),
                recibo == null ? pago.getContratoInfo() : recibo.getContratoInfo(),
                pago.getFechaPago().toString(),
                pago.getMonedaOriginal(),
                decimal(pago.getMontoOriginal()),
                decimal(pago.getMontoContrato()),
                pago.getMonedaContrato(),
                pago.getMetodo(),
                pago.getReferencia(),
                pago.getNotas(),
                pago.getEstado(),
                recibo == null ? null : recibo.getNumero(),
                decimal(credito),
                aplicacionesDto,
                pago.getMotivoAnulacion(),
                pago.getAnuladoEn() == null ? null : pago.getAnuladoEn().toString(),
                pago.getAnuladoPor());
    }

    private static DatosPago validar(PagoRequest solicitud) {
        if (solicitud == null) {
            throw ExcepcionApi.entidadNoProcesable("El cuerpo del pago es obligatorio.");
        }
        UUID contratoId = uuid(solicitud.contratoId(), "contratoId");
        LocalDate fechaPago = fecha(solicitud.fechaPago(), "fechaPago");
        String moneda = textoRequerido(solicitud.moneda(), "moneda", 3)
                .toUpperCase(Locale.ROOT);
        if (!MONEDAS.contains(moneda)) {
            throw ExcepcionApi.entidadNoProcesable("La moneda debe ser USD o NIO.");
        }
        BigDecimal monto = monto(solicitud.monto(), "monto");
        String metodo = textoRequerido(solicitud.metodo(), "metodo", 24);
        String referencia = textoOpcional(solicitud.referencia(), "referencia", 100);
        String notas = textoOpcional(solicitud.notas(), "notas", 255);

        List<Distribucion> distribucion = new ArrayList<>();
        if (solicitud.distribucion() != null) {
            if (solicitud.distribucion().size() > MAXIMO_DISTRIBUCIONES) {
                throw ExcepcionApi.entidadNoProcesable(
                        "La distribución no puede superar " + MAXIMO_DISTRIBUCIONES + " cargos.");
            }
            Set<UUID> vistos = new HashSet<>();
            for (var fila : solicitud.distribucion()) {
                if (fila == null) {
                    throw ExcepcionApi.entidadNoProcesable(
                            "La distribución contiene una fila vacía.");
                }
                UUID cargoId = uuid(fila.cargoId(), "cargoId");
                if (!vistos.add(cargoId)) {
                    throw ExcepcionApi.entidadNoProcesable(
                            "La distribución repite el mismo cargo.");
                }
                distribucion.add(new Distribucion(cargoId,
                        monto(fila.monto(), "monto de distribución")));
            }
        }
        return new DatosPago(contratoId, fechaPago, moneda, monto, metodo,
                referencia, notas, List.copyOf(distribucion));
    }

    private static String claveUuid(String valor) {
        String limpio = textoRequerido(valor, "Idempotency-Key", 36);
        try {
            return UUID.fromString(limpio).toString();
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "La cabecera Idempotency-Key debe ser un UUID válido.");
        }
    }

    private static String huella(DatosPago datos) {
        String distribucion = datos.distribucion().stream()
                .map(d -> d.cargoId() + ":" + d.monto().toPlainString())
                .sorted(Comparator.naturalOrder())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        String canonica = datos.contratoId() + "|" + datos.fechaPago() + "|"
                + datos.moneda() + "|" + datos.monto().toPlainString() + "|"
                + datos.metodo() + "|" + nulo(datos.referencia()) + "|"
                + nulo(datos.notas()) + "|" + distribucion;
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(canonica.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular la huella de la petición.", e);
        }
    }

    private static String nulo(String valor) {
        return valor == null ? "" : valor;
    }

    private static UUID uuid(String valor, String campo) {
        String limpio = textoRequerido(valor, campo, 36);
        try {
            return UUID.fromString(limpio);
        } catch (IllegalArgumentException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El campo " + campo + " no es un identificador válido.");
        }
    }

    private static LocalDate fecha(String valor, String campo) {
        String limpio = textoRequerido(valor, campo, 10);
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El campo " + campo + " debe tener el formato AAAA-MM-DD.");
        }
    }

    private static BigDecimal monto(String valor, String campo) {
        String limpio = textoRequerido(valor, campo, 40);
        BigDecimal numero;
        try {
            numero = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw ExcepcionApi.solicitudInvalida(
                    "El campo " + campo + " debe ser un número decimal.");
        }
        if (numero.signum() <= 0) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " debe ser mayor que cero.");
        }
        if (numero.stripTrailingZeros().scale() > 2) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " admite como máximo 2 decimales.");
        }
        return numero.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String textoRequerido(String valor, String campo, int maximo) {
        String limpio = textoOpcional(valor, campo, maximo);
        if (limpio == null) {
            throw ExcepcionApi.entidadNoProcesable(
                    "El campo " + campo + " es obligatorio.");
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

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String decimal(BigDecimal valor) {
        return nz(valor).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    private record DatosPago(UUID contratoId, LocalDate fechaPago, String moneda,
                             BigDecimal monto, String metodo, String referencia,
                             String notas, List<Distribucion> distribucion) { }

    private record Distribucion(UUID cargoId, BigDecimal monto) { }
}