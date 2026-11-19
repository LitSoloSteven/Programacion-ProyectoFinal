package ni.edu.uam.proyectofinalprogramacionappescritorio.api.piloto;

import ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.ExcepcionApi;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SolicitudHttp {
    private SolicitudHttp() { }

    static List<String> normalizarRuta(String ruta) {
        try {
            return segmentosRuta(ruta);
        } catch (IllegalArgumentException error) {
            throw ExcepcionApi.solicitudInvalida("La ruta solicitada no es válida.");
        }
    }

    static Map<String, String> analizarConsulta(String consultaCruda) {
        if (consultaCruda == null || consultaCruda.isBlank()) return Map.of();
        Map<String, String> valores = new LinkedHashMap<>();
        for (String par : consultaCruda.split("&")) {
            if (par.isEmpty()) continue;
            int separador = par.indexOf('=');
            String clave = separador < 0 ? par : par.substring(0, separador);
            String valor = separador < 0 ? "" : par.substring(separador + 1);
            try {
                valores.put(URLDecoder.decode(clave, StandardCharsets.UTF_8),
                        URLDecoder.decode(valor, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw ExcepcionApi.solicitudInvalida("La cadena de consulta no es válida.");
            }
        }
        return valores;
    }

    static List<String> segmentosRuta(String ruta) {
        String limpia = ruta.startsWith("/") ? ruta.substring(1) : ruta;
        if (limpia.endsWith("/")) limpia = limpia.substring(0, limpia.length() - 1);
        if (limpia.isEmpty()) return List.of();
        String[] partes = limpia.split("/", -1);
        List<String> resultado = new ArrayList<>(partes.length);
        for (String parte : partes) {
            // En una ruta, '+' es un carácter literal; en la consulta sí representa espacio.
            String decodificada = URLDecoder.decode(
                    parte.replace("+", "%2B"), StandardCharsets.UTF_8);
            if (contieneEscapePendiente(decodificada)) {
                throw new IllegalArgumentException("La ruta contiene una codificación anidada.");
            }
            resultado.add(decodificada);
        }
        return List.copyOf(resultado);
    }

    private static boolean contieneEscapePendiente(String valor) {
        for (int i = 0; i + 2 < valor.length(); i++) {
            if (valor.charAt(i) == '%' && esHexadecimal(valor.charAt(i + 1))
                    && esHexadecimal(valor.charAt(i + 2))) {
                return true;
            }
        }
        return false;
    }

    private static boolean esHexadecimal(char valor) {
        return valor >= '0' && valor <= '9'
                || valor >= 'a' && valor <= 'f'
                || valor >= 'A' && valor <= 'F';
    }

}
