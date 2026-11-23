package com.mediagenda.notificaciones.infrastructure.correo;

import com.mediagenda.notificaciones.domain.model.exception.EnvioCorreoException;
import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * En AWS: envia el correo con la API de Amazon SES (SDK v2). No usa usuario ni clave SMTP:
 * los permisos vienen del rol de IAM de la instancia.
 */
public class SesCorreoAdapter implements CorreoGateway {

    private final SesV2Client ses;
    private final String remitente;

    public SesCorreoAdapter(SesV2Client ses, String remitente) {
        this.ses = ses;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        try {
            ses.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(remitente)
                    .destination(Destination.builder().toAddresses(destinatario).build())
                    .content(EmailContent.builder().simple(Message.builder()
                            .subject(texto(asunto))
                            .body(Body.builder().text(texto(cuerpo)).build())
                            .build()).build())
                    .build());
        } catch (SdkException e) {
            throw new EnvioCorreoException("Amazon SES rechazo el envio: " + e.getMessage(), e);
        }
    }

    private static Content texto(String valor) {
        return Content.builder().data(valor).charset("UTF-8").build();
    }
}
