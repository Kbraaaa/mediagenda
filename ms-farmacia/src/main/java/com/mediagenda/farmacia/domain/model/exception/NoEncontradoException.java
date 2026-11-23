package com.mediagenda.farmacia.domain.model.exception;

import lombok.Getter;

/** 404: el recurso pedido no existe. codigoError: CITA_NO_ENCONTRADA, MEDICO_NO_ENCONTRADO, ... */
@Getter
public class NoEncontradoException extends RuntimeException {

    private final String codigoError;

    public NoEncontradoException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }
}
