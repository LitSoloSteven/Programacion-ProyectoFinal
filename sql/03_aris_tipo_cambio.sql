-- Ejecutar sobre la base de Caleb, despues de crear latch.usuario.
BEGIN;
CREATE TABLE IF NOT EXISTS latch.tipo_cambio (
    id UUID PRIMARY KEY,
    fecha DATE NOT NULL,
    nio_por_usd NUMERIC(19,8) NOT NULL CHECK (nio_por_usd > 0),
    fuente VARCHAR(500) NOT NULL CHECK (length(trim(fuente)) > 0),
    registrado_en TIMESTAMPTZ NOT NULL,
    autorizado_por UUID REFERENCES latch.usuario(id),
    motivo VARCHAR(500),
    CONSTRAINT ck_tasa_motivo CHECK (
        autorizado_por IS NULL OR (motivo IS NOT NULL AND length(trim(motivo)) > 0)
    )
);
CREATE INDEX IF NOT EXISTS ix_tipo_cambio_fecha
    ON latch.tipo_cambio(fecha, registrado_en DESC, id DESC);
COMMIT;
