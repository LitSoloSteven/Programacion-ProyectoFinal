package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.*;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.TipoCambio;
import org.hibernate.SessionFactory;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Usa identidad y permisos vigentes de main, comprobados dentro de la transaccion. */
public final class TipoCambioService {
    private final HibernateTransactions tx;
    private final Supplier<SesionUsuario> identidad;
    private final Clock reloj;
    private final TipoCambioDao dao = new HibernateTipoCambioDao();
    private final PermisoService permisos = new PermisoService();

    public TipoCambioService(SessionFactory factory) {
        this(factory, SesionUsuario::getActual, Clock.systemUTC());
    }
    public TipoCambioService(SessionFactory factory, Supplier<SesionUsuario> identidad, Clock reloj) {
        tx = new HibernateTransactions(factory);
        this.identidad = Objects.requireNonNull(identidad);
        this.reloj = Objects.requireNonNull(reloj);
    }
    public TipoCambio autorizar(LocalDate fecha, BigDecimal nioPorUsd, String fuente, String motivo) {
        return tx.ejecutar(s -> {
            var actor = permisos.exigir(s, identidad.get(), Permiso.CAMBIAR_CONFIGURACION);
            TipoCambio tasa = TipoCambio.autorizada(fecha, nioPorUsd, fuente, reloj.instant(), actor, motivo);
            dao.guardar(s, tasa);
            return tasa;
        });
    }
    public List<TipoCambio> historial() {
        return tx.ejecutar(s -> {
            permisos.exigir(s, identidad.get(), Permiso.CONSULTAR_REPORTES);
            return List.copyOf(dao.historial(s));
        });
    }
    public TipoCambio autorizadaPara(LocalDate fecha) {
        Objects.requireNonNull(fecha, "Falta la fecha.");
        return tx.ejecutar(s -> {
            permisos.exigir(s, identidad.get(), Permiso.OPERAR_ALQUILERES);
            return dao.autorizadaPara(s, fecha).orElseThrow(() ->
                    new IllegalStateException("No hay tasa autorizada para la fecha del pago."));
        });
    }
    public TipoCambio consultarInformativa(ClienteTipoCambio cliente)
            throws java.io.IOException, InterruptedException {
        // La llamada HTTP ocurre despues del commit: no mantiene bloqueada una conexion.
        tx.ejecutar(s -> {
            permisos.exigir(s, identidad.get(), Permiso.CONSULTAR_REPORTES);
            return null;
        });
        return Objects.requireNonNull(cliente).consultar();
    }
}
