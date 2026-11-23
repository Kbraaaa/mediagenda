-- Canal del aviso: CORREO o SMS. Un mismo evento puede generar un correo y un SMS,
-- por eso la unicidad pasa de evento_id a (evento_id, canal).
ALTER TABLE notificaciones ADD COLUMN canal VARCHAR(6) NOT NULL DEFAULT 'CORREO'
    CHECK (canal IN ('CORREO', 'SMS'));
ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_evento_id_key;
ALTER TABLE notificaciones ADD CONSTRAINT uq_notificacion_evento_canal UNIQUE (evento_id, canal);
