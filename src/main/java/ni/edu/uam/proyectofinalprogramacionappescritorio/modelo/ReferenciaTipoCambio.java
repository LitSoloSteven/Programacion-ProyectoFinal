package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "referencia_tipo_cambio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenciaTipoCambio {
    @Id private UUID id;
    @Column(length = 80, nullable = false) private String proveedor;
    @Column(length = 3, nullable = false) private String base;
    @Column(length = 3, nullable = false) private String destino;
    @Column(precision = 19, scale = 8, nullable = false) private BigDecimal valor;
    @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    @Column(name = "proximo_en", nullable = false) private Instant proximoEn;
    @Column(name = "consultado_en", nullable = false) private Instant consultadoEn;
    @Column(name = "huella_respuesta", length = 64, nullable = false) private String huellaRespuesta;

    public ReferenciaTipoCambio(BigDecimal valor, Instant actualizadoEn, Instant proximoEn,
                                Instant consultadoEn, String huellaRespuesta) {
        this.id = UUID.randomUUID();
        this.proveedor = "ExchangeRate-API / AYR Tech";
        this.base = "USD";
        this.destino = "NIO";
        this.valor = valor;
        this.actualizadoEn = actualizadoEn;
        this.proximoEn = proximoEn;
        this.consultadoEn = consultadoEn;
        this.huellaRespuesta = huellaRespuesta;
    }
}
