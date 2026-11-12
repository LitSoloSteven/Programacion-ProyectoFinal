package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CargoFormattingTest {
    @Test
    void importeSinContratoSeMuestraSinSimboloDeMoneda() {
        Cargo cargo = new Cargo();
        cargo.setImporte(new BigDecimal("1234.5"));

        assertEquals("1,234.50", cargo.getImporteFormateado());
        assertEquals("0.00", new Cargo().getImporteFormateado());
    }
}
