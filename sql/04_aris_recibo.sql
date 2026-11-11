-- Requiere las tablas latch.pago, latch.contrato y latch.usuario de la base del equipo.
-- No asigna numeros: el contador transaccional se integra con el Service de pagos.
BEGIN;
CREATE TABLE IF NOT EXISTS latch.recibo (
    id UUID PRIMARY KEY,
    numero BIGINT NOT NULL CONSTRAINT uq_recibo_numero UNIQUE CHECK (numero > 0),
    pago_id UUID NOT NULL CONSTRAINT uq_recibo_pago UNIQUE REFERENCES latch.pago(id),
    contrato_id UUID NOT NULL REFERENCES latch.contrato(id),
    fecha_pago DATE NOT NULL,
    contrato_info VARCHAR(500) NOT NULL CHECK (length(trim(contrato_info)) > 0),
    monto_original NUMERIC(19,2) NOT NULL CHECK (monto_original > 0),
    moneda_original VARCHAR(3) NOT NULL CHECK (moneda_original IN ('USD', 'NIO')),
    monto_contrato NUMERIC(19,2) NOT NULL CHECK (monto_contrato > 0),
    metodo VARCHAR(24) NOT NULL CHECK (length(trim(metodo)) > 0),
    referencia VARCHAR(100),
    emitido_por UUID NOT NULL REFERENCES latch.usuario(id),
    emitido_en TIMESTAMPTZ NOT NULL,
    anulado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(500),
    anulado_por UUID REFERENCES latch.usuario(id),
    anulado_en TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_recibo_anulacion CHECK (
        (NOT anulado AND motivo_anulacion IS NULL AND anulado_por IS NULL AND anulado_en IS NULL)
        OR (anulado AND motivo_anulacion IS NOT NULL AND length(trim(motivo_anulacion)) > 0
            AND anulado_por IS NOT NULL AND anulado_en IS NOT NULL AND anulado_en >= emitido_en)
    )
);
CREATE INDEX IF NOT EXISTS ix_recibo_contrato_fecha ON latch.recibo(contrato_id, fecha_pago);
COMMIT;
