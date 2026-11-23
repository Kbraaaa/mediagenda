package com.mediagenda.farmacia.domain.model.gateway;

import com.mediagenda.farmacia.domain.model.EventoFormula;

/** Publica eventos en RabbitMQ. Si RabbitMQ falla, se registra el error pero la formula no se cae. */
public interface EventoGateway {

    void publicar(EventoFormula evento);
}
