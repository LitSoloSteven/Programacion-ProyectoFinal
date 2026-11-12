CREATE TABLE IF NOT EXISTS propietario (
    usuario_id BIGINT PRIMARY KEY REFERENCES usuario (id) ON DELETE RESTRICT,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20)
);

ALTER TABLE IF EXISTS propiedad
    ADD COLUMN IF NOT EXISTS propietario_id BIGINT;

DO $$
BEGIN
    IF to_regclass('public.propiedad') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.propiedad'::regclass
             AND contype = 'f'
             AND pg_get_constraintdef(oid) LIKE 'FOREIGN KEY (propietario_id) REFERENCES propietario(usuario_id)%'
       ) THEN
        ALTER TABLE propiedad
            ADD CONSTRAINT fk_propiedad_propietario
                FOREIGN KEY (propietario_id) REFERENCES propietario (usuario_id) ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.propiedad') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS ix_propiedad_propietario ON propiedad (propietario_id);
    END IF;
END $$;
