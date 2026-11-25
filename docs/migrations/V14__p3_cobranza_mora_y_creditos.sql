-- P3 (M05 y M06): mora automática, anulación trazable de cargos,
-- origen/reversión de aplicaciones de pago y créditos automáticos.

ALTER TABLE cargo
    ADD COLUMN IF NOT EXISTS cargo_origen_id UUID REFERENCES cargo (id) ON DELETE RESTRICT;

ALTER TABLE cargo
    ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMP WITH TIME ZONE;

ALTER TABLE cargo
    ADD COLUMN IF NOT EXISTS anulado_por BIGINT REFERENCES usuario (id) ON DELETE RESTRICT;

ALTER TABLE cargo
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(500);

ALTER TABLE cargo
    ADD COLUMN IF NOT EXISTS creado_por BIGINT REFERENCES usuario (id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cargo_renta
    ON cargo (contrato_id, periodo)
    WHERE tipo = 'Renta';

CREATE UNIQUE INDEX IF NOT EXISTS uq_cargo_mora
    ON cargo (cargo_origen_id)
    WHERE tipo = 'Mora';

CREATE INDEX IF NOT EXISTS ix_cargo_cobranza
    ON cargo (contrato_id, vence_el, id)
    WHERE estado NOT IN ('Cancelado', 'Anulado');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_cargo_mora'
    ) THEN
        ALTER TABLE cargo
            ADD CONSTRAINT ck_cargo_mora CHECK (tipo <> 'Mora' OR cargo_origen_id IS NOT NULL);
    END IF;
END $$;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS fecha_aplicacion DATE;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP WITH TIME ZONE;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS creado_por BIGINT REFERENCES usuario (id) ON DELETE RESTRICT;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS revertida_en TIMESTAMP WITH TIME ZONE;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS revertida_por BIGINT REFERENCES usuario (id) ON DELETE RESTRICT;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS motivo_reversion VARCHAR(500);

ALTER TABLE aplicacion_pago
    DROP CONSTRAINT IF EXISTS uk_aplicacion_pago_pago_cargo;

CREATE UNIQUE INDEX IF NOT EXISTS uq_aplicacion_pago_activa
    ON aplicacion_pago (pago_id, cargo_id)
    WHERE pago_id IS NOT NULL AND revertida_en IS NULL;

CREATE INDEX IF NOT EXISTS ix_aplicacion_pago_activas
    ON aplicacion_pago (pago_id)
    WHERE revertida_en IS NULL;

CREATE INDEX IF NOT EXISTS ix_aplicacion_cargo_fecha
    ON aplicacion_pago (cargo_id, fecha_aplicacion);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_aplicacion_reversion'
    ) THEN
        ALTER TABLE aplicacion_pago
            ADD CONSTRAINT ck_aplicacion_reversion CHECK (
                (revertida_en IS NULL AND revertida_por IS NULL AND motivo_reversion IS NULL)
                OR (revertida_en IS NOT NULL AND revertida_por IS NOT NULL AND length(trim(motivo_reversion)) > 0)
            );
    END IF;
END $$;
