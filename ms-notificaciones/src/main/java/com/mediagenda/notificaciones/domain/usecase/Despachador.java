package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Canal;
import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.EnvioFallidoException;
import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Intenta enviar una notificacion por su canal (correo o SMS) y guarda el resultado
 * (ENVIADA o FALLIDA con el error).
 */
@RequiredArgsConstructor
public class Despachador {

    private final CorreoGateway correo;
    private final SmsGateway sms;
    private final NotificacionGateway notificaciones;
    private final Clock reloj;

    public Notificacion despachar(Notificacion n) {
        n.setIntentos(n.getIntentos() + 1);
        try {
            if (n.getCanal() == Canal.SMS) {
                sms.enviar(n.getDestinatario(), n.getCuerpo());
            } else {
                correo.enviar(n.getDestinatario(), n.getAsunto(), n.getCuerpo());
            }
            n.setEstado(EstadoNotificacion.ENVIADA);
            n.setFechaEnvio(LocalDateTime.now(reloj));
            n.setUltimoError(null);
        } catch (EnvioFallidoException e) {
            n.setEstado(EstadoNotificacion.FALLIDA);
            String error = e.getMessage() == null ? "Error desconocido" : e.getMessage();
            n.setUltimoError(error.length() > 500 ? error.substring(0, 500) : error);
        }
        return notificaciones.guardar(n);
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
