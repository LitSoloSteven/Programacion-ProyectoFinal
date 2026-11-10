module ni.edu.uam.proyectofinalprogramacionappescritorio {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Persistencia y Base de Datos (Hibernate y PostgreSQL)
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires java.naming;

    // Integración y Utilidades
    requires com.google.gson;
    requires static lombok;

    // 1. Permisos para la aplicación principal
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.application;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.application to javafx.fxml;

    // 2. Permisos para los controladores (necesario para JavaFX)
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.controllers;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.controllers to javafx.fxml;

    // 3. Permisos críticos para Hibernate sobre tus modelos
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.modelos;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.modelos to org.hibernate.orm.core, javafx.base;
}