package ni.edu.uam.proyectofinalprogramacionappescritorio.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Modelo tabular inmutable para exportar reportes de Latch a XLSX con tipos numéricos y fechas reales.
 */
public record TablaReporteXlsx(
        String codigoReporte,
        String nombreHoja,
        String titulo,
        Map<String, String> metadatos,
        List<Columna> columnas,
        List<List<Object>> filas,
        List<Resumen> resumenes) {

    public enum Tipo {
        TEXTO,
        MONTO,
        TASA,
        ENTERO,
        FECHA,
        PORCENTAJE
    }

    public record Columna(String encabezado, Tipo tipo) {
        public Columna {
            if (encabezado == null || encabezado.isBlank()) {
                throw new IllegalArgumentException("El encabezado de la columna es obligatorio.");
            }
            Objects.requireNonNull(tipo, "Falta el tipo de columna.");
            encabezado = encabezado.strip();
        }
    }

    public record Resumen(String etiqueta, String moneda, BigDecimal valorNumerico, String textoAlterno) {
        public Resumen {
            if (etiqueta == null || etiqueta.isBlank()) {
                throw new IllegalArgumentException("La etiqueta de resumen es obligatoria.");
            }
            etiqueta = etiqueta.strip();
            moneda = moneda == null ? "" : moneda.strip();
        }

        public static Resumen numerico(String etiqueta, String moneda, BigDecimal valor) {
            return new Resumen(etiqueta, moneda, valor == null ? BigDecimal.ZERO : valor, null);
        }

        public static Resumen texto(String etiqueta, String texto) {
            return new Resumen(etiqueta, "", null, texto == null ? "—" : texto);
        }
    }

    public TablaReporteXlsx {
        if (codigoReporte == null || codigoReporte.isBlank()) {
            throw new IllegalArgumentException("Falta el código del reporte.");
        }
        codigoReporte = codigoReporte.strip();
        nombreHoja = sanitizarNombreHoja(nombreHoja);
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Falta el título del reporte.");
        }
        titulo = titulo.strip();

        Map<String, String> metaCopia = new LinkedHashMap<>();
        if (metadatos != null) {
            metadatos.forEach((k, v) -> {
                if (k != null && !k.isBlank()) {
                    metaCopia.put(k.strip(), v == null ? "—" : v);
                }
            });
        }
        metadatos = Collections.unmodifiableMap(metaCopia);

        Objects.requireNonNull(columnas, "Faltan las columnas.");
        if (columnas.isEmpty()) {
            throw new IllegalArgumentException("El reporte XLSX requiere al menos una columna.");
        }
        columnas = List.copyOf(columnas);

        List<List<Object>> filasCopia = new ArrayList<>();
        if (filas != null) {
            for (List<Object> fila : filas) {
                filasCopia.add(fila == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(fila)));
            }
        }
        filas = Collections.unmodifiableList(filasCopia);
        resumenes = resumenes == null ? List.of() : List.copyOf(resumenes);
    }

    private static String sanitizarNombreHoja(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Reporte";
        }
        String limpio = nombre.replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
        if (limpio.isEmpty()) {
            return "Reporte";
        }
        return limpio.length() > 31 ? limpio.substring(0, 31).strip() : limpio;
    }
}
