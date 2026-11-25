-- P5 (M02, M03, M04, M08, M12): mantenimiento de unidades, gastos de propiedad,
-- metadatos de documentos adjuntos y puerta compartida para exclusión.

CREATE TABLE IF NOT EXISTS solicitud_mantenimiento (
    id UUID PRIMARY KEY,
    unidad_id UUID NOT NULL REFERENCES unidad (id) ON DELETE RESTRICT,
    descripcion VARCHAR(1000) NOT NULL,
    prioridad VARCHAR(24) NOT NULL DEFAULT 'Media',
    estado VARCHAR(24) NOT NULL DEFAULT 'Abierta',
    responsable VARCHAR(120),
    abierta_el DATE NOT NULL,
    cerrada_el DATE,
    motivo_cancelacion VARCHAR(500),
    detalle_resolucion VARCHAR(1000),
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_por BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_solicitud_mantenimiento_prioridad CHECK (prioridad IN ('Baja', 'Media', 'Alta')),
    CONSTRAINT ck_solicitud_mantenimiento_estado CHECK (estado IN ('Abierta', 'En proceso', 'Resuelta', 'Cancelada')),
    CONSTRAINT ck_solicitud_mantenimiento_version CHECK (version >= 0),
    CONSTRAINT ck_solicitud_mantenimiento_cierre CHECK ((estado IN ('Resuelta', 'Cancelada')) = (cerrada_el IS NOT NULL)),
    CONSTRAINT ck_solicitud_mantenimiento_cancelacion CHECK (
        (estado = 'Cancelada') = (motivo_cancelacion IS NOT NULL AND length(trim(motivo_cancelacion)) > 0)
    )
);

CREATE INDEX IF NOT EXISTS ix_mantenimiento_unidad
    ON solicitud_mantenimiento (unidad_id, estado);

CREATE TABLE IF NOT EXISTS gasto_propiedad (
    id UUID PRIMARY KEY,
    propiedad_id UUID NOT NULL REFERENCES propiedad (id) ON DELETE RESTRICT,
    solicitud_id UUID REFERENCES solicitud_mantenimiento (id) ON DELETE RESTRICT,
    fecha_gasto DATE NOT NULL,
    importe NUMERIC(19, 2) NOT NULL,
    moneda VARCHAR(3) NOT NULL,
    concepto VARCHAR(300) NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_por BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_gasto_propiedad_importe CHECK (importe > 0),
    CONSTRAINT ck_gasto_propiedad_moneda CHECK (moneda IN ('NIO', 'USD'))
);

CREATE INDEX IF NOT EXISTS ix_gasto_propiedad
    ON gasto_propiedad (propiedad_id, fecha_gasto);

CREATE INDEX IF NOT EXISTS ix_gasto_solicitud
    ON gasto_propiedad (solicitud_id);

CREATE TABLE IF NOT EXISTS documento_adjunto (
    id UUID PRIMARY KEY,
    propiedad_id UUID REFERENCES propiedad (id) ON DELETE RESTRICT,
    unidad_id UUID REFERENCES unidad (id) ON DELETE RESTRICT,
    inquilino_id UUID REFERENCES inquilino (id) ON DELETE RESTRICT,
    contrato_id UUID REFERENCES contrato (id) ON DELETE RESTRICT,
    parametro_id UUID REFERENCES parametro_sistema (id) ON DELETE RESTRICT,
    nombre_original VARCHAR(180) NOT NULL,
    ruta_interna VARCHAR(80) NOT NULL UNIQUE,
    mime VARCHAR(30) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_por BIGINT NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_documento_adjunto_ruta_interna CHECK (ruta_interna ~ '^[0-9a-f-]{36}\.(pdf|png|jpg)$'),
    CONSTRAINT ck_documento_adjunto_mime CHECK (mime IN ('application/pdf', 'image/png', 'image/jpeg')),
    CONSTRAINT ck_documento_adjunto_tamano_bytes CHECK (tamano_bytes BETWEEN 1 AND 5242880),
    CONSTRAINT ck_documento_adjunto_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_documento_adjunto_version CHECK (version >= 0),
    CONSTRAINT ck_documento_dueno CHECK (
        num_nonnulls(propiedad_id, unidad_id, inquilino_id, contrato_id, parametro_id) = 1
    )
);

CREATE INDEX IF NOT EXISTS ix_adjunto_propiedad
    ON documento_adjunto (propiedad_id)
    WHERE propiedad_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_adjunto_unidad
    ON documento_adjunto (unidad_id)
    WHERE unidad_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_adjunto_inquilino
    ON documento_adjunto (inquilino_id)
    WHERE inquilino_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_adjunto_contrato
    ON documento_adjunto (contrato_id)
    WHERE contrato_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_adjunto_parametro
    ON documento_adjunto (parametro_id)
    WHERE parametro_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS puerta_mantenimiento (
    id UUID PRIMARY KEY,
    descripcion VARCHAR(80) NOT NULL,
    CONSTRAINT ck_puerta_unica CHECK (id = '00000000-0000-0000-0000-000000000002')
);

INSERT INTO puerta_mantenimiento (id, descripcion)
VALUES ('00000000-0000-0000-0000-000000000002', 'Puerta de respaldo y adjuntos')
ON CONFLICT (id) DO NOTHING;
