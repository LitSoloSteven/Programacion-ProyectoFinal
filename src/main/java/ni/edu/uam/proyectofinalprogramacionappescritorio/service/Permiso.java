package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

public final class Permiso {
    public static final String GESTIONAR_USUARIOS = "GESTIONAR_USUARIOS";
    public static final String CAMBIAR_CONFIGURACION = "CAMBIAR_CONFIGURACION";
    public static final String CONSULTAR_AUDITORIA = "CONSULTAR_AUDITORIA";
    public static final String OPERAR_ALQUILERES = "OPERAR_ALQUILERES";
    public static final String CONSULTAR_REPORTES = "CONSULTAR_REPORTES";
    // Permisos exclusivos del propietario: el conjunto del asistente no los incluye.
    public static final String AUTORIZAR_TASA = "AUTORIZAR_TASA";
    public static final String ANULAR_PAGOS = "ANULAR_PAGOS";
    public static final String ANULAR_CARGOS = "ANULAR_CARGOS";
    public static final String LIQUIDAR_GARANTIA = "LIQUIDAR_GARANTIA";
    public static final String GESTIONAR_PROPIEDADES = "GESTIONAR_PROPIEDADES";
    public static final String GESTIONAR_UNIDADES = "GESTIONAR_UNIDADES";
    public static final String DESACTIVAR_INQUILINO = "DESACTIVAR_INQUILINO";

    private Permiso() {}

    public static boolean esValido(String permiso) {
        return GESTIONAR_USUARIOS.equals(permiso) || CAMBIAR_CONFIGURACION.equals(permiso)
                || CONSULTAR_AUDITORIA.equals(permiso) || OPERAR_ALQUILERES.equals(permiso)
                || CONSULTAR_REPORTES.equals(permiso) || AUTORIZAR_TASA.equals(permiso)
                || ANULAR_PAGOS.equals(permiso) || ANULAR_CARGOS.equals(permiso)
                || LIQUIDAR_GARANTIA.equals(permiso) || GESTIONAR_PROPIEDADES.equals(permiso)
                || GESTIONAR_UNIDADES.equals(permiso) || DESACTIVAR_INQUILINO.equals(permiso);
    }
}
