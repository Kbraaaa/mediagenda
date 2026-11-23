-- Codigos legibles: MED-0001 y CIT-2026-000001
CREATE SEQUENCE medico_codigo_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cita_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE medicos (
    id_medico     UUID         PRIMARY KEY,
    codigo        VARCHAR(10)  NOT NULL UNIQUE,
    nombre        VARCHAR(100) NOT NULL,
    especialidad  VARCHAR(30)  NOT NULL,
    correo        VARCHAR(120) NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_medico_correo ON medicos (LOWER(correo));

CREATE TABLE franjas (
    id_franja    BIGSERIAL    PRIMARY KEY,
    id_medico    UUID         NOT NULL REFERENCES medicos (id_medico),
    fecha        DATE         NOT NULL,
    hora_inicio  TIME         NOT NULL,
    hora_fin     TIME         NOT NULL,
    estado       VARCHAR(12)  NOT NULL CHECK (estado IN ('DISPONIBLE', 'RESERVADA')),
    CONSTRAINT ck_franja_horas CHECK (hora_fin > hora_inicio),
    CONSTRAINT uq_franja_medico_inicio UNIQUE (id_medico, fecha, hora_inicio)
);
CREATE INDEX ix_franja_fecha_estado ON franjas (fecha, estado);

CREATE TABLE citas (
    id_cita                UUID          PRIMARY KEY,
    codigo                 VARCHAR(16)   NOT NULL UNIQUE,
    codigo_paciente        VARCHAR(20)   NOT NULL,
    nombre_paciente        VARCHAR(170)  NOT NULL,
    correo_paciente        VARCHAR(120)  NOT NULL,
    id_franja              BIGINT        NOT NULL REFERENCES franjas (id_franja),
    sintomas               VARCHAR(1000) NOT NULL,
    especialidad_sugerida  VARCHAR(30),
    prioridad              VARCHAR(5)    CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA')),
    motivo_triage          VARCHAR(500),
    estado_triage          VARCHAR(12)   NOT NULL CHECK (estado_triage IN ('PENDIENTE', 'COMPLETADO', 'FALLIDO')),
    estado                 VARCHAR(10)   NOT NULL CHECK (estado IN ('AGENDADA', 'CANCELADA', 'ATENDIDA')),
    recordatorio_enviado   BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_creacion         TIMESTAMP     NOT NULL
);

-- Segunda barrera contra la doble reserva: solo puede haber UNA cita AGENDADA por franja.
CREATE UNIQUE INDEX uq_cita_agendada_por_franja ON citas (id_franja) WHERE estado = 'AGENDADA';
CREATE INDEX ix_cita_paciente ON citas (codigo_paciente);
CREATE INDEX ix_cita_estado ON citas (estado);
