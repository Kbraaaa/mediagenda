package com.mediagenda.notificaciones.infrastructure.correo;

import com.mediagenda.notificaciones.domain.model.exception.EnvioCorreoException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesCorreoAdapterTest {

    @Test
    void enviaElCorreoConRemitenteDestinoAsuntoYCuerpo() {
        SesV2Client ses = mock(SesV2Client.class);

        new SesCorreoAdapter(ses, "avisos@mediagenda.co").enviar("laura@correo.com", "Cita agendada", "Hola Laura");

        ArgumentCaptor<SendEmailRequest> correo = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses).sendEmail(correo.capture());
        assertThat(correo.getValue().fromEmailAddress()).isEqualTo("avisos@mediagenda.co");
        assertThat(correo.getValue().destination().toAddresses()).containsExactly("laura@correo.com");
        assertThat(correo.getValue().content().simple().subject().data()).isEqualTo("Cita agendada");
        assertThat(correo.getValue().content().simple().body().text().data()).isEqualTo("Hola Laura");
    }

    @Test
    void siSesRechazaElEnvioSeLanzaEnvioCorreoException() {
        SesV2Client ses = mock(SesV2Client.class);
        when(ses.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(MessageRejectedException.builder().message("Email address is not verified").build());

        assertThatThrownBy(() -> new SesCorreoAdapter(ses, "a@b.co").enviar("x@y.co", "a", "b"))
                .isInstanceOf(EnvioCorreoException.class)
                .hasMessageContaining("not verified");
    }
}
