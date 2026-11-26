package com.mediagenda.pacientes.domain.model.exception;

import lombok.Getter;

/** Los datos son validos pero una regla del negocio impide la operacion (HTTP 422). */
@Getter
public class ReglaNegocioException extends RuntimeException {

    private final String codigoError;

    public ReglaNegocioException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }
}
