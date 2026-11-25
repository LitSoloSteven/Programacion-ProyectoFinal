package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dto.HistorialInquilinoDTO;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Cargo;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InquilinoHistorialTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private InquilinoService servicio;
    private SesionUsuario operador;
    private UUID unidadId;

    @BeforeEach
    void iniciar() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        props.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:inquilino_hist_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        props.setProperty("hibernate.connection.username", "sa");
        props.setProperty("hibernate.connection.password", "");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(props);
        tx = new HibernateTransactions(factory);
        servicio = new InquilinoService(tx);
        String hash = new PasswordHasher().crear("ClaveSintetica-2026".toCharArray());

        tx.ejecutar(session -> {
            Usuario usuario = new Usuario("asistente." + UUID.randomUUID().toString().substring(0, 8),
                    hash, Rol.ASISTENTE);
            session.persist(usuario);
            Usuario dueno = new Usuario("dueno." + UUID.randomUUID().toString().substring(0, 8),
                    hash, Rol.PROPIETARIO);
            session.persist(dueno);
            session.flush();
            Propietario propietario = new Propietario(dueno, "Dueño", "dueno@latch.test", null);
            session.persist(propietario);
            Propiedad propiedad = new Propiedad();
            propiedad.setNombre("Torre Norte");
            propiedad.setDireccion("Avenida 1");
            propiedad.setPropietario(propietario);
            session.persist(propiedad);
            session.flush();
            Unidad unidad = new Unidad();
            unidad.setPropiedad(propiedad);
            unidad.setCodigo("N-101");
            unidad.setCanonReferencia(new BigDecimal("350.00"));
            session.persist(unidad);
            session.flush();
            unidadId = unidad.getId();
            operador = SesionUsuario.paraSesionAutenticada(usuario.getId(), usuario.getNombre(), usuario.getRol());
            return null;
        });
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void advierteCoincidenciasDeIdentificacionYConsultaHistorialCompleto() {
        UUID inquilinoId = tx.ejecutar(session -> {
            Inquilino inquilino = new Inquilino();
            inquilino.setNombre("Elena Arrendataria");
            inquilino.setIdentificacion("001-150595-0002C");
            session.persist(inquilino);
            session.flush();

            Contrato contrato = new Contrato();
            contrato.setUnidadId(unidadId);
            contrato.setInquilinoId(inquilino.getId());
            contrato.setInicio(LocalDate.of(2026, 1, 1));
            contrato.setFin(LocalDate.of(2026, 12, 31));
            contrato.setCanon(new BigDecimal("350.00"));
            contrato.setMoneda("USD");
            contrato.setDiaPago(5);
            contrato.setDepositoPactado(new BigDecimal("350.00"));
            contrato.setEstado("Vigente");
            contrato.inicializarGarantiaSeguimiento();
            session.persist(contrato);
            session.flush();

            Cargo cargo = new Cargo();
            cargo.setContratoId(contrato.getId());
            cargo.setTipo("Renta");
            cargo.setPeriodo(LocalDate.of(2026, 1, 1));
            cargo.setFechaEmision(LocalDate.of(2026, 1, 1));
            cargo.setVenceEl(LocalDate.of(2026, 1, 5));
            cargo.setImporte(new BigDecimal("350.00"));
            cargo.setConcepto("Renta enero");
            cargo.setEstado("Pagado");
            session.persist(cargo);

            Pago pago = new Pago();
            pago.setContratoId(contrato.getId());
            pago.setFechaPago(LocalDate.of(2026, 1, 4));
            pago.setMonedaOriginal("USD");
            pago.setMontoOriginal(new BigDecimal("350.00"));
            pago.setMetodo("Transferencia");
            pago.setEstado("Registrado");
            pago.aplicarConversion("USD", new BigDecimal("350.00"), null);
            session.persist(pago);
            session.flush();

            session.persist(new Recibo(105L, pago, "Torre Norte — N-101"));
            return inquilino.getId();
        });

        SesionUsuario.ejecutarComo(operador, () -> {
            List<Inquilino> coincidencias = servicio.buscarCoincidenciasIdentificacion(" 001-150595-0002c ", null);
            assertEquals(1, coincidencias.size());
            assertEquals(inquilinoId, coincidencias.getFirst().getId());
            assertTrue(servicio.buscarCoincidenciasIdentificacion("001-150595-0002C", inquilinoId).isEmpty());

            HistorialInquilinoDTO historial = servicio.consultarHistorial(inquilinoId);
            assertEquals("Elena Arrendataria", historial.nombre());
            assertEquals(1, historial.contratos().size());
            assertEquals(1, historial.cargos().size());
            assertEquals(1, historial.pagos().size());
            assertEquals(105L, historial.pagos().getFirst().numeroRecibo());
        });
    }
}
