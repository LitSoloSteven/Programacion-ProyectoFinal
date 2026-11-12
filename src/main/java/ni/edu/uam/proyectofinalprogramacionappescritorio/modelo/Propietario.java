package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import ni.edu.uam.proyectofinalprogramacionappescritorio.entity.Usuario;

/** Datos de contacto del propietario autenticado. */
@Entity
@Table(name = "propietario")
public class Propietario {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    private String correo;

    @Column(length = 20)
    private String telefono;

    protected Propietario() {}

    public Propietario(Usuario usuario, String nombre, String correo, String telefono) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
