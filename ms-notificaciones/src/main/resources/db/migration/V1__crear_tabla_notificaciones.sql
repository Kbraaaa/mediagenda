CREATE SEQUENCE notificacion_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE notificaciones (
    id_notificacion  UUID           PRIMARY KEY,
    codigo           VARCHAR(16)    NOT NULL UNIQUE,
    evento_id        VARCHAR(40)    UNIQUE,
    tipo             VARCHAR(40)    NOT NULL,
    codigo_paciente  VARCHAR(20),
    destinatario     VARCHAR(120)   NOT NULL,
    asunto           VARCHAR(200)   NOT NULL,
    cuerpo           VARCHAR(2000)  NOT NULL,
    estado           VARCHAR(10)    NOT NULL CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'LEIDA')),
    intentos         INTEGER        NOT NULL DEFAULT 0,
    ultimo_error     VARCHAR(500),
    fecha_creacion   TIMESTAMP      NOT NULL,
    fecha_envio      TIMESTAMP
);
CREATE INDEX ix_notificacion_paciente ON notificaciones (codigo_paciente);
CREATE INDEX ix_notificacion_estado ON notificaciones (estado);
