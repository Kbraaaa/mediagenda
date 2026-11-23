-- Telefono del paciente copiado al emitir la formula, para los avisos por SMS (Amazon SNS)
ALTER TABLE formulas ADD COLUMN telefono_paciente VARCHAR(20);
