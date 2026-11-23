package com.mediagenda.notificaciones.infrastructure.eventos;

import com.mediagenda.notificaciones.domain.model.EventoRecibido;
import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqsEventoConsumidorTest {

    private static final String COLA = "https://sqs.us-east-1.amazonaws.com/123/notificaciones";

    private static Message mensaje(String id, String cuerpo) {
        return Message.builder().messageId(id).receiptHandle("recibo-" + id).body(cuerpo).build();
    }

    @Test
    void procesaYBorraSoloLosMensajesValidos() {
        SqsClient sqs = mock(SqsClient.class);
        ProcesarEventoUseCase procesar = mock(ProcesarEventoUseCase.class);
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(mensaje("m1", """
                                {"eventoId":"e1","tipo":"formula.emitida","correoPaciente":"laura@correo.com",
                                 "codigoFormula":"FOR-2026-000001","medicamentos":"Acetaminofen x10"}"""),
                        mensaje("m2", "esto no es json"))
                .build());

        int procesados = new SqsEventoConsumidor(sqs, COLA, procesar).sondear();

        assertThat(procesados).isEqualTo(1);
        ArgumentCaptor<EventoRecibido> evento = ArgumentCaptor.forClass(EventoRecibido.class);
        verify(procesar).ejecutar(evento.capture());
        assertThat(evento.getValue().codigoFormula()).isEqualTo("FOR-2026-000001");
        ArgumentCaptor<DeleteMessageRequest> borrado = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqs, times(1)).deleteMessage(borrado.capture());
        assertThat(borrado.getValue().receiptHandle()).isEqualTo("recibo-m1");
        ArgumentCaptor<ReceiveMessageRequest> lectura = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(lectura.capture());
        assertThat(lectura.getValue().waitTimeSeconds()).isEqualTo(20);
    }

    @Test
    void siLaColaNoRespondeNoProcesaNada() {
        SqsClient sqs = mock(SqsClient.class);
        ProcesarEventoUseCase procesar = mock(ProcesarEventoUseCase.class);
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(SdkClientException.create("sin red"));

        assertThat(new SqsEventoConsumidor(sqs, COLA, procesar).sondear()).isZero();
        verify(procesar, never()).ejecutar(any());
    }
}
