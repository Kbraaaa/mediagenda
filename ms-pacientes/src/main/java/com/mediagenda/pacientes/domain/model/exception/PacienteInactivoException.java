package com.mediagenda.pacientes.domain.model.exception;

/** 409: el paciente existe pero esta desactivado y no se puede modificar. */
public class PacienteInactivoException extends RuntimeException {

    public PacienteInactivoException(String codigo) {
        super("El paciente " + codigo + " esta inactivo y no se puede modificar");
    }
}
