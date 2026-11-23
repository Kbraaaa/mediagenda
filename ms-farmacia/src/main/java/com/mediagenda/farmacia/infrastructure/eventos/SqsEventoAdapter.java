package com.mediagenda.farmacia.infrastructure.eventos;

import com.mediagenda.farmacia.domain.model.EventoFormula;
import com.mediagenda.farmacia.domain.model.gateway.EventoGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Version en AWS del mismo puerto EventoGateway: envia el evento a la cola Amazon SQS de
 * ms-notificaciones. El dominio no cambia; solo se elige otro adaptador con la propiedad
 * mediagenda.mensajeria=sqs. Igual que con RabbitMQ, si SQS falla la operacion del usuario NO falla.
 */
public class SqsEventoAdapter implements EventoGateway {

    private static final Logger log = LoggerFactory.getLogger(SqsEventoAdapter.class);

    private final SqsClient sqs;
    private final String colaUrl;
    private final JsonMapper json = JsonMapper.builder().build();

    public SqsEventoAdapter(SqsClient sqs, String colaUrl) {
        this.sqs = sqs;
        this.colaUrl = colaUrl;
    }

    @Override
    public void publicar(EventoFormula evento) {
        try {
            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(colaUrl)
                    .messageBody(json.writeValueAsString(evento))
                    .messageAttributes(Map.of("tipo", MessageAttributeValue.builder()
                            .dataType("String").stringValue(evento.tipo()).build()))
                    .build());
        } catch (RuntimeException e) {
            log.error("No se pudo publicar en SQS el evento {} de la formula {}: {}",
                    evento.tipo(), evento.codigoFormula(), e.getMessage());
        }
    }
}
