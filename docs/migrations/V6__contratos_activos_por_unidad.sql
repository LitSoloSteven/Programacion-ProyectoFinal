-- Una unidad solo puede tener una ocupación activa; borradores no reservan la unidad.
CREATE UNIQUE INDEX uq_contrato_activo
    ON contrato (unidad_id)
    WHERE estado IN ('Vigente', 'Por vencer', 'Vencido');
