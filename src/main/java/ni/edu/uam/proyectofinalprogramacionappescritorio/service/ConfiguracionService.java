package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Configuracion;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import java.util.UUID;

public final class ConfiguracionService {
    private final HibernateTransactions transactions;
    private final PermisoService permisos = new PermisoService();
    private final AuditoriaService auditoria = new AuditoriaService();
    public ConfiguracionService(HibernateTransactions transactions) { this.transactions = transactions; }

    public Configuracion consultar(SesionUsuario identidad, String clave) {
        return transactions.ejecutar(session -> {
            permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            return session.find(Configuracion.class, clave);
        });
    }

    public void guardar(SesionUsuario identidad, String clave, String valor, long versionEsperada) {
        transactions.ejecutar(session -> {
            Usuario actor = permisos.exigir(session, identidad, Permiso.CAMBIAR_CONFIGURACION);
            Configuracion config = session.find(Configuracion.class, clave);
            if (config == null) {
                if (versionEsperada != -1) throw new IllegalArgumentException("La configuración ya no existe.");
                session.persist(new Configuracion(clave, valor));
            } else {
                if (config.getVersion() != versionEsperada) {
                    throw new IllegalArgumentException(
                            "La configuración cambió. Actualiza los datos antes de guardar.");
                }
                config.cambiarValor(valor);
            }
            auditoria.registrar(session, actor, UUID.randomUUID(), "GUARDAR_CONFIGURACION", "configuracion:" + clave);
            return null;
        });
    }
}
