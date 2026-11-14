package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Auditoria;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.Session;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public final class AuditoriaService {
    private final RelojService reloj = new RelojService();

    // Participa en la transacción de negocio: si ésta falla, tampoco queda una auditoría de éxito.
    public void registrar(Session session, Usuario actor, UUID correlacion, String accion, String referencia) {
        session.persist(new Auditoria(actor, reloj.leer(session).instante(), correlacion,
                accion, referencia));
    }

    public void registrarIntentoLoginFallido(Session session, UUID correlacion,
                                             String usuario, String origen) {
        String identificador = usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
        String origenSeguro = origen == null ? "desconocido" : origen.strip().toLowerCase(Locale.ROOT);
        if (!origenSeguro.matches("[a-z0-9:.%_-]{1,64}")) origenSeguro = "desconocido";
        String referencia = "usuario_sha256:" + sha256(identificador) + ";origen:" + origenSeguro;
        session.persist(Auditoria.intentoLoginFallido(reloj.leer(session).instante(),
                correlacion, referencia));
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256 para registrar el intento.", e);
        }
    }
}
