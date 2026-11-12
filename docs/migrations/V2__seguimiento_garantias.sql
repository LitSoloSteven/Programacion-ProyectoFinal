CREATE TABLE IF NOT EXISTS garantia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contrato_id UUID NOT NULL UNIQUE REFERENCES contrato (id) ON DELETE RESTRICT,
    monto_inicial NUMERIC(19, 2) NOT NULL,
    saldo_actual NUMERIC(19, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_garantia_montos CHECK (
        monto_inicial >= 0 AND saldo_actual >= 0 AND saldo_actual <= monto_inicial
    )
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_garantia_montos') THEN
        ALTER TABLE garantia
            ADD CONSTRAINT ck_garantia_montos CHECK (
                monto_inicial >= 0 AND saldo_actual >= 0 AND saldo_actual <= monto_inicial
            );
    END IF;
END $$;

INSERT INTO garantia (contrato_id, monto_inicial, saldo_actual)
SELECT id, deposito_pactado, deposito_pactado
FROM contrato
ON CONFLICT (contrato_id) DO NOTHING;
