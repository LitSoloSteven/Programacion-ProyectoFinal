package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConflictoDatosException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresMigrationIT {
    private static SessionFactory factory;
    private static HibernateTransactions tx;

    @BeforeAll
    static void iniciar() {
        Properties properties = PostgresTestSupport.propiedadesHibernate();
        factory = HibernateUtil.construir(properties);
        PostgresTestSupport.aplicarMigraciones(properties);
        tx = new HibernateTransactions(factory);
    }

    @AfterAll
    static void cerrar() {
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
