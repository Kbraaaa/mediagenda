package com.mediagenda.notificaciones.domain.model.gateway;

/** Envia un SMS (Amazon SNS en AWS, simulado en local). Lanza EnvioSmsException si falla. */
public interface SmsGateway {

    void enviar(String telefonoE164, String mensaje);
}
