package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class DashboardService {
    private final HibernateTransactions transactions =
            new HibernateTransactions(HibernateUtil.getSessionFactory());
    private final PermisoService permisos = new PermisoService();

    public Map<String, Object> obtenerMetricas() {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, SesionUsuario.getActual(), Permiso.CONSULTAR_REPORTES);
            Map<String, Object> datos = new HashMap<>();
            datos.put("totalPropiedades", session.createQuery("select count(p) from Propiedad p where p.activa = true", Long.class).getSingleResult().intValue());
            datos.put("totalUnidades", session.createQuery("select count(u) from Unidad u", Long.class).getSingleResult().intValue());
            datos.put("totalInquilinos", session.createQuery("select count(i) from Inquilino i where i.activo = true", Long.class).getSingleResult().intValue());
            datos.put("totalContratosVigentes", session.createQuery("select count(c) from Contrato c where c.estado = 'Vigente'", Long.class).getSingleResult().intValue());
            datos.put("cargosPendientesCant", session.createQuery("select count(c) from Cargo c where c.estado = 'Pendiente'", Long.class).getSingleResult().intValue());
            datos.put("cargosPendientesMonto", session.createQuery("select coalesce(sum(c.importe), 0) from Cargo c where c.estado = 'Pendiente'", BigDecimal.class).getSingleResult());
            datos.put("pagosRegistradosCant", session.createQuery("select count(p) from Pago p where p.estado = 'Registrado'", Long.class).getSingleResult().intValue());
            datos.put("pagosRegistradosMonto", session.createQuery("select coalesce(sum(p.montoContrato), 0) from Pago p where p.estado = 'Registrado'", BigDecimal.class).getSingleResult());
            return datos;
        });
    }
}
