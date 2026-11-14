package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import ni.edu.uam.proyectofinalprogramacionappescritorio.service.SesionUsuario;

/**
 * Identidad mínima que viaja en el token Bearer. El rol se vuelve a validar
 * contra la base de datos en cada operación de negocio mediante PermisoService.
 */
public record SesionApi(Long usuarioId, String nombre, String rol) {
    public SesionApi {
        if (usuarioId == null || nombre == null || nombre.isBlank()
                || rol == null || rol.isBlank()) {
            throw new IllegalArgumentException("Sesión de API inválida.");
        }
    }

    public SesionUsuario comoUsuario() {
        return SesionUsuario.paraSesionAutenticada(usuarioId, nombre, rol);
    }
}