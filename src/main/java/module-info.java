module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.controller to javafx.fxml;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.modelo to javafx.base;

    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.controller;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.service;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.dao;
}
