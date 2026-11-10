package ni.edu.uam.proyectofinalprogramacionappescritorio.modelos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inquilino")
@Getter
@Setter
@NoArgsConstructor
public class Inquilino {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 50, unique = true)
    private String documentoIdentidad;

    @Column(nullable = false, length = 150)
    private String medioContacto;
}