package com.mediagenda.notificaciones.infrastructure.correo;

import com.mediagenda.notificaciones.domain.model.exception.EnvioCorreoException;
import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envia por SMTP. En AWS el servidor es Amazon SES (email-smtp.us-east-2.amazonaws.com);
 * en local es Mailpit, que guarda los correos y los muestra en http://localhost:8025.
 */
public class SmtpCorreoAdapter implements CorreoGateway {

    private final JavaMailSender mailSender;
    private final String remitente;

    public SmtpCorreoAdapter(JavaMailSender mailSender, String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            throw new EnvioCorreoException("El servidor de correo rechazo el envio: " + e.getMessage(), e);
        }
    }
}
