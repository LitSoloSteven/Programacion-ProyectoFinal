package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.util.List;

/** Excepción tipada para bloqueo de cierre H6 cuando una tarea supera los 10 segundos (ERR-056 / MSG-061). */
public class TareasActivasException extends IllegalStateException {
    public static final String CODIGO = "ERR-056";
    public static final String MENSAJE_MSG_061 =
            "Algunas tareas siguen en curso tras diez segundos. El proceso permanecerá abierto hasta liberar los recursos.";

    private final List<String> tareasPendientes;

    public TareasActivasException(List<String> tareasPendientes) {
        super(MENSAJE_MSG_061);
        this.tareasPendientes = tareasPendientes == null ? List.of() : List.copyOf(tareasPendientes);
    }

    public String getCodigo() {
        return CODIGO;
    }

    public List<String> getTareasPendientes() {
        return tareasPendientes;
    }
}
