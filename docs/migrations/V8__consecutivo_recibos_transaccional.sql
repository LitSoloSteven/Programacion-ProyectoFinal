-- RN-07: el contador se actualiza dentro de la misma transacción del pago.
-- Los recibos históricos conservan su número; los nuevos continúan desde MAX(numero) + 1.
ALTER TABLE recibo
    ALTER COLUMN numero DROP IDENTITY IF EXISTS;

ALTER TABLE recibo
    ALTER COLUMN contrato_info TYPE TEXT;

CREATE TABLE IF NOT EXISTS consecutivo_recibo (
    id SMALLINT PRIMARY KEY,
    siguiente_numero BIGINT NOT NULL CHECK (siguiente_numero > 0),
    CONSTRAINT ck_consecutivo_recibo_unico CHECK (id = 1)
);

INSERT INTO consecutivo_recibo (id, siguiente_numero)
SELECT 1, COALESCE(MAX(numero), 0) + 1
FROM recibo
WHERE true
ON CONFLICT (id) DO UPDATE
    SET siguiente_numero = GREATEST(
            consecutivo_recibo.siguiente_numero,
            EXCLUDED.siguiente_numero);

CREATE OR REPLACE FUNCTION public.fn_reservar_numero()
RETURNS BIGINT
LANGUAGE plpgsql
VOLATILE
SET search_path = public, pg_catalog
AS $$
DECLARE
    reservado BIGINT;
BEGIN
    SELECT siguiente_numero
    INTO STRICT reservado
    FROM consecutivo_recibo
    WHERE id = 1
    FOR UPDATE;

    UPDATE consecutivo_recibo
    SET siguiente_numero = reservado + 1
    WHERE id = 1;

    RETURN reservado;
END;
$$;
