package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador transaccional único de recibos (RN-07 / H4).
 * En PostgreSQL se reserva mediante fn_reservar_numero(); en H2 usa bloqueo
 * pesimista sobre la fila única id = 1.
 */
@Entity
@Table(name = "consecutivo_recibo")
@Getter
@Setter
@NoArgsConstructor
public class ConsecutivoRecibo {

    @Id
    private short id = 1;

    @Column(name = "siguiente_numero", nullable = false)
    private long siguienteNumero = 1L;

    public ConsecutivoRecibo(short id, long siguienteNumero) {
        this.id = id;
        this.siguienteNumero = siguienteNumero;
    }
}
