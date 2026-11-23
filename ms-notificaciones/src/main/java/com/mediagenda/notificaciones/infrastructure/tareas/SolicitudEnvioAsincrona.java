package com.mediagenda.notificaciones.infrastructure.tareas;

import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import com.mediagenda.notificaciones.domain.usecase.EnviarPendienteUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/** Envia en un hilo aparte (pool "correoExecutor") para que la API responda 202 sin esperar al SMTP. */
public class SolicitudEnvioAsincrona implements SolicitudEnvioGateway {

    private static final Logger log = LoggerFactory.getLogger(SolicitudEnvioAsincrona.class);

    private final EnviarPendienteUseCase enviarPendiente;

    public SolicitudEnvioAsincrona(EnviarPendienteUseCase enviarPendiente) {
        this.enviarPendiente = enviarPendiente;
    }

    @Override
    @Async("correoExecutor")
    public void solicitar(String codigoNotificacion) {
        try {
            enviarPendiente.ejecutar(codigoNotificacion);
        } catch (RuntimeException e) {
            log.error("Fallo el envio de {}: {}", codigoNotificacion, e.getMessage());
        }
    }
}
