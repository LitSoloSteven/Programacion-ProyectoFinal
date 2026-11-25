package ni.edu.uam.proyectofinalprogramacionappescritorio.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresTestSupportTest {
    @Test
    void separaMigracionesSinPartirBloquesDollar() throws IOException {
        List<String> tasas = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V4__tasas_autorizadas.sql")));
        List<String> recibos = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V8__consecutivo_recibos_transaccional.sql")));

        assertEquals(2, tasas.size());
        assertEquals(5, recibos.size());
        assertTrue(recibos.get(4).contains("CREATE OR REPLACE FUNCTION public.fn_reservar_numero()"));
        assertTrue(recibos.get(4).contains("RETURN reservado;"), recibos.toString());
    }

    @Test
    void separaEsquemaInicialV0SinInvadirMigracionesPosteriores() throws IOException {
        String contenido = Files.readString(Path.of("docs", "migrations", "V0__esquema_inicial.sql"));
        List<String> sentencias = PostgresTestSupport.separarSentencias(contenido);

        long tablas = sentencias.stream()
                .filter(s -> s.contains("CREATE TABLE IF NOT EXISTS "))
                .count();
        long indices = sentencias.stream()
                .filter(s -> s.contains("CREATE INDEX IF NOT EXISTS "))
                .count();

        assertEquals(11, tablas, sentencias.toString());
        assertEquals(2, indices, sentencias.toString());
        assertEquals(13, sentencias.size(), sentencias.toString());
        assertTrue(!contenido.contains("uq_contrato_activo"), "V6 crea uq_contrato_activo sin IF NOT EXISTS.");
        assertTrue(!contenido.contains("CREATE TABLE IF NOT EXISTS propietario "));
        assertTrue(!contenido.contains("CREATE TABLE IF NOT EXISTS garantia "));
        assertTrue(!contenido.contains("CREATE TABLE IF NOT EXISTS solicitud_idempotente "));
        assertTrue(!contenido.contains("CREATE TABLE IF NOT EXISTS aplicacion_pago "));
        assertTrue(!contenido.contains("CREATE TABLE IF NOT EXISTS consecutivo_recibo "));
    }

    @Test
    void separaMigracionV11EnUnSoloBloqueDollar() throws IOException {
        List<String> sentencias = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V11__idempotencia_respuesta_text.sql")));

        assertEquals(1, sentencias.size(), sentencias.toString());
        assertTrue(sentencias.get(0).contains("ALTER COLUMN respuesta_json TYPE TEXT"), sentencias.toString());
        assertTrue(sentencias.get(0).contains("lo_get"), sentencias.toString());
    }

    @Test
    void separaMigracionV12DeCuentasYParametros() throws IOException {
        List<String> sentencias = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V12__p1_cuentas_y_parametros_historial.sql")));

        assertEquals(4, sentencias.size(), sentencias.toString());
        assertTrue(sentencias.get(0).contains("debe_cambiar_password"), sentencias.toString());
        assertTrue(sentencias.get(1).contains("emisor_info"), sentencias.toString());
        assertTrue(sentencias.get(2).contains("CREATE TABLE IF NOT EXISTS parametro_sistema"), sentencias.toString());
        assertTrue(sentencias.get(3).contains("ix_parametro_sistema_clave_vigencia"), sentencias.toString());
    }

    @Test
    void separaMigracionV13DeContratosYMovimientosGarantia() throws IOException {
        List<String> sentencias = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V13__p2_contratos_y_movimientos_garantia.sql")));

        assertEquals(12, sentencias.size(), sentencias.toString());
        assertTrue(sentencias.get(0).contains("predecesor_id"), sentencias.toString());
        assertTrue(sentencias.get(4).contains("ck_contrato_duracion"), sentencias.toString());
        assertTrue(sentencias.get(5).contains("CREATE TABLE IF NOT EXISTS movimiento_garantia"), sentencias.toString());
        assertTrue(sentencias.get(11).contains("ck_aplicacion_pago_fuente"), sentencias.toString());
    }

    @Test
    void separaMigracionV14DeCobranzaMoraYCreditos() throws IOException {
        List<String> sentencias = PostgresTestSupport.separarSentencias(
                Files.readString(Path.of("docs", "migrations", "V14__p3_cobranza_mora_y_creditos.sql")));

        assertEquals(20, sentencias.size(), sentencias.toString());
        assertTrue(sentencias.get(0).contains("cargo_origen_id"), sentencias.toString());
        assertTrue(sentencias.get(5).contains("uq_cargo_renta"), sentencias.toString());
        assertTrue(sentencias.get(6).contains("uq_cargo_mora"), sentencias.toString());
        assertTrue(sentencias.get(8).contains("ck_cargo_mora"), sentencias.toString());
        assertTrue(sentencias.get(16).contains("uq_aplicacion_pago_activa"), sentencias.toString());
        assertTrue(sentencias.get(19).contains("ck_aplicacion_reversion"), sentencias.toString());
    }
}
