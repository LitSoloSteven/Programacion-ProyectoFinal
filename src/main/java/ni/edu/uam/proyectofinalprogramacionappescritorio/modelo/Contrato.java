package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 - Entidad de dominio que representa un Contrato de arrendamiento en Latch.

 */
@Entity
@Table(name = "contrato")
public class Contrato {

    private static final Set<String> ESTADOS = Set.of(
            "Borrador", "Vigente", "Por vencer", "Vencido", "Terminado", "Cancelado");
    private static final Set<String> ESTADOS_QUE_RESERVAN_UNIDAD = Set.of(
            "Vigente", "Por vencer", "Vencido");
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "Borrador", Set.of("Vigente", "Cancelado"),
            "Vigente", Set.of("Por vencer", "Vencido", "Terminado"),
            "Por vencer", Set.of("Vigente", "Vencido", "Terminado"),
            "Vencido", Set.of("Vigente", "Por vencer", "Terminado"),
            "Terminado", Set.of(),
            "Cancelado", Set.of());

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    @Setter
    private UUID id;

    @Column(name = "unidad_id", nullable = false)
    @Getter
    @Setter
    private UUID unidadId;

    @Transient
    @Setter
    private String unidadInfo;

    @Column(name = "inquilino_id", nullable = false)
    @Getter
    @Setter
    private UUID inquilinoId;

    @Transient
    @Setter
    private String inquilinoNombre;

    @Column(nullable = false)
    @Getter
    @Setter
    private LocalDate inicio;

    @Column(nullable = false)
    @Getter
    @Setter
    private LocalDate fin;

    @Column(precision = 19, scale = 2, nullable = false)
    @Getter
    @Setter
    private BigDecimal canon;

    @Column(length = 3, nullable = false)
    @Getter
    @Setter
    private String moneda;

    @Column(name = "dia_pago", nullable = false)
    @Getter
    @Setter
    private int diaPago;

    @OneToOne(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter
    private Garantia garantiaSeguimiento;

    @Column(name = "deposito_pactado", nullable = false, precision = 19, scale = 2)
    @Getter
    private BigDecimal depositoPactado = BigDecimal.ZERO;

    @Column(length = 24, nullable = false)
    @Getter
    private String estado;

    @Version
    @Getter
    @Setter
    private long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unidad_id", insertable = false, updatable = false)
    private Unidad unidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inquilino_id", insertable = false, updatable = false)
    private Inquilino inquilino;

    @Column(name = "terminado_el")
    private LocalDate terminadoEl;

    public Contrato() {
        this.moneda = "USD";
        this.diaPago = 5;
        this.canon = BigDecimal.ZERO;
        this.estado = "Borrador";
        this.version = 0;
    }

    public Contrato(UUID id, UUID unidadId, String unidadInfo, UUID inquilinoId, String inquilinoNombre,
                    LocalDate inicio, LocalDate fin, BigDecimal canon, String moneda,
                    int diaPago, BigDecimal depositoPactado, String estado, long version) {
        this.id = id;
        this.unidadId = unidadId;
        this.unidadInfo = unidadInfo;
        this.inquilinoId = inquilinoId;
        this.inquilinoNombre = inquilinoNombre;
        this.inicio = inicio;
        this.fin = fin;
        this.canon = canon;
        this.moneda = moneda;
        this.diaPago = diaPago;
        this.depositoPactado = new DepositoPactado(depositoPactado).getMontoPactado();
        this.estado = estado;
        this.version = version;
    }

    public void setUnidad(Unidad unidad) {
        this.unidad = unidad;
        this.unidadId = unidad == null ? null : unidad.getId();
    }

    public String getUnidadInfo() {
        return unidad == null ? unidadInfo : unidad.toString();
    }

    public void setInquilino(Inquilino inquilino) {
        this.inquilino = inquilino;
        this.inquilinoId = inquilino == null ? null : inquilino.getId();
    }

    public String getInquilinoNombre() {
        return inquilino == null ? inquilinoNombre : inquilino.getNombre();
    }

    public String getGarantiaSaldoFormateado() {
        if (garantiaSeguimiento == null || garantiaSeguimiento.getSaldoActual() == null) return "—";
        return String.format("%.2f %s", garantiaSeguimiento.getSaldoActual(), moneda);
    }

    public void inicializarGarantiaSeguimiento() {
        if (garantiaSeguimiento == null) {
            BigDecimal monto = getDepositoPactado();
            garantiaSeguimiento = new Garantia(this, monto, monto);
        }
    }

    public void setDepositoPactado(BigDecimal depositoPactado) {
        DepositoPactado nuevoDeposito = new DepositoPactado(depositoPactado);
        BigDecimal montoPactado = nuevoDeposito.getMontoPactado();
        if (garantiaSeguimiento != null) {
            garantiaSeguimiento.actualizarMontoInicial(montoPactado);
        }
        this.depositoPactado = montoPactado;
    }

    public void setEstado(String estado) {
        transicionarA(estado, LocalDate.now());
    }

    public static boolean esEstadoValido(String estado) {
        return ESTADOS.contains(estado);
    }

    public static boolean reservaUnidad(String estado) {
        return ESTADOS_QUE_RESERVAN_UNIDAD.contains(estado);
    }

    public void validarTransicionA(String nuevoEstado) {
        if (!esEstadoValido(nuevoEstado)) {
            throw new IllegalArgumentException("El estado del contrato no es válido.");
        }
        if (!Objects.equals(estado, nuevoEstado)
                && !TRANSICIONES.getOrDefault(estado, Set.of()).contains(nuevoEstado)) {
            throw new IllegalArgumentException("La transición de estado del contrato no está permitida.");
        }
    }

    public void transicionarA(String nuevoEstado, LocalDate fechaTerminacion) {
        validarTransicionA(nuevoEstado);
        if (Objects.equals(estado, nuevoEstado)) return;
        if ("Terminado".equals(nuevoEstado)) {
            if (fechaTerminacion == null) {
                throw new IllegalArgumentException("La fecha de terminación del contrato es obligatoria.");
            }
            terminadoEl = fechaTerminacion;
        } else {
            terminadoEl = null;
        }
        estado = nuevoEstado;
    }

    public String getCanonFormateado() {
        if (canon == null) return "0.00 " + moneda;
        return String.format("%.2f %s", canon, moneda);
    }

    public String getDepositoFormateado() {
        if (getDepositoPactado() == null) return "0.00 " + moneda;
        return String.format("%.2f %s", getDepositoPactado(), moneda);
    }

    @Override
    public String toString() {
        return (getInquilinoNombre() != null ? getInquilinoNombre() : "Contrato") + " — " + (getUnidadInfo() != null ? getUnidadInfo() : "");
    }

    public void terminar(LocalDate fecha) {
        transicionarA("Terminado", fecha);
    }
}
