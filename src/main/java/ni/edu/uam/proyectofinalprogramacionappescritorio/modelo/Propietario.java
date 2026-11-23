package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;

/** Datos de contacto del propietario autenticado. */
@Entity
@Table(name = "propietario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Propietario {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    @Setter
    private String nombre;

    @Column(nullable = false, length = 150, unique = true)
    @Setter
    private String correo;

    @Column(length = 20)
    @Setter
    private String telefono;

    public Propietario(Usuario usuario, String nombre, String correo, String telefono) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
    }
}
