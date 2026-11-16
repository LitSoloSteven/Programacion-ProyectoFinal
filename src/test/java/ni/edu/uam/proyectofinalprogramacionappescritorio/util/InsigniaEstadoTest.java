package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InsigniaEstadoTest {
    @Test
    void cubreLosEstadosDelDominioConUnaEtiquetaVisible() {
        List<String> estados = List.of(
                "Pagado", "Parcial", "Vencido", "Pendiente", "Anulado", "Cancelado",
                "Registrado", "Vigente", "Borrador", "Por vencer", "Terminado",
                "Disponible", "En mantenimiento", "Inactiva", "Activo", "Inactivo",
                "Activa", "Inactiva", "Autorizada", "Pendiente de autorización");
        for (String estado : estados) {
            assertFalse(InsigniaEstado.textoVisible(estado).isBlank(), estado);
        }
        assertEquals("Pendiente de autorización",
                InsigniaEstado.textoVisible("Pendiente de autorización"));
    }

    @Test
    void estadoNoMapeadoConservaSuTextoCrudo() {
        assertEquals("Estado nuevo", InsigniaEstado.textoVisible("Estado nuevo"));
    }
}

