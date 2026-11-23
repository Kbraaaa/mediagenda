package com.mediagenda.notificaciones.domain.model.exception;

/** El servidor de correo rechazo o no recibio el mensaje. La notificacion queda FALLIDA. */
public class EnvioCorreoException extends EnvioFallidoException {

    public EnvioCorreoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
