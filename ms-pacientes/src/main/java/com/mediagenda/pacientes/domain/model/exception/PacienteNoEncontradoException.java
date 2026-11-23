package com.mediagenda.pacientes.domain.model.exception;

/** 404: no existe un paciente con ese codigo. */
public class PacienteNoEncontradoException extends RuntimeException {

    public PacienteNoEncontradoException(String codigo) {
        super("No existe un paciente con codigo " + codigo);
    }
}
