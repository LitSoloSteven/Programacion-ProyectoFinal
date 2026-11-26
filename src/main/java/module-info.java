module ni.edu.uam.proyectofinalprogramacionappescritorio {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.prefs;
    requires java.desktop;
    requires com.google.gson;
    requires static lombok;

    opens ni.edu.uam.proyectofinalprogramacionappescritorio to javafx.fxml;
    exports ni.edu.uam.proyectofinalprogramacionappescritorio;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.controller to javafx.fxml;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.cliente to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.contrato to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.comun.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.auth.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.catalogos.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.indicadores.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.mantenimiento.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.pagos.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.tasas.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.validaciones.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.admin.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.adjuntos.dto to com.google.gson;
    opens ni.edu.uam.proyectofinalprogramacionappescritorio.api.reportes.dto to com.google.gson;
}
