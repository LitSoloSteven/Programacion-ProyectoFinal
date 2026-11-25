\set ON_ERROR_STOP on

-- Ejecutar con el administrador de un clúster desechable. Los roles y datos
-- se revierten al terminar, y V11 corre con un rol sin privilegios elevados.
BEGIN;
CREATE ROLE latch_v11_test_owner NOLOGIN;
CREATE ROLE latch_v11_test_writer NOLOGIN;
CREATE SCHEMA latch_v11_test AUTHORIZATION latch_v11_test_owner;
SET LOCAL search_path TO latch_v11_test, public;
SET LOCAL ROLE latch_v11_test_owner;
CREATE TABLE solicitud_idempotente (respuesta_json oid);
GRANT USAGE ON SCHEMA latch_v11_test TO latch_v11_test_writer;
GRANT INSERT, SELECT ON solicitud_idempotente TO latch_v11_test_writer;
INSERT INTO solicitud_idempotente VALUES (NULL);

SET LOCAL ROLE latch_v11_test_writer;
INSERT INTO solicitud_idempotente
VALUES (lo_from_bytea(0, convert_to('{"recibo":42}', 'UTF8')))
RETURNING respuesta_json AS oid_respuesta \gset

SET LOCAL ROLE latch_v11_test_owner;
SAVEPOINT antes_de_v11;
\set ON_ERROR_STOP off
\ir ../../../../docs/migrations/V11__idempotencia_respuesta_text.sql
\set estado_v11 :SQLSTATE
\set mensaje_v11 :LAST_ERROR_MESSAGE
\set ON_ERROR_STOP on
ROLLBACK TO SAVEPOINT antes_de_v11;

SELECT :'estado_v11' = '42501'
       AND :'mensaje_v11' LIKE 'V11:%'
       AND (SELECT pg_typeof(respuesta_json)::text
            FROM solicitud_idempotente LIMIT 1) = 'oid'
       AND (SELECT count(*) FROM solicitud_idempotente) = 2 AS rechazo_correcto \gset
\if :rechazo_correcto
\else
DO $$ BEGIN RAISE EXCEPTION 'V11 debe conservar la columna OID y explicar el permiso faltante'; END $$;
\endif

SET LOCAL ROLE latch_v11_test_writer;
GRANT SELECT ON LARGE OBJECT :oid_respuesta TO latch_v11_test_owner;
SET LOCAL ROLE latch_v11_test_owner;
\ir ../../../../docs/migrations/V11__idempotencia_respuesta_text.sql

DO $$
BEGIN
    IF (SELECT count(*) FROM solicitud_idempotente WHERE respuesta_json = '{"recibo":42}') <> 1
       OR (SELECT count(*) FROM solicitud_idempotente WHERE respuesta_json IS NULL) <> 1
       OR (SELECT pg_typeof(respuesta_json)::text FROM solicitud_idempotente LIMIT 1) <> 'text'
    THEN
        RAISE EXCEPTION 'V11 debe preservar el JSON y el nulo como TEXT';
    END IF;
END $$;
ROLLBACK;
