package ni.edu.uam.proyectofinalprogramacionappescritorio;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.RutasCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.ServicioCatalogos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ApiConfig;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.Router;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ServidorApi;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.TokenService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.RutasIndicadores;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.RutasPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.ServicioPagos;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.RutasTasas;
import ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.RutasValidaciones;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.AutenticacionService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.CargoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.ContratoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.DashboardService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.GarantiaService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.InquilinoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PagoService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.PropiedadService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.TipoCambioService;
import ni.edu.uam.proyectofinalprogramacionappescritorio.service.UnidadService;
import org.hibernate.SessionFactory;

import java.util.Objects;

/** Construye una sola vez los servicios compartidos por JavaFX y la API local. */
public final class ComposicionAplicacion {
    private final HibernateTransactions transacciones;
    private final AutenticacionService autenticacion;
    private final TipoCambioService tasas;
    private final PropiedadService propiedades;
    private final UnidadService unidades;
    private final InquilinoService inquilinos;
    private final ContratoService contratos;
    private final PagoService pagos;
    private final CargoService cargos;
    private final GarantiaService garantias;
    private final DashboardService dashboard;
    private final ServicioCatalogos catalogosApi;
    private final ServicioPagos pagosApi;

    public ComposicionAplicacion(SessionFactory factory) {
        Objects.requireNonNull(factory, "Falta la fábrica de sesiones.");
        transacciones = new HibernateTransactions(factory);
        autenticacion = new AutenticacionService(transacciones);
        tasas = new TipoCambioService(transacciones);
        propiedades = new PropiedadService(transacciones);
        unidades = new UnidadService(transacciones);
        inquilinos = new InquilinoService(transacciones);
        contratos = new ContratoService(transacciones);
        pagos = new PagoService(transacciones);
        cargos = new CargoService(transacciones);
        garantias = new GarantiaService(transacciones);
        dashboard = new DashboardService(transacciones);
        catalogosApi = new ServicioCatalogos(transacciones, propiedades, unidades,
                inquilinos, contratos);
        pagosApi = new ServicioPagos(transacciones, pagos, cargos);
    }

    public ServidorApi crearServidorApi(ApiConfig config) {
        Router router = new Router();
        new RutasTasas(tasas).registrar(router);
        new RutasCatalogos(catalogosApi).registrar(router);
        new RutasValidaciones(transacciones).registrar(router);
        new RutasPagos(pagosApi).registrar(router);
        new RutasIndicadores(dashboard).registrar(router);
        return new ServidorApi(config, autenticacion, new TokenService(), router);
    }

    public HibernateTransactions transacciones() { return transacciones; }
    public AutenticacionService autenticacion() { return autenticacion; }
    public TipoCambioService tasas() { return tasas; }
    public PropiedadService propiedades() { return propiedades; }
    public UnidadService unidades() { return unidades; }
    public InquilinoService inquilinos() { return inquilinos; }
    public ContratoService contratos() { return contratos; }
    public PagoService pagos() { return pagos; }
    public CargoService cargos() { return cargos; }
    public GarantiaService garantias() { return garantias; }
    public DashboardService dashboard() { return dashboard; }
    public ServicioCatalogos catalogosApi() { return catalogosApi; }
    public ServicioPagos pagosApi() { return pagosApi; }
}
