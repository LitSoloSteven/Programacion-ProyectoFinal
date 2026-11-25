package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "control_fx")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControlFx {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Id private UUID id;
    @Column(name = "concesion_id") private UUID concesionId;
    @Column(name = "concesion_hasta") private Instant concesionHasta;
    @Column(name = "proximo_intento", nullable = false) private Instant proximoIntento;
    @Column(nullable = false) private LocalDate mes;
    @Column(name = "intentos_mes", nullable = false) private int intentosMes;
    @Column(nullable = false) private boolean reintento;
    @Column(length = 40) private String aviso;
    @Column(name = "huella_configuracion", length = 64) private String huellaConfiguracion;

    public ControlFx(Instant ahora, LocalDate mes) {
        this.id = ID;
        this.proximoIntento = ahora;
        this.mes = mes;
    }

    public boolean conConcesionVigente(Instant ahora) {
        return concesionHasta != null && ahora.isBefore(concesionHasta);
    }

    public void abrirMes(LocalDate mesActual) {
        if (mesActual.isAfter(mes)) {
            mes = mesActual;
            intentosMes = 0;
            if ("LIMITE_MENSUAL".equals(aviso)) aviso = null;
        }
    }

    public void reservar(UUID concesion, Instant hasta) {
        concesionId = concesion;
        concesionHasta = hasta;
        intentosMes++;
    }

    public void terminar(Instant siguiente, boolean reintento, String aviso, String huellaConfiguracion) {
        concesionId = null;
        concesionHasta = null;
        proximoIntento = siguiente;
        this.reintento = reintento;
        this.aviso = aviso;
        this.huellaConfiguracion = huellaConfiguracion;
    }

    public void liberar() {
        concesionId = null;
        concesionHasta = null;
    }
}
