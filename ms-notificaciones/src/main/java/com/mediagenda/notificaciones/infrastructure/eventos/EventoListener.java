package com.mediagenda.notificaciones.infrastructure.eventos;

import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.nio.charset.StandardCharsets;

/**
 * Local: escucha la cola notificaciones.q (enlazada al exchange mediagenda.eventos con la llave "#").
 * Un mensaje que no se puede leer o que no trae correo se rechaza y va a la cola de mensajes muertos.
 */
public class EventoListener {

    private static final Logger log = LoggerFactory.getLogger(EventoListener.class);

    private final ProcesarEventoUseCase procesarEvento;

    public EventoListener(ProcesarEventoUseCase procesarEvento) {
        this.procesarEvento = procesarEvento;
    }

    @RabbitListener(queues = "${notificaciones.cola}")
    public void recibir(Message mensaje) {
        try {
            procesarEvento.ejecutar(LectorEventos.leer(new String(mensaje.getBody(), StandardCharsets.UTF_8)));
        } catch (RuntimeException e) {
            log.error("Evento descartado a la cola de mensajes muertos: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Evento invalido", e);
        }
    }
}
