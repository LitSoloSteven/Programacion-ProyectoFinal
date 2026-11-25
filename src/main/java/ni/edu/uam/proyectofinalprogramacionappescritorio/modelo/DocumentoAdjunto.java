package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Metadatos de un archivo adjunto controlado en disco (M02, M03, M04, M12 / RN-25).
 */
@Entity
@Table(name = "documento_adjunto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentoAdjunto {
    public static final long MAX_TAMANO_BYTES = 5L * 1024 * 1024;
    public static final String MIME_PDF = "application/pdf";
    public static final String MIME_PNG = "image/png";
    public static final String MIME_JPEG = "image/jpeg";
    public static final Set<String> MIMES_PERMITIDOS = Set.of(MIME_PDF, MIME_PNG, MIME_JPEG);
    public static final Set<String> MIMES_IMAGEN = Set.of(MIME_PNG, MIME_JPEG);

    @Id
    private UUID id;

    @Column(name = "propiedad_id", updatable = false)
    private UUID propiedadId;

    @Column(name = "unidad_id", updatable = false)
    private UUID unidadId;

    @Column(name = "inquilino_id", updatable = false)
    private UUID inquilinoId;

    @Column(name = "contrato_id", updatable = false)
    private UUID contratoId;

    @Column(name = "parametro_id", updatable = false)
    private UUID parametroId;

    @Column(name = "nombre_original", nullable = false, length = 180, updatable = false)
    private String nombreOriginal;

    @Column(name = "ruta_interna", nullable = false, unique = true, length = 80, updatable = false)
    private String rutaInterna;

    @Column(nullable = false, length = 30, updatable = false)
    private String mime;

    @Column(name = "tamano_bytes", nullable = false, updatable = false)
    private long tamanoBytes;

    @Column(nullable = false, length = 64, updatable = false)
    private String sha256;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false, updatable = false)
    private Usuario creadoPor;

    @Version
    private long version;

    private DocumentoAdjunto(UUID propiedadId, UUID unidadId, UUID inquilinoId, UUID contratoId, UUID parametroId,
                             String nombreOriginal, String rutaInterna, String mime, long tamanoBytes,
                             String sha256, Instant creadoEn, Usuario creadoPor) {
        int duenos = (propiedadId != null ? 1 : 0) + (unidadId != null ? 1 : 0)
                + (inquilinoId != null ? 1 : 0) + (contratoId != null ? 1 : 0) + (parametroId != null ? 1 : 0);
        if (duenos != 1) {
            throw new IllegalArgumentException("El documento adjunto debe pertenecer a una única entidad.");
        }
        this.id = UUID.randomUUID();
        this.propiedadId = propiedadId;
        this.unidadId = unidadId;
        this.inquilinoId = inquilinoId;
        this.contratoId = contratoId;
        this.parametroId = parametroId;
        if (nombreOriginal == null || nombreOriginal.isBlank() || nombreOriginal.strip().length() > 180) {
            throw new IllegalArgumentException("El nombre del archivo es inválido.");
        }
        this.nombreOriginal = nombreOriginal.strip();
        if (rutaInterna == null || !rutaInterna.matches("^[0-9a-f-]{36}\\.(pdf|png|jpg)$")) {
            throw new IllegalArgumentException("La ruta interna del adjunto es inválida.");
        }
        this.rutaInterna = rutaInterna;
        if (mime == null || !MIMES_PERMITIDOS.contains(mime)) {
            throw new IllegalArgumentException("El tipo MIME del adjunto no está permitido.");
        }
        this.mime = mime;
        if (tamanoBytes < 1 || tamanoBytes > MAX_TAMANO_BYTES) {
            throw new IllegalArgumentException("ERR-019: El adjunto debe medir entre 1 byte y 5 MiB.");
        }
        this.tamanoBytes = tamanoBytes;
        if (sha256 == null || !sha256.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("La huella SHA-256 del adjunto es inválida.");
        }
        this.sha256 = sha256;
        this.activo = true;
        this.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        this.creadoPor = Objects.requireNonNull(creadoPor, "Falta el usuario autor.");
    }

    public static DocumentoAdjunto paraPropiedad(UUID propiedadId, String nombreOriginal, String rutaInterna,
                                               String mime, long tamanoBytes, String sha256,
                                               Instant creadoEn, Usuario creadoPor) {
        return new DocumentoAdjunto(Objects.requireNonNull(propiedadId), null, null, null, null,
                nombreOriginal, rutaInterna, mime, tamanoBytes, sha256, creadoEn, creadoPor);
    }

    public static DocumentoAdjunto paraUnidad(UUID unidadId, String nombreOriginal, String rutaInterna,
                                            String mime, long tamanoBytes, String sha256,
                                            Instant creadoEn, Usuario creadoPor) {
        return new DocumentoAdjunto(null, Objects.requireNonNull(unidadId), null, null, null,
                nombreOriginal, rutaInterna, mime, tamanoBytes, sha256, creadoEn, creadoPor);
    }

    public static DocumentoAdjunto paraInquilino(UUID inquilinoId, String nombreOriginal, String rutaInterna,
                                               String mime, long tamanoBytes, String sha256,
                                               Instant creadoEn, Usuario creadoPor) {
        return new DocumentoAdjunto(null, null, Objects.requireNonNull(inquilinoId), null, null,
                nombreOriginal, rutaInterna, mime, tamanoBytes, sha256, creadoEn, creadoPor);
    }

    public static DocumentoAdjunto paraContrato(UUID contratoId, String nombreOriginal, String rutaInterna,
                                              String mime, long tamanoBytes, String sha256,
                                              Instant creadoEn, Usuario creadoPor) {
        return new DocumentoAdjunto(null, null, null, Objects.requireNonNull(contratoId), null,
                nombreOriginal, rutaInterna, mime, tamanoBytes, sha256, creadoEn, creadoPor);
    }

    public static DocumentoAdjunto paraParametro(UUID parametroId, String nombreOriginal, String rutaInterna,
                                               String mime, long tamanoBytes, String sha256,
                                               Instant creadoEn, Usuario creadoPor) {
        return new DocumentoAdjunto(null, null, null, null, Objects.requireNonNull(parametroId),
                nombreOriginal, rutaInterna, mime, tamanoBytes, sha256, creadoEn, creadoPor);
    }

    public void desactivar() {
        this.activo = false;
    }

    public String entidadTipo() {
        if (propiedadId != null) return "Propiedad";
        if (unidadId != null) return "Unidad";
        if (inquilinoId != null) return "Inquilino";
        if (contratoId != null) return "Contrato";
        return "Emisor";
    }

    public UUID entidadId() {
        if (propiedadId != null) return propiedadId;
        if (unidadId != null) return unidadId;
        if (inquilinoId != null) return inquilinoId;
        if (contratoId != null) return contratoId;
        return parametroId;
    }
}
