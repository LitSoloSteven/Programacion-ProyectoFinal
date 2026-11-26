-- P1 (RF-M01-11, RF-M12-01, RF-M12-02): cambio de contraseña pendiente,
-- emisor en recibos nuevos y parámetros operativos inmutables con historial.
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE recibo
    ADD COLUMN IF NOT EXISTS emisor_info TEXT;

CREATE TABLE IF NOT EXISTS parametro_sistema (
    id UUID PRIMARY KEY,
    clave VARCHAR(80) NOT NULL,
    valor TEXT NOT NULL,
    vigente_desde TIMESTAMP WITH TIME ZONE NOT NULL,
    motivo VARCHAR(300) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_por BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT uk_parametro_sistema_clave_vigencia UNIQUE (clave, vigente_desde)
);

CREATE INDEX IF NOT EXISTS ix_parametro_sistema_clave_vigencia
    ON parametro_sistema (clave, vigente_desde DESC);
