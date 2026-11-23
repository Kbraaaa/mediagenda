-- Codigos legibles: MDT-0001 y FOR-2026-000001
CREATE SEQUENCE medicamento_codigo_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE formula_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE medicamentos (
    id_medicamento  UUID          PRIMARY KEY,
    codigo          VARCHAR(10)   NOT NULL UNIQUE,
    nombre          VARCHAR(120)  NOT NULL,
    presentacion    VARCHAR(80)   NOT NULL,
    stock           INTEGER       NOT NULL CHECK (stock >= 0),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_medicamento_nombre ON medicamentos (LOWER(nombre), LOWER(presentacion));

CREATE TABLE formulas (
    id_formula          UUID          PRIMARY KEY,
    codigo              VARCHAR(16)   NOT NULL UNIQUE,
    codigo_cita         VARCHAR(16)   NOT NULL,
    codigo_paciente     VARCHAR(20)   NOT NULL,
    nombre_paciente     VARCHAR(170)  NOT NULL,
    correo_paciente     VARCHAR(120)  NOT NULL,
    estado              VARCHAR(10)   NOT NULL CHECK (estado IN ('EMITIDA', 'DISPENSADA', 'ANULADA')),
    fecha_emision       TIMESTAMP     NOT NULL,
    fecha_dispensacion  TIMESTAMP
);
-- Una sola formula activa (no anulada) por cita
CREATE UNIQUE INDEX uq_formula_activa_por_cita ON formulas (codigo_cita) WHERE estado <> 'ANULADA';
CREATE INDEX ix_formula_paciente ON formulas (codigo_paciente);

CREATE TABLE items_formula (
    id_item         BIGSERIAL     PRIMARY KEY,
    id_formula      UUID          NOT NULL REFERENCES formulas (id_formula),
    id_medicamento  UUID          NOT NULL REFERENCES medicamentos (id_medicamento),
    cantidad        INTEGER       NOT NULL CHECK (cantidad > 0 AND cantidad <= 100),
    indicaciones    VARCHAR(300),
    CONSTRAINT uq_item_formula_medicamento UNIQUE (id_formula, id_medicamento)
);
