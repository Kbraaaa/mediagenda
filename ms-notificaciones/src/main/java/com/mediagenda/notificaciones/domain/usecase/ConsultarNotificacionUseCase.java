package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.exception.NoEncontradoException;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarNotificacionUseCase {

    private final NotificacionGateway notificaciones;

    public Notificacion ejecutar(String codigo) {
        return notificaciones.buscarPorCodigo(codigo).orElseThrow(() ->
                new NoEncontradoException("NOTIFICACION_NO_ENCONTRADA", "No existe la notificacion " + codigo));
    }
}
