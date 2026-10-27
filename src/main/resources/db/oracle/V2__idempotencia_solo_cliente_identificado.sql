-- Oracle incluye las claves parcialmente nulas en un índice UNIQUE compuesto.
-- Sin cliente_id no hay clave de idempotencia: ambas expresiones deben ser NULL.
ALTER TABLE labor DROP CONSTRAINT uq_labor_cliente;
CREATE UNIQUE INDEX uq_labor_cliente ON labor (
    CASE WHEN cliente_id IS NOT NULL THEN autor_id END,
    CASE WHEN cliente_id IS NOT NULL THEN cliente_id END
);
