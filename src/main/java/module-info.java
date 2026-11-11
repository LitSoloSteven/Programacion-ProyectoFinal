module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;
    requires org.postgresql.jdbc;
    requires com.google.gson;
    requires static lombok;

    exports ni.edu.uam.proyectofinalprogramacionappescritorio.application;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.application to javafx.fxml;

    exports ni.edu.uam.proyectofinalprogramacionappescritorio.config;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.service;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.util;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.dao;

    exports ni.edu.uam.proyectofinalprogramacionappescritorio.modelos;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.modelos to org.hibernate.orm.core, javafx.base;
}