package com.mediagenda.notificaciones.infrastructure.eventos;

import com.mediagenda.notificaciones.domain.model.EventoRecibido;
import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventoListenerTest {

    private static Message mensaje(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void leeElEventoDeCitaConSusCampos() {
        ProcesarEventoUseCase procesar = mock(ProcesarEventoUseCase.class);

        new EventoListener(procesar).recibir(mensaje("""
                {"eventoId":"e1","tipo":"cita.agendada","codigoCita":"CIT-2026-000001","codigoPaciente":"PAC-000001",
                 "nombrePaciente":"Laura","correoPaciente":"laura@correo.com","medico":"Andrea","especialidad":"PEDIATRIA",
                 "fecha":"2026-11-21","hora":"08:00:00","detalle":"Cita agendada","campoNuevo":123}"""));

        ArgumentCaptor<EventoRecibido> evento = ArgumentCaptor.forClass(EventoRecibido.class);
        verify(procesar).ejecutar(evento.capture());
        assertThat(evento.getValue().eventoId()).isEqualTo("e1");
        assertThat(evento.getValue().hora()).isEqualTo("08:00:00");
        assertThat(evento.getValue().codigoFormula()).isNull();
    }

    @Test
    void mensajeIlegibleOInvalidoVaALaColaDeMuertos() {
        ProcesarEventoUseCase procesar = mock(ProcesarEventoUseCase.class);
        when(procesar.ejecutar(any())).thenThrow(new IllegalArgumentException("sin correo"));
        EventoListener listener = new EventoListener(procesar);

        assertThatThrownBy(() -> listener.recibir(mensaje("esto no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.recibir(mensaje("{\"eventoId\":\"e2\"}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }
}
