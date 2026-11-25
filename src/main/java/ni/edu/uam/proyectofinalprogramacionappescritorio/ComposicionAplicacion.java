package ni.edu.uam.proyectofinalprogramacionappescritorio;

import lombok.Getter;
import lombok.experimental.Accessors;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.ServicioCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AdjuntoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AuditoriaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CargoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CobranzaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ConfiguracionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.GarantiaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.MantenimientoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PagoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.RespaldoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UsuarioService;
import org.hibernate.SessionFactory;

import java.util.Objects;

/** Construye una sola vez los servicios compartidos por JavaFX y la API local. */
@Getter
@Accessors(fluent = true)
public final class ComposicionAplicacion {
    private final HibernateTransactions transacciones;
    private final AutenticacionService autenticacion;
    private final UsuarioService usuarios;
    private final ConfiguracionService configuracion;
    private final AuditoriaService auditoria;
    private final TipoCambioService tasas;
    private final PropiedadService propiedades;
    private final UnidadService unidades;
    private final InquilinoService inquilinos;
    private final ContratoService contratos;
    private final PagoService pagos;
    private final CargoService cargos;
    private final CobranzaService cobranza;
    private final GarantiaService garantias;
    private final DashboardService dashboard;
    private final MantenimientoService mantenimiento;
    private final AdjuntoService adjuntos;
    private final RespaldoService respaldos;
    private final ServicioCatalogos catalogosApi;
    private final ServicioPagos pagosApi;

    public ComposicionAplicacion(SessionFactory factory) {
        Objects.requireNonNull(factory, "Falta la fábrica de sesiones.");
        transacciones = new HibernateTransactions(factory);
        autenticacion = new AutenticacionService(transacciones);
        usuarios = new UsuarioService(transacciones);
        configuracion = new ConfiguracionService(transacciones);
        auditoria = new AuditoriaService(transacciones);
        tasas = new TipoCambioService(transacciones);
        propiedades = new PropiedadService(transacciones);
        unidades = new UnidadService(transacciones);
        inquilinos = new InquilinoService(transacciones);
        contratos = new ContratoService(transacciones);
        pagos = new PagoService(transacciones);
        cargos = new CargoService(transacciones);
        cobranza = new CobranzaService(transacciones);
        garantias = new GarantiaService(transacciones);
        dashboard = new DashboardService(transacciones);
        mantenimiento = new MantenimientoService(transacciones);
        adjuntos = new AdjuntoService(transacciones);
        respaldos = new RespaldoService(transacciones);
        catalogosApi = new ServicioCatalogos(transacciones, propiedades, unidades,
                inquilinos, contratos, garantias);
        pagosApi = new ServicioPagos(transacciones, pagos, cargos, cobranza);
    }
}
