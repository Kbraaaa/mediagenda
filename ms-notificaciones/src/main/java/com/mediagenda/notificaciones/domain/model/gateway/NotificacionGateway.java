package com.mediagenda.notificaciones.domain.model.gateway;

import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Pagina;

import java.util.List;
import java.util.Optional;

public interface NotificacionGateway {

    Notificacion guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorCodigo(String codigo);

    Pagina<Notificacion> listar(FiltroNotificaciones filtro, int pagina, int tamano);

    boolean existeEvento(String eventoId);

    /** FALLIDA con menos intentos que el maximo: candidatas al reintento automatico. */
    List<Notificacion> fallidasParaReintento(int intentosMaximos);

    long siguienteConsecutivo();
}
