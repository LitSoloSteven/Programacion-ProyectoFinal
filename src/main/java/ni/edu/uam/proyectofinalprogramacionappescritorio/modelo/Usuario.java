package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Locale;
import java.util.Objects;

@Entity
@org.hibernate.annotations.Check(constraints = "rol in ('PROPIETARIO', 'ASISTENTE')")
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_nombre", columnNames = "nombre"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80, updatable = false)
    private String nombre;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false, length = 20)
    private String rol;
    @Column(nullable = false)
    private boolean activo = true;
    @Version
    private long version;

    public Usuario(String nombre, String passwordHash, String rol) {
        this.nombre = normalizarNombre(nombre);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        if (!Rol.esValido(rol)) throw new IllegalArgumentException("Rol inválido.");
        this.rol = rol;
    }

    public static String normalizarNombre(String nombre) {
        if (nombre == null || !nombre.strip().matches("[a-zA-Z0-9._-]{3,80}")) {
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 80 letras, números, puntos, guiones o guiones bajos.");
        }
        return nombre.strip().toLowerCase(Locale.ROOT);
    }

    public void desactivar() { activo = false; }
    public void cambiarRol(String rol) {
        if (!Rol.esValido(rol)) throw new IllegalArgumentException("Rol inválido.");
        this.rol = rol;
    }
}
