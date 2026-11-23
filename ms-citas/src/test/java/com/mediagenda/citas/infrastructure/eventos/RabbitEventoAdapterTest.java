package com.mediagenda.citas.infrastructure.eventos;

import com.mediagenda.citas.Datos;
import com.mediagenda.citas.domain.model.EventoCita;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitEventoAdapterTest {

    @Test
    void publicaJsonConLaRoutingKeyDelTipo() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        EventoCita evento = EventoCita.de(EventoCita.AGENDADA, Datos.cita(), "Cita agendada");

        new RabbitEventoAdapter(rabbit, "mediagenda.eventos").publicar(evento);

        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("mediagenda.eventos"), eq("cita.agendada"), mensaje.capture());
        String json = new String(mensaje.getValue().getBody(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"codigoCita\":\"CIT-2026-000001\"")
                .contains("\"correoPaciente\":\"laura@correo.com\"")
                .contains("\"telefonoPaciente\":\"3101234567\"")
                .contains("2026-11-21");
        assertThat(mensaje.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(mensaje.getValue().getMessageProperties().getMessageId()).isEqualTo(evento.eventoId());
    }

    @Test
    void siRabbitEstaCaidoNoFallaLaOperacion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new AmqpConnectException(new RuntimeException("sin conexion")))
                .when(rabbit).send(anyString(), anyString(), any(Message.class));

        assertThatCode(() -> new RabbitEventoAdapter(rabbit, "x")
                .publicar(EventoCita.de(EventoCita.CANCELADA, Datos.cita(), "d")))
                .doesNotThrowAnyException();
    }
}
