module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires com.google.gson;
    requires java.naming;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.postgresql.jdbc;

    opens ni.edu.uam.proyectofinalprogramacionappescritorio.modelo to org.hibernate.orm.core;


    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
}
