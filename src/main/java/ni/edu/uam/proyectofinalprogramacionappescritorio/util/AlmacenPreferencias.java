package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Abstracción mínima para persistir preferencias visuales locales de forma
 * desacoplada de tablas de dominio y con claves opacas por usuario.
 */
public interface AlmacenPreferencias {
    String NODO_RAIZ = "ni/edu/uam/latch/presentation";

    Optional<String> leer(String clave);

    void guardar(String clave, String valor) throws Exception;

    static AlmacenPreferencias predeterminado() {
        return new AlmacenPreferenciasJava(NODO_RAIZ);
    }

    static String idOpacoUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Falta el identificador del usuario.");
        }
        return UUID.nameUUIDFromBytes(("latch-user:" + usuarioId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    final class AlmacenPreferenciasJava implements AlmacenPreferencias {
        private final Preferences preferencias;

        AlmacenPreferenciasJava(String nodo) {
            this.preferencias = Preferences.userRoot().node(nodo);
        }

        @Override
        public Optional<String> leer(String clave) {
            if (clave == null || clave.isBlank()) return Optional.empty();
            return Optional.ofNullable(preferencias.get(clave, null));
        }

        @Override
        public void guardar(String clave, String valor) throws Exception {
            if (clave == null || clave.isBlank() || valor == null) return;
            preferencias.put(clave, valor);
            preferencias.flush();
        }
    }
}
