-- Telefono del paciente copiado al agendar, para los avisos por SMS (Amazon SNS)
ALTER TABLE citas ADD COLUMN telefono_paciente VARCHAR(20);
