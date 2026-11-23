package com.mediagenda.farmacia.infrastructure.eventos;

import com.mediagenda.farmacia.Datos;
import com.mediagenda.farmacia.domain.model.EventoFormula;
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
    void publicaJson() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);

        new RabbitEventoAdapter(rabbit, "mediagenda.eventos")
                .publicar(EventoFormula.de(EventoFormula.DISPENSADA, Datos.formula(), "entregada"));

        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("mediagenda.eventos"), eq("formula.dispensada"), mensaje.capture());
        assertThat(new String(mensaje.getValue().getBody(), StandardCharsets.UTF_8))
                .contains("\"codigoFormula\":\"FOR-2026-000001\"").contains("Acetaminofen");
    }

    @Test
    void rabbitCaidoNoFalla() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new AmqpConnectException(new RuntimeException("x"))).when(rabbit)
                .send(anyString(), anyString(), any(Message.class));

        assertThatCode(() -> new RabbitEventoAdapter(rabbit, "x")
                .publicar(EventoFormula.de(EventoFormula.EMITIDA, Datos.formula(), "d"))).doesNotThrowAnyException();
    }
}
