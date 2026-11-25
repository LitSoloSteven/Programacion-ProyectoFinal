package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.AplicacionPago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Configuracion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ControlFx;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.MovimientoGarantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.ParametroSistema;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.PuertaMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudIdempotente;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AdjuntoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AlmacenAdjuntos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.MotorBaseRespaldo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.MotorPostgresRespaldo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RespaldoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RestauracionException;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresMigrationIT {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class MigracionesSobreEsquemaHibernate {
        private SessionFactory factory;
        private HibernateTransactions tx;

        @BeforeAll
        void iniciar() {
            Properties properties = PostgresTestSupport.propiedadesHibernate();
            factory = HibernateUtil.construir(properties);
            PostgresTestSupport.aplicarMigraciones(properties);
            tx = new HibernateTransactions(factory);
        }

        @AfterAll
        void cerrar() {
            if (factory != null) factory.close();
            PostgresTestSupport.eliminarFuncionRecibo();
        }

        @Test
        void migracionesCreanLosIndicesParcialesYLaColumnaOperativa() {
            String indiceTasas = definicionIndice("uk_tipo_cambio_autorizada_fecha");
            assertTrue(indiceTasas.toLowerCase(Locale.ROOT).contains("autorizado_por is not null"), indiceTasas);

            String indiceContratos = definicionIndice("uq_contrato_activo").toLowerCase(Locale.ROOT);
            assertTrue(indiceContratos.contains("vigente"), indiceContratos);
            assertTrue(indiceContratos.contains("por vencer"), indiceContratos);
            assertTrue(indiceContratos.contains("vencido"), indiceContratos);

            String indiceRenta = definicionIndice("uq_cargo_renta").toLowerCase(Locale.ROOT);
            assertTrue(indiceRenta.contains("renta"), indiceRenta);
            String indiceMora = definicionIndice("uq_cargo_mora").toLowerCase(Locale.ROOT);
            assertTrue(indiceMora.contains("mora"), indiceMora);
            String indiceAppActiva = definicionIndice("uq_aplicacion_pago_activa").toLowerCase(Locale.ROOT);
            assertTrue(indiceAppActiva.contains("revertida_en is null"), indiceAppActiva);

            String indiceIdempotencia = definicionIndice("ix_solicitud_idempotente_completado")
                    .toLowerCase(Locale.ROOT);
            assertTrue(indiceIdempotencia.contains("completada"), indiceIdempotencia);

            Boolean funcion = tx.ejecutar(session -> session.createNativeQuery(
                            "select to_regprocedure('public.fn_reservar_numero()') is not null", Boolean.class)
                    .getSingleResult());
            assertTrue(funcion);

            String nullable = tx.ejecutar(session -> session.createNativeQuery("""
                            select is_nullable from information_schema.columns
                            where table_schema = current_schema()
                              and table_name = 'pago' and column_name = 'creado_en'
                            """, String.class).getSingleResult());
            String defaultValue = tx.ejecutar(session -> session.createNativeQuery("""
                            select column_default from information_schema.columns
                            where table_schema = current_schema()
                              and table_name = 'pago' and column_name = 'creado_en'
                            """, String.class).getSingleResult());
            assertEquals("NO", nullable);
            assertTrue(defaultValue.toLowerCase(Locale.ROOT).contains("current_timestamp"), defaultValue);

            String usuarioAuditoriaNullable = tx.ejecutar(session -> session.createNativeQuery("""
                            select is_nullable from information_schema.columns
                            where table_schema = current_schema()
                              and table_name = 'auditoria' and column_name = 'usuario_id'
                            """, String.class).getSingleResult());
            assertEquals("YES", usuarioAuditoriaNullable);
        }

        @Test
        void indiceParcialDeTasasPermiteInformativasYRechazaSegundaAutorizada() {
            LocalDate fecha = LocalDate.of(2026, 11, 14);
            Long usuarioId = tx.ejecutar(session -> {
                Usuario usuario = new Usuario("pg-tasa-" + UUID.randomUUID(), "hash", Rol.PROPIETARIO);
                session.persist(usuario);
                session.flush();

                session.persist(TipoCambio.informativa(fecha, new BigDecimal("36.50000000"),
                        "prueba", Instant.parse("2026-11-14T10:00:00Z")));
                session.persist(TipoCambio.informativa(fecha, new BigDecimal("36.60000000"),
                        "prueba", Instant.parse("2026-11-14T10:01:00Z")));
                session.persist(TipoCambio.autorizada(fecha, new BigDecimal("36.62430000"),
                        "prueba", Instant.parse("2026-11-14T10:02:00Z"), usuario, "primera"));
                return usuario.getId();
            });

            assertThrows(ConflictoDatosException.class, () -> tx.ejecutar(session -> {
                Usuario usuario = session.find(Usuario.class, usuarioId);
                session.persist(TipoCambio.autorizada(fecha, new BigDecimal("36.70000000"),
                        "prueba", Instant.parse("2026-11-14T10:03:00Z"), usuario, "duplicada"));
                return null;
            }));

            Long informativas = tx.ejecutar(session -> session.createQuery("""
                            select count(t) from TipoCambio t
                            where t.fecha = :fecha and t.autorizadoPor is null
                            """, Long.class)
                    .setParameter("fecha", fecha).getSingleResult());
            assertEquals(2L, informativas);
        }

        @Test
        void indiceParcialDeContratosSoloRestringeOcupacionesActivas() {
            ContratoIds ids = tx.ejecutar(this::crearContratos);

            assertThrows(ConflictoDatosException.class, () -> tx.ejecutar(session -> {
                session.persist(contrato(ids, "Vigente"));
                return null;
            }));

            Long borradores = tx.ejecutar(session -> session.createQuery("""
                            select count(c) from Contrato c
                            where c.unidadId = :unidad and c.estado = 'Borrador'
                            """, Long.class)
                    .setParameter("unidad", ids.unidadId()).getSingleResult());
            Long activos = tx.ejecutar(session -> session.createQuery("""
                            select count(c) from Contrato c
                            where c.unidadId = :unidad and c.estado = 'Vigente'
                            """, Long.class)
                    .setParameter("unidad", ids.unidadId()).getSingleResult());
            assertEquals(2L, borradores);
            assertEquals(1L, activos);
        }

        @Test
        void reservarNumeroEnTransaccionRevertidaNoConsumeConsecutivo() {
            long primero = reservarNumero();
            assertThrows(IllegalArgumentException.class, () -> tx.ejecutar(session -> {
                reservarNumero(session);
                throw new IllegalArgumentException("reversión de prueba");
            }));
            assertEquals(primero + 1, reservarNumero());
        }

        private String definicionIndice(String nombre) {
            return tx.ejecutar(session -> session.createNativeQuery("""
                            select indexdef from pg_indexes
                            where schemaname = current_schema() and indexname = :nombre
                            """, String.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult());
        }

        private long reservarNumero() {
            return tx.ejecutar(this::reservarNumero);
        }

        private long reservarNumero(Session session) {
            return session.createNativeQuery("select public.fn_reservar_numero()", Long.class)
                    .getSingleResult();
        }

        private ContratoIds crearContratos(Session session) {
            String sufijo = UUID.randomUUID().toString();
            Usuario usuario = new Usuario("pg-contrato-" + sufijo, "hash", Rol.PROPIETARIO);
            session.persist(usuario);
            session.flush();
            Propietario propietario = new Propietario(usuario, "Propietario PG", "pg@latch.test", null);
            session.persist(propietario);
            session.flush();

            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Propiedad PG " + sufijo);
            propiedad.setDireccion("Calle de prueba");
            propiedad.setPropietario(propietario);
            session.persist(propiedad);
            session.flush();

            Unidad unidad = new Unidad();
            unidad.setPropiedadId(propiedad.getId());
            unidad.setCodigo("PG-" + sufijo.substring(0, 8));
            unidad.setTipo("Apartamento");
            unidad.setEstado("Disponible");
            unidad.setMoneda("USD");
            unidad.setCanonReferencia(new BigDecimal("100.00"));
            session.persist(unidad);
            session.flush();

            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Inquilino PG " + sufijo);
            session.persist(inquilino);
            session.flush();

            ContratoIds ids = new ContratoIds(unidad.getId(), inquilino.getId());
            session.persist(contrato(ids, "Borrador"));
            session.persist(contrato(ids, "Borrador"));
            session.persist(contrato(ids, "Vigente"));
            return ids;
        }

        private Contrato contrato(ContratoIds ids, String estado) {
            Contrato contrato = new Contrato();
            contrato.setUnidadId(ids.unidadId());
            contrato.setInquilinoId(ids.inquilinoId());
            contrato.setInicio(LocalDate.of(2026, 1, 1));
            contrato.setFin(LocalDate.of(2026, 12, 31));
            contrato.setCanon(new BigDecimal("100.00"));
            contrato.setMoneda("USD");
            contrato.setDiaPago(5);
            contrato.setDepositoPactado(BigDecimal.ZERO);
            contrato.setEstado(estado);
            contrato.inicializarGarantiaSeguimiento();
            return contrato;
        }

        private record ContratoIds(UUID unidadId, UUID inquilinoId) {}
    }

    @Nested
    class InstalacionYActualizacionSqlPuro {
        @Test
        void instalacionLimpiaDesdeBaseVaciaValidaEnHibernate() throws SQLException {
            Properties validateProps = PostgresTestSupport.propiedadesHibernateValidate();
            PostgresTestSupport.limpiarEsquemaPrueba(validateProps);
            asegurarEsquemaPublicoVacio(validateProps);

            SessionFactory factory = null;
            try {
                PostgresTestSupport.aplicarEsquemaInicial(validateProps);
                PostgresTestSupport.aplicarMigraciones(validateProps);

                factory = HibernateUtil.construir(validateProps);
                HibernateTransactions tx = new HibernateTransactions(factory);
                var controlFx = tx.ejecutar(sesion -> sesion.find(ControlFx.class, ControlFx.ID));
                assertNotNull(controlFx, "V15 debe inicializar la fila compartida antes de arrancar H3.");
                assertEquals(1, controlFx.getMes().getDayOfMonth());
                assertEquals(0, controlFx.getIntentosMes());
                var puerta = tx.ejecutar(sesion -> sesion.find(PuertaMantenimiento.class, PuertaMantenimiento.ID));
                assertNotNull(puerta, "V16 debe inicializar la fila compartida de puerta_mantenimiento.");

                Instant ahora = Instant.parse("2026-11-24T12:00:00Z");
                LocalDate fecha = LocalDate.of(2026, 11, 24);
                tx.ejecutar(session -> {
                    Usuario usuario = new Usuario("sql-limpia-" + UUID.randomUUID(), "hash", Rol.PROPIETARIO);
                    session.persist(usuario);
                    session.flush();

                    Propietario propietario = new Propietario(usuario, "Propietario SQL", "sql@latch.test", "88880000");
                    session.persist(propietario);

                    Propiedad propiedad = new Propiedad();
                    propiedad.setNombre("Propiedad SQL");
                    propiedad.setDireccion("Avenida Central 10");
                    propiedad.setPropietario(propietario);
                    session.persist(propiedad);
                    session.flush();

                    Unidad unidad = new Unidad();
                    unidad.setPropiedad(propiedad);
                    unidad.setCodigo("SQL-101");
                    unidad.setDescripcion("Unidad de prueba en esquema SQL puro");
                    unidad.setTipo("Apartamento");
                    unidad.setEstado("Disponible");
                    unidad.setCanonReferencia(new BigDecimal("200.00"));
                    unidad.setMoneda("USD");
                    session.persist(unidad);

                    Inquilino inquilino = new Inquilino();
                    inquilino.setNombre("Inquilino SQL");
                    inquilino.setIdentificacion("001-010190-0001A");
                    session.persist(inquilino);
                    session.flush();

                    Contrato contrato = new Contrato();
                    contrato.setUnidad(unidad);
                    contrato.setInquilino(inquilino);
                    contrato.setInicio(LocalDate.of(2026, 1, 1));
                    contrato.setFin(LocalDate.of(2026, 12, 31));
                    contrato.setCanon(new BigDecimal("200.00"));
                    contrato.setMoneda("USD");
                    contrato.setDiaPago(5);
                    contrato.setDepositoPactado(new BigDecimal("200.00"));
                    contrato.setEstado("Vigente");
                    contrato.inicializarGarantiaSeguimiento();
                    session.persist(contrato);
                    session.flush();

                    Cargo cargo = new Cargo();
                    cargo.setContratoId(contrato.getId());
                    cargo.setTipo("Renta");
                    cargo.setPeriodo(LocalDate.of(2026, 11, 1));
                    cargo.setFechaEmision(fecha);
                    cargo.setVenceEl(fecha.plusDays(5));
                    cargo.setImporte(new BigDecimal("200.00"));
                    cargo.setConcepto("Renta noviembre 2026");
                    cargo.setEstado("Pendiente");
                    cargo.setCreadoPor(usuario.getId());
                    session.persist(cargo);
                    session.flush();

                    Cargo cargoMora = new Cargo();
                    cargoMora.setContratoId(contrato.getId());
                    cargoMora.setCargoOrigenId(cargo.getId());
                    cargoMora.setTipo("Mora");
                    cargoMora.setPeriodo(LocalDate.of(2026, 11, 1));
                    cargoMora.setFechaEmision(fecha.plusDays(10));
                    cargoMora.setVenceEl(fecha.plusDays(10));
                    cargoMora.setImporte(new BigDecimal("10.00"));
                    cargoMora.setConcepto("Mora noviembre 2026");
                    cargoMora.setEstado("Pendiente");
                    cargoMora.setCreadoPor(usuario.getId());
                    session.persist(cargoMora);

                    TipoCambio informativa = TipoCambio.informativa(fecha, new BigDecimal("36.60000000"),
                            "Banca local", ahora);
                    TipoCambio autorizada = TipoCambio.autorizada(fecha, new BigDecimal("36.62430000"),
                            "BCN", ahora, usuario, "Tasa oficial");
                    session.persist(informativa);
                    session.persist(autorizada);

                    Pago pago = new Pago();
                    pago.setContratoId(contrato.getId());
                    pago.setFechaPago(fecha);
                    pago.setMonedaOriginal("USD");
                    pago.setMontoOriginal(new BigDecimal("200.00"));
                    pago.setMetodo("Efectivo");
                    pago.setReferencia("REF-SQL-1");
                    pago.setEstado("Registrado");
                    pago.aplicarConversion("USD", new BigDecimal("200.00"), autorizada);
                    session.persist(pago);
                    session.flush();
                    session.refresh(pago);
                    assertNotNull(pago.getCreadoEn());

                    session.persist(AplicacionPago.inicial(
                            pago.getId(), cargo.getId(), new BigDecimal("200.00"),
                            fecha, ahora, usuario.getId()));
                    AplicacionPago creditoApp = AplicacionPago.desdeCredito(
                            pago.getId(), cargoMora.getId(), new BigDecimal("10.00"),
                            fecha, ahora, usuario.getId());
                    session.persist(creditoApp);
                    session.flush();
                    creditoApp.revertir("Reversión de prueba P3", usuario.getId(), ahora.plusSeconds(5));
                    long numeroRecibo = session.createNativeQuery("select public.fn_reservar_numero()", Long.class)
                            .getSingleResult();
                    assertEquals(1L, numeroRecibo);
                    session.persist(new Recibo(numeroRecibo, pago, "Contrato SQL-101", "Emisor SQL · RUC-123"));

                    SolicitudIdempotente idempotencia = new SolicitudIdempotente(
                            UUID.randomUUID().toString(), usuario.getId(), "POST /api/v1/pagos",
                            "a".repeat(64), ahora);
                    idempotencia.completar(201, "{\"ok\":true}", ahora);
                    session.persist(idempotencia);

                    session.persist(new Auditoria(usuario, ahora, UUID.randomUUID(), "INSTALACION_LIMPIA", "test"));
                    session.persist(Auditoria.intentoLoginFallido(ahora, UUID.randomUUID(), "usuario_sha256:test"));
                    session.persist(new Configuracion("moneda.base", "USD"));
                    session.persist(new ParametroSistema("dias.gracia.mora", "5", ahora,
                            "Parámetro inicial", ahora, usuario));

                    Cargo cargoGarantia = new Cargo();
                    cargoGarantia.setContratoId(contrato.getId());
                    cargoGarantia.setTipo("Reparación");
                    cargoGarantia.setPeriodo(LocalDate.of(2026, 11, 1));
                    cargoGarantia.setFechaEmision(fecha);
                    cargoGarantia.setVenceEl(fecha.plusDays(5));
                    cargoGarantia.setImporte(new BigDecimal("100.00"));
                    cargoGarantia.setConcepto("Reparación menor");
                    cargoGarantia.setEstado("Pendiente");
                    session.persist(cargoGarantia);
                    session.flush();

                    MovimientoGarantia movGarantia = MovimientoGarantia.aplicacion(
                            contrato.getGarantiaSeguimiento().getId(),
                            cargoGarantia.getId(),
                            UUID.randomUUID(),
                            "c".repeat(64),
                            (short) 1,
                            new BigDecimal("100.00"),
                            fecha,
                            "Aplicación parcial de garantía en PostgreSQL",
                            ahora,
                            usuario.getId());
                    session.persist(movGarantia);
                    session.flush();
                    session.persist(AplicacionPago.desdeGarantia(
                            movGarantia.getId(), cargoGarantia.getId(), new BigDecimal("100.00")));
                    contrato.getGarantiaSeguimiento().ajustarSaldo(new BigDecimal("100.00"));

                    Contrato sucesor = new Contrato();
                    sucesor.setUnidad(unidad);
                    sucesor.setInquilino(inquilino);
                    sucesor.setPredecesorId(contrato.getId());
                    sucesor.setInicio(LocalDate.of(2026, 11, 24));
                    sucesor.setFin(LocalDate.of(2027, 11, 24));
                    sucesor.setCanon(new BigDecimal("220.00"));
                    sucesor.setMoneda("USD");
                    sucesor.setDiaPago(5);
                    sucesor.setDepositoPactado(BigDecimal.ZERO);
                    sucesor.setEstado("Borrador");
                    sucesor.inicializarGarantiaSeguimiento();
                    session.persist(sucesor);
                    session.flush();

                    contrato.terminar(fecha, "Renovación atómica verificada en PostgreSQL");
                    session.flush();
                    sucesor.transicionarA("Vigente", fecha);
                    session.flush();
                    return null;
                });

                Long recibos = tx.ejecutar(session -> session.createQuery("select count(r) from Recibo r", Long.class)
                        .getSingleResult());
                assertEquals(1L, recibos);
            } finally {
                if (factory != null) factory.close();
                PostgresTestSupport.limpiarEsquemaPrueba(validateProps);
                asegurarEsquemaPublicoVacio(validateProps);
            }
        }

        @Test
        void actualizacionDesdeEsquemaPrevioPreservaDatosYValida() throws SQLException {
            Properties validateProps = PostgresTestSupport.propiedadesHibernateValidate();
            PostgresTestSupport.limpiarEsquemaPrueba(validateProps);
            asegurarEsquemaPublicoVacio(validateProps);

            SessionFactory factory = null;
            try {
                PostgresTestSupport.aplicarEsquemaInicial(validateProps);
                UUID contratoId = UUID.randomUUID();
                UUID pagoId = UUID.randomUUID();
                String claveIdempotencia = UUID.randomUUID().toString();
                insertarDatosPrevios(validateProps, contratoId, pagoId, claveIdempotencia);

                PostgresTestSupport.aplicarMigraciones(validateProps);
                factory = HibernateUtil.construir(validateProps);
                HibernateTransactions tx = new HibernateTransactions(factory);

                tx.ejecutar(session -> {
                    Garantia garantia = session.createQuery(
                                    "from Garantia g where g.contrato.id = :contratoId", Garantia.class)
                            .setParameter("contratoId", contratoId)
                            .getSingleResult();
                    assertEquals(new BigDecimal("250.00"), garantia.getMontoInicial());
                    assertEquals(new BigDecimal("250.00"), garantia.getSaldoActual());

                    long siguienteRecibo = session.createNativeQuery("select public.fn_reservar_numero()", Long.class)
                            .getSingleResult();
                    assertEquals(43L, siguienteRecibo);

                    Pago pago = session.find(Pago.class, pagoId);
                    assertNotNull(pago);
                    assertNotNull(pago.getCreadoEn());

                    String udtRespuestaJson = session.createNativeQuery("""
                                    select udt_name from information_schema.columns
                                    where table_schema = current_schema()
                                      and table_name = 'solicitud_idempotente'
                                      and column_name = 'respuesta_json'
                                    """, String.class).getSingleResult();
                    assertEquals("text", udtRespuestaJson);

                    SolicitudIdempotente idempotenciaMigrada = session.createQuery(
                                    "from SolicitudIdempotente s where s.clave = :clave",
                                    SolicitudIdempotente.class)
                            .setParameter("clave", claveIdempotencia)
                            .getSingleResult();
                    assertEquals("{\"recibo\":42}", idempotenciaMigrada.getRespuestaJson());

                    Usuario usuarioPrevio = session.createQuery(
                                    "from Usuario u where u.nombre like 'previo-%'", Usuario.class)
                            .getSingleResult();
                    assertFalse(usuarioPrevio.isDebeCambiarPassword());
                    Recibo reciboPrevio = session.find(Recibo.class, 42L);
                    assertNotNull(reciboPrevio);
                    assertEquals(null, reciboPrevio.getEmisorInfo());

                    session.persist(Auditoria.intentoLoginFallido(
                            Instant.parse("2026-11-24T12:30:00Z"),
                            UUID.randomUUID(),
                            "usuario_sha256:actualizacion"));
                    return null;
                });
            } finally {
                if (factory != null) factory.close();
                PostgresTestSupport.limpiarEsquemaPrueba(validateProps);
                asegurarEsquemaPublicoVacio(validateProps);
            }
        }

        private void insertarDatosPrevios(Properties props, UUID contratoId, UUID pagoId,
                                          String claveIdempotencia) throws SQLException {
            UUID propiedadId = UUID.randomUUID();
            UUID unidadId = UUID.randomUUID();
            UUID inquilinoId = UUID.randomUUID();
            try (Connection conexion = abrirConexion(props)) {
                conexion.setAutoCommit(false);
                try {
                    long usuarioId;
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO usuario (nombre, password_hash, rol, activo, version)
                            VALUES (?, ?, 'PROPIETARIO', TRUE, 0)
                            RETURNING id
                            """)) {
                        ps.setString(1, "previo-" + UUID.randomUUID());
                        ps.setString(2, "hash-previo");
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            usuarioId = rs.getLong(1);
                        }
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO propiedad (id, nombre, direccion, activa, version)
                            VALUES (?, 'Propiedad Previa', 'Direccion Previa', TRUE, 0)
                            """)) {
                        ps.setObject(1, propiedadId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO unidad (id, propiedad_id, codigo, tipo, estado, canon_referencia, moneda, version)
                            VALUES (?, ?, 'PREV-01', 'Apartamento', 'Ocupada', 250.00, 'USD', 0)
                            """)) {
                        ps.setObject(1, unidadId);
                        ps.setObject(2, propiedadId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO inquilino (id, nombre, activo, version)
                            VALUES (?, 'Inquilino Previo', TRUE, 0)
                            """)) {
                        ps.setObject(1, inquilinoId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO contrato (
                                id, unidad_id, inquilino_id, inicio, fin, canon, moneda,
                                dia_pago, deposito_pactado, estado, version
                            )
                            VALUES (?, ?, ?, ?, ?, 250.00, 'USD', 5, 250.00, 'Vigente', 0)
                            """)) {
                        ps.setObject(1, contratoId);
                        ps.setObject(2, unidadId);
                        ps.setObject(3, inquilinoId);
                        ps.setDate(4, Date.valueOf(LocalDate.of(2026, 1, 1)));
                        ps.setDate(5, Date.valueOf(LocalDate.of(2026, 12, 31)));
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO pago (
                                id, contrato_id, fecha_pago, moneda_original, monto_original,
                                monto_contrato, moneda_contrato, metodo, estado, version
                            )
                            VALUES (?, ?, ?, 'USD', 250.00, 250.00, 'USD', 'Efectivo', 'Registrado', 0)
                            """)) {
                        ps.setObject(1, pagoId);
                        ps.setObject(2, contratoId);
                        ps.setDate(3, Date.valueOf(LocalDate.of(2026, 2, 5)));
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO recibo (numero, pago_id, contrato_info)
                            VALUES (42, ?, 'Contrato previo PREV-01')
                            """)) {
                        ps.setObject(1, pagoId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO auditoria (usuario_id, instante, correlacion, accion, referencia)
                            VALUES (?, CURRENT_TIMESTAMP, ?, 'DATOS_PREVIOS', 'migracion:v0')
                            """)) {
                        ps.setLong(1, usuarioId);
                        ps.setObject(2, UUID.randomUUID());
                        ps.executeUpdate();
                    }
                    try (Statement st = conexion.createStatement()) {
                        st.execute("""
                                CREATE TABLE solicitud_idempotente (
                                    id UUID PRIMARY KEY,
                                    clave VARCHAR(36) NOT NULL,
                                    usuario_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
                                    recurso VARCHAR(60) NOT NULL,
                                    hash_peticion VARCHAR(64) NOT NULL,
                                    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
                                    estado VARCHAR(20) NOT NULL,
                                    codigo_estado INTEGER,
                                    respuesta_json OID,
                                    completado_en TIMESTAMP WITH TIME ZONE,
                                    CONSTRAINT uk_solicitud_idempotente_clave UNIQUE (clave),
                                    CONSTRAINT ck_solicitud_idempotente_estado
                                        CHECK (estado IN ('EN_PROCESO', 'COMPLETADA'))
                                )
                                """);
                    }
                    try (PreparedStatement ps = conexion.prepareStatement("""
                            INSERT INTO solicitud_idempotente (
                                id, clave, usuario_id, recurso, hash_peticion, creado_en,
                                estado, codigo_estado, respuesta_json, completado_en
                            )
                            VALUES (
                                ?, ?, ?, 'POST /api/v1/pagos', ?, CURRENT_TIMESTAMP,
                                'COMPLETADA', 201, lo_from_bytea(0, convert_to('{"recibo":42}', 'UTF8')), CURRENT_TIMESTAMP
                            )
                            """)) {
                        ps.setObject(1, UUID.randomUUID());
                        ps.setString(2, claveIdempotencia);
                        ps.setLong(3, usuarioId);
                        ps.setString(4, "b".repeat(64));
                        ps.executeUpdate();
                    }
                    conexion.commit();
                } catch (SQLException e) {
                    conexion.rollback();
                    throw e;
                }
            }
        }

        private void asegurarEsquemaPublicoVacio(Properties props) throws SQLException {
            try (Connection conexion = abrirConexion(props);
                 Statement st = conexion.createStatement()) {
                try (ResultSet rs = st.executeQuery("""
                        select count(*) from information_schema.tables
                        where table_schema = 'public' and table_type = 'BASE TABLE'
                        """)) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "El esquema public debe quedar sin tablas residuales.");
                }
                try (ResultSet rs = st.executeQuery("select to_regprocedure('public.fn_reservar_numero()') is not null")) {
                    rs.next();
                    assertFalse(rs.getBoolean(1), "public.fn_reservar_numero() debe eliminarse al limpiar.");
                }
            }
        }

        private Connection abrirConexion(Properties props) throws SQLException {
            return DriverManager.getConnection(
                    props.getProperty("hibernate.connection.url"),
                    props.getProperty("hibernate.connection.username"),
                    props.getProperty("hibernate.connection.password"));
        }
    }

    @Nested
    class RespaldoYRestauracionPostgres {
        @TempDir
        Path tempDir;

        @Test
        void respaldoCifradoYRestauracionAisladaConPgDumpYPgRestoreValidaEnHibernate() throws Exception {
            Properties validateProps = PostgresTestSupport.propiedadesHibernateValidate();
            PostgresTestSupport.limpiarEsquemaPrueba(validateProps);

            MotorBaseRespaldo.ConexionJdbc jdbcPrueba = new MotorBaseRespaldo.ConexionJdbc(
                    validateProps.getProperty("hibernate.connection.url"),
                    validateProps.getProperty("hibernate.connection.username"),
                    validateProps.getProperty("hibernate.connection.password").toCharArray());
            char[] claveRespaldo = "PgRespaldoSeguro2026!".toCharArray();

            SessionFactory factory = null;
            try {
                PostgresTestSupport.aplicarEsquemaInicial(validateProps);
                PostgresTestSupport.aplicarMigraciones(validateProps);
                factory = HibernateUtil.construir(validateProps);
                HibernateTransactions tx = new HibernateTransactions(factory);

                Path dirAdjOrigen = tempDir.resolve("pg-adj-origen");
                Path dirRespaldos = tempDir.resolve("pg-respaldos");
                Path dirAdjDestino = tempDir.resolve("pg-adj-destino");
                Path dirCopiaPrevia = tempDir.resolve("pg-copia-previa");
                Files.createDirectories(dirRespaldos);

                AlmacenAdjuntos almacenOrigen = new AlmacenAdjuntos(dirAdjOrigen);
                MotorPostgresRespaldo motorPg = new MotorPostgresRespaldo(jdbcPrueba);
                RespaldoService respaldoService = new RespaldoService(tx, almacenOrigen, motorPg, Clock.systemUTC());

                UUID[] ids = new UUID[2];
                SesionUsuario[] sesionRef = new SesionUsuario[1];
                tx.ejecutar(session -> {
                    Usuario u = new Usuario("pg-resp-" + UUID.randomUUID().toString().substring(0, 6), "hash", Rol.PROPIETARIO);
                    session.persist(u);
                    session.flush();
                    sesionRef[0] = SesionUsuario.paraSesionAutenticada(u.getId(), u.getNombre(), u.getRol(), u.getVersion());

                    Propietario propPersona = new Propietario(u, "Propietario PG", "pg@latch.test", "88881111");
                    session.persist(propPersona);

                    Propiedad prop = new Propiedad();
                    prop.setNombre("Torre PG");
                    prop.setDireccion("Managua");
                    prop.setPropietario(propPersona);
                    session.persist(prop);

                    Unidad un = new Unidad();
                    un.setPropiedad(prop);
                    un.setCodigo("PG-201");
                    un.setTipo("Oficina");
                    un.setEstado("Ocupada");
                    un.setCanonReferencia(new BigDecimal("400.00"));
                    un.setMoneda("USD");
                    session.persist(un);

                    Inquilino inq = new Inquilino();
                    inq.setNombre("Inquilino PG Respaldo");
                    session.persist(inq);
                    session.flush();

                    Contrato ct = new Contrato();
                    ct.setUnidad(un);
                    ct.setInquilino(inq);
                    ct.setInicio(LocalDate.of(2026, 1, 1));
                    ct.setFin(LocalDate.of(2026, 12, 31));
                    ct.setCanon(new BigDecimal("400.00"));
                    ct.setMoneda("USD");
                    ct.setDiaPago(5);
                    ct.setDepositoPactado(new BigDecimal("400.00"));
                    ct.setEstado("Vigente");
                    ct.inicializarGarantiaSeguimiento();
                    session.persist(ct);
                    session.flush();

                    TipoCambio tc = TipoCambio.autorizada(
                            LocalDate.of(2026, 11, 24), new BigDecimal("36.62430000"),
                            "BCN", Instant.now(), u, "PG IT");
                    session.persist(tc);
                    Pago pg = new Pago();
                    pg.setContratoId(ct.getId());
                    pg.setFechaPago(LocalDate.of(2026, 11, 24));
                    pg.setMonedaOriginal("USD");
                    pg.setMontoOriginal(new BigDecimal("400.00"));
                    pg.setMetodo("Efectivo");
                    pg.setEstado("Registrado");
                    pg.aplicarConversion("USD", new BigDecimal("400.00"), tc);
                    session.persist(pg);
                    session.flush();

                    long num = session.createNativeQuery("select public.fn_reservar_numero()", Long.class).getSingleResult();
                    session.persist(new Recibo(num, pg, "Contrato PG-201", "Emisor PG"));
                    ids[0] = ct.getId();
                    ids[1] = prop.getId();
                    return null;
                });

                byte[] pdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n"
                        .getBytes(StandardCharsets.US_ASCII);
                var adjDto = new AdjuntoService(tx, almacenOrigen)
                        .adjuntarDocumentoContrato(sesionRef[0], ids[0], "contrato-pg.pdf", pdfBytes);

                var resultadoRespaldo = respaldoService.crearRespaldo(sesionRef[0], dirRespaldos, claveRespaldo);
                assertEquals(1, resultadoRespaldo.cantidadAdjuntos());
                assertEquals(1L, resultadoRespaldo.ultimoNumeroRecibo());

                assertThrows(RestauracionException.class,
                        () -> respaldoService.verificarPaquete(resultadoRespaldo.archivo(), "ClaveMalaPg2026".toCharArray()));

                // Cerrar SessionFactory para dejar cero sesiones operativas antes de restaurar en el esquema limpio.
                factory.close();
                factory = null;
                PostgresTestSupport.limpiarEsquemaPrueba(validateProps);

                var restauracion = respaldoService.restaurarEnEntornoAislado(
                        new RespaldoService.SolicitudRestauracion(
                                null,
                                resultadoRespaldo.archivo(),
                                claveRespaldo,
                                jdbcPrueba,
                                dirAdjDestino,
                                dirCopiaPrevia,
                                true,
                                false,
                                false));
                assertEquals(0, restauracion.hashesDiscordantes());
                assertEquals(1, restauracion.adjuntosVerificados());

                byte[] recuperado = new AlmacenAdjuntos(dirAdjDestino)
                        .leerYVerificar(restauracion.manifiesto().adjuntos().getFirst().rutaInterna(), adjDto.sha256());
                assertEquals(pdfBytes.length, recuperado.length);

                factory = HibernateUtil.construir(validateProps);
                Long recibos = new HibernateTransactions(factory).ejecutar(
                        s -> s.createQuery("select count(r) from Recibo r", Long.class).getSingleResult());
                assertEquals(1L, recibos);

                factory.close();
                factory = null;
                Path dump = tempDir.resolve("transaccion-restauracion.dump");
                motorPg.exportarBase(dump);
                try (var conn = DriverManager.getConnection(jdbcPrueba.url(), jdbcPrueba.usuario(), jdbcPrueba.claveTexto());
                     var sql = conn.createStatement()) {
                    sql.execute("create table bloqueo_prueba_restore (usuario_id bigint references usuario(id))");
                    sql.executeUpdate("update propiedad set nombre = 'Estado previo del destino'");
                    try {
                        assertThrows(IllegalStateException.class,
                                () -> motorPg.restaurarEnBaseAislada(dump, jdbcPrueba));
                        try (var rs = sql.executeQuery("select nombre from propiedad")) {
                            assertTrue(rs.next());
                            assertEquals("Estado previo del destino", rs.getString(1));
                        }
                        try (var rs = sql.executeQuery("select count(*) from recibo")) {
                            assertTrue(rs.next());
                            assertEquals(1L, rs.getLong(1));
                        }
                    } finally {
                        sql.execute("drop table bloqueo_prueba_restore");
                    }
                }
            } finally {
                if (factory != null) factory.close();
                PostgresTestSupport.limpiarEsquemaPrueba(validateProps);
            }
        }
    }
}
