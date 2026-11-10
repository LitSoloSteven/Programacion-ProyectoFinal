module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;

    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio.dao;
}
