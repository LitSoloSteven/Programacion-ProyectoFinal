module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;
    requires org.postgresql.jdbc;
    requires jdk.httpserver;
    requires java.net.http;
    requires com.google.gson;


    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.controller to javafx.fxml;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.modelo to org.hibernate.orm.core, javafx.base;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.dto;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.config;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.service;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.util;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.dto to com.google.gson;
}
