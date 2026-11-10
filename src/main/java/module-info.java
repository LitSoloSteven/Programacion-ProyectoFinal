module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
}