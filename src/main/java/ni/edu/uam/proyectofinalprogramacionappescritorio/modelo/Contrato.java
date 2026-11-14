package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * Entidad de dominio que representa un Contrato de arrendamiento en Latch.
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
    private UUID id;
    @Column(name = "unidad_id", nullable = false)
    private UUID unidadId;
    @Transient
    private String unidadInfo;
    @Column(name = "inquilino_id", nullable = false)
    private UUID inquilinoId;
    @Transient
    private String inquilinoNombre;
    @Column(nullable = false)
    private LocalDate inicio;
    @Column(nullable = false)
    private LocalDate fin;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal canon;
    @Column(length = 3, nullable = false)
    private String moneda;
    @Column(name = "dia_pago", nullable = false)
    private int diaPago;
    @Embedded
    private DepositoPactado deposito = new DepositoPactado(BigDecimal.ZERO);
    @OneToOne(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private Garantia garantiaSeguimiento;
    @Column(length = 24, nullable = false)
    private String estado;
    @Version
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
        this.deposito = new DepositoPactado(depositoPactado);
        this.estado = estado;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUnidadId() {
        return unidadId;
    }

    public void setUnidadId(UUID unidadId) {
        this.unidadId = unidadId;
    }

    public String getUnidadInfo() {
        return unidad == null ? unidadInfo : unidad.toString();
    }

    public void setUnidadInfo(String unidadInfo) {
        this.unidadInfo = unidadInfo;
    }

    public UUID getInquilinoId() {
        return inquilinoId;
    }

    public void setInquilinoId(UUID inquilinoId) {
        this.inquilinoId = inquilinoId;
    }

    public String getInquilinoNombre() {
        return inquilino == null ? inquilinoNombre : inquilino.getNombre();
    }

    public void setInquilinoNombre(String inquilinoNombre) {
        this.inquilinoNombre = inquilinoNombre;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public void setFin(LocalDate fin) {
        this.fin = fin;
    }

    public BigDecimal getCanon() {
        return canon;
    }

    public void setCanon(BigDecimal canon) {
        this.canon = canon;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public int getDiaPago() {
        return diaPago;
    }

    public void setDiaPago(int diaPago) {
        this.diaPago = diaPago;
    }

    public BigDecimal getDepositoPactado() {
        return deposito == null ? BigDecimal.ZERO : deposito.getMontoPactado();
    }

    public void setDepositoPactado(BigDecimal depositoPactado) {
        DepositoPactado nuevoDeposito = new DepositoPactado(depositoPactado);
        if (garantiaSeguimiento != null) {
            garantiaSeguimiento.actualizarMontoInicial(nuevoDeposito.getMontoPactado());
        }
        this.deposito = nuevoDeposito;
    }

    public Garantia getGarantiaSeguimiento() { return garantiaSeguimiento; }

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

    public String getEstado() {
        return estado;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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
