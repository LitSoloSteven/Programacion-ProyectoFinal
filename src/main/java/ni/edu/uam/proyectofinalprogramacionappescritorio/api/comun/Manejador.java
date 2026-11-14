package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

@FunctionalInterface
public interface Manejador {
    RespuestaJson manejar(ContextoPeticion contexto) throws Exception;
}