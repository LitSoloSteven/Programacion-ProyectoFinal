package ni.edu.uam.proyectofinalprogramacionappescritorio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CifradoRespaldoTest {
    private static final char[] CLAVE_VALIDA = "ClaveRespaldoLatch2026!".toCharArray();

    @TempDir
    Path tempDir;

    @Test
    void idaYVueltaParaArchivoVacioPequenoMultiBloqueYMultiploExacto() throws Exception {
        byte[] vacio = new byte[0];
        byte[] pequeno = "Manifiesto y datos Latch UTF-8 áéíóú ñ C$ US$".getBytes(StandardCharsets.UTF_8);
        byte[] multiBloqueExacto = new byte[8192];
        byte[] multiBloqueConResto = new byte[8192 + 37];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(multiBloqueExacto);
        rng.nextBytes(multiBloqueConResto);

        verificarIdaYVuelta("vacio.bin", vacio, 4096);
        verificarIdaYVuelta("pequeno.bin", pequeno, 4096);
        verificarIdaYVuelta("exacto.bin", multiBloqueExacto, 4096);
        verificarIdaYVuelta("resto.bin", multiBloqueConResto, 4096);
    }

    @Test
    void rechazaContrasenaIncorrectaSinDejarArchivoPlano() throws Exception {
        Path origen = tempDir.resolve("plano.bin");
        Path cifrado = tempDir.resolve("paquete.lbackup");
        Path destino = tempDir.resolve("salida.bin");
        Files.writeString(origen, "contenido confidencial de prueba", StandardCharsets.UTF_8);

        CifradoRespaldo.cifrarArchivo(origen, cifrado, CLAVE_VALIDA, 4096);

        RestauracionException err = assertThrows(RestauracionException.class, () ->
                CifradoRespaldo.descifrarYVerificarArchivo(cifrado, destino, "OtraClaveIncorrecta123".toCharArray()));
        assertEquals(RestauracionException.CODIGO_AUTENTICIDAD, err.getCodigo());
        assertFalse(Files.exists(destino), "No debe quedar archivo plano tras fallo de autenticidad.");
    }

    @Test
    void rechazaAlteracionesEnEncabezadoCargaEtiquetaTruncamientoYBytesExtra() throws Exception {
        Path origen = tempDir.resolve("datos.bin");
        Path cifradoBase = tempDir.resolve("base.lbackup");
        byte[] datos = new byte[6000];
        new SecureRandom().nextBytes(datos);
        Files.write(origen, datos);
        CifradoRespaldo.cifrarArchivo(origen, cifradoBase, CLAVE_VALIDA, 4096);
        byte[] originales = Files.readAllBytes(cifradoBase);

        // 1. Alterar encabezado (Aad de bloque 0)
        byte[] alteradoHeader = originales.clone();
        alteradoHeader[10] ^= 0x01;
        assertFallaAutenticidad(alteradoHeader, "header");

        // 2. Alterar contenido cifrado
        byte[] alteradoPayload = originales.clone();
        alteradoPayload[70] ^= 0x40;
        assertFallaAutenticidad(alteradoPayload, "payload");

        // 3. Alterar etiqueta GCM final
        byte[] alteradoTag = originales.clone();
        alteradoTag[alteradoTag.length - 1] ^= (byte) 0xFF;
        assertFallaAutenticidad(alteradoTag, "tag");

        // 4. Truncar archivo cifrado
        byte[] truncado = java.util.Arrays.copyOf(originales, originales.length - 5);
        assertFallaAutenticidad(truncado, "truncado");

        // 5. Agregar byte extra al final tras el bloque final
        byte[] conExtra = java.util.Arrays.copyOf(originales, originales.length + 1);
        conExtra[conExtra.length - 1] = 0x2A;
        assertFallaAutenticidad(conExtra, "extra");
    }

    @Test
    void validaPoliticaDeContrasenaDeRespaldo() {
        assertThrows(IllegalArgumentException.class, () -> CifradoRespaldo.validarContrasenaRespaldo(null));
        assertThrows(IllegalArgumentException.class,
                () -> CifradoRespaldo.validarContrasenaRespaldo("corta123".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> CifradoRespaldo.validarContrasenaRespaldo("            ".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> CifradoRespaldo.validarContrasenaRespaldo("x".repeat(129).toCharArray()));
        CifradoRespaldo.validarContrasenaRespaldo(CLAVE_VALIDA);
    }

    @Test
    void rendimientoDeFlujoMultiBloqueEsCompatibleConRnf036YRnf037() throws Exception {
        Path origen = tempDir.resolve("ocho-mib.bin");
        Path cifrado = tempDir.resolve("ocho-mib.lbackup");
        Path restaurado = tempDir.resolve("ocho-mib.out");
        byte[] bloque = new byte[1024 * 1024];
        new SecureRandom().nextBytes(bloque);
        try (var out = Files.newOutputStream(origen)) {
            for (int i = 0; i < 8; i++) {
                out.write(bloque);
            }
        }

        long t0 = System.nanoTime();
        CifradoRespaldo.cifrarArchivo(origen, cifrado, CLAVE_VALIDA);
        CifradoRespaldo.descifrarYVerificarArchivo(cifrado, restaurado, CLAVE_VALIDA);
        long ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();

        assertEquals(CifradoRespaldo.calcularSha256(origen), CifradoRespaldo.calcularSha256(restaurado));
        assertTrue(ms < 15_000, "El cifrado y descifrado de 8 MiB tardó demasiado: " + ms + " ms");
    }

    private void verificarIdaYVuelta(String nombre, byte[] contenido, int tamanoBloque) throws Exception {
        Path origen = tempDir.resolve(nombre);
        Path cifrado = tempDir.resolve(nombre + ".lbackup");
        Path restaurado = tempDir.resolve(nombre + ".rest");
        Files.write(origen, contenido);

        CifradoRespaldo.cifrarArchivo(origen, cifrado, CLAVE_VALIDA, tamanoBloque);
        CifradoRespaldo.descifrarYVerificarArchivo(cifrado, restaurado, CLAVE_VALIDA);

        assertArrayEquals(contenido, Files.readAllBytes(restaurado));
    }

    private void assertFallaAutenticidad(byte[] bytesCorruptos, String etiqueta) throws Exception {
        Path corrupto = tempDir.resolve("corrupto-" + etiqueta + ".lbackup");
        Path destino = tempDir.resolve("destino-" + etiqueta + ".bin");
        Files.write(corrupto, bytesCorruptos);

        RestauracionException err = assertThrows(RestauracionException.class,
                () -> CifradoRespaldo.descifrarYVerificarArchivo(corrupto, destino, CLAVE_VALIDA),
                "Debe fallar para variante: " + etiqueta);
        assertEquals(RestauracionException.CODIGO_AUTENTICIDAD, err.getCodigo());
        assertFalse(Files.exists(destino), "No debe quedar archivo plano en variante: " + etiqueta);
    }
}
