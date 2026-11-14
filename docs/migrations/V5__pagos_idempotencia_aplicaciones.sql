-- Fase 4: idempotencia de escrituras financieras, aplicación de pagos a
-- cargos y datos de anulación. Ejecutar con el esquema en modo validate.

CREATE TABLE IF NOT EXISTS solicitud_idempotente (
    id UUID PRIMARY KEY,
    clave VARCHAR(36) NOT NULL,
    usuario_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    recurso VARCHAR(60) NOT NULL,
    hash_peticion VARCHAR(64) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    codigo_estado INTEGER,
    respuesta_json TEXT,
    completado_en TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_solicitud_idempotente_clave UNIQUE (clave),
    CONSTRAINT ck_solicitud_idempotente_estado
        CHECK (estado IN ('EN_PROCESO', 'COMPLETADA'))
);

CREATE INDEX IF NOT EXISTS ix_solicitud_idempotente_usuario
    ON solicitud_idempotente (usuario_id, creado_en);

CREATE TABLE IF NOT EXISTS aplicacion_pago (
    id UUID PRIMARY KEY,
    pago_id UUID NOT NULL REFERENCES pago (id) ON DELETE RESTRICT,
    cargo_id UUID NOT NULL REFERENCES cargo (id) ON DELETE RESTRICT,
    monto NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_aplicacion_pago_pago_cargo UNIQUE (pago_id, cargo_id),
    CONSTRAINT ck_aplicacion_pago_monto CHECK (monto > 0)
);

CREATE INDEX IF NOT EXISTS ix_aplicacion_pago_cargo
    ON aplicacion_pago (cargo_id);

ALTER TABLE IF EXISTS pago
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(500);

ALTER TABLE IF EXISTS pago
    ADD COLUMN IF NOT EXISTS anulado_por BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_pago_anulado_por'
    ) THEN
        ALTER TABLE pago
            ADD CONSTRAINT fk_pago_anulado_por
                FOREIGN KEY (anulado_por) REFERENCES usuario (id) ON DELETE RESTRICT;
    END IF;
END $$;

ALTER TABLE IF EXISTS pago
    ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMP WITH TIME ZONE;