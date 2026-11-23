package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import java.io.IOException;

public interface PeticionApi {
    String parametro(String nombre);

    String consulta(String nombre);

    String cabecera(String nombre);

    SesionApi sesion();

    String leerCuerpo() throws IOException;
}
