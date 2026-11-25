package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.GastoPropiedadDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ResumenGastosMantenimientoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.SolicitudMantenimientoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Garantia;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.SolicitudMantenimiento;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MantenimientoServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private MantenimientoService servicio;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private UUID propiedad1Id;
    private UUID propiedad2Id;
    private UUID unidad1Id;
    private UUID unidad2Id;
    private UUID contratoActivoId;

    @BeforeEach
    void iniciar() {
        Properties p = new Properties();
        p.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        p.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:mantenimiento_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");
        p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        p.setProperty("hibernate.jdbc.time_zone", "UTC");
        factory = HibernateUtil.construir(p);
        tx = new HibernateTransactions(factory);
        servicio = new MantenimientoService(tx);

        tx.ejecutar(session -> {
            Usuario uProp = new Usuario("prop-m8-" + UUID.randomUUID().toString().substring(0, 6), "hash", Rol.PROPIETARIO);
            Usuario uAsis = new Usuario("asis-m8-" + UUID.randomUUID().toString().substring(0, 6), "hash", Rol.ASISTENTE);
            session.persist(uProp);
            session.persist(uAsis);
            session.flush();

            propietario = SesionUsuario.paraSesionAutenticada(uProp.getId(), uProp.getNombre(), uProp.getRol(), uProp.getVersion());
            asistente = SesionUsuario.paraSesionAutenticada(uAsis.getId(), uAsis.getNombre(), uAsis.getRol(), uAsis.getVersion());

            Propiedad p1 = new Propiedad();
            p1.setNombre("Residencial Norte");
            p1.setDireccion("Km 4");
            session.persist(p1);

            Propiedad p2 = new Propiedad();
            p2.setNombre("Edificio Sur");
            p2.setDireccion("Av. Bolívar");
            session.persist(p2);
            session.flush();
            propiedad1Id = p1.getId();
            propiedad2Id = p2.getId();

            Unidad u1 = new Unidad();
            u1.setPropiedad(p1);
            u1.setCodigo("N-101");
            u1.setEstado("Ocupada");
            u1.setCanonReferencia(new BigDecimal("400.00"));
            u1.setMoneda("USD");
            session.persist(u1);

            Unidad u2 = new Unidad();
            u2.setPropiedad(p2);
            u2.setCodigo("S-201");
            u2.setEstado("Disponible");
            u2.setCanonReferencia(new BigDecimal("350.00"));
            u2.setMoneda("USD");
            session.persist(u2);

            Inquilino inq = new Inquilino();
            inq.setNombre("Inquilino M8");
            session.persist(inq);
            session.flush();

            unidad1Id = u1.getId();
            unidad2Id = u2.getId();

            Contrato c = new Contrato(null, unidad1Id, null, inq.getId(), null,
                    LocalDate.of(2026, 11, 1), LocalDate.of(2027, 10, 31),
                    new BigDecimal("400.00"), "USD", 5, new BigDecimal("400.00"), "Vigente", 0);
            c.inicializarGarantiaSeguimiento();
            session.persist(c);
            session.flush();
            contratoActivoId = c.getId();
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null && !factory.isClosed()) factory.close();
    }

    @Test
    void abreSolicitudEnUnidadOcupadaSinAlterarSuEstadoYPermiteResponsableNominal() {
        SolicitudMantenimientoDTO creada = servicio.abrir(
                asistente, unidad1Id, "Fuga en lavamanos del baño", "Alta",
                "Plomería Central (sin cuenta)", LocalDate.of(2026, 11, 20));

        assertEquals(SolicitudMantenimiento.ESTADO_ABIERTA, creada.estado());
        assertEquals(SolicitudMantenimiento.PRIORIDAD_ALTA, creada.prioridad());
        assertEquals("Plomería Central (sin cuenta)", creada.responsable());
        assertEquals("Ocupada", tx.ejecutar(s -> s.find(Unidad.class, unidad1Id).getEstado()));

        List<SolicitudMantenimientoDTO> filtradas = servicio.listarSolicitudes(
                asistente, propiedad1Id, null, "Alta", "Abierta");
        assertEquals(1, filtradas.size());
        assertEquals(creada.id(), filtradas.getFirst().id());
    }

    @Test
    void cp037TransicionesValidasIdempotenciaTerminalesYErr066() {
        SolicitudMantenimientoDTO s1 = servicio.abrir(
                propietario, unidad1Id, "Revisión eléctrica", "Media", "Técnico A",
                LocalDate.of(2026, 11, 10));

        // Repetir estado actual (Abierta -> Abierta) es idempotente: no incrementa versión ni duplica auditoría.
        long auditoriasAntes = contarAuditorias("CAMBIAR_ESTADO_MANTENIMIENTO");
        SolicitudMantenimientoDTO repetida = servicio.cambiarEstado(
                propietario, s1.id(), s1.version(), SolicitudMantenimiento.ESTADO_ABIERTA, null, null);
        assertEquals(s1.version(), repetida.version());
        assertEquals(auditoriasAntes, contarAuditorias("CAMBIAR_ESTADO_MANTENIMIENTO"));

        // Transición inválida directa Abierta -> Resuelta
        TransicionInvalidaException errDirecta = assertThrows(TransicionInvalidaException.class, () ->
                servicio.cambiarEstado(propietario, s1.id(), s1.version(),
                        SolicitudMantenimiento.ESTADO_RESUELTA, LocalDate.of(2026, 11, 11), "Hecho"));
        assertTrue(errDirecta.getMessage().contains("ERR-066"));

        // Abierta -> En proceso -> Resuelta
        SolicitudMantenimientoDTO enProceso = servicio.cambiarEstado(
                asistente, s1.id(), s1.version(), SolicitudMantenimiento.ESTADO_EN_PROCESO, null, "Técnico B");
        assertEquals(SolicitudMantenimiento.ESTADO_EN_PROCESO, enProceso.estado());
        assertEquals("Técnico B", enProceso.responsable());

        // Conflicto optimista con versión vieja
        assertThrows(ConflictoDatosException.class, () ->
                servicio.cambiarEstado(propietario, s1.id(), s1.version(),
                        SolicitudMantenimiento.ESTADO_CANCELADA, LocalDate.of(2026, 11, 12), "Cambio"));

        SolicitudMantenimientoDTO resuelta = servicio.cambiarEstado(
                propietario, s1.id(), enProceso.version(), SolicitudMantenimiento.ESTADO_RESUELTA,
                LocalDate.of(2026, 11, 12), "Se reemplazó interruptor principal");
        assertEquals(SolicitudMantenimiento.ESTADO_RESUELTA, resuelta.estado());
        assertEquals(LocalDate.of(2026, 11, 12), resuelta.cerradaEl());

        // Resuelta es terminal: no vuelve a Abierta ni pasa a Cancelada
        assertThrows(TransicionInvalidaException.class, () ->
                servicio.cambiarEstado(propietario, s1.id(), resuelta.version(),
                        SolicitudMantenimiento.ESTADO_ABIERTA, null, null));
        assertThrows(TransicionInvalidaException.class, () ->
                servicio.cambiarEstado(propietario, s1.id(), resuelta.version(),
                        SolicitudMantenimiento.ESTADO_CANCELADA, LocalDate.of(2026, 11, 13), "Motivo"));

        // Repetir Resuelta es idempotente
        long auditoriasTrasResolver = contarAuditorias("CAMBIAR_ESTADO_MANTENIMIENTO");
        SolicitudMantenimientoDTO resueltaRepetida = servicio.cambiarEstado(
                propietario, s1.id(), resuelta.version(), SolicitudMantenimiento.ESTADO_RESUELTA, null, null);
        assertEquals(resuelta.version(), resueltaRepetida.version());
        assertEquals(auditoriasTrasResolver, contarAuditorias("CAMBIAR_ESTADO_MANTENIMIENTO"));

        // Cancelación desde Abierta y terminalidad de Cancelada
        SolicitudMantenimientoDTO s2 = servicio.abrir(
                asistente, unidad2Id, "Pintura fachada", "Baja", null, LocalDate.of(2026, 11, 15));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.cambiarEstado(asistente, s2.id(), s2.version(),
                        SolicitudMantenimiento.ESTADO_CANCELADA, LocalDate.of(2026, 11, 16), "   "));
        SolicitudMantenimientoDTO cancelada = servicio.cambiarEstado(
                asistente, s2.id(), s2.version(), SolicitudMantenimiento.ESTADO_CANCELADA,
                LocalDate.of(2026, 11, 16), "Pospuesto por lluvia");
        assertEquals(SolicitudMantenimiento.ESTADO_CANCELADA, cancelada.estado());
        assertThrows(TransicionInvalidaException.class, () ->
                servicio.cambiarEstado(propietario, s2.id(), cancelada.version(),
                        SolicitudMantenimiento.ESTADO_EN_PROCESO, null, null));
    }

    @Test
    void cp038GastoNoGeneraCargoNiTocaGarantiaYRechazaPropiedadIncompatibleConErr065() {
        SolicitudMantenimientoDTO s1 = servicio.abrir(
                propietario, unidad1Id, "Cambio de grifería", "Alta", "Ferretería",
                LocalDate.of(2026, 11, 10));
        SolicitudMantenimientoDTO enProceso = servicio.cambiarEstado(
                propietario, s1.id(), s1.version(), SolicitudMantenimiento.ESTADO_EN_PROCESO, null, null);

        // Registrar gasto de 25.00 USD asociado a s1
        GastoPropiedadDTO g1 = servicio.registrarGasto(
                propietario, propiedad1Id, s1.id(), LocalDate.of(2026, 11, 12),
                new BigDecimal("25.00"), "USD", "Grifo y empaques");
        assertEquals(new BigDecimal("25.00"), g1.importe());

        // Registrar gasto en NIO en la misma solicitud y otro en propiedad 2
        servicio.registrarGasto(
                asistente, propiedad1Id, s1.id(), LocalDate.of(2026, 11, 12),
                new BigDecimal("500.00"), "NIO", "Mano de obra");
        servicio.registrarGasto(
                propietario, propiedad2Id, null, LocalDate.of(2026, 11, 14),
                new BigDecimal("80.00"), "USD", "Limpieza general");

        // Verificar que no existe ningún cargo al inquilino ni alteración de garantía
        Long totalCargos = tx.ejecutar(s -> s.createQuery("select count(c) from Cargo c", Long.class).getSingleResult());
        Garantia garantia = tx.ejecutar(s -> s.createQuery(
                "from Garantia g where g.contrato.id = :id", Garantia.class)
                .setParameter("id", contratoActivoId).getSingleResult());
        assertEquals(0L, totalCargos);
        assertEquals(new BigDecimal("400.00"), garantia.getSaldoActual());

        // Propiedad incompatible (solicitud de unidad1 en propiedad1, seleccionada propiedad2) -> ERR-065
        PropiedadIncompatibleException errProp = assertThrows(PropiedadIncompatibleException.class, () ->
                servicio.registrarGasto(propietario, propiedad2Id, s1.id(),
                        LocalDate.of(2026, 11, 13), new BigDecimal("10.00"), "USD", "Error"));
        assertTrue(errProp.getMessage().contains("ERR-065"));

        // Cancelar solicitud en proceso conserva gastos existentes
        SolicitudMantenimientoDTO cancelada = servicio.cambiarEstado(
                propietario, s1.id(), enProceso.version(), SolicitudMantenimiento.ESTADO_CANCELADA,
                LocalDate.of(2026, 11, 13), "Se cerró la reparación parcial");
        assertEquals(new BigDecimal("25.00"), cancelada.totalGastoUsd());
        assertEquals(new BigDecimal("500.00"), cancelada.totalGastoNio());

        // Consulta de gastos por propiedad y período suma USD y NIO por separado sin duplicar solicitudes
        ResumenGastosMantenimientoDTO resumenP1 = servicio.consultarGastos(
                asistente, propiedad1Id, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        assertEquals(2, resumenP1.gastos().size());
        assertEquals(new BigDecimal("25.00"), resumenP1.totalUsd());
        assertEquals(new BigDecimal("500.00"), resumenP1.totalNio());

        List<SolicitudMantenimientoDTO> listaP1 = servicio.listarSolicitudes(
                propietario, propiedad1Id, null, null, null);
        assertEquals(1, listaP1.size());
        assertEquals(new BigDecimal("25.00"), listaP1.getFirst().totalGastoUsd());
        assertEquals(new BigDecimal("500.00"), listaP1.getFirst().totalGastoNio());
    }

    @Test
    void sesionInvalidaFallaCerrada() {
        SesionUsuario falsa = SesionUsuario.paraSesionAutenticada(999999L, "fantasma", Rol.PROPIETARIO, 0);
        assertThrows(SecurityException.class, () ->
                servicio.abrir(falsa, unidad1Id, "Prueba", "Media", null, null));
        assertNotNull(servicio.listarSolicitudes(asistente, null, null, null, null));
    }

    private long contarAuditorias(String accion) {
        return tx.ejecutar(s -> s.createQuery(
                "select count(a) from Auditoria a where a.accion = :accion", Long.class)
                .setParameter("accion", accion)
                .getSingleResult());
    }
}
