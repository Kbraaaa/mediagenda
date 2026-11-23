-- Secuencia para el codigo legible PAC-000001, PAC-000002, ...
CREATE SEQUENCE paciente_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pacientes (
    id_paciente       UUID         PRIMARY KEY,
    codigo            VARCHAR(12)  NOT NULL UNIQUE,
    tipo_documento    VARCHAR(2)   NOT NULL CHECK (tipo_documento IN ('CC', 'TI', 'CE', 'PA')),
    numero_documento  VARCHAR(20)  NOT NULL,
    nombres           VARCHAR(80)  NOT NULL,
    apellidos         VARCHAR(80)  NOT NULL,
    correo            VARCHAR(120) NOT NULL,
    telefono          VARCHAR(20),
    fecha_nacimiento  DATE         NOT NULL,
    tipo_sangre       VARCHAR(3)   CHECK (tipo_sangre IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    alergias          VARCHAR(500),
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_paciente_documento UNIQUE (tipo_documento, numero_documento)
);

-- El correo es unico sin importar mayusculas
CREATE UNIQUE INDEX uq_paciente_correo ON pacientes (LOWER(correo));

-- Acelera el listado paginado de pacientes activos
CREATE INDEX ix_paciente_activo_codigo ON pacientes (activo, codigo);
