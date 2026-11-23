package com.mediagenda.citas.domain.model.exception;

import lombok.Getter;

/** 409: lo enviado es valido pero el estado actual no lo permite (franja ocupada, cita ya cancelada...). */
@Getter
public class ConflictoException extends RuntimeException {

    private final String codigoError;

    public ConflictoException(String codigoError, String mensaje) {
        super(mensaje);
        this.codigoError = codigoError;
    }
}
