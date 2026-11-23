package com.mediagenda.notificaciones.infrastructure.correo;

import com.mediagenda.notificaciones.domain.model.exception.EnvioCorreoException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpCorreoAdapterTest {

    @Test
    void armaYEnviaElCorreo() {
        JavaMailSender sender = mock(JavaMailSender.class);

        new SmtpCorreoAdapter(sender, "no-responder@mediagenda.co").enviar("laura@correo.com", "Asunto", "Cuerpo");

        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(mensaje.capture());
        assertThat(mensaje.getValue().getFrom()).isEqualTo("no-responder@mediagenda.co");
        assertThat(mensaje.getValue().getTo()).containsExactly("laura@correo.com");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Asunto");
        assertThat(mensaje.getValue().getText()).isEqualTo("Cuerpo");
    }

    @Test
    void errorDelServidorDeCorreo() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("conexion rechazada")).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> new SmtpCorreoAdapter(sender, "x@y.co").enviar("a@b.co", "s", "c"))
                .isInstanceOf(EnvioCorreoException.class).hasMessageContaining("conexion rechazada");
    }
}
