package ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Serialización JSON compartida. Los DTOs de frontera usan String para
 * fechas, instantes, UUID e importes; así se evita float/double y se mantiene
 * el formato decimal exigido.
 */
public final class Json {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Json() { }

    public static Gson gson() {
        return GSON;
    }

    public static <T> T leer(String json, Class<T> tipo) {
        try {
            T valor = GSON.fromJson(json, tipo);
            if (valor == null) throw ExcepcionApi.jsonInvalido();
            return valor;
        } catch (JsonParseException e) {
            throw ExcepcionApi.jsonInvalido();
        }
    }

    public static String escribir(Object valor) {
        return GSON.toJson(valor);
    }
}