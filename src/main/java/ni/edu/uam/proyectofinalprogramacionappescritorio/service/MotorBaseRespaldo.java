package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contrato interno para exportar, inspeccionar y restaurar bases de datos en entornos aislados (M12 / RN-29).
 */
public interface MotorBaseRespaldo {
    List<String> TABLAS_VERIFICADAS = List.of(
            "usuario",
            "propiedad",
            "unidad",
            "inquilino",
            "contrato",
            "cargo",
            "pago",
            "recibo",
            "garantia",
            "movimiento_garantia",
            "tipo_cambio",
            "solicitud_mantenimiento",
            "gasto_propiedad",
            "documento_adjunto",
            "parametro_sistema",
            "auditoria"
    );

    List<String> TABLAS_ESQUEMA_MINIMO = List.of(
            "usuario",
            "propiedad",
            "unidad",
            "inquilino",
            "contrato",
            "cargo",
            "pago",
            "recibo",
            "consecutivo_recibo",
            "garantia",
            "movimiento_garantia",
            "tipo_cambio",
            "solicitud_mantenimiento",
            "gasto_propiedad",
            "documento_adjunto",
            "parametro_sistema",
            "auditoria",
            "puerta_mantenimiento"
    );

    String identificadorMotor();

    void exportarBase(Path archivoDump);

    void exportarBaseDestino(ConexionJdbc destino, Path archivoDump);

    void restaurarEnBaseAislada(Path archivoDump, ConexionJdbc destino);

    EstadoRecibosBase inspeccionarRecibos(ConexionJdbc destino);

    Map<String, Long> contarTablas(ConexionJdbc destino);

    int contarSesionesOperativasActivas(ConexionJdbc destino);

    void validarEsquemaRestaurado(ConexionJdbc destino);

    record ConexionJdbc(String url, String usuario, char[] clave) {
        public ConexionJdbc {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Falta la URL JDBC de destino.");
            }
            url = url.strip();
            usuario = usuario == null ? "" : usuario.strip();
            clave = clave == null ? new char[0] : clave.clone();
        }

        public String claveTexto() {
            return new String(clave);
        }

        @Override
        public String toString() {
            return "ConexionJdbc[url=" + url + ", usuario=" + usuario + ", clave=***]";
        }
    }

    record EstadoRecibosBase(long ultimoNumeroRecibo, long siguienteNumeroRecibo) {
        public EstadoRecibosBase {
            if (ultimoNumeroRecibo < 0 || siguienteNumeroRecibo < 1) {
                throw new IllegalArgumentException("Estado de recibos inválido.");
            }
        }
    }

    static ConexionJdbc validarNoNula(ConexionJdbc conexion) {
        return Objects.requireNonNull(conexion, "Faltan los datos de conexión JDBC.");
    }
}
