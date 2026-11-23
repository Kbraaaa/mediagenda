package com.mediagenda.notificaciones.domain.model.exception;

/** El proveedor de SMS rechazo el mensaje (por ejemplo, numero no verificado en el sandbox de SNS). */
public class EnvioSmsException extends EnvioFallidoException {

    public EnvioSmsException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
