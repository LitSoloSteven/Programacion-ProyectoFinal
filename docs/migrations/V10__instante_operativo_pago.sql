-- RN-14: conservar la fecha efectiva y el instante operativo por separado.
ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP WITH TIME ZONE;

UPDATE pago
SET creado_en = CURRENT_TIMESTAMP
WHERE creado_en IS NULL;

ALTER TABLE pago
    ALTER COLUMN creado_en SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN creado_en SET NOT NULL;
