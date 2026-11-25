-- D-P0-3: alinea solicitud_idempotente.respuesta_json a TEXT en bases
-- inicializadas previamente con Hibernate update (donde @Lob generó oid).
-- En instalaciones nuevas (V0 + V1..V10), V5 ya creó la columna como TEXT.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'solicitud_idempotente'
          AND column_name = 'respuesta_json'
          AND udt_name = 'oid'
    ) THEN
        ALTER TABLE solicitud_idempotente
            ALTER COLUMN respuesta_json TYPE TEXT
            USING CASE
                WHEN respuesta_json IS NULL THEN NULL
                ELSE convert_from(lo_get(respuesta_json), 'UTF8')
            END;
    END IF;
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE EXCEPTION USING
            ERRCODE = SQLSTATE,
            MESSAGE = 'V11: el rol de migracion no puede convertir respuesta_json.',
            DETAIL = SQLERRM,
            HINT = 'Ejecute V11 con el propietario de la tabla. Si el detalle indica un Large Object, su propietario debe conceder SELECT sobre ese objeto al rol de migracion y volver a ejecutar V11.';
END $$;
