package com.mediagenda.notificaciones.infrastructure.sms;

import com.mediagenda.notificaciones.domain.model.exception.EnvioSmsException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsAdaptersTest {

    @Test
    void snsPublicaDirectoAlNumeroComoTransaccional() {
        SnsClient sns = mock(SnsClient.class);

        new SnsSmsAdapter(sns).enviar("+573101234567", "MediAgenda: cita agendada");

        ArgumentCaptor<PublishRequest> envio = ArgumentCaptor.forClass(PublishRequest.class);
        verify(sns).publish(envio.capture());
        assertThat(envio.getValue().phoneNumber()).isEqualTo("+573101234567");
        assertThat(envio.getValue().message()).isEqualTo("MediAgenda: cita agendada");
        assertThat(envio.getValue().messageAttributes().get("AWS.SNS.SMS.SMSType").stringValue())
                .isEqualTo("Transactional");
    }

    @Test
    void siSnsRechazaSeLanzaEnvioSmsException() {
        SnsClient sns = mock(SnsClient.class);
        when(sns.publish(any(PublishRequest.class)))
                .thenThrow(AuthorizationErrorException.builder().message("destino no verificado").build());

        assertThatThrownBy(() -> new SnsSmsAdapter(sns).enviar("+573101234567", "x"))
                .isInstanceOf(EnvioSmsException.class)
                .hasMessageContaining("no verificado");
    }

    @Test
    void simuladoSoloEscribeEnElLogConElNumeroEnmascarado() {
        assertThatCode(() -> new SmsSimuladoAdapter().enviar("+573101234567", "hola")).doesNotThrowAnyException();
        assertThat(SmsSimuladoAdapter.enmascarar("+573101234567")).isEqualTo("*********4567");
        assertThat(SmsSimuladoAdapter.enmascarar("123")).isEqualTo("****");
    }
}
