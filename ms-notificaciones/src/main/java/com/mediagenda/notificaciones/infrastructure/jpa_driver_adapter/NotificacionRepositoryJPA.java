package com.mediagenda.notificaciones.infrastructure.jpa_driver_adapter;

import com.mediagenda.notificaciones.domain.model.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepositoryJPA extends JpaRepository<NotificacionEntity, UUID>,
        JpaSpecificationExecutor<NotificacionEntity> {

    Optional<NotificacionEntity> findByCodigo(String codigo);

    boolean existsByEventoId(String eventoId);

    List<NotificacionEntity> findTop50ByEstadoAndIntentosLessThanOrderByFechaCreacion(EstadoNotificacion estado,
                                                                                       int intentos);

    @Query(value = "SELECT nextval('notificacion_codigo_seq')", nativeQuery = true)
    Long siguienteConsecutivo();
}
