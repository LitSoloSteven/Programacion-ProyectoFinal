package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Parámetro operativo inmutable con historial y vigencia por inserción.
 * No tiene columna version ni permite UPDATE.
 */
@Entity
@Immutable
@Table(name = "parametro_sistema",
        uniqueConstraints = @UniqueConstraint(name = "uk_parametro_sistema_clave_vigencia",
                columnNames = {"clave", "vigente_desde"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParametroSistema {
    @Id
    private UUID id;

    @Column(nullable = false, length = 80, updatable = false)
    private String clave;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String valor;

    @Column(name = "vigente_desde", nullable = false, updatable = false)
    private Instant vigenteDesde;

    @Column(nullable = false, length = 300, updatable = false)
    private String motivo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false, updatable = false)
    private Usuario creadoPor;

    public ParametroSistema(String clave, String valor, Instant vigenteDesde,
                            String motivo, Instant creadoEn, Usuario creadoPor) {
        this.id = UUID.randomUUID();
        this.clave = texto(clave, 80, "La clave del parámetro es inválida.");
        this.valor = textoSinMaximo(valor, "El valor del parámetro es obligatorio.");
        this.vigenteDesde = Objects.requireNonNull(vigenteDesde, "Falta la fecha de vigencia.");
        this.motivo = texto(motivo, 300, "El motivo es obligatorio (máximo 300 caracteres).");
        this.creadoEn = Objects.requireNonNull(creadoEn, "Falta el instante de creación.");
        this.creadoPor = Objects.requireNonNull(creadoPor, "Falta el usuario creador.");
    }

    private static String texto(String valor, int maximo, String mensaje) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }

    private static String textoSinMaximo(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }
}
