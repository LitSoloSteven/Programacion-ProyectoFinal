-- P2 (M04 y M07): renovación enlazada, motivo de cierre, duración mínima
-- de un mes calendario, movimientos de garantía y aplicaciones desde garantía.

ALTER TABLE contrato
    ADD COLUMN IF NOT EXISTS predecesor_id UUID REFERENCES contrato (id) ON DELETE RESTRICT;

ALTER TABLE contrato
    ADD COLUMN IF NOT EXISTS motivo_cierre VARCHAR(500);

CREATE INDEX IF NOT EXISTS ix_contrato_predecesor
    ON contrato (predecesor_id);

CREATE INDEX IF NOT EXISTS ix_contrato_fin_activo
    ON contrato (fin)
    WHERE estado IN ('Vigente', 'Por vencer', 'Vencido');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_contrato_duracion'
    ) THEN
        ALTER TABLE contrato
            ADD CONSTRAINT ck_contrato_duracion CHECK (fin >= (inicio + INTERVAL '1 month')::date);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS movimiento_garantia (
    id UUID PRIMARY KEY,
    garantia_id UUID NOT NULL REFERENCES garantia (id) ON DELETE RESTRICT,
    cargo_id UUID REFERENCES cargo (id) ON DELETE RESTRICT,
    liquidacion_id UUID NOT NULL,
    liquidacion_hash VARCHAR(64) NOT NULL,
    orden SMALLINT NOT NULL,
    tipo VARCHAR(24) NOT NULL,
    importe NUMERIC(19, 2) NOT NULL,
    fecha_movimiento DATE NOT NULL,
    metodo VARCHAR(24),
    concepto VARCHAR(300) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_por BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT uq_movimiento_garantia_liquidacion_orden UNIQUE (liquidacion_id, orden),
    CONSTRAINT ck_movimiento_garantia_tipo CHECK (tipo IN ('Aplicacion', 'Deduccion', 'Devolucion')),
    CONSTRAINT ck_movimiento_garantia_importe CHECK (importe > 0),
    CONSTRAINT ck_movimiento_garantia_metodo CHECK (
        metodo IS NULL OR metodo IN ('Efectivo', 'Transferencia', 'Depósito bancario', 'Otro')
    ),
    CONSTRAINT ck_movimiento_garantia_destino CHECK (
        (tipo = 'Aplicacion' AND cargo_id IS NOT NULL AND metodo IS NULL)
        OR (tipo = 'Deduccion' AND cargo_id IS NULL AND metodo IS NULL)
        OR (tipo = 'Devolucion' AND cargo_id IS NULL AND metodo IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS ix_movimiento_garantia_garantia_fecha
    ON movimiento_garantia (garantia_id, fecha_movimiento, orden);

ALTER TABLE aplicacion_pago
    ALTER COLUMN pago_id DROP NOT NULL;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS movimiento_id UUID REFERENCES movimiento_garantia (id) ON DELETE RESTRICT;

ALTER TABLE aplicacion_pago
    ADD COLUMN IF NOT EXISTS origen VARCHAR(24) NOT NULL DEFAULT 'Inicial';

CREATE UNIQUE INDEX IF NOT EXISTS uq_aplicacion_pago_movimiento
    ON aplicacion_pago (movimiento_id)
    WHERE movimiento_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_aplicacion_pago_fuente'
    ) THEN
        ALTER TABLE aplicacion_pago
            ADD CONSTRAINT ck_aplicacion_pago_fuente CHECK (
                (pago_id IS NOT NULL AND movimiento_id IS NULL AND origen IN ('Inicial', 'Credito'))
                OR (pago_id IS NULL AND movimiento_id IS NOT NULL AND origen = 'Garantia')
            );
    END IF;
END $$;
