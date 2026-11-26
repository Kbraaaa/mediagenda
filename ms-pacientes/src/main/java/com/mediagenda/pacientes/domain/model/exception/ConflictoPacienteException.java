package com.mediagenda.pacientes.domain.model.exception;

import lombok.Getter;

/** El estado actual del paciente no permite la operacion (HTTP 409). Lleva un codigo de error propio. */
@Getter
public class ConflictoPacienteException extends RuntimeException {

    private final String codigoError;

    public ConflictoPacienteException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }
}
