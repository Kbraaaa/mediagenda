package com.mediagenda.notificaciones.domain.model.gateway;

/** Envia un correo (API de Amazon SES en AWS, SMTP con Mailpit en local). Lanza EnvioCorreoException si falla. */
public interface CorreoGateway {

    void enviar(String destinatario, String asunto, String cuerpo);
}
