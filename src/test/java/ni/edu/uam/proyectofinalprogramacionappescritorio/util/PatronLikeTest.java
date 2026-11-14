package ni.edu.uam.proyectofinalprogramacionappescritorio.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatronLikeTest {
    @Test
    void escapaComodinesYElPropioCaracterEscape() {
        assertEquals("%A!%!_!!B%", PatronLike.contieneLiteral("A%_!B"));
    }
}
