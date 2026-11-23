package com.mediagenda.notificaciones.domain.usecase;

import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Tarea programada: reintenta los avisos FALLIDA que aun no llegan al maximo de intentos. */
@RequiredArgsConstructor
public class ReintentarFallidasUseCase {

    private final NotificacionGateway notificaciones;
    private final Despachador despachador;
    private final int intentosMaximos;

    public int ejecutar() {
        List<Notificacion> fallidas = notificaciones.fallidasParaReintento(intentosMaximos);
        fallidas.forEach(despachador::despachar);
        return fallidas.size();
    }
}
