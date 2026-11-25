package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.AuditoriaRegistroDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.DocumentoReporte;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.ParametroHistorialDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.PerfilEmisorDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Contrato;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Inquilino;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Pago;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propietario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Recibo;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Unidad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import ni.edu.uam.proyectofinalprogramacionappescritorio.util.PasswordHasher;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionAuditoriaTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private ConfiguracionService configuracion;
    private AuditoriaService auditoria;
    private SesionUsuario propietario;
    private SesionUsuario asistente;
    private UUID contratoId;

    @BeforeEach
    void iniciar() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        props.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:config_aud_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        props.setProperty("hibernate.connection.username", "sa");
        props.setProperty("hibernate.connection.password", "");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(props);
        tx = new HibernateTransactions(factory);
        configuracion = new ConfiguracionService(tx);
        auditoria = new AuditoriaService(tx);
        String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());

        tx.ejecutar(session -> {
            Usuario uProp = new Usuario("owner." + UUID.randomUUID().toString().substring(0, 8),
                    hash, Rol.PROPIETARIO);
            Usuario uAsis = new Usuario("asis." + UUID.randomUUID().toString().substring(0, 8),
                    hash, Rol.ASISTENTE);
            session.persist(uProp);
            session.persist(uAsis);
            session.flush();
            propietario = SesionUsuario.paraSesionAutenticada(uProp.getId(), uProp.getNombre(), uProp.getRol());
            asistente = SesionUsuario.paraSesionAutenticada(uAsis.getId(), uAsis.getNombre(), uAsis.getRol());

            Propietario perfil = new Propietario(uProp, "Dueño Demo", "dueno@latch.test", null);
            session.persist(perfil);
            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Edificio M12");
            propiedad.setDireccion("Calle M12");
            propiedad.setPropietario(perfil);
            session.persist(propiedad);
            session.flush();

            Unidad unidad = new Unidad();
            unidad.setPropiedad(propiedad);
            unidad.setCodigo("M12-01");
            unidad.setCanonReferencia(new BigDecimal("400.00"));
            session.persist(unidad);
            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Inquilino M12");
            session.persist(inquilino);
            session.flush();

            Contrato contrato = new Contrato();
            contrato.setUnidad(unidad);
            contrato.setInquilino(inquilino);
            contrato.setInicio(LocalDate.of(2026, 1, 1));
            contrato.setFin(LocalDate.of(2026, 12, 31));
            contrato.setCanon(new BigDecimal("400.00"));
            contrato.setMoneda("USD");
            contrato.setDiaPago(5);
            contrato.setDepositoPactado(BigDecimal.ZERO);
            contrato.setEstado("Vigente");
            contrato.inicializarGarantiaSeguimiento();
            session.persist(contrato);
            session.flush();
            contratoId = contrato.getId();
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void parametrosConHistorialInsertOnlyVigenciaYValidacionErr064() {
        Instant pasado = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant hoy = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant futuro = Instant.now().plus(5, ChronoUnit.DAYS);

        configuracion.registrarParametro(propietario, "dias.gracia.mora", "3", pasado, "Política inicial");
        configuracion.registrarParametro(propietario, "dias.gracia.mora", "5", hoy, "Ajuste actual");
        configuracion.registrarParametro(propietario, "dias.gracia.mora", "10", futuro, "Política futura");

        ParametroHistorialDTO vigente = configuracion.consultarParametroVigente(propietario, "dias.gracia.mora")
                .orElseThrow();
        assertEquals("5", vigente.valor());

        List<ParametroHistorialDTO> historial = configuracion.listarHistorialParametros(
                propietario, "dias.gracia.mora");
        assertEquals(3, historial.size());
        assertEquals("10", historial.get(0).valor());
        assertEquals("5", historial.get(1).valor());
        assertEquals("3", historial.get(2).valor());

        assertThrows(ConflictoDatosException.class, () -> configuracion.registrarParametro(
                propietario, "dias.gracia.mora", "7", hoy, "Duplicado"));
        IllegalArgumentException errNegativo = assertThrows(IllegalArgumentException.class,
                () -> configuracion.registrarParametro(propietario, "dias.gracia.mora", "-1", null, "Negativo"));
        assertTrue(errNegativo.getMessage().contains("ERR-064"));

        assertThrows(SecurityException.class, () -> configuracion.registrarParametro(
                asistente, "dias.gracia.mora", "4", null, "Sin permiso"));
    }

    @Test
    void perfilEmisorSeFijaSoloEnNuevosRecibosSinAlterarHistoricos() {
        PagoService pagoService = new PagoService(tx);
        ReporteService reporteService = new ReporteService(tx);

        PagoService.RegistroPago primero = SesionUsuario.ejecutarComo(propietario, () -> {
            Pago pago = new Pago();
            pago.setContratoId(contratoId);
            pago.setFechaPago(LocalDate.of(2026, 2, 1));
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("100.00"));
            pago.setMetodo("Efectivo");
            return pagoService.registrarPago(pago, List.of());
        });
        assertNull(primero.recibo().getEmisorInfo());

        configuracion.guardarPerfilEmisor(propietario, new PerfilEmisorDTO(
                "Inmobiliaria Latch S.A.", "J0310000123456",
                "Managua, Rotonda 1", "+505 2222-3333", "cobros@latch.test"),
                "Configuración de emisor oficial");

        PagoService.RegistroPago segundo = SesionUsuario.ejecutarComo(propietario, () -> {
            Pago pago = new Pago();
            pago.setContratoId(contratoId);
            pago.setFechaPago(LocalDate.of(2026, 2, 2));
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("150.00"));
            pago.setMetodo("Transferencia");
            return pagoService.registrarPago(pago, List.of());
        });

        Recibo recibo1 = tx.ejecutar(s -> s.find(Recibo.class, primero.recibo().getNumero()));
        Recibo recibo2 = tx.ejecutar(s -> s.find(Recibo.class, segundo.recibo().getNumero()));
        assertNull(recibo1.getEmisorInfo());
        assertNotNull(recibo2.getEmisorInfo());
        assertTrue(recibo2.getEmisorInfo().contains("Inmobiliaria Latch S.A."));

        DocumentoReporte doc = SesionUsuario.ejecutarComo(propietario,
                () -> reporteService.generarDocumentoRecibo(segundo.pago().getId()));
        assertTrue(doc.jasperPrint().getPages().size() >= 1);
        byte[] pdf = reporteService.exportarPdf(doc.jasperPrint());
        assertTrue(pdf.length > 0);
    }

    @Test
    void consultaAuditoriaFiltraPorUsuarioAccionYFechasSoloParaPropietario() {
        configuracion.registrarParametro(propietario, "dias.aviso.vencimiento", "15", null, "Inicial");
        Instant desde = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant hasta = Instant.now().plus(1, ChronoUnit.HOURS);

        List<AuditoriaRegistroDTO> registros = auditoria.consultar(
                propietario, propietario.getNombre(), "REGISTRAR_PARAMETRO", desde, hasta, 50);
        assertEquals(1, registros.size());
        assertEquals("parametro:dias.aviso.vencimiento", registros.getFirst().referencia());
        assertFalse(registros.getFirst().referencia().contains("password"));

        assertTrue(auditoria.consultar(propietario, "no-existe", null, desde, hasta, 50).isEmpty());
        assertThrows(SecurityException.class,
                () -> auditoria.consultar(asistente, null, null, null, null, 50));
        assertThrows(IllegalArgumentException.class,
                () -> auditoria.consultar(propietario, null, null, hasta, desde, 50));
    }
}
