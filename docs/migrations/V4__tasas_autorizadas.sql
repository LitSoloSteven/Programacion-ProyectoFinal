-- Una sola tasa autorizada por fecha: los pagos no deben elegir entre dos
-- referencias vigentes para el mismo día. Las tasas informativas no se limitan.
CREATE UNIQUE INDEX IF NOT EXISTS uk_tipo_cambio_autorizada_fecha
    ON tipo_cambio (fecha)
    WHERE autorizado_por IS NOT NULL;

-- Refuerzo de la regla de dominio: la tasa siempre es positiva.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_tipo_cambio_nio_por_usd_positiva'
    ) THEN
        ALTER TABLE tipo_cambio
            ADD CONSTRAINT ck_tipo_cambio_nio_por_usd_positiva
            CHECK (nio_por_usd > 0);
    END IF;
END $$;