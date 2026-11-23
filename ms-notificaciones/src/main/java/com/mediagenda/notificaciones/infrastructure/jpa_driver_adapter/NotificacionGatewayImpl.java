package com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import com.mediagenda.notificaciones.domain.model.FiltroNotificaciones;
import com.mediagenda.notificaciones.domain.model.Notificacion;
import com.mediagenda.notificaciones.domain.model.Pagina;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.infrastructure.mapper.EntidadesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificacionGatewayImpl implements NotificacionGateway {

    private final NotificacionRepositoryJPA repository;
    private final EntidadesMapper mapper;

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        return mapper.toDomain(repository.save(mapper.toEntity(notificacion)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notificacion> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Notificacion> listar(FiltroNotificaciones filtro, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return Paginas.desde(repository.findAll(Especificaciones.notificaciones(filtro), solicitud), mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEvento(String eventoId) {
        return repository.existsByEventoId(eventoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacion> fallidasParaReintento(int intentosMaximos) {
        return repository.findTop50ByEstadoAndIntentosLessThanOrderByFechaCreacion(
                EstadoNotificacion.FALLIDA, intentosMaximos).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long siguienteConsecutivo() {
        return repository.siguienteConsecutivo();
    }
}
