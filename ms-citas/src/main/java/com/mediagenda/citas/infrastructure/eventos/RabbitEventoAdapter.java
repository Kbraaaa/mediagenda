package com.mediagenda.citas.infrastructure.eventos;

import com.mediagenda.citas.domain.model.EventoCita;
import com.mediagenda.citas.domain.model.gateway.EventoGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Publica el evento en el exchange topic "mediagenda.eventos" con la routing key del tipo
 * (cita.agendada, cita.cancelada...). Si RabbitMQ no esta disponible, se registra el error y la
 * operacion del usuario NO falla: el aviso es secundario frente a la cita.
 */
public class RabbitEventoAdapter implements EventoGateway {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventoAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final JsonMapper json = JsonMapper.builder().build();

    public RabbitEventoAdapter(RabbitTemplate rabbitTemplate, String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publicar(EventoCita evento) {
        try {
            MessageProperties propiedades = new MessageProperties();
            propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            propiedades.setMessageId(evento.eventoId());
            byte[] cuerpo = json.writeValueAsString(evento).getBytes(StandardCharsets.UTF_8);
            rabbitTemplate.send(exchange, evento.tipo(), new Message(cuerpo, propiedades));
        } catch (RuntimeException e) {
            log.error("No se pudo publicar el evento {} de la cita {}: {}",
                    evento.tipo(), evento.codigoCita(), e.getMessage());
        }
    }
}
