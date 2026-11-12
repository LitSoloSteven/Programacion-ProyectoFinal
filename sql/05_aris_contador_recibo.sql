-- Ejecutar despues de 04_aris_recibo.sql y antes de habilitar la emision.
-- Inicializa desde los recibos existentes; no repara huecos historicos.
BEGIN;
LOCK TABLE latch.recibo IN EXCLUSIVE MODE;
CREATE TABLE IF NOT EXISTS latch.contador_recibo (
    id VARCHAR(16) PRIMARY KEY CHECK (id = 'RECIBOS'),
    ultimo_numero BIGINT NOT NULL CHECK (ultimo_numero >= 0)
);
INSERT INTO latch.contador_recibo (id, ultimo_numero)
SELECT 'RECIBOS', COALESCE(MAX(numero), 0) FROM latch.recibo
ON CONFLICT (id) DO NOTHING;
-- Si ya estaba inicializado, no se reduce ni reinicia el contador.
COMMIT;
