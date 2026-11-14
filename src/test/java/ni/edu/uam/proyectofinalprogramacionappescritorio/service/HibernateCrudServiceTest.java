package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateTransactions;
import ni.edu.uam.proyectofinalprogramacionappescritorio.config.HibernateUtil;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Propiedad;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Rol;
import ni.edu.uam.proyectofinalprogramacionappescritorio.modelo.Usuario;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HibernateCrudServiceTest {
    private SessionFactory factory;
    private HibernateTransactions tx;
    private HibernateCrudService crud;
    private SesionUsuario identidad;

    @BeforeEach
    void iniciar() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        properties.setProperty("hibernate.connection.url",
                "jdbc:h2:mem:crud_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        properties.setProperty("hibernate.connection.username", "sa");
        properties.setProperty("hibernate.connection.password", "");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        factory = HibernateUtil.construir(properties);
        tx = new HibernateTransactions(factory);
        crud = new HibernateCrudService(tx);
        String nombre = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        long usuarioId = tx.ejecutar(session -> {
            Usuario usuario = new Usuario(nombre, "hash", Rol.PROPIETARIO);
            session.persist(usuario);
            session.flush();
            return usuario.getId();
        });
        identidad = SesionUsuario.paraSesionAutenticada(usuarioId, nombre, Rol.PROPIETARIO);
    }

    @AfterEach
    void cerrar() {
        SesionUsuario.cerrarSesion();
        if (factory != null) factory.close();
    }

    @Test
    void guardarSoloAceptaAltasYNoMezclaCamposDesconectados() {
        Propiedad nueva = new Propiedad();
        nueva.setNombre("Edificio original");
        nueva.setDireccion("Calle original");
        SesionUsuario.ejecutarComo(identidad, () ->
                crud.guardar(nueva, Propiedad::getId, propiedad -> { }));
        UUID id = nueva.getId();

        Propiedad desconectada = new Propiedad(id, "Edificio alterado",
                "Dirección alterada", false, null, 0);
        assertThrows(IllegalArgumentException.class, () -> SesionUsuario.ejecutarComo(
                identidad, () -> crud.guardar(desconectada, Propiedad::getId, propiedad -> { })));

        Propiedad persistida = tx.ejecutar(session -> session.find(Propiedad.class, id));
        assertEquals("Edificio original", persistida.getNombre());
        assertEquals("Calle original", persistida.getDireccion());
        assertEquals(true, persistida.isActiva());
    }
}
