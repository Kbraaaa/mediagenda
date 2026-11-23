package com.mediagenda.notificaciones.domain.model.gateway;

/** Pide el envio en segundo plano, para que la API responda 202 de inmediato. */
public interface SolicitudEnvioGateway {

    void solicitar(String codigoNotificacion);
}
