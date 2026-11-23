package com.mediagenda.citas.domain.model.gateway;

import com.mediagenda.citas.domain.model.EventoCita;

/** Publica eventos en RabbitMQ. Si RabbitMQ falla, se registra el error pero la cita no se cae. */
public interface EventoGateway {

    void publicar(EventoCita evento);
}
