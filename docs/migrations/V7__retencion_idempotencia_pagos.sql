-- Mantiene eficiente la limpieza por fecha de finalización.
-- La política conserva la respuesta original durante 30 días.
CREATE INDEX IF NOT EXISTS ix_solicitud_idempotente_completado
    ON solicitud_idempotente (completado_en)
    WHERE estado = 'COMPLETADA';
