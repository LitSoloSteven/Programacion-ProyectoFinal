module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;
    requires org.postgresql.jdbc;


    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.entity to org.hibernate.orm.core;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.config;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.entity;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.service;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.util;
}
