package com.mediagenda.pacientes.domain.model.exception;

/** 409: ya existe un paciente con el mismo documento o el mismo correo. */
public class PacienteDuplicadoException extends RuntimeException {

    public PacienteDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
