package com.mediagenda.citas.infrastructure.eventos;

import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.Datos;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqsEventoAdapterTest {

    private static final EventoCita EVENTO = EventoCita.de(EventoCita.AGENDADA, Datos.cita(), "Cita agendada");
    private static final String COLA = "https://sqs.us-east-1.amazonaws.com/123/mediagenda-eventos";

    @Test
    void enviaElEventoComoJsonConElTipoComoAtributo() {
        SqsClient sqs = mock(SqsClient.class);
        EventoCita evento = EVENTO;

        new SqsEventoAdapter(sqs, COLA).publicar(evento);

        ArgumentCaptor<SendMessageRequest> envio = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqs).sendMessage(envio.capture());
        assertThat(envio.getValue().queueUrl()).isEqualTo(COLA);
        assertThat(envio.getValue().messageBody()).contains("\"eventoId\":\"" + evento.eventoId() + "\"");
        assertThat(envio.getValue().messageAttributes().get("tipo").stringValue()).isEqualTo(evento.tipo());
    }

    @Test
    void siSqsFallaNoFallaLaOperacion() {
        SqsClient sqs = mock(SqsClient.class);
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenThrow(SdkClientException.create("sin red"));

        assertThatCode(() -> new SqsEventoAdapter(sqs, COLA).publicar(EVENTO)).doesNotThrowAnyException();
    }

    @Test
    void precalentarConsultaLaCola() {
        SqsClient sqs = mock(SqsClient.class);

        new SqsEventoAdapter(sqs, COLA).precalentar();

        ArgumentCaptor<GetQueueAttributesRequest> consulta = ArgumentCaptor.forClass(GetQueueAttributesRequest.class);
        verify(sqs).getQueueAttributes(consulta.capture());
        assertThat(consulta.getValue().queueUrl()).isEqualTo(COLA);
    }
}
