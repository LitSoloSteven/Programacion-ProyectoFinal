package ni.edu.uam.proyectofinalprogramacionappescritorio.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracion")
public class Configuracion {
    @Id @Column(length = 80)
    private String clave;
    @Column(nullable = false, length = 500)
    private String valor;
    @Version
    private long version;

    protected Configuracion() {}

    public Configuracion(String clave, String valor) {
        if (clave == null || clave.isBlank() || clave.length() > 80) {
            throw new IllegalArgumentException("Clave de configuración inválida.");
        }
        this.clave = clave;
        cambiarValor(valor);
    }

    public void cambiarValor(String valor) {
        if (valor == null || valor.isBlank() || valor.length() > 500) {
            throw new IllegalArgumentException("Valor de configuración inválido.");
        }
        this.valor = valor;
    }
    public String getClave() { return clave; }
    public String getValor() { return valor; }
    public long getVersion() { return version; }
}
