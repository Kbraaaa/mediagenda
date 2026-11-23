package com.mediagenda.notificaciones.domain.model.exception;

/** El proveedor (correo o SMS) rechazo o no recibio el mensaje. La notificacion queda FALLIDA. */
public abstract class EnvioFallidoException extends RuntimeException {

    protected EnvioFallidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
