package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Fila compartida para exclusión entre escrituras de adjuntos y respaldo/restauración (ADR-22).
 */
@Entity
@Table(name = "puerta_mantenimiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuertaMantenimiento {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String descripcion;

    public PuertaMantenimiento(String descripcion) {
        this.id = ID;
        this.descripcion = descripcion == null || descripcion.isBlank()
                ? "Puerta de respaldo y adjuntos" : descripcion.strip();
    }
}
