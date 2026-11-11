package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.dao.TipoCambioDao;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Autorizacion de tasas y consulta del historial, con permisos en el servicio. */
public final class TipoCambioService {
    private final TipoCambioDao dao;
    private final Supplier<Usuario> usuarioActual;
    private final Clock reloj;

    public TipoCambioService(TipoCambioDao dao) {
        this(dao, SesionUsuario::getUsuarioActual, Clock.systemUTC());
    }

    public TipoCambioService(TipoCambioDao dao, Supplier<Usuario> usuarioActual, Clock reloj) {
        this.dao = Objects.requireNonNull(dao);
        this.usuarioActual = Objects.requireNonNull(usuarioActual);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public TipoCambio autorizar(LocalDate fecha, BigDecimal nioPorUsd,
                                 String fuente, String motivo) {
        Usuario actor = exigirUsuario();
        if (!actor.esPropietario()) {
            throw new SecurityException("Solo el Propietario puede autorizar tasas.");
        }
        TipoCambio tasa = TipoCambio.autorizada(fecha, nioPorUsd, fuente,
                reloj.instant(), actor.getId(), motivo);
        dao.guardar(tasa);
        return tasa;
    }

    public List<TipoCambio> historial() {
        exigirUsuario();
        return List.copyOf(dao.historial());
    }

    public TipoCambio autorizadaPara(LocalDate fecha) {
        exigirUsuario();
        Objects.requireNonNull(fecha, "Falta la fecha.");
        return dao.autorizadaPara(fecha).orElseThrow(() ->
                new IllegalStateException("No hay tasa autorizada para la fecha del pago."));
    }

    public TipoCambio consultarInformativa(ClienteTipoCambio cliente)
            throws java.io.IOException, InterruptedException {
        exigirUsuario();
        return Objects.requireNonNull(cliente).consultar();
    }

    private Usuario exigirUsuario() {
        Usuario actor = usuarioActual.get();
        if (actor == null || !actor.isActivo() || actor.getId() == null
                || !(actor.esPropietario() || "Asistente".equalsIgnoreCase(actor.getRolNombre()))) {
            throw new SecurityException("Se requiere una sesion activa y autorizada.");
        }
        return actor;
    }
}
