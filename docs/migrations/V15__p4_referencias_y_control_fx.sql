CREATE TABLE IF NOT EXISTS referencia_tipo_cambio (
    id UUID PRIMARY KEY,
    proveedor VARCHAR(80) NOT NULL,
    base VARCHAR(3) NOT NULL CHECK (base = 'USD'),
    destino VARCHAR(3) NOT NULL CHECK (destino = 'NIO'),
    valor NUMERIC(19,8) NOT NULL CHECK (valor BETWEEN 1 AND 100),
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    proximo_en TIMESTAMP WITH TIME ZONE NOT NULL,
    consultado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    huella_respuesta VARCHAR(64) NOT NULL CHECK (huella_respuesta ~ '^[0-9a-f]{64}$'),
    CHECK (proximo_en > actualizado_en)
);

CREATE INDEX IF NOT EXISTS ix_referencia_fx_ultima
    ON referencia_tipo_cambio (consultado_en DESC, id DESC);

CREATE TABLE IF NOT EXISTS control_fx (
    id UUID PRIMARY KEY CHECK (id = '00000000-0000-0000-0000-000000000003'),
    concesion_id UUID,
    concesion_hasta TIMESTAMP WITH TIME ZONE,
    proximo_intento TIMESTAMP WITH TIME ZONE NOT NULL,
    mes DATE NOT NULL CHECK (EXTRACT(DAY FROM mes) = 1),
    intentos_mes INTEGER NOT NULL CHECK (intentos_mes BETWEEN 0 AND 100),
    reintento BOOLEAN NOT NULL,
    aviso VARCHAR(40),
    huella_configuracion VARCHAR(64),
    CHECK ((concesion_id IS NULL) = (concesion_hasta IS NULL))
);

INSERT INTO control_fx (id, proximo_intento, mes, intentos_mes, reintento)
VALUES ('00000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP,
        date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'America/Managua')::date,
        0, FALSE)
ON CONFLICT (id) DO NOTHING;
